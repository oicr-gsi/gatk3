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
