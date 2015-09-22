/**
 * Copyright (C) 2015 Ontario Institute of Cancer Research
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * Contact us:
 *
 * Ontario Institute for Cancer Research
 * MaRS Centre, West Tower
 * 661 University Avenue, Suite 510
 * Toronto, Ontario, Canada M5G 0A3
 * Phone: 416-977-7599
 * Toll-free: 1-866-678-6427
 * www.oicr.on.ca
 *
 */
package ca.on.oicr.pde.workflows;

import ca.on.oicr.pde.tools.BEDFileUtils;
import ca.on.oicr.pde.tools.CompressAndIndexVcf;
import ca.on.oicr.pde.tools.gatk3.UnifiedGenotyper;
import ca.on.oicr.pde.tools.MergeVcf;
import ca.on.oicr.pde.tools.SortVcf;
import ca.on.oicr.pde.tools.gatk3.AnalyzeCovariates;
import ca.on.oicr.pde.tools.gatk3.BaseRecalibrator;
import ca.on.oicr.pde.tools.gatk3.HaplotypeCaller;
import ca.on.oicr.pde.tools.gatk3.IndelRealigner;
import ca.on.oicr.pde.tools.gatk3.PrintReads;
import ca.on.oicr.pde.tools.gatk3.RealignerTargetCreator;
import ca.on.oicr.pde.utilities.workflows.OicrWorkflow;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;
import net.sourceforge.seqware.pipeline.workflowV2.model.Job;
import net.sourceforge.seqware.pipeline.workflowV2.model.SqwFile;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

public class GATK3Workflow extends OicrWorkflow {

    private final String tmpDir = "tmp/";
    private final String dataDir = "data/";
    private final Set<VariantCaller> variantCallers = new HashSet<>();
    private final List<String> inputBamFiles = new LinkedList<>();

    public enum VariantCaller {

        HAPLOTYPE_CALLER, UNIFIED_GENOTYPER;
    }

    public GATK3Workflow() {
        super();
    }

    @Override
    public void setupDirectory() {
        init();
        this.addDirectory(tmpDir);
        this.addDirectory(dataDir);
        for (VariantCaller vc : variantCallers) {
            this.addDirectory(dataDir + vc.toString());
        }
    }

