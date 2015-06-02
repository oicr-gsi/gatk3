package ca.on.oicr.pde.workflows;

import ca.on.oicr.pde.workflows.GATK3Workflow.VariantCaller;
import static ca.on.oicr.pde.workflows.GATK3Workflow.VariantCaller.HAPLOTYPE_CALLER;
import static ca.on.oicr.pde.workflows.GATK3Workflow.VariantCaller.UNIFIED_GENOTYPER;
import com.google.common.base.Joiner;
import com.google.common.collect.Sets;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import static net.sourceforge.seqware.pipeline.workflowV2.MockWorkflowDataModelFactory.buildWorkflowModel;
import net.sourceforge.seqware.pipeline.workflowV2.model.AbstractJob;
import net.sourceforge.seqware.pipeline.workflowV2.model.SqwFile;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 *
 * @author mlaszloffy
 */
public class GATK3WorkflowTest {

    public GATK3WorkflowTest() {
    }

    private GATK3Workflow getWorkflowClientObject(Map<String, String> config) throws IOException, IllegalAccessException {
        GATK3Workflow w = new GATK3Workflow();
        w.setConfigs(config);
        buildWorkflowModel(System.getProperty("bundleDirectory"), w);
        return w;
    }

    @Test(expectedExceptions = java.lang.AssertionError.class)
    public void checkForNull() throws IOException, IllegalAccessException {
        Map<String, String> config = getDefaultConfig();
        config.put("identifier", "null");
        config.put("input_files", "/data/test/PCSI0022P.val.bam,/data/test/PCSI0022R.val.bam,/data/test/PCSI0022X.val.bam,/data/test/PCSI0022C.val.bam,"
                + "/data/test/PCSI0022P.val.bai,/data/test/PCSI0022R.val.bai,/data/test/PCSI0022X.val.bai,/data/test/PCSI0022C.val.bai");
        GATK3Workflow w = getWorkflowClientObject(config);
        validateWorkflow(w);
    }

    @Test(enabled = true)
    public void checkJobNumber() throws IOException, IllegalAccessException {
        Map<String, String> config;
        GATK3Workflow w;

        config = getDefaultConfig();
        config.put("identifier", "gatk.ex");
        config.put("input_files", "/data/test/PCSI0022P.val.bam,/data/test/PCSI0022R.val.bam,/data/test/PCSI0022X.val.bam,/data/test/PCSI0022C.val.bam,"
                + "/data/test/PCSI0022P.val.bai,/data/test/PCSI0022R.val.bai,/data/test/PCSI0022X.val.bai,/data/test/PCSI0022C.val.bai");
        config.put("variant_caller", "Unified_GeNoTyPeR");
        w = getWorkflowClientObject(config);
        validateWorkflow(w);

        Assert.assertEquals(w.getWorkflow().getJobs().size(), 131);
        Assert.assertEquals(GATK3WorkflowTest.this.getExpectedJobCount(4, 25, VariantCaller.UNIFIED_GENOTYPER), 131);

        config.put("variant_caller", "haplotype_caller");
        w = getWorkflowClientObject(config);
        validateWorkflow(w);

        Assert.assertEquals(w.getWorkflow().getJobs().size(), 104);
        Assert.assertEquals(GATK3WorkflowTest.this.getExpectedJobCount(4, 25, VariantCaller.HAPLOTYPE_CALLER), 104);

        config.put("variant_caller", "haplotype_caller,unified_genotyper");
        w = getWorkflowClientObject(config);
        validateWorkflow(w);

        Assert.assertEquals(w.getWorkflow().getJobs().size(), 158);
        Assert.assertEquals(getExpectedJobCount(4, 25, Sets.newHashSet(VariantCaller.HAPLOTYPE_CALLER, VariantCaller.UNIFIED_GENOTYPER)), 158);

    }

    @Test(enabled = true)
    public void splitByChromosome() throws IOException, IllegalAccessException {

        int numInputFiles = 500;
        List<String> inputFiles = new LinkedList<>();
        for (int i = 0; i < numInputFiles; i++) {
            String fileName = RandomStringUtils.random(5, true, true);
            inputFiles.add(fileName + ".bam");
            inputFiles.add(fileName + ".bai");
        }

        Map<String, String> config;
        GATK3Workflow w;
        config = getDefaultConfig();
        config.put("identifier", "gatk.ex");
        config.put("input_files", StringUtils.join(inputFiles, ","));
//        config.put("chr_sizes", "chr1,chr2,chr3,chr4,chr5,chr6,chr7,chr8,chr9,chr10,"
//                + "chr11,chr12,chr13,chr14,chr15,chr16,chr17,chr18,chr19,chr20,"
//                + "chr21,chr22,chrX,chrY,chrM");

        int parallelismLevel = Arrays.asList(StringUtils.split(config.get("chr_sizes"), ",")).size();

        //haplotype caller
        w = getWorkflowClientObject(config);
        validateWorkflow(w);
        Assert.assertEquals(w.getWorkflow().getJobs().size(),
                GATK3WorkflowTest.this.getExpectedJobCount(numInputFiles, parallelismLevel, VariantCaller.HAPLOTYPE_CALLER));

        //unified genotyper
        config.put("variant_caller", "unified_genotyper");
        w = getWorkflowClientObject(config);
        validateWorkflow(w);
        Assert.assertEquals(w.getWorkflow().getJobs().size(),
                GATK3WorkflowTest.this.getExpectedJobCount(numInputFiles, parallelismLevel, VariantCaller.UNIFIED_GENOTYPER));

        //haplotype caller and unified genotyper
        config.put("variant_caller", "haplotype_caller,unified_genotyper");
        w = getWorkflowClientObject(config);
        validateWorkflow(w);
        Assert.assertEquals(w.getWorkflow().getJobs().size(),
                getExpectedJobCount(numInputFiles, parallelismLevel, Sets.newHashSet(VariantCaller.HAPLOTYPE_CALLER, VariantCaller.UNIFIED_GENOTYPER)));
    }

