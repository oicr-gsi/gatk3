package ca.on.oicr.pde.workflows;

import ca.on.oicr.pde.commands.CompressAndIndexVcf;
import ca.on.oicr.pde.commands.gatk3.UnifiedGenotyper;
import ca.on.oicr.pde.commands.MergeVcf;
import ca.on.oicr.pde.commands.SortVcf;
import ca.on.oicr.pde.commands.gatk3.AbstractGatkBuilder;
import ca.on.oicr.pde.commands.gatk3.AnalyzeCovariates;
import ca.on.oicr.pde.commands.gatk3.BaseRecalibrator;
import ca.on.oicr.pde.commands.gatk3.HaplotypeCaller;
import ca.on.oicr.pde.commands.gatk3.IndelRealigner;
import ca.on.oicr.pde.commands.gatk3.PrintReads;
import ca.on.oicr.pde.commands.gatk3.RealignerTargetCreator;
import ca.on.oicr.pde.utilities.workflows.OicrWorkflow;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import java.util.*;
import java.util.Map.Entry;
import net.sourceforge.seqware.pipeline.workflowV2.model.Job;
import net.sourceforge.seqware.pipeline.workflowV2.model.SqwFile;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

public class GATK3Workflow extends OicrWorkflow {

    private String binDir;
    private String tmpDir;
    private String dataDir;

    private boolean manualOutput;
    private String queue;

    private String perl;
    private String java;
    private String tabixDir;
    private String gatk;
    private String rDir;
    private String mergeVCFScript;

    private String gatkKey;

    private String identifier = null;

    private final List<String> inputBamFiles = new LinkedList<>();

    private String refFasta = null;
    private String dbsnpVcf = null;

    private final List<String> chrSizes = new LinkedList<>();
    private final List<String> intervalFiles = new LinkedList<>();

    private String standCallConf = null;
    private String standEmitConf = null;

    private String preserveQscoresLessThan = null;

    private final Set<VariantCaller> variantCallers = new HashSet<>();

    private Integer gatkRealignTargetCreatorMem;
    private Integer gatkIndelRealignerMem;
    private Integer gatkPrintReadsMem;
    private Integer gatkHaplotypeCallerMem;
    private Integer gatkHaplotypeCallerThreads;
    private Integer gatkUnifiedGenotyperMem;
    private Integer gatkUnifiedGenotyperThreads;
    private Integer gatkBaseRecalibratorXmx;
    private Integer gatkBaseRecalibratorMem;
    private Integer gatkBaseRecalibratorNct;
    private Integer gatkBaseRecalibratorSmp;
    private Integer gatkOverhead;

    public enum VariantCaller {

        HAPLOTYPE_CALLER, UNIFIED_GENOTYPER;
    }

    public GATK3Workflow() {
        super();
    }

    @Override
    public void setupDirectory() {
        init();
        this.addDirectory(dataDir);
        for (VariantCaller vc : variantCallers) {
            this.addDirectory(dataDir + vc.toString());
        }
    }

