package ca.on.oicr.workflows;

import ca.on.oicr.pde.workflows.WorkflowClient;
import com.google.common.base.Joiner;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import net.sourceforge.seqware.pipeline.workflowV2.AbstractWorkflowDataModel;
import net.sourceforge.seqware.pipeline.workflowV2.model.AbstractJob;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.powermock.api.support.membermodification.MemberModifier;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 *
 * @author mlaszloffy
 */
public class WorkflowClientTest {

    public WorkflowClientTest() {
    }

    private WorkflowClient getWorkflowClientObject(Map<String, String> config) throws IOException, IllegalAccessException {
        WorkflowClient w = new WorkflowClient();
        MemberModifier.field(WorkflowClient.class, "basedir").set(w, System.getProperty("bundleDirectory"));

        w.setConfigs(config);
        buildWorkflowModel(w);

        return w;
    }

    @Test(expectedExceptions = java.lang.AssertionError.class)
    public void checkForNull() throws IOException, IllegalAccessException {
        Map<String, String> config = getDefaultConfig();
        config.put("identifier", "null");
        config.put("bam_inputs", "/data/test/PCSI0022P.val.bam,/data/test/PCSI0022R.val.bam,/data/test/PCSI0022X.val.bam,/data/test/PCSI0022C.val.bam");
        WorkflowClient w = getWorkflowClientObject(config);
        validateWorkflow(w);
    }

    @Test
    public void checkJobNumber() throws IOException, IllegalAccessException {
        Map<String, String> config;
        WorkflowClient w;

        config = getDefaultConfig();
        config.put("identifier", "gatk.ex");
        config.put("bam_inputs", "/data/test/PCSI0022P.val.bam,/data/test/PCSI0022R.val.bam,/data/test/PCSI0022X.val.bam,/data/test/PCSI0022C.val.bam");
        w = getWorkflowClientObject(config);
        validateWorkflow(w);

        Assert.assertEquals(w.getWorkflow().getJobs().size(), 209);
        Assert.assertEquals(getNumberOfExpectedJobs(4, 25), 209);
    }

    @Test
    public void splitByChromosome() throws IOException, IllegalAccessException {

        int numInputFiles = 500;
        List<String> inputFiles = new LinkedList<>();
        for (int i = 0; i < numInputFiles; i++) {
            inputFiles.add(RandomStringUtils.random(5) + ".bam");
        }

        Map<String, String> config;
        WorkflowClient w;
        config = getDefaultConfig();
        config.put("identifier", "gatk.ex");
        config.put("bam_inputs", StringUtils.join(inputFiles, ","));
//        config.put("chr_sizes", "chr1,chr2,chr3,chr4,chr5,chr6,chr7,chr8,chr9,chr10,"
//                + "chr11,chr12,chr13,chr14,chr15,chr16,chr17,chr18,chr19,chr20,"
//                + "chr21,chr22,chrX,chrY,chrM");

        int parallelismLevel = Arrays.asList(StringUtils.split(config.get("chr_sizes"), ",")).size();

        w = getWorkflowClientObject(config);
        validateWorkflow(w);

        Assert.assertEquals(w.getWorkflow().getJobs().size(), getNumberOfExpectedJobs(numInputFiles, parallelismLevel));
    }

    @Test
    public void noSplit() throws IOException, IllegalAccessException {

        Map<String, String> config;
        WorkflowClient w;
        config = getDefaultConfig();
        config.put("identifier", "gatk.ex");
        config.put("bam_inputs", "1.bam");
        config.put("chr_sizes", "");

        w = getWorkflowClientObject(config);
        validateWorkflow(w);

        Assert.assertEquals(w.getWorkflow().getJobs().size(), getNumberOfExpectedJobs(1, 0));
    }

    private int getNumberOfExpectedJobs(int numInputFiles, int parallelismLevel) {
        parallelismLevel = Math.max(parallelismLevel, 1); //even if chr_sizes is empty, the workflow will run

        return numInputFiles //reorder
                + (parallelismLevel * 3) // for each chr_size interval, create targets + realign + sort
                + 1 //calculate base recalibration table
                + (parallelismLevel * 5) // for each realigned bam (parallelism level), recalibrate + indel UG + filter + snv UG + filter
                + 4; //merge snv + merge indel + merge + SortAnnotateCompress
    }

    private void validateWorkflow(WorkflowClient w) {
        for (AbstractJob j : w.getWorkflow().getJobs()) {
            String c = Joiner.on(" ").join(j.getCommand().getArguments());
            Assert.assertFalse(c.contains("null"), "Warning: command contains \"null\":\n" + c + "\n");
        }
    }

    private Map<String, String> getDefaultConfig() throws IOException {
        Properties p = new Properties();
        File defaults = new File(System.getProperty("bundleDirectory") + "/config/defaults.ini");
        p.load(FileUtils.openInputStream(defaults));
        return new HashMap<>((Map) p);
    }

    private void buildWorkflowModel(AbstractWorkflowDataModel workflowObject) {
        AbstractWorkflowDataModel.prepare(workflowObject);
        workflowObject.setupDirectory();
        workflowObject.setupFiles();
        workflowObject.setupWorkflow();
        workflowObject.setupEnvironment();
        workflowObject.buildWorkflow();
        workflowObject.wrapup();
    }
}