    public void init() {
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

        final String binDir = this.getWorkflowBaseDir() + "/bin/";
        final Boolean manualOutput = BooleanUtils.toBoolean(getProperty("manual_output"), "true", "false");
        final String queue = getOptionalProperty("queue", "");
        final String perl = getProperty("perl");
        final String java = getProperty("java");
        final String tabixDir = getProperty("tabix_dir");
        final String gatk = getOptionalProperty("gatk_jar", binDir);
        final String rDir = getProperty("r_dir");
        final String mergeVCFScript = binDir + "sw_module_merge_GATK_VCF.pl";
        final String gatkKey = getProperty("gatk_key");
        final String identifier = getProperty("identifier");
        final String refFasta = getProperty("ref_fasta");
        final String dbsnpVcf = getProperty("gatk_dbsnp_vcf");
        final Integer intervalPadding = hasPropertyAndNotNull("interval_padding") ? Integer.parseInt(getProperty("interval_padding")) : null;
        final String standCallConf = getProperty("stand_call_conf");
        final String standEmitConf = getProperty("stand_emit_conf");
        final Integer downsamplingCoverage = hasPropertyAndNotNull("downsampling_coverage") ? Integer.parseInt(getProperty("downsampling_coverage")) : null;
        final String downsamplingType = getOptionalProperty("downsampling_type", null);
        final Integer preserveQscoresLessThan = hasPropertyAndNotNull("preserve_qscores_less_than") ? Integer.parseInt(getProperty("preserve_qscores_less_than")) : null;
        final Set<String> bqsrCovariates = Sets.newHashSet(StringUtils.split(getProperty("bqsr_covariates"), ","));
        final Boolean doBQSR = Boolean.valueOf(getOptionalProperty("do_bqsr", "true"));
        final Integer gatkRealignTargetCreatorXmx = Integer.parseInt(getProperty("gatk_realign_target_creator_xmx"));
        final Integer gatkIndelRealignerXmx = Integer.parseInt(getProperty("gatk_indel_realigner_xmx"));
        final Integer gatkPrintReadsXmx = Integer.parseInt(getProperty("gatk_print_reads_xmx"));
        final Integer gatkBaseRecalibratorXmx = Integer.parseInt(getProperty("gatk_base_recalibrator_xmx"));
        final Integer gatkBaseRecalibratorMem = Integer.parseInt(getProperty("gatk_base_recalibrator_mem"));
        final Integer gatkBaseRecalibratorNct = Integer.parseInt(getProperty("gatk_base_recalibrator_nct"));
        final Integer gatkBaseRecalibratorSmp = Integer.parseInt(getProperty("gatk_base_recalibrator_smp"));
        final Integer gatkHaplotypeCallerThreads = Integer.parseInt(getProperty("gatk_haplotype_caller_threads"));
        final Integer gatkHaplotypeCallerXmx = Integer.parseInt(getProperty("gatk_haplotype_caller_xmx"));
        final Integer gatkUnifiedGenotyperXmx = Integer.parseInt(getProperty("gatk_unified_genotyper_xmx"));
        final Integer gatkUnifiedGenotyperThreads = Integer.parseInt(getProperty("gatk_unified_genotyper_threads"));
        final Integer gatkOverhead = Integer.parseInt(getProperty("gatk_sched_overhead_mem"));
        final String realignerTargetCreatorParams = getOptionalProperty("gatk_realigner_target_creator_params", null);
        final String indelRealignerParams = getOptionalProperty("gatk_indel_realigner_params", null);
        final String baseRecalibratorParams = getOptionalProperty("gatk_base_recalibrator_params", null);
        final String analyzeCovariatesParams = getOptionalProperty("gatk_analyze_covariates_params", null);
        final String printReadsParams = getOptionalProperty("gatk_print_reads_params", null);
        final String haplotypeCallerParams = getOptionalProperty("gatk_haplotype_caller_params", null);
        final String unifiedGenotyperParams = getOptionalProperty("gatk_unified_genotyper_params", null);

        final List<String> intervalFilesList = Arrays.asList(StringUtils.split(getOptionalProperty("interval_files", ""), ","));
        final Set<String> intervalFiles = new HashSet<>(intervalFilesList);
        if (intervalFiles.size() != intervalFilesList.size()) {
            throw new RuntimeException("Duplicate interval_files detected");
        }

        final Set<String> chrSizes;
        if (hasProperty("chr_sizes")) {
            //chr_sizes has been set
            List<String> chrSizesList = Arrays.asList(StringUtils.split(getProperty("chr_sizes"), ","));
            chrSizes = new HashSet<>(chrSizesList);
            if (chrSizes.size() != chrSizesList.size()) {
                throw new RuntimeException("Duplicate chr_sizes detected.");
            }
        } else if (!intervalFiles.isEmpty()) {
            //chr_sizes not set, interval_files has been set - use interval files to calculate chrSizes
            try {
                chrSizes = BEDFileUtils.getChromosomes(intervalFiles);
            } catch (IOException ioe) {
                throw new RuntimeException(ioe);
            }
        } else {
            //chr_sizes and interval_files not set - can not calculate chrSizes
            chrSizes = new HashSet<>();
        }

        // one chrSize record is required, null will result in no parallelization
        if (chrSizes.isEmpty()) {
            chrSizes.add(null);
        }

        Multimap<String, Pair<String, Job>> realignedBams = HashMultimap.create();
        for (String chrSize : chrSizes) {

            //GATK Realigner Target Creator ( https://www.broadinstitute.org/gatk/gatkdocs/org_broadinstitute_gatk_tools_walkers_indels_RealignerTargetCreator.php )
            RealignerTargetCreator realignerTargetCreatorCommand = new RealignerTargetCreator.Builder(java, gatkRealignTargetCreatorXmx + "g", tmpDir, gatk, gatkKey, dataDir)
                    .setReferenceSequence(refFasta)
                    .addInputBamFiles(inputBamFiles)
                    .setKnownIndels(dbsnpVcf)
                    .addInterval(chrSize)
                    .addIntervalFiles(intervalFiles)
                    .setIntervalPadding(intervalPadding)
                    .setDownsamplingCoverageThreshold(downsamplingCoverage)
                    .setDownsamplingType(downsamplingType)
                    .setOutputFileName("gatk" + (chrSize != null ? "." + chrSize.replace(":", "-") : ""))
                    .setExtraParameters(realignerTargetCreatorParams)
                    .build();
            Job realignerTargetCreatorJob = getWorkflow().createBashJob("GATKRealignerTargetCreator")
                    .setMaxMemory(Integer.toString((gatkRealignTargetCreatorXmx + gatkOverhead) * 1024))
                    .setQueue(queue);
            realignerTargetCreatorJob.getCommand().setArguments(realignerTargetCreatorCommand.getCommand());

            //GATK Indel Realigner ( https://www.broadinstitute.org/gatk/gatkdocs/org_broadinstitute_gatk_tools_walkers_indels_IndelRealigner.php )
            IndelRealigner indelRealignerCommand = new IndelRealigner.Builder(java, gatkIndelRealignerXmx + "g", tmpDir, gatk, gatkKey, dataDir)
                    .setReferenceSequence(refFasta)
                    .addInputBamFiles(inputBamFiles)
                    .addKnownIndelFile(dbsnpVcf)
                    .addInterval(chrSize)
                    .setIntervalPadding(intervalPadding)
                    .setTargetIntervalFile(realignerTargetCreatorCommand.getOutputFile())
                    .setExtraParameters(indelRealignerParams)
                    .build();
            Job indelRealignerJob = getWorkflow().createBashJob("GATKIndelRealigner")
                    .setMaxMemory(Integer.toString((gatkIndelRealignerXmx + gatkOverhead) * 1024))
                    .setQueue(queue)
                    .addParent(realignerTargetCreatorJob);
            indelRealignerJob.getCommand().setArguments(indelRealignerCommand.getCommand());

            if (realignedBams.containsKey(chrSize)) {
                throw new RuntimeException("Unexpected state: Duplicate interval key.");
            }
            for (String outputFile : indelRealignerCommand.getOutputFiles()) {
                realignedBams.put(chrSize, Pair.of(outputFile, indelRealignerJob));
            }

        }

        Multimap<String, Pair<String, Job>> inputBams;
        if (doBQSR) {
            Multimap<String, Pair<String, Job>> recalibratedBams = HashMultimap.create();

            //GATK Base Recalibrator ( https://www.broadinstitute.org/gatk/gatkdocs/org_broadinstitute_gatk_tools_walkers_bqsr_BaseRecalibrator.php )
            BaseRecalibrator baseRecalibratorCommand = new BaseRecalibrator.Builder(java, gatkBaseRecalibratorXmx + "m", tmpDir, gatk, gatkKey, dataDir)
                    .setReferenceSequence(refFasta)
                    .setCovariates(bqsrCovariates)
                    .addKnownSite(dbsnpVcf)
                    .addInputFiles(getLeftCollection(realignedBams.values()))
                    .addIntervalFiles(intervalFiles)
                    .setIntervalPadding(intervalPadding)
                    .setNumCpuThreadsPerDataThread(gatkBaseRecalibratorNct)
                    .setExtraParameters(baseRecalibratorParams)
                    .build();
            Job baseRecalibratorJob = getWorkflow().createBashJob("GATKBaseRecalibrator")
                    .setMaxMemory(gatkBaseRecalibratorMem.toString())
                    .setThreads(gatkBaseRecalibratorSmp)
                    .setQueue(queue);
            baseRecalibratorJob.getParents().addAll(getRightCollection(realignedBams.values()));
            baseRecalibratorJob.getCommand().setArguments(baseRecalibratorCommand.getCommand());

            //GATK Analyze Covariates ( https://www.broadinstitute.org/gatk/guide/tooldocs/org_broadinstitute_gatk_tools_walkers_bqsr_AnalyzeCovariates.php )
            AnalyzeCovariates analyzeCovariatesCommand = new AnalyzeCovariates.Builder(java, "4g", tmpDir, gatk, gatkKey, rDir, dataDir)
                    .setReferenceSequence(refFasta)
                    .setRecalibrationTable(baseRecalibratorCommand.getOutputFile())
                    .setOutputFileName(identifier)
                    .setExtraParameters(analyzeCovariatesParams)
                    .build();
            Job analyzeCovariatesJob = getWorkflow().createBashJob("GATKAnalyzeCovariates")
                    .setMaxMemory(Integer.toString((4 + gatkOverhead) * 1024))
                    .setQueue(queue)
                    .addParent(baseRecalibratorJob);
            analyzeCovariatesJob.getCommand().setArguments(analyzeCovariatesCommand.getCommand());
            analyzeCovariatesJob.addFile(createOutputFile(analyzeCovariatesCommand.getPlotsReportFile(), "application/pdf", manualOutput));

            for (String chrSize : chrSizes) {

                Job printReadsJob = getWorkflow().createBashJob("GATKTableRecalibration")
                        .setMaxMemory(Integer.toString((gatkPrintReadsXmx + gatkOverhead) * 1024))
                        .setQueue(queue)
                        .addParent(baseRecalibratorJob);

                List<String> command = new LinkedList<>();
                for (String inputBam : getLeftCollection(realignedBams.get(chrSize))) {
                    //GATK Print Reads (https://www.broadinstitute.org/gatk/gatkdocs/org_broadinstitute_gatk_tools_walkers_readutils_PrintReads.php)
                    PrintReads printReadsCommand = new PrintReads.Builder(java, gatkPrintReadsXmx + "g", tmpDir, gatk, gatkKey, dataDir)
                            .setReferenceSequence(refFasta)
                            .setCovariatesTablesFile(baseRecalibratorCommand.getOutputFile())
                            .setInputFile(inputBam)
                            .setPreserveQscoresLessThan(preserveQscoresLessThan)
                            .addInterval(chrSize)
                            .setIntervalPadding(intervalPadding)
                            .setExtraParameters(printReadsParams)
                            .build();
                    command.addAll(printReadsCommand.getCommand());
                    command.add(";\n");
                    recalibratedBams.put(chrSize, Pair.of(printReadsCommand.getOutputFile(), printReadsJob));
                }
                printReadsJob.getCommand().setArguments(command);
            }

            //BQSR enabled, pass recalibrated bams to variant calling
            inputBams = recalibratedBams;
        } else {
            //BQSR disabled, pass realigned bams to variant calling
            inputBams = realignedBams;
        }

        Multimap<VariantCaller, Pair<String, Job>> snvFiles = HashMultimap.create();
        Multimap<VariantCaller, Pair<String, Job>> indelFiles = HashMultimap.create();
        Multimap<VariantCaller, Pair<String, Job>> finalFiles = HashMultimap.create();
        for (String chrSize : chrSizes) {
            for (VariantCaller vc : variantCallers) {
                String workingDir = dataDir + vc.toString() + "/";
                switch (vc) {
                    case HAPLOTYPE_CALLER:
                        //GATK Haplotype Caller ( https://www.broadinstitute.org/gatk/gatkdocs/org_broadinstitute_gatk_tools_walkers_haplotypecaller_HaplotypeCaller.php )
                        HaplotypeCaller haplotypeCallerCommand = new HaplotypeCaller.Builder(java, Integer.toString(gatkHaplotypeCallerXmx) + "g", tmpDir, gatk, gatkKey, dataDir)
                                .setInputBamFiles(getLeftCollection(inputBams.values()))
                                .setReferenceSequence(refFasta)
                                .setDbsnpFilePath(dbsnpVcf)
                                .setStandardCallConfidence(standCallConf)
                                .setStandardEmitConfidence(standEmitConf)
                                .setGenotypingMode(getOptionalProperty("haplotype_caller_genotyping_mode", null))
                                .setOutputMode(getOptionalProperty("haplotype_caller_output_mode", null))
                                .setOperatingMode(HaplotypeCaller.OperatingMode.VCF)
                                .addInterval(chrSize)
                                .addIntervalFiles(intervalFiles)
                                .setIntervalPadding(intervalPadding)
                                .setDownsamplingCoverageThreshold(downsamplingCoverage)
                                .setDownsamplingType(downsamplingType)
                                .setOutputFileName("gatk" + (chrSize != null ? "." + chrSize.replace(":", "-") : ""))
                                .setNumCpuThreadsPerDataThread(gatkHaplotypeCallerThreads)
                                .setExtraParameters(haplotypeCallerParams)
                                .build();
                        Job haplotypeCallerJob = this.getWorkflow().createBashJob("GATKHaplotypeCaller")
                                .setMaxMemory(Integer.toString((gatkHaplotypeCallerXmx + gatkOverhead) * 1024))
                                .setQueue(queue);
                        haplotypeCallerJob.getParents().addAll(getRightCollection(inputBams.get(chrSize)));
                        haplotypeCallerJob.getCommand().setArguments(haplotypeCallerCommand.getCommand());

                        finalFiles.put(vc, Pair.of(haplotypeCallerCommand.getOutputFile(), haplotypeCallerJob));
                        break;

                    case UNIFIED_GENOTYPER:
                        //GATK Unified Genotyper (INDELS) ( https://www.broadinstitute.org/gatk/guide/tooldocs/org_broadinstitute_gatk_tools_walkers_genotyper_UnifiedGenotyper.php )
                        UnifiedGenotyper indelsUnifiedGenotyperCommand = new UnifiedGenotyper.Builder(java, Integer.toString(gatkUnifiedGenotyperXmx) + "g", tmpDir, gatk, gatkKey, workingDir)
                                .setInputBamFiles(getLeftCollection(inputBams.get(chrSize)))
                                .setReferenceSequence(refFasta)
                                .setDbsnpFilePath(dbsnpVcf)
                                .setStandardCallConfidence(standCallConf)
                                .setStandardEmitConfidence(standEmitConf)
                                .setGenotypeLikelihoodsModel("INDEL")
                                .setGroup("Standard")
                                .addInterval(chrSize)
                                .addIntervalFiles(intervalFiles)
                                .setIntervalPadding(intervalPadding)
                                .setDownsamplingCoverageThreshold(downsamplingCoverage)
                                .setDownsamplingType(downsamplingType)
                                .setOutputFileName("gatk" + (chrSize != null ? "." + chrSize.replace(":", "-") : ""))
                                .setNumCpuThreadsPerDataThread(gatkUnifiedGenotyperThreads)
                                .setExtraParameters(unifiedGenotyperParams)
                                .build();
                        Job indelsUnifiedGenotyperJob = this.getWorkflow().createBashJob("GATKUnifiedGenotyperIndel")
                                .setMaxMemory(Integer.toString((gatkUnifiedGenotyperXmx + gatkOverhead) * 1024))
                                .setQueue(queue);
                        indelsUnifiedGenotyperJob.getParents().addAll(getRightCollection(inputBams.get(chrSize)));
                        indelsUnifiedGenotyperJob.getCommand().setArguments(indelsUnifiedGenotyperCommand.getCommand());

                        indelFiles.put(vc, Pair.of(indelsUnifiedGenotyperCommand.getOutputFile(), indelsUnifiedGenotyperJob));

                        //GATK Unified Genotyper (SNVS) ( https://www.broadinstitute.org/gatk/gatkdocs/org_broadinstitute_gatk_tools_walkers_genotyper_UnifiedGenotyper.php )
                        UnifiedGenotyper snvsUnifiedGenotyperCommand = new UnifiedGenotyper.Builder(java, Integer.toString(gatkUnifiedGenotyperXmx) + "g", tmpDir, gatk, gatkKey, workingDir)
                                .setInputBamFiles(getLeftCollection(inputBams.get(chrSize)))
                                .setReferenceSequence(refFasta)
                                .setDbsnpFilePath(dbsnpVcf)
                                .setStandardCallConfidence(standCallConf)
                                .setStandardEmitConfidence(standEmitConf)
                                .setGenotypeLikelihoodsModel("SNP")
                                .addInterval(chrSize)
                                .addIntervalFiles(intervalFiles)
                                .setIntervalPadding(intervalPadding)
                                .setDownsamplingCoverageThreshold(downsamplingCoverage)
                                .setDownsamplingType(downsamplingType)
                                .setOutputFileName("gatk" + (chrSize != null ? "." + chrSize.replace(":", "-") : ""))
                                .setNumCpuThreadsPerDataThread(gatkUnifiedGenotyperThreads)
                                .setExtraParameters(unifiedGenotyperParams)
                                .build();
                        Job snvsUnifiedGenotyperJob = this.getWorkflow().createBashJob("GATKUnifiedGenotyperSNV")
                                .setMaxMemory(Integer.toString((gatkUnifiedGenotyperXmx + gatkOverhead) * 1024))
                                .setQueue(queue);
                        snvsUnifiedGenotyperJob.getParents().addAll(getRightCollection(inputBams.get(chrSize)));
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
                    .setOutputFileName(identifier + "." + StringUtils.lowerCase(vc.toString()) + ".raw")
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

    private <T, S> Set<T> getLeftCollection(Collection<Pair<T, S>> pairs) {
        Set<T> ts = new HashSet<>();
        for (Pair<T, S> p : pairs) {
            ts.add(p.getLeft());
        }
        return ts;
    }

    private <S, T> Set<T> getRightCollection(Collection<Pair<S, T>> pairs) {
        Set<T> ts = new HashSet<>();
        for (Pair<S, T> p : pairs) {
            ts.add(p.getRight());
        }
        return ts;
    }
}
