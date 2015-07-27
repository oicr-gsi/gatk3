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

import java.util.LinkedList;
import java.util.List;
import net.sourceforge.seqware.pipeline.workflowV2.model.Job;
import org.apache.commons.lang3.tuple.Pair;
import org.testng.Assert;
import org.testng.annotations.Test;

public class GATKGenotypeGVCFsWorkflowTest {

    public GATKGenotypeGVCFsWorkflowTest() {

    }

    @Test
    public void batchGvcfsTest() {
        int maxBins = 200;
        int maxFilesPerBin = 50;

        List<String> inputFiles = new LinkedList<>();
        for (int nInputFiles = 1; nInputFiles <= 400; nInputFiles++) {
            inputFiles.add("/tmp/" + nInputFiles + ".g.vcf.gz");

            GATKGenotypeGVCFsWorkflow wf = new GATKGenotypeGVCFsWorkflow();
            List<Pair<String, Job>> combinedInputFiles = wf.batchGVCFs(inputFiles, maxBins, maxFilesPerBin,
                    "", Integer.valueOf("0"), Integer.valueOf("0"), "", "", "", "", "", "");

            if (nInputFiles >= maxBins) {
                Assert.assertEquals(combinedInputFiles.size(), maxBins);
            } else {
                Assert.assertEquals(combinedInputFiles.size(), nInputFiles);
            }
        }
    }

}
