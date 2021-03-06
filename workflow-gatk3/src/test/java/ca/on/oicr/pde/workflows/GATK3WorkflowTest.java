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

import ca.on.oicr.pde.testing.workflow.DryRun;
import ca.on.oicr.pde.testing.workflow.TestDefinition;
import ca.on.oicr.pde.workflows.GATK3Workflow.VariantCaller;
import static ca.on.oicr.pde.workflows.GATK3Workflow.VariantCaller.HAPLOTYPE_CALLER;
import static ca.on.oicr.pde.workflows.GATK3Workflow.VariantCaller.UNIFIED_GENOTYPER;
import com.google.common.base.Joiner;
import com.google.common.collect.Sets;
import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.sourceforge.seqware.pipeline.workflowV2.AbstractWorkflowDataModel;
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

    @Test
    public void dryRunRegressionTests() throws Exception {
        TestDefinition td = TestDefinition.buildFromJson(FileUtils.readFileToString(new File("src/test/resources/developmentTests.json")));

        for (TestDefinition.Test t : td.getTests()) {
            //disable calculating chr_sizes from interval_files
            Map<String, String> params = new HashMap<>(t.getParameters());
            if (!params.containsKey("chr_sizes")) {
                params.put("chr_sizes", "");
            }

            DryRun d = new DryRun(System.getProperty("bundleDirectory"), params, GATK3Workflow.class);
            d.buildWorkflowModel();
            d.validateWorkflow();
        }
    }

    @Test(expectedExceptions = java.lang.AssertionError.class)
    public void checkForNull() throws Exception {
        Map<String, String> config = new HashMap<>();
        config.put("identifier", "null");
        config.put("input_files", "/data/test/PCSI0022.val.bam,"
                + "/data/test/PCSI0022.val.bai");
        config.put("gatk_dbsnp_vcf", "/tmp/dbsnp.vcf");
        AbstractWorkflowDataModel w = getWorkflowClientObject(config);
        validateWorkflow(w);
    }

    @Test(enabled = true)
    public void checkJobNumber() throws Exception {
        Map<String, String> config = new HashMap<>();
        config.put("identifier", "gatk.ex");
        config.put("input_files", "/data/test/PCSI0022.val.bam,"
                + "/data/test/PCSI0022.val.bai");
        config.put("variant_caller", "Unified_GeNoTyPeR");
        config.put("gatk_dbsnp_vcf", "/tmp/dbsnp.vcf");
        config.put("chr_sizes", "chr1,chr2,chr3,chr4,chr5,chr6,chr7,chr8,chr9,chr10,chr11,chr12,chr13,chr14,chr15,chr16,chr17,chr18,chr19,chr20,chr21,chr22,chrX,chrY,chrM");
        AbstractWorkflowDataModel w = getWorkflowClientObject(config);
        validateWorkflow(w);

        Assert.assertEquals(w.getWorkflow().getJobs().size(), 54);
        Assert.assertEquals(GATK3WorkflowTest.this.getExpectedJobCount(4, 25, VariantCaller.UNIFIED_GENOTYPER), 54);

        config.put("variant_caller", "haplotype_caller");
        w = getWorkflowClientObject(config);
        validateWorkflow(w);

        Assert.assertEquals(w.getWorkflow().getJobs().size(), 27);
        Assert.assertEquals(GATK3WorkflowTest.this.getExpectedJobCount(4, 25, VariantCaller.HAPLOTYPE_CALLER), 27);

        config.put("variant_caller", "haplotype_caller,unified_genotyper");
        w = getWorkflowClientObject(config);
        validateWorkflow(w);

        Assert.assertEquals(w.getWorkflow().getJobs().size(), 81);
        Assert.assertEquals(getExpectedJobCount(4, 25, Sets.newHashSet(VariantCaller.HAPLOTYPE_CALLER, VariantCaller.UNIFIED_GENOTYPER)), 81);

    }

    @Test(enabled = true)
    public void splitByChromosome() throws Exception {

        int numInputFiles = 1;
        List<String> inputFiles = new LinkedList<>();
        for (int i = 0; i < numInputFiles; i++) {
            String fileName = RandomStringUtils.random(5, true, true);
            inputFiles.add(fileName + ".bam");
            inputFiles.add(fileName + ".bai");
        }

        Map<String, String> config = new HashMap<>();
        config.put("identifier", "gatk.ex");
        config.put("input_files", StringUtils.join(inputFiles, ","));
        config.put("gatk_dbsnp_vcf", "/tmp/dbsnp.vcf");
        config.put("chr_sizes", "chr1,chr2,chr3,chr4,chr5,chr6,chr7,chr8,chr9,chr10,chr11,chr12,chr13,chr14,chr15,chr16,chr17,chr18,chr19,chr20,chr21,chr22,chrX,chrY,chrM");
//        config.put("chr_sizes", "chr1,chr2,chr3,chr4,chr5,chr6,chr7,chr8,chr9,chr10,"
//                + "chr11,chr12,chr13,chr14,chr15,chr16,chr17,chr18,chr19,chr20,"
//                + "chr21,chr22,chrX,chrY,chrM");

        int parallelismLevel = Arrays.asList(StringUtils.split(config.get("chr_sizes"), ",")).size();

        //haplotype caller
        AbstractWorkflowDataModel w = getWorkflowClientObject(config);
        validateWorkflow(w);
        Assert.assertEquals(w.getWorkflow().getJobs().size(),
                getExpectedJobCount(numInputFiles, parallelismLevel, Sets.newHashSet(VariantCaller.HAPLOTYPE_CALLER)));

        //unified genotyper
        config.put("variant_caller", "unified_genotyper");
        w = getWorkflowClientObject(config);
        validateWorkflow(w);
        Assert.assertEquals(w.getWorkflow().getJobs().size(),
                getExpectedJobCount(numInputFiles, parallelismLevel, VariantCaller.UNIFIED_GENOTYPER));

        //haplotype caller and unified genotyper
        config.put("variant_caller", "haplotype_caller,unified_genotyper");
        w = getWorkflowClientObject(config);
        validateWorkflow(w);
        Assert.assertEquals(w.getWorkflow().getJobs().size(),
                getExpectedJobCount(numInputFiles, parallelismLevel, Sets.newHashSet(VariantCaller.HAPLOTYPE_CALLER, VariantCaller.UNIFIED_GENOTYPER)));

        //reset
        config = new HashMap<>();
        config.put("identifier", "gatk.ex");
        config.put("input_files", StringUtils.join(inputFiles, ","));
        config.put("gatk_dbsnp_vcf", "/tmp/dbsnp.vcf");
        config.put("chr_sizes", "chr1,chr2,chr3,chr4,chr5,chr6,chr7,chr8,chr9,chr10,chr11,chr12,chr13,chr14,chr15,chr16,chr17,chr18,chr19,chr20,chr21,chr22,chrX,chrY,chrM");

        //no BQSR
        //haplotype caller and unified genotyper
        config.put("do_bqsr", "false");
        w = getWorkflowClientObject(config);
        validateWorkflow(w);
        Assert.assertEquals(w.getWorkflow().getJobs().size(),
                getExpectedJobCount(numInputFiles, parallelismLevel, Sets.newHashSet(VariantCaller.HAPLOTYPE_CALLER)));

        //unified genotyper
        config.put("variant_caller", "unified_genotyper");
        config.put("do_bqsr", "false");
        w = getWorkflowClientObject(config);
        validateWorkflow(w);
        Assert.assertEquals(w.getWorkflow().getJobs().size(),
                getExpectedJobCount(numInputFiles, parallelismLevel, VariantCaller.UNIFIED_GENOTYPER));

        //haplotype caller and unified genotyper
        config.put("variant_caller", "haplotype_caller,unified_genotyper");
        config.put("do_bqsr", "false");
        w = getWorkflowClientObject(config);
        validateWorkflow(w);
        Assert.assertEquals(w.getWorkflow().getJobs().size(),
                getExpectedJobCount(numInputFiles, parallelismLevel, Sets.newHashSet(VariantCaller.HAPLOTYPE_CALLER, VariantCaller.UNIFIED_GENOTYPER)));
    }

    @Test(enabled = true)
    public void noSplit() throws Exception {

        Map<String, String> config = new HashMap<>();
        config.put("identifier", "gatk.ex");
        config.put("input_files", "/test/1.bam,/test/1.bai");
        config.put("chr_sizes", "");
        config.put("gatk_dbsnp_vcf", "/tmp/dbsnp.vcf");

        //unified gentotyper
        config.put("variant_caller", "unified_genotyper");
        AbstractWorkflowDataModel w = getWorkflowClientObject(config);
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

    @Test
    public void extraParamSuccessTest() throws Exception {
        Map<String, String> config = new HashMap<>();
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
        config.put("chr_sizes", "chr1,chr2,chr3,chr4,chr5,chr6,chr7,chr8,chr9,chr10,chr11,chr12,chr13,chr14,chr15,chr16,chr17,chr18,chr19,chr20,chr21,chr22,chrX,chrY,chrM");
        AbstractWorkflowDataModel w = getWorkflowClientObject(config);
        validateWorkflow(w);
    }

    @Test(expectedExceptions = java.lang.AssertionError.class)
    public void extraParamFailureTest() throws Exception {
        Map<String, String> config = new HashMap<>();
        config.put("identifier", "gatk.ex");
        config.put("input_files", "/test/1.bam,/test/1.bai");
        config.put("gatk_dbsnp_vcf", "/tmp/dbsnp.vcf");
        config.put("gatk_haplotype_caller_params", "--param1--param2");
        AbstractWorkflowDataModel w = getWorkflowClientObject(config);
        validateWorkflow(w);
    }

    private int getExpectedJobCount(int numInputFiles, int parallelismLevel, VariantCaller vc) {
        return getExpectedJobCount(numInputFiles, parallelismLevel, Sets.newHashSet(vc));
    }

    private int getExpectedJobCount(int numSamples, int parallelismLevel, Collection<VariantCaller> vcs) {
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
        numJobs += variantCallingJobCount;
        numJobs += mergeJobCount;
        numJobs += sortCompressIndexJobCount;

        return numJobs;
    }

    private void validateWorkflow(AbstractWorkflowDataModel w) {

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
        Set<VariantCaller> vc = new HashSet<>();
        for (String s : StringUtils.split(w.getConfigs().get("variant_caller"), ",")) {
            vc.add(VariantCaller.valueOf(StringUtils.upperCase(s)));
        }
        int parallelism = Math.max(1, StringUtils.split(w.getConfigs().get("chr_sizes"), ",").length);
        int expectedParentNodeCount = parallelism * (vc.contains(HAPLOTYPE_CALLER) ? 1 : 0)
                + parallelism * (vc.contains(UNIFIED_GENOTYPER) ? 2 : 0); //ug indels and ug snvs
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

    private AbstractWorkflowDataModel getWorkflowClientObject(Map<String, String> config) throws Exception {
        DryRun d = new DryRun(System.getProperty("bundleDirectory"), config, GATK3Workflow.class);
        AbstractWorkflowDataModel w = d.buildWorkflowModel();
        d.validateWorkflow();
        return w;
    }

}
