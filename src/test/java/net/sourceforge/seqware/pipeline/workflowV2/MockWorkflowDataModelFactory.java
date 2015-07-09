package net.sourceforge.seqware.pipeline.workflowV2;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import org.apache.commons.io.FileUtils;
import org.powermock.api.support.membermodification.MemberModifier;

/**
 *
 * @author mlaszloffy
 */
public class MockWorkflowDataModelFactory {

    // Mocking of net.sourceforge.seqware.pipeline.workflowV2.WorkflowDataModelFactory.getWorkflowDataModel()
    public static void buildWorkflowModel(String bundleDirectory, AbstractWorkflowDataModel w) throws IllegalAccessException {

        //Need to mock basedir as it is retrieved from the WS
        MemberModifier.field(AbstractWorkflowDataModel.class, "basedir").set(w, bundleDirectory);

        //Get the data from metadata.xml
        Map<String, String> metaInfo = WorkflowV2Utility.parseMetaInfo(FileUtils.getFile(bundleDirectory));

        //Build the workflow model
        AbstractWorkflowDataModel.prepare(w);
        w.setMetadata_output_file_prefix(w.getConfigs().get("output_prefix"));
        w.setMetadata_output_dir(w.getConfigs().get("output_dir"));
        w.setName(metaInfo.get("name"));
        w.setVersion(metaInfo.get("workflow_version"));
        w.setRandom("" + new Random(System.currentTimeMillis()).nextInt(100000000)); //seqware's random int method
        w.setupDirectory();
        w.setupFiles();
        w.setupWorkflow();
        w.setupEnvironment();
        w.buildWorkflow();
        w.wrapup();
    }
}
