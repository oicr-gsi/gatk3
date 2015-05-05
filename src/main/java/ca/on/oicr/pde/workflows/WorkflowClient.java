package ca.on.oicr.pde.workflows;

import ca.on.oicr.pde.commands.CompressFile;
import ca.on.oicr.pde.commands.gatk3.UnifiedGenotyper;
import ca.on.oicr.pde.commands.gatk3.VariantFiltration;
import ca.on.oicr.pde.commands.MergeVcf;
import ca.on.oicr.pde.commands.SortVcf;
import ca.on.oicr.pde.commands.gatk3.BaseRecalibrator;
import ca.on.oicr.pde.commands.gatk3.IndelRealigner;
import ca.on.oicr.pde.commands.gatk3.PrintReads;
import ca.on.oicr.pde.commands.gatk3.RealignerTargetCreator;
import ca.on.oicr.pde.commands.gatk3.VariantAnnotator;
import ca.on.oicr.pde.commands.picard.ReorderSam;
import ca.on.oicr.pde.commands.picard.SortSam;
import ca.on.oicr.pde.utilities.workflows.OicrWorkflow;
import java.util.*;
import java.util.Map.Entry;
import net.sourceforge.seqware.pipeline.workflowV2.model.Job;
import net.sourceforge.seqware.pipeline.workflowV2.model.SqwFile;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

public class WorkflowClient extends OicrWorkflow {

    private String binDir;
    private String tmpDir;
    private String dataDir;

    private boolean manualOutput;
    private String identifier = null;
    private String java = null;
    private String gatk = null;

    private String refFasta = null;
    private String dbsnpVcf = null;
    private Integer picardOverhead;
    private Integer gatkOverhead;
    private SqwFile[] inputBamFiles;
    private String queue;

    private String gatkKey;

    private Integer gatkCountCovariateMem = null;
    private String[] chrSizes = null;

    private String picardDir = null;

    private Integer picardReorderSamMem = null;
    private Integer picardSortMem = null;
    private Integer gatkRealignTargetCreatorMem = null;
    private Integer gatkIndelRealignerMem = null;

    private String annotateParams = null;
    private Integer annotationMem = null;

    private String perl = null;

    private Integer gatkUnifiedGenotyperMem = null;
    private Integer gatkUnifiedGenotyperThreads = null;
    private Integer gatkIndelGenotyperMem = null;
    private Integer variantFilterMem = null;

    private String standCallConf = null;
    private String standEmitConf = null;

    private String preserveQscoresLessThan = null;

    private String mergeVCFScript;

    public WorkflowClient() {
        super();
    }

    @Override
    public void setupDirectory() {
        init();
        setupCommands();
        this.addDirectory(dataDir);
    }

    public void init() {

        binDir = this.getWorkflowBaseDir() + "/bin/";
        tmpDir = "tmp/";
        dataDir = "data/";

        manualOutput = BooleanUtils.toBoolean(getProperty("manual_output"), "true", "false");
        queue = getOptionalProperty("queue", "");

        perl = getProperty("perl");
        java = getProperty("java");
        gatk = getProperty("gatk_jar");

        mergeVCFScript = binDir + "sw_module_merge_GATK_VCF.pl";

        picardDir = getProperty("picard_dir");

        identifier = getProperty("identifier");
        refFasta = getProperty("ref_fasta");
        dbsnpVcf = getProperty("gatk_dbsnp_vcf");

        chrSizes = StringUtils.split(getProperty("chr_sizes"), ",");
        annotateParams = getProperty("annotate_params");
        gatkUnifiedGenotyperThreads = Integer.parseInt(getProperty("gatk_unified_genotyper_threads"));

        standCallConf = getProperty("stand_call_conf");
        standEmitConf = getProperty("stand_emit_conf");
        preserveQscoresLessThan = getProperty("preserve_qscores_less_than");

        picardOverhead = Integer.parseInt(getProperty("picard_sched_overhead_mem"));
        gatkOverhead = Integer.parseInt(getProperty("gatk_sched_overhead_mem"));

        gatkUnifiedGenotyperMem = Integer.parseInt(getProperty("gatk_unified_genotyper_mem"));
        annotationMem = Integer.valueOf(getProperty("annotation_mem_gb"));

        variantFilterMem = Integer.parseInt(getProperty("gatk_variant_filter_mem"));
        picardReorderSamMem = Integer.parseInt(getProperty("picard_reorder_sam_mem"));
        picardSortMem = Integer.parseInt(getProperty("picard_sort_mem"));
        gatkRealignTargetCreatorMem = Integer.parseInt(getProperty("gatk_realign_target_creator_mem"));
        gatkIndelRealignerMem = Integer.parseInt(getProperty("gatk_indel_realigner_mem"));

        gatkCountCovariateMem = Integer.parseInt(getProperty("gatk_count_covariate_mem"));
        gatkIndelGenotyperMem = Integer.parseInt(getProperty("gatk_indel_genotyper_mem"));

        gatkKey = getProperty("gatk_key");

    }

