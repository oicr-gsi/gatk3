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
import net.sourceforge.seqware.pipeline.workflowV2.model.Job;
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
        config.put("gatk_dbsnp_vcf", "/tmp/dbsnp.vcf");
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
        config.put("gatk_dbsnp_vcf", "/tmp/dbsnp.vcf");
        w = getWorkflowClientObject(config);
        validateWorkflow(w);

        Assert.assertEquals(w.getWorkflow().getJobs().size(), 206);
        Assert.assertEquals(GATK3WorkflowTest.this.getExpectedJobCount(4, 25, VariantCaller.UNIFIED_GENOTYPER, true), 206);

        config.put("variant_caller", "haplotype_caller");
        w = getWorkflowClientObject(config);
        validateWorkflow(w);

        Assert.assertEquals(w.getWorkflow().getJobs().size(), 179);
        Assert.assertEquals(GATK3WorkflowTest.this.getExpectedJobCount(4, 25, VariantCaller.HAPLOTYPE_CALLER, true), 179);

        config.put("variant_caller", "haplotype_caller,unified_genotyper");
        w = getWorkflowClientObject(config);
        validateWorkflow(w);

        Assert.assertEquals(w.getWorkflow().getJobs().size(), 233);
        Assert.assertEquals(getExpectedJobCount(4, 25, Sets.newHashSet(VariantCaller.HAPLOTYPE_CALLER, VariantCaller.UNIFIED_GENOTYPER), true), 233);

    }

    @Test(enabled = true)
    public void splitByChromosome() throws IOException, IllegalAccessException {

        int numInputFiles = 200;
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
        config.put("gatk_dbsnp_vcf", "/tmp/dbsnp.vcf");
//        config.put("chr_sizes", "chr1,chr2,chr3,chr4,chr5,chr6,chr7,chr8,chr9,chr10,"
//                + "chr11,chr12,chr13,chr14,chr15,chr16,chr17,chr18,chr19,chr20,"
//                + "chr21,chr22,chrX,chrY,chrM");

        int parallelismLevel = Arrays.asList(StringUtils.split(config.get("chr_sizes"), ",")).size();

        //haplotype caller and unified genotyper
        w = getWorkflowClientObject(config);
        validateWorkflow(w);
        Assert.assertEquals(w.getWorkflow().getJobs().size(),
                getExpectedJobCount(numInputFiles, parallelismLevel, Sets.newHashSet(VariantCaller.HAPLOTYPE_CALLER, VariantCaller.UNIFIED_GENOTYPER), true));

        //unified genotyper
        config.put("variant_caller", "unified_genotyper");
        w = getWorkflowClientObject(config);
        validateWorkflow(w);
        Assert.assertEquals(w.getWorkflow().getJobs().size(),
                getExpectedJobCount(numInputFiles, parallelismLevel, VariantCaller.UNIFIED_GENOTYPER, true));

        //haplotype caller
        config.put("variant_caller", "haplotype_caller");
        w = getWorkflowClientObject(config);
        validateWorkflow(w);
        Assert.assertEquals(w.getWorkflow().getJobs().size(),
                getExpectedJobCount(numInputFiles, parallelismLevel, Sets.newHashSet(VariantCaller.HAPLOTYPE_CALLER), true));

        //reset
        config = getDefaultConfig();
        config.put("identifier", "gatk.ex");
        config.put("input_files", StringUtils.join(inputFiles, ","));
        config.put("gatk_dbsnp_vcf", "/tmp/dbsnp.vcf");

        //no BQSR
        //haplotype caller and unified genotyper
        config.put("do_bqsr", "false");
        w = getWorkflowClientObject(config);
        validateWorkflow(w);
        Assert.assertEquals(w.getWorkflow().getJobs().size(),
                getExpectedJobCount(numInputFiles, parallelismLevel, Sets.newHashSet(VariantCaller.HAPLOTYPE_CALLER, VariantCaller.UNIFIED_GENOTYPER), false));

        //unified genotyper
        config.put("variant_caller", "unified_genotyper");
        config.put("do_bqsr", "false");
        w = getWorkflowClientObject(config);
        validateWorkflow(w);
        Assert.assertEquals(w.getWorkflow().getJobs().size(),
                getExpectedJobCount(numInputFiles, parallelismLevel, VariantCaller.UNIFIED_GENOTYPER, false));

        //haplotype caller
        config.put("variant_caller", "haplotype_caller");
        config.put("do_bqsr", "false");
        w = getWorkflowClientObject(config);
        validateWorkflow(w);
        Assert.assertEquals(w.getWorkflow().getJobs().size(),
                getExpectedJobCount(numInputFiles, parallelismLevel, Sets.newHashSet(VariantCaller.HAPLOTYPE_CALLER), false));
    }

    @Test(enabled = true)
    public void noSplit() throws IOException, IllegalAccessException {

        Map<String, String> config;
        GATK3Workflow w;
        config = getDefaultConfig();
        config.put("identifier", "gatk.ex");
        config.put("input_files", "/test/1.bam,/test/1.bai");
        config.put("chr_sizes", "");
        config.put("gatk_dbsnp_vcf", "/tmp/dbsnp.vcf");

        //unified gentotyper
        config.put("variant_caller", "unified_genotyper");
        w = getWorkflowClientObject(config);
        validateWorkflow(w);
        Assert.assertEquals(w.getWorkflow().getJobs().size(),
                GATK3WorkflowTest.this.getExpectedJobCount(1, 0, VariantCaller.UNIFIED_GENOTYPER, true));

        //haplotype caller
        config.put("variant_caller", "haplotype_caller");
        w = getWorkflowClientObject(config);
        validateWorkflow(w);
        Assert.assertEquals(w.getWorkflow().getJobs().size(),
                GATK3WorkflowTest.this.getExpectedJobCount(1, 0, VariantCaller.HAPLOTYPE_CALLER, true));

        //haplotype caller and unified genotyper
        config.put("variant_caller", "haplotype_caller,unified_genotyper");
        w = getWorkflowClientObject(config);
        validateWorkflow(w);
        Assert.assertEquals(w.getWorkflow().getJobs().size(),
                getExpectedJobCount(1, 0, Sets.newHashSet(VariantCaller.HAPLOTYPE_CALLER, VariantCaller.UNIFIED_GENOTYPER), true));
    }

    @Test
    public void extraParamSuccessTest() throws IOException, IllegalAccessException {
        Map<String, String> config;
        GATK3Workflow w;
        config = getDefaultConfig();
        config.put("identifier", "gatk.ex");
        config.put("input_files", "/test/1.bam,/test/1.bai");
        config.put("gatk_dbsnp_vcf", "/tmp/dbsnp.vcf");
        config.put("gatk_realigner_target_creator_params", "--param1 --param2");
        config.put("gatk_indel_realigner_params", "--param1 --param2");
        config.put("gatk_recalibrator_params", "--param1 --param2");
        config.put("gatk_analyze_covariates_params", "--param1 --param2");
        config.put("gatk_print_reads_params", "--param1 --param2");
        config.put("gatk_haplotype_caller_params", "--param1 --param2");
        config.put("gatk_unified_genotyper_params", "--param1 --param2");
        w = getWorkflowClientObject(config);
        validateWorkflow(w);
    }

    @Test(expectedExceptions = java.lang.AssertionError.class)
    public void extraParamFailureTest() throws IOException, IllegalAccessException {
        Map<String, String> config;
        GATK3Workflow w;
        config = getDefaultConfig();
        config.put("identifier", "gatk.ex");
        config.put("input_files", "/test/1.bam,/test/1.bai");
        config.put("gatk_dbsnp_vcf", "/tmp/dbsnp.vcf");
        config.put("gatk_realigner_target_creator_params", "--param1--param2");
        w = getWorkflowClientObject(config);
        validateWorkflow(w);
    }

    private int getExpectedJobCount(int numInputFiles, int parallelismLevel, VariantCaller vc, boolean doBQSR) {
        return getExpectedJobCount(numInputFiles, parallelismLevel, Sets.newHashSet(vc), doBQSR);
    }

    private int getExpectedJobCount(int numSamples, int parallelismLevel, Collection<VariantCaller> vcs, boolean doBQSR) {
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
                    variantCallingJobCount += parallelismLevel * 2; //indel UG + snv UG
                    mergeJobCount += 3;  //merge snv + merge indel + merge
                    sortCompressIndexJobCount += 1; //sort, compress, index
                    break;
            }
        }

        int numJobs = 0;
        numJobs += (parallelismLevel * 2); // for each chr_size interval: create targets + realign
        if (doBQSR) {
            numJobs += 2; //calculate base recalibration table + analyze covariates
            numJobs += (numSamples * parallelismLevel); //print reads/recalibrate each realigned bam
        }
        numJobs += variantCallingJobCount;
        numJobs += mergeJobCount;
        numJobs += sortCompressIndexJobCount;

        return numJobs;
    }

    private void validateWorkflow(GATK3Workflow w) {

        //check for null string
        for (AbstractJob j : w.getWorkflow().getJobs()) {

            String c = Joiner.on(" ").useForNull("null").join(j.getCommand().getArguments());

            //check for null string
            Assert.assertFalse(c.contains("null"), "Warning: command contains \"null\":\n" + c + "\n");

            // check for missing spaces
            Assert.assertFalse(c.matches("(.*)[^ ]--(.*)"));
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
        for (AbstractJob j : w.getWorkflow().getJobs()) {
            for (SqwFile f : j.getFiles()) {
                if (f.isOutput()) {
                    System.out.println(f.getProvisionedPath());
                }
            }
        }
    }

    private Map<String, String> getDefaultConfig() throws IOException {
        Properties p = new Properties();
        File defaults = new File(System.getProperty("bundleDirectory") + "/config/defaults.ini");
        p.load(FileUtils.openInputStream(defaults));
        return new HashMap<>((Map) p);
    }

}