    @Test(enabled = true)
    public void noSplit() throws IOException, IllegalAccessException {

        Map<String, String> config;
        GATK3Workflow w;
        config = getDefaultConfig();
        config.put("identifier", "gatk.ex");
        config.put("input_files", "/test/1.bam,/test/1.bai");
        config.put("chr_sizes", "");

        //unified gentotyper
        config.put("variant_caller", "unified_genotyper");
        w = getWorkflowClientObject(config);
        validateWorkflow(w);
        Assert.assertEquals(w.getWorkflow().getJobs().size(),
                GATK3WorkflowTest.this.getExpectedJobCount(1, 0, VariantCaller.UNIFIED_GENOTYPER));

        //haplotype caller
        config.put("variant_caller", "haplotype_caller");
        w = getWorkflowClientObject(config);
        validateWorkflow(w);
        Assert.assertEquals(w.getWorkflow().getJobs().size(),
                GATK3WorkflowTest.this.getExpectedJobCount(1, 0, VariantCaller.HAPLOTYPE_CALLER));

        //haplotype caller and unified genotyper
        config.put("variant_caller", "haplotype_caller,unified_genotyper");
        w = getWorkflowClientObject(config);
        validateWorkflow(w);
        Assert.assertEquals(w.getWorkflow().getJobs().size(),
                getExpectedJobCount(1, 0, Sets.newHashSet(VariantCaller.HAPLOTYPE_CALLER, VariantCaller.UNIFIED_GENOTYPER)));
    }

    private int getExpectedJobCount(int numInputFiles, int parallelismLevel, VariantCaller vc) {
        return getExpectedJobCount(numInputFiles, parallelismLevel, Sets.newHashSet(vc));
    }

    private int getExpectedJobCount(int numInputFiles, int parallelismLevel, Collection<VariantCaller> vcs) {
        parallelismLevel = Math.max(parallelismLevel, 1); //even if chr_sizes is empty, the workflow will run

        int variantCallingJobCount = 0;
        int mergeJobCount = 0;
        int sortCompressIndexJobCount = 0;
        for (VariantCaller vc : vcs) {
            switch (vc) {
                case HAPLOTYPE_CALLER:
                    variantCallingJobCount += parallelismLevel * 1; //hc
                    mergeJobCount += ((parallelismLevel > 1) ? 1 : 0); //merge (only if there are files to merge)
                    sortCompressIndexJobCount += 1; //sort, compress, index
                    break;
                case UNIFIED_GENOTYPER:
                    variantCallingJobCount += parallelismLevel * 2; //recalibrate + indel UG + filter + snv UG + filter
                    mergeJobCount += 3;  //merge snv + merge indel + merge
                    sortCompressIndexJobCount += 1; //sort, compress, index
                    break;
            }
        }

        return (parallelismLevel * 2) // for each chr_size interval: create targets + realign
                + 2 //calculate base recalibration table + analyze covariates
                + parallelismLevel //recalibrate
                + variantCallingJobCount
                + mergeJobCount
                + sortCompressIndexJobCount;
    }

    private void validateWorkflow(GATK3Workflow w) {

        //check for null string
        for (AbstractJob j : w.getWorkflow().getJobs()) {
            String c = Joiner.on(" ").useForNull("null").join(j.getCommand().getArguments());
            Assert.assertFalse(c.contains("null"), "Warning: command contains \"null\":\n" + c + "\n");
        }

        //verify bai is located in the correct provision directory
        Map<String, String> bamFileDirectories = new HashMap<>();
        for (SqwFile f : w.getFiles().values()) {
            if (FilenameUtils.isExtension(f.getProvisionedPath(), "bam")) {
                bamFileDirectories.put(FilenameUtils.removeExtension(f.getSourcePath()), FilenameUtils.getPath(f.getProvisionedPath()));
            }
        }
        for (SqwFile f : w.getFiles().values()) {
            //FIXME: bai.getProvisionedPath != bai.getOutputPath ...
            // at least with seqware 1.1.0, setting output path changes where the output file will be stored,
            // but the commonly used get provisioned path will return the incorrect path to the file
            if (FilenameUtils.isExtension(f.getProvisionedPath(), "bai")) {
                //check bai is in the same provision directory its corresponding bam is in
                Assert.assertEquals(FilenameUtils.getPath(f.getOutputPath()),
                        bamFileDirectories.get(FilenameUtils.removeExtension(f.getSourcePath())));
            }
        }

        //check number of parent nodes
        int expectedParentNodeCount = Math.max(1, StringUtils.split(w.getConfigs().get("chr_sizes"), ",").length);
        int actualParentNodeCount = 0;
        for (AbstractJob j : w.getWorkflow().getJobs()) {
            if (j.getParents().isEmpty()) {
                actualParentNodeCount++;
            }
        }
        Assert.assertEquals(actualParentNodeCount, expectedParentNodeCount);

        //view output files
        List<SqwFile> files = new LinkedList<>();
        for (AbstractJob j : w.getWorkflow().getJobs()) {
            files.addAll(j.getFiles());
        }
        for(SqwFile f : files){
            System.out.println(f.getProvisionedPath());
        }
    }

    private Map<String, String> getDefaultConfig() throws IOException {
        Properties p = new Properties();
        File defaults = new File(System.getProperty("bundleDirectory") + "/config/defaults.ini");
        p.load(FileUtils.openInputStream(defaults));
        return new HashMap<>((Map) p);
    }

}