    public void init() {

        binDir = this.getWorkflowBaseDir() + "/bin/";
        tmpDir = "tmp/";
        dataDir = "data/";

        manualOutput = BooleanUtils.toBoolean(getProperty("manual_output"), "true", "false");
        queue = getOptionalProperty("queue", "");

        perl = getProperty("perl");
        java = getProperty("java");
        tabixDir = getProperty("tabix_dir");
        gatk = getProperty("gatk_jar");
        rDir = getProperty("r_dir");
        mergeVCFScript = binDir + "sw_module_merge_GATK_VCF.pl";

        gatkKey = getProperty("gatk_key");

        identifier = getProperty("identifier");
        refFasta = getProperty("ref_fasta");
        dbsnpVcf = getProperty("gatk_dbsnp_vcf");

        chrSizes.addAll(Arrays.asList(StringUtils.split(getProperty("chr_sizes"), ",")));
        intervalFiles.addAll(Arrays.asList(StringUtils.split(getOptionalProperty("interval_files", ""), ",")));

        standCallConf = getProperty("stand_call_conf");
        standEmitConf = getProperty("stand_emit_conf");
        preserveQscoresLessThan = getProperty("preserve_qscores_less_than");

        gatkRealignTargetCreatorMem = Integer.parseInt(getProperty("gatk_realign_target_creator_mem"));
        gatkIndelRealignerMem = Integer.parseInt(getProperty("gatk_indel_realigner_mem"));
        gatkPrintReadsMem = Integer.parseInt(getProperty("gatk_print_reads_mem"));
        gatkHaplotypeCallerThreads = Integer.parseInt(getProperty("gatk_haplotype_caller_threads"));
        gatkHaplotypeCallerMem = Integer.parseInt(getProperty("gatk_haplotype_caller_mem"));
        gatkUnifiedGenotyperMem = Integer.parseInt(getProperty("gatk_unified_genotyper_mem"));
        gatkUnifiedGenotyperThreads = Integer.parseInt(getProperty("gatk_unified_genotyper_threads"));
        gatkBaseRecalibratorXmx = Integer.parseInt(getProperty("gatk_baserecalibrator_xmx"));
        gatkBaseRecalibratorMem = Integer.parseInt(getProperty("gatk_baserecalibrator_mem"));
        gatkBaseRecalibratorNct = Integer.parseInt(getProperty("gatk_baserecalibrator_nct"));
        gatkBaseRecalibratorSmp = Integer.parseInt(getProperty("gatk_baserecalibrator_smp"));
        gatkOverhead = Integer.parseInt(getProperty("gatk_sched_overhead_mem"));

        for (String s : StringUtils.split(getProperty("variant_caller"), ",")) {
            variantCallers.add(VariantCaller.valueOf(StringUtils.upperCase(s)));
        }

    }

    @Override
    public Map<String, SqwFile> setupFiles() {

        List<String> inputFilesList = Arrays.asList(StringUtils.split(getProperty("input_files"), ","));
        Set<String> inputFilesSet = new HashSet<>(inputFilesList);

        if (inputFilesList.size() != inputFilesSet.size()) {
            throw new RuntimeException("Duplicate files detected in input_files");
        }

        if ((inputFilesSet.size() % 2) != 0) {
            throw new RuntimeException("Each bam should have a corresponding index");
        }

        Map<String, String> bams = new HashMap<>();
        Map<String, String> bais = new HashMap<>();
        for (String f : inputFilesSet) {
            String fileExtension = FilenameUtils.getExtension(f);
            String fileKey = FilenameUtils.removeExtension(f);
            if (null != fileExtension) {
                switch (fileExtension) {
                    case "bam":
                        bams.put(fileKey, f);
                        break;
                    case "bai":
                        bais.put(fileKey, f);
                        break;
                    default:
                        throw new RuntimeException("Unsupported input file type");
                }
            }
        }

        int id = 0;
        for (Entry<String, String> e : bams.entrySet()) {
            String key = e.getKey();
            String bamFilePath = e.getValue();

            String baiFilePath = bais.get(key);
            if (baiFilePath == null) {
                throw new RuntimeException("Missing index for " + FilenameUtils.getName(bamFilePath));
            }

            SqwFile bam = this.createFile("file_in_" + id++);
            bam.setSourcePath(bamFilePath);
            bam.setType("application/bam");
            bam.setIsInput(true);

            SqwFile bai = this.createFile("file_in_" + id++);
            bai.setSourcePath(baiFilePath);
            bai.setType("application/bam-index");
            bai.setIsInput(true);

            //FIXME: this seems to work for now, it would be better to be able to set the provisionedPath as
            //bai.getProvisionedPath != bai.getOutputPath ...
            //at least with seqware 1.1.0, setting output path changes where the output file will be stored,
            //but the commonly used get provisioned path will return the incorrect path to the file
            bai.setOutputPath(FilenameUtils.getPath(bam.getProvisionedPath()));

            inputBamFiles.add(bam.getProvisionedPath());
        }

        return this.getFiles();
    }

