package ca.on.oicr.workflows;

import ca.on.oicr.pde.workflows.WorkflowClient;
import com.google.common.base.Joiner;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import net.sourceforge.seqware.pipeline.workflowV2.AbstractWorkflowDataModel;
import net.sourceforge.seqware.pipeline.workflowV2.model.AbstractJob;
import org.apache.commons.io.FileUtils;
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

    @Test
    public void checkForNull() throws IOException, IllegalAccessException {
        Map<String, String> config = getDefaultConfig();
        config.put("identifier", "gatk.ex");
        config.put("bam_inputs", "/data/test/PCSI0022P.val.bam,/data/test/PCSI0022R.val.bam,/data/test/PCSI0022X.val.bam,/data/test/PCSI0022C.val.bam");
        WorkflowClient w = getWorkflowClientObject(config);
        validateWorkflowModel(w);
    }

    @Test
    public void checkJobNumber() throws IOException, IllegalAccessException {
        Map<String, String> config;
        WorkflowClient w;

        config = getDefaultConfig();
        config.put("identifier", "gatk.ex");
        config.put("bam_inputs", "/data/test/PCSI0022P.val.bam,/data/test/PCSI0022R.val.bam,/data/test/PCSI0022X.val.bam,/data/test/PCSI0022C.val.bam");
        w = getWorkflowClientObject(config);
        validateWorkflowModel(w);
        Assert.assertEquals(w.getWorkflow().getJobs().size(), 211);

//        config = getDefaultConfig();
//        config.put("identifier", "gatk.ex");
//        config.put("bam_inputs", "/data/test/PCSI0022P.val.bam,/data/test/PCSI0022R.val.bam,/data/test/PCSI0022X.val.bam,/data/test/PCSI0022C.val.bam");
//        config.put("chr_sizes", "");
//        w = getWorkflowClientObject(config);
//        validateWorkflowModel(w);
//        Assert.assertEquals(w.getWorkflow().getJobs().size(), 50);
    }

    private void validateWorkflowModel(WorkflowClient w) {
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