    public void setupCommands() {

    }

    @Override
    public Map<String, SqwFile> setupFiles() {
        inputBamFiles = provisionInputFiles("bam_inputs");
        return this.getFiles();
    }

    @Override
    public void buildWorkflow() {

        Job join1 = getWorkflow().createBashJob("Join");
        join1.setCommand("true");
        join1.setLocal(true);

        List<String> reorderedBams = new LinkedList<>();

        for (SqwFile inputFile : Arrays.asList(inputBamFiles)) {
            ReorderSam reorder = new ReorderSam.Builder(java, picardReorderSamMem + "g", tmpDir, picardDir, dataDir)
                    .setInputFile(inputFile.getProvisionedPath())
                    .setReferenceSequence(refFasta)
                    .setCreateIndex(true)
                    .build();

            Job j = getWorkflow().createBashJob("PicardReorderAndIndexBam")
                    .setMaxMemory(Integer.toString((picardReorderSamMem + picardOverhead) * 1024))
                    .setQueue(queue);
            j.getCommand().setArguments(reorder.getCommand());

            reorderedBams.add(reorder.getOutputFile());

            join1.addParent(j);

        }

        Map<String, String> intervalFiles = new HashMap<>();

        Job join2 = getWorkflow().createBashJob("Join");
        join2.setCommand("true");

        for (String chrSize : chrSizes) {

            RealignerTargetCreator target = new RealignerTargetCreator.Builder(java, gatkRealignTargetCreatorMem + "g", tmpDir, gatk, gatkKey, dataDir)
                    .setIntervals(chrSize)
                    .setReferenceSequence(refFasta)
                    .addInputBamFiles(reorderedBams)
                    .setKnownIndels(dbsnpVcf)
                    .build();

            Job id20 = getWorkflow().createBashJob("GATKRealignerTargetCreator")
                    .setMaxMemory(Integer.toString((gatkRealignTargetCreatorMem + gatkOverhead) * 1024))
                    .setQueue(queue)
                    .addParent(join1);
            id20.getCommand().setArguments(target.getCommand());

            IndelRealigner realign = new IndelRealigner.Builder(java, gatkIndelRealignerMem + "g", tmpDir, gatk, gatkKey, dataDir)
                    .setIntervals(chrSize)
                    .setReferenceSequence(refFasta)
                    .addInputBamFiles(reorderedBams)
                    .setTargetIntervalFile(target.getOutputFile())
                    .build();

            Job id30 = getWorkflow().createBashJob("GATKIndelRealigner")
                    .setMaxMemory(Integer.toString((gatkIndelRealignerMem + gatkOverhead) * 1024))
                    .setQueue(queue)
                    .addParent(id20);
            id30.getCommand().setArguments(realign.getCommand());

            SortSam sort = new SortSam.Builder(java, picardSortMem + "g", tmpDir, picardDir, dataDir)
                    .setInputFile(realign.getOutputFile())
                    .setSortOrder("coordinate")
                    .setCreateIndex(true)
                    .build();

            Job id35 = getWorkflow().createBashJob("PicardSortAndIndexByQuery")
                    .setMaxMemory(Integer.toString((picardSortMem + picardOverhead) * 1024))
                    .setQueue(queue)
                    .addParent(id30);
            id35.getCommand().setArguments(sort.getCommand());

            if (intervalFiles.put(chrSize, sort.getOutputFile()) != null) {
                throw new RuntimeException("Unexpected state: Duplicate interval key.");
            }

            join2.addParent(id35);
        }

        BaseRecalibrator recalibrator = new BaseRecalibrator.Builder(java, gatkCountCovariateMem + "g", tmpDir, gatk, gatkKey, dataDir)
                .setReferenceSequence(refFasta)
                .addCovariate("ReadGroupCovariate")
                .addCovariate("QualityScoreCovariate")
                .addCovariate("CycleCovariate")
                .addCovariate("ContextCovariate") //gatk_recal_base_recal_params=-cov ContextCovariate
                .addKnownSite(dbsnpVcf)
                .addInputFiles(intervalFiles.values())
                .build();

        Job id60 = getWorkflow().createBashJob("BaseRecalibrator")
                .setMaxMemory(Integer.toString((gatkCountCovariateMem + gatkOverhead) * 1024))
                .setQueue(queue)
                .addParent(join2);
        id60.getCommand().setArguments(recalibrator.getCommand());

        Map<String, Job> snvFiles = new HashMap<>();
        Map<String, Job> indelFiles = new HashMap<>();

        for (Entry<String, String> e : intervalFiles.entrySet()) {

            String chrSize = e.getKey();
            String inputBam = e.getValue();

            PrintReads recalibrate = new PrintReads.Builder(java, gatkCountCovariateMem + "g", tmpDir, gatk, gatkKey, dataDir)
                    //.setIntervals(chrSize) //http://gatkforums.broadinstitute.org/discussion/4133/when-should-i-use-l-to-pass-in-a-list-of-intervals
                    .setReferenceSequence(refFasta)
                    .setCovariatesTablesFile(recalibrator.getOutputFile())
                    .setPreserveQscoresLessThan(preserveQscoresLessThan)
                    .setInputFile(inputBam)
                    .build();

            Job id70 = getWorkflow().createBashJob("TableRecalibration")
                    .setMaxMemory(Integer.toString((gatkCountCovariateMem + gatkOverhead) * 1024))
                    .setQueue(queue)
                    .addParent(id60);
            id70.getCommand().setArguments(recalibrate.getCommand());

            //INDELS
            UnifiedGenotyper rawIndels = new UnifiedGenotyper.Builder(java, Integer.toString(gatkIndelGenotyperMem) + "g", tmpDir, gatk, gatkKey, dataDir)
                    .setInputBamFile(recalibrate.getOutputFile())
                    .setReferenceSequence(refFasta)
                    .setDbsnpFilePath(dbsnpVcf)
                    .setIntervals(chrSize)
                    .setStandardCallConfidence(standCallConf)
                    .setStandardEmitConfidence(standEmitConf)
                    .setGenotypeLikelihoodsModel("INDEL")
                    .setGroup("Standard")
                    .build();

            Job id100 = this.getWorkflow().createBashJob("GATKUnifiedGenotyperIndel")
                    .setMaxMemory(Integer.toString((gatkIndelGenotyperMem + gatkOverhead) * 1024))
                    .setQueue(queue)
                    .addParent(id70);
            id100.getCommand().setArguments(rawIndels.getCommand());

            VariantFiltration filterIndels = new VariantFiltration.Builder(java, variantFilterMem + "g", tmpDir, gatk, gatkKey, dataDir)
                    .setInputVcfFile(rawIndels.getOutputFile())
                    .setIntervals(chrSize)
                    .setReferenceSequence(refFasta)
                    .addFilter("Indel_HARD_VALIDATE", "MQ0 >= 4 && ((MQ0 / (1.0 * DP)) > 0.1)")
                    .addFilter("Indel_StrandBias", "SB > -10.0")
                    .addFilter("Indel_VeryLowQual", "QUAL < 30")
                    .addFilter("Indel_LowCoverage", "DP < 5")
                    .addFilter("Indel_StrandBiasFishers", "FS > 60.0")
                    .addFilter("Indel_LowQual", "QUAL > 30.0 && QUAL < 50.0")
                    .build();

            Job id110 = this.getWorkflow().createBashJob("GATKUnifiedGenotyperIndelFilter")
                    .setMaxMemory(Integer.toString((gatkUnifiedGenotyperMem + gatkOverhead) * 1024))
                    .setQueue(queue)
                    .addParent(id100);
            id110.getCommand().setArguments(filterIndels.getCommand());

            indelFiles.put(filterIndels.getOutputFile(), id110);

            //SNVS
            UnifiedGenotyper rawSnvs = new UnifiedGenotyper.Builder(java, Integer.toString(gatkIndelGenotyperMem) + "g", tmpDir, gatk, gatkKey, dataDir)
                    .setInputBamFile(recalibrate.getOutputFile())
                    .setReferenceSequence(refFasta)
                    .setDbsnpFilePath(dbsnpVcf)
                    .setIntervals(chrSize)
                    .setNumThreads(Integer.toString(gatkUnifiedGenotyperThreads))
                    .setStandardCallConfidence(standCallConf)
                    .setStandardEmitConfidence(standEmitConf)
                    .setGenotypeLikelihoodsModel("SNP").build();

            Job id90 = this.getWorkflow().createBashJob("GATKUnifiedGenotyperSNV")
                    .setMaxMemory(Integer.toString((gatkIndelGenotyperMem + gatkOverhead) * 1024))
                    .setQueue(queue)
                    .addParent(id70);
            id90.getCommand().setArguments(rawSnvs.getCommand());

            VariantFiltration filterSnvs = new VariantFiltration.Builder(java, variantFilterMem + "g", tmpDir, gatk, gatkKey, dataDir)
                    .setInputVcfFile(rawSnvs.getOutputFile())
                    .setIntervals(chrSize)
                    .setReferenceSequence(refFasta)
                    .setClusterWindowSize("10")
                    .setClusterSize("3")
                    .addFilter("HARD_TO_VALIDATE", "MQ0 >= 4 && ((MQ0 / (1.0 * DP)) > 0.1)")
                    .addFilter("LowCoverage", "DP < 5")
                    .addFilter("VeryLowQual", "QUAL < 30.0")
                    .addFilter("LowQual", "QUAL > 30.0 && QUAL < 50.0")
                    .addFilter("LowQD", "QD < 2.0 ")
                    .addFilter("StrandBiasFishers", "FS > 60.0")
                    .addFilter("StrandBias", "SB > -10.0")
                    .addMask("InDel", rawIndels.getOutputFile())
                    .build();

            Job id120 = this.getWorkflow().createBashJob("GATKUnifiedGenotyperSNVFilter")
                    .setMaxMemory(Integer.toString((variantFilterMem + gatkOverhead) * 1024))
                    .setQueue(queue)
                    .addParent(id90) //rawSnvs
                    .addParent(id100); //rawIndels
            id120.getCommand().setArguments(filterSnvs.getCommand());

            snvFiles.put(filterSnvs.getOutputFile(), id120);

        }

        MergeVcf mergeSnvs = new MergeVcf.Builder(perl, mergeVCFScript, dataDir)
                .addInputFiles(snvFiles.keySet())
                .setOutputFileName("gatk.snps.filtered.merged.vcf")
                .build();

        Job id140 = this.getWorkflow().createBashJob("MergeFilteredSNV")
                .setMaxMemory("4096")
                .setQueue(queue);
        id140.getParents().addAll(snvFiles.values());
        id140.getCommand().setArguments(mergeSnvs.getCommand());

        MergeVcf mergeIndels = new MergeVcf.Builder(perl, mergeVCFScript, dataDir)
                .addInputFiles(indelFiles.keySet())
                .setOutputFileName("gatk.indels.filtered.merged.vcf")
                .build();

        Job id160 = this.getWorkflow().createBashJob("MergeFilteredIndel")
                .setMaxMemory("4096")
                .setQueue(queue);
        id160.getParents().addAll(indelFiles.values());
        id160.getCommand().setArguments(mergeIndels.getCommand());

        MergeVcf mergeFinal = new MergeVcf.Builder(perl, mergeVCFScript, dataDir)
                .addInputFile(mergeSnvs.getOutputFile())
                .addInputFile(mergeIndels.getOutputFile())
                .setOutputFileName(identifier + ".merged.vcf")
                .build();

        Job id180 = this.getWorkflow().createBashJob("MergeFilteredVariants")
                .setMaxMemory("4096")
                .setQueue(queue)
                .addParent(id140)
                .addParent(id160);
        id180.getCommand().setArguments(mergeFinal.getCommand());

        SortVcf sortVcf = new SortVcf.Builder(dataDir)
                .setInputFile(mergeFinal.getOutputFile())
                .build();
        VariantAnnotator annotateVcf = new VariantAnnotator.Builder(java, annotationMem + "g", tmpDir, gatk, gatkKey, dataDir)
                .setReferenceSequence(refFasta)
                .setInputVcfFile(sortVcf.getOutputFile())
                .setIntervals(sortVcf.getOutputFile())
                .setAdditionalParams(annotateParams)
                .build();
        CompressFile compressVcf = new CompressFile.Builder(dataDir)
                .setInputFile(annotateVcf.getOutputFile())
                .build();

        List<String> cmd = new LinkedList<>();
        cmd.addAll(sortVcf.getCommand());
        cmd.add("&&");
        cmd.addAll(annotateVcf.getCommand());
        cmd.add("&&");
        cmd.addAll(compressVcf.getCommand());

        Job job12 = getWorkflow().createBashJob("SortAnnotateCompressVcf")
                .setMaxMemory(Integer.toString(Collections.max(Arrays.asList(annotationMem, annotationMem, annotationMem * 2)) * 1024))
                .setQueue(queue)
                .addParent(id180);
        job12.getCommand().setArguments(cmd);

        job12.addFile(createOutputFile(compressVcf.getOutputFile(), "application/vcf-4-gzip", manualOutput));

    }

}