    @Override
    public void buildWorkflow() {
        Map<String, String> filesSplitByIntervals = new HashMap<>();
        List<Job> filesSplitsByIntervalJobs = new LinkedList<>();

        // one chrSize record is required, null will result in no parallelization
        if (chrSizes.isEmpty()) {
            chrSizes.add(null);
        }

        for (String chrSize : chrSizes) {

            //GATK Realigner Target Creator (https://www.broadinstitute.org/gatk/gatkdocs/org_broadinstitute_gatk_tools_walkers_indels_RealignerTargetCreator.php)
            RealignerTargetCreator.Builder realignerTargetCreatorBuilder = new RealignerTargetCreator.Builder(java, gatkRealignTargetCreatorMem + "g", tmpDir, gatk, gatkKey, dataDir)
                    .setReferenceSequence(refFasta)
                    .addInputBamFiles(inputBamFiles)
                    .setKnownIndels(dbsnpVcf);
            if (chrSize != null) {
                realignerTargetCreatorBuilder.addInterval(chrSize);
            }
            if (!intervalFiles.isEmpty()) {
                realignerTargetCreatorBuilder.addIntervalFiles(intervalFiles);
            }
            if (chrSize != null && !intervalFiles.isEmpty()) {
                realignerTargetCreatorBuilder.setIntervalSetRule(AbstractGatkBuilder.SetRule.INTERSECTION);
            }
            RealignerTargetCreator realignerTargetCreatorCommand = realignerTargetCreatorBuilder.build();
            Job realignerTargetCreatorJob = getWorkflow().createBashJob("GATKRealignerTargetCreator")
                    .setMaxMemory(Integer.toString((gatkRealignTargetCreatorMem + gatkOverhead) * 1024))
                    .setQueue(queue);
            realignerTargetCreatorJob.getCommand().setArguments(realignerTargetCreatorCommand.getCommand());

            //GATK Indel Realigner (https://www.broadinstitute.org/gatk/gatkdocs/org_broadinstitute_gatk_tools_walkers_indels_IndelRealigner.php)
            IndelRealigner.Builder indelRealignerBuilder = new IndelRealigner.Builder(java, gatkIndelRealignerMem + "g", tmpDir, gatk, gatkKey, dataDir)
                    .setReferenceSequence(refFasta)
                    .addInputBamFiles(inputBamFiles)
                    .setTargetIntervalFile(realignerTargetCreatorCommand.getOutputFile());
            if (chrSize != null) {
                indelRealignerBuilder.addInterval(chrSize);
            }
            IndelRealigner indelRealignerCommand = indelRealignerBuilder.build();
            Job indelRealignerJob = getWorkflow().createBashJob("GATKIndelRealigner")
                    .setMaxMemory(Integer.toString((gatkIndelRealignerMem + gatkOverhead) * 1024))
                    .setQueue(queue)
                    .addParent(realignerTargetCreatorJob);
            indelRealignerJob.getCommand().setArguments(indelRealignerCommand.getCommand());

            if (filesSplitByIntervals.put(chrSize, indelRealignerCommand.getOutputFile()) != null) {
                throw new RuntimeException("Unexpected state: Duplicate interval key.");
            }
            filesSplitsByIntervalJobs.add(indelRealignerJob);
        }

        //GATK Base Recalibrator (https://www.broadinstitute.org/gatk/gatkdocs/org_broadinstitute_gatk_tools_walkers_bqsr_BaseRecalibrator.php)
        BaseRecalibrator.Builder baseRecalibratorBuilder = new BaseRecalibrator.Builder(java, gatkBaseRecalibratorXmx + "m", tmpDir, gatk, gatkKey, dataDir)
                .setReferenceSequence(refFasta)
                .addCovariate("ReadGroupCovariate")
                .addCovariate("QualityScoreCovariate")
                .addCovariate("CycleCovariate")
                .addCovariate("ContextCovariate")
                .addKnownSite(dbsnpVcf)
                .addInputFiles(filesSplitByIntervals.values())
                .setNumCpuThreadsPerDataThread(gatkBaseRecalibratorNct);
        if (!intervalFiles.isEmpty()) {
            //"This excludes off-target sequences and sequences that may be poorly mapped, which have a higher error rate. 
            // Including them could lead to a skewed model and bad recalibration."
            // https://www.broadinstitute.org/gatk/guide/article?id=4133
            baseRecalibratorBuilder.addIntervalFiles(intervalFiles);
        }
        BaseRecalibrator baseRecalibratorCommand = baseRecalibratorBuilder.build();
        Job baseRecalibratorJob = getWorkflow().createBashJob("GATKBaseRecalibrator")
                .setMaxMemory(gatkBaseRecalibratorMem.toString())
                .setThreads(gatkBaseRecalibratorSmp)
                .setQueue(queue);
        baseRecalibratorJob.getParents().addAll(filesSplitsByIntervalJobs);
        baseRecalibratorJob.getCommand().setArguments(baseRecalibratorCommand.getCommand());

        //GATK Analyze Covariates (https://www.broadinstitute.org/gatk/guide/tooldocs/org_broadinstitute_gatk_tools_walkers_bqsr_AnalyzeCovariates.php)
        AnalyzeCovariates analyzeCovariatesCommand = new AnalyzeCovariates.Builder(java, "4g", tmpDir, gatk, gatkKey, rDir, dataDir)
                .setReferenceSequence(refFasta)
                .setRecalibrationTable(baseRecalibratorCommand.getOutputFile())
                .build();
        Job analyzeCovariatesJob = getWorkflow().createBashJob("GATKAnalyzeCovariates")
                .setMaxMemory(Integer.toString((4 + gatkOverhead) * 1024))
                .setQueue(queue)
                .addParent(baseRecalibratorJob);
        analyzeCovariatesJob.getCommand().setArguments(analyzeCovariatesCommand.getCommand());
        analyzeCovariatesJob.addFile(createOutputFile(analyzeCovariatesCommand.getPlotsReportFile(), "application/pdf", manualOutput));

        Multimap<VariantCaller, Pair<String, Job>> snvFiles = HashMultimap.create();
        Multimap<VariantCaller, Pair<String, Job>> indelFiles = HashMultimap.create();
        Multimap<VariantCaller, Pair<String, Job>> finalFiles = HashMultimap.create();

        for (Entry<String, String> e : filesSplitByIntervals.entrySet()) {

            String chrSize = e.getKey();
            String inputBam = e.getValue();

            //GATK Print Reads (https://www.broadinstitute.org/gatk/gatkdocs/org_broadinstitute_gatk_tools_walkers_readutils_PrintReads.php)
            PrintReads printReadsCommand = new PrintReads.Builder(java, gatkPrintReadsMem + "g", tmpDir, gatk, gatkKey, dataDir)
                    .setReferenceSequence(refFasta)
                    .setCovariatesTablesFile(baseRecalibratorCommand.getOutputFile())
                    .setPreserveQscoresLessThan(preserveQscoresLessThan)
                    .setInputFile(inputBam)
                    .build();
            Job printReadsJob = getWorkflow().createBashJob("GATKTableRecalibration")
                    .setMaxMemory(Integer.toString((gatkPrintReadsMem + gatkOverhead) * 1024))
                    .setQueue(queue)
                    .addParent(baseRecalibratorJob);
            printReadsJob.getCommand().setArguments(printReadsCommand.getCommand());

            for (VariantCaller vc : variantCallers) {
                String workingDir = dataDir + vc.toString() + "/";
                switch (vc) {
                    case HAPLOTYPE_CALLER:
                        //GATK Haplotype Caller (https://www.broadinstitute.org/gatk/gatkdocs/org_broadinstitute_gatk_tools_walkers_haplotypecaller_HaplotypeCaller.php)
                        HaplotypeCaller.Builder haplotypeCallerBuilder = new HaplotypeCaller.Builder(java, Integer.toString(gatkHaplotypeCallerMem) + "g", tmpDir, gatk, gatkKey, workingDir)
                                .setInputBamFile(printReadsCommand.getOutputFile())
                                .setReferenceSequence(refFasta)
                                .setDbsnpFilePath(dbsnpVcf)
                                .setStandardCallConfidence(standCallConf)
                                .setStandardEmitConfidence(standEmitConf);
                        if (chrSize != null) {
                            haplotypeCallerBuilder.addInterval(chrSize);
                        }
                        if (!intervalFiles.isEmpty()) {
                            haplotypeCallerBuilder.addIntervalFiles(intervalFiles);
                        }
                        if (chrSize != null && !intervalFiles.isEmpty()) {
                            haplotypeCallerBuilder.setIntervalSetRule(AbstractGatkBuilder.SetRule.INTERSECTION);
                        }
                        HaplotypeCaller haplotypeCallerCommand = haplotypeCallerBuilder.build();
                        Job haplotypeCallerJob = this.getWorkflow().createBashJob("GATKHaplotypeCaller")
                                .setMaxMemory(Integer.toString((gatkHaplotypeCallerMem + gatkOverhead) * 1024))
                                .setQueue(queue)
                                .addParent(printReadsJob);
                        haplotypeCallerJob.getCommand().setArguments(haplotypeCallerCommand.getCommand());

                        finalFiles.put(vc, Pair.of(haplotypeCallerCommand.getOutputFile(), haplotypeCallerJob));
                        break;

                    case UNIFIED_GENOTYPER:
                        //GATK Unified Genotyper (INDELS) (https://www.broadinstitute.org/gatk/gatkdocs/org_broadinstitute_gatk_tools_walkers_genotyper_UnifiedGenotyper.php)
                        UnifiedGenotyper.Builder indelsUnifiedGenotyperBuilder = new UnifiedGenotyper.Builder(java, Integer.toString(gatkUnifiedGenotyperMem) + "g", tmpDir, gatk, gatkKey, workingDir)
                                .setInputBamFile(printReadsCommand.getOutputFile())
                                .setReferenceSequence(refFasta)
                                .setDbsnpFilePath(dbsnpVcf)
                                .setStandardCallConfidence(standCallConf)
                                .setStandardEmitConfidence(standEmitConf)
                                .setGenotypeLikelihoodsModel("INDEL")
                                .setGroup("Standard");
                        if (chrSize != null) {
                            indelsUnifiedGenotyperBuilder.addInterval(chrSize);
                        }
                        if (!intervalFiles.isEmpty()) {
                            indelsUnifiedGenotyperBuilder.addIntervalFiles(intervalFiles);
                        }
                        if (chrSize != null && !intervalFiles.isEmpty()) {
                            indelsUnifiedGenotyperBuilder.setIntervalSetRule(AbstractGatkBuilder.SetRule.INTERSECTION);
                        }
                        UnifiedGenotyper indelsUnifiedGenotyperCommand = indelsUnifiedGenotyperBuilder.build();
                        Job indelsUnifiedGenotyperJob = this.getWorkflow().createBashJob("GATKUnifiedGenotyperIndel")
                                .setMaxMemory(Integer.toString((gatkUnifiedGenotyperMem + gatkOverhead) * 1024))
                                .setQueue(queue)
                                .addParent(printReadsJob);
                        indelsUnifiedGenotyperJob.getCommand().setArguments(indelsUnifiedGenotyperCommand.getCommand());

                        indelFiles.put(vc, Pair.of(indelsUnifiedGenotyperCommand.getOutputFile(), indelsUnifiedGenotyperJob));

                        //GATK Unified Genotyper (SNVS) (https://www.broadinstitute.org/gatk/gatkdocs/org_broadinstitute_gatk_tools_walkers_genotyper_UnifiedGenotyper.php)
                        UnifiedGenotyper.Builder snvsUnifiedGenotyperBuilder = new UnifiedGenotyper.Builder(java, Integer.toString(gatkUnifiedGenotyperMem) + "g", tmpDir, gatk, gatkKey, workingDir)
                                .setInputBamFile(printReadsCommand.getOutputFile())
                                .setReferenceSequence(refFasta)
                                .setDbsnpFilePath(dbsnpVcf)
                                .setStandardCallConfidence(standCallConf)
                                .setStandardEmitConfidence(standEmitConf)
                                .setGenotypeLikelihoodsModel("SNP");
                        if (chrSize != null) {
                            snvsUnifiedGenotyperBuilder.addInterval(chrSize);
                        }
                        if (!intervalFiles.isEmpty()) {
                            snvsUnifiedGenotyperBuilder.addIntervalFiles(intervalFiles);
                        }
                        if (chrSize != null && !intervalFiles.isEmpty()) {
                            snvsUnifiedGenotyperBuilder.setIntervalSetRule(AbstractGatkBuilder.SetRule.INTERSECTION);
                        }
                        UnifiedGenotyper snvsUnifiedGenotyperCommand = snvsUnifiedGenotyperBuilder.build();
                        Job snvsUnifiedGenotyperJob = this.getWorkflow().createBashJob("GATKUnifiedGenotyperSNV")
                                .setMaxMemory(Integer.toString((gatkUnifiedGenotyperMem + gatkOverhead) * 1024))
                                .setQueue(queue)
                                .addParent(printReadsJob);
                        snvsUnifiedGenotyperJob.getCommand().setArguments(snvsUnifiedGenotyperCommand.getCommand());

                        snvFiles.put(vc, Pair.of(snvsUnifiedGenotyperCommand.getOutputFile(), snvsUnifiedGenotyperJob));
                        break;

                    default:
                        throw new RuntimeException("Unsupported mode: " + variantCallers.toString());
                }
            }
        }

        for (VariantCaller vc : variantCallers) {
            Collection<Pair<String, Job>> snvs = snvFiles.get(vc);
            Collection<Pair<String, Job>> indels = indelFiles.get(vc);
            Collection<Pair<String, Job>> all = finalFiles.get(vc);
            String workingDir = dataDir + vc.toString() + "/";

            if (!snvs.isEmpty()) {
                MergeVcf mergeSnvsCommand = new MergeVcf.Builder(perl, mergeVCFScript, workingDir)
                        .addInputFiles(getLeftCollection(snvs))
                        //.setOutputFileName("gatk.snps.filtered.merged.vcf")
                        .build();
                Job mergeSnvsJob = this.getWorkflow().createBashJob("MergeRawSNVs")
                        .setMaxMemory("4096")
                        .setQueue(queue);
                mergeSnvsJob.getParents().addAll(getRightCollection(snvs));
                mergeSnvsJob.getCommand().setArguments(mergeSnvsCommand.getCommand());

                snvs.clear();
                snvs.add(Pair.of(mergeSnvsCommand.getOutputFile(), mergeSnvsJob));
            }

            if (!indels.isEmpty()) {
                MergeVcf mergeIndelsCommand = new MergeVcf.Builder(perl, mergeVCFScript, workingDir)
                        .addInputFiles(getLeftCollection(indels))
                        //.setOutputFileName("gatk.indels.filtered.merged.vcf")
                        .build();
                Job mergeIndelsJob = this.getWorkflow().createBashJob("MergeRawIndels")
                        .setMaxMemory("4096")
                        .setQueue(queue);
                mergeIndelsJob.getParents().addAll(getRightCollection(indels));
                mergeIndelsJob.getCommand().setArguments(mergeIndelsCommand.getCommand());

                indels.clear();
                indels.add(Pair.of(mergeIndelsCommand.getOutputFile(), mergeIndelsJob));
            }

            if (!snvs.isEmpty() && !indels.isEmpty() && all.isEmpty()) {
                MergeVcf mergeFinalCommand = new MergeVcf.Builder(perl, mergeVCFScript, workingDir)
                        .addInputFiles(getLeftCollection(snvs))
                        .addInputFiles(getLeftCollection(indels))
                        .build();
                Job mergeFinalJob = this.getWorkflow().createBashJob("MergeFinal")
                        .setMaxMemory("4096")
                        .setQueue(queue);
                mergeFinalJob.getParents().addAll(getRightCollection(snvs));
                mergeFinalJob.getParents().addAll(getRightCollection(indels));
                mergeFinalJob.getCommand().setArguments(mergeFinalCommand.getCommand());

                all.add(Pair.of(mergeFinalCommand.getOutputFile(), mergeFinalJob));
            } else if (snvs.isEmpty() && indels.isEmpty() && !all.isEmpty()) {
                if (all.size() > 1) {
                    MergeVcf mergeFinalCommand = new MergeVcf.Builder(perl, mergeVCFScript, workingDir)
                            .addInputFiles(getLeftCollection(all))
                            .build();
                    Job mergeFinalJob = this.getWorkflow().createBashJob("MergeFinal")
                            .setMaxMemory("4096")
                            .setQueue(queue);
                    mergeFinalJob.getParents().addAll(getRightCollection(all));
                    mergeFinalJob.getCommand().setArguments(mergeFinalCommand.getCommand());

                    all.clear();
                    all.add(Pair.of(mergeFinalCommand.getOutputFile(), mergeFinalJob));
                } else {
                    //there is one vcf, no need to merge
                }
            } else {
                throw new RuntimeException(String.format("Unexpected state: snvs file = [%s], indels size = [%s], final size = [%s]",
                        snvs.size(), indels.size(), all.size()));
            }

            //Sort and compress the final vcf
            SortVcf sortVcfCommand = new SortVcf.Builder(workingDir)
                    .setInputFile(Iterables.getOnlyElement(getLeftCollection(all)))
                    .setOutputFileName(identifier + "." + StringUtils.lowerCase(vc.toString()))
                    .build();
            CompressAndIndexVcf compressIndexVcfCommand = new CompressAndIndexVcf.Builder(tabixDir, workingDir)
                    .setInputFile(sortVcfCommand.getOutputFile())
                    .build();
            List<String> cmd = new LinkedList<>();
            cmd.addAll(sortVcfCommand.getCommand());
            cmd.add("&&");
            cmd.addAll(compressIndexVcfCommand.getCommand());
            Job sortCompressIndexVcfJob = getWorkflow().createBashJob("SortCompressIndexVcf")
                    .setMaxMemory(Integer.toString(4096))
                    .setQueue(queue)
                    .addParent(Iterables.getOnlyElement(getRightCollection(all)));
            sortCompressIndexVcfJob.getCommand().setArguments(cmd);

            //final output file
            SqwFile vcf = createOutputFile(compressIndexVcfCommand.getOutputVcfFile(), "application/vcf-4-gzip", manualOutput);
            SqwFile tbi = createOutputFile(compressIndexVcfCommand.getOutputTabixFile(), "application/tbi", manualOutput);
            vcf.getAnnotations().put("variant_caller", vc.toString());
            tbi.getAnnotations().put("variant_caller", vc.toString());
            sortCompressIndexVcfJob.addFile(vcf);
            sortCompressIndexVcfJob.addFile(tbi);
        }
    }

    private <T, S> Collection<T> getLeftCollection(Collection<Pair<T, S>> pairs) {
        List<T> ts = new LinkedList<>();
        for (Pair<T, S> p : pairs) {
            ts.add(p.getLeft());
        }
        return ts;
    }

    private <S, T> Collection<T> getRightCollection(Collection<Pair<S, T>> pairs) {
        List<T> ts = new LinkedList<>();
        for (Pair<S, T> p : pairs) {
            ts.add(p.getRight());
        }
        return ts;
    }
}
