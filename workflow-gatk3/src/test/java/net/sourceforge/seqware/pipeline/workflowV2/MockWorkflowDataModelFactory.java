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
