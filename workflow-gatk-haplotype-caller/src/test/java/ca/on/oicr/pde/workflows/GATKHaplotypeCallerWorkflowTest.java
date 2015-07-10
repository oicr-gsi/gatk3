package ca.on.oicr.pde.workflows;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import net.sourceforge.seqware.common.util.maptools.MapTools;
import org.apache.commons.io.FileUtils;
import org.testng.Assert;
import org.testng.annotations.Test;

public class GATKHaplotypeCallerWorkflowTest {

    public GATKHaplotypeCallerWorkflowTest() {

    }

//    private GATKHaplotypeCallerWorkflow getWorkflow() throws IOException {
//        File defaultIniFile = new File(System.getProperty("bundleDirectory") + "/config/defaults.ini");
//        String defaultIniFileContents = FileUtils.readFileToString(defaultIniFile);
//        
//        GATKHaplotypeCallerWorkflow wf = new GATKHaplotypeCallerWorkflow();
//        wf.setConfigs(MapTools.iniString2Map(defaultIniFileContents));
//        
//        return wf;
//    }
//
//    @Test
//    public void testInit() throws IOException {
//
//        Map<String, String> config = new HashMap<String, String>();
//        config.put("greeting", "new greeting");
//
//        GATKHaplotypeCallerWorkflow wf = getWorkflow();
//        wf.getConfigs().putAll(config);
//        wf.setupDirectory();
//
//        Assert.assertEquals(wf.getProperty("greeting"), "new greeting");
//        Assert.assertEquals(wf.getProperty("output_prefix"), "./");
//        Assert.assertEquals(wf.getProperty("manual_output"), "false");
//    }

}
