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
