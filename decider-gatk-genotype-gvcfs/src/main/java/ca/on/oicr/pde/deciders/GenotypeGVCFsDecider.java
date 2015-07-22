package ca.on.oicr.pde.deciders;

import ca.on.oicr.pde.deciders.gatk3.AbstractGatkDecider;
import ca.on.oicr.pde.workflows.GATKGenotypeGVCFsWorkflow;
import java.util.*;

public class GenotypeGVCFsDecider extends AbstractGatkDecider<GATKGenotypeGVCFsWorkflow> {

    public GenotypeGVCFsDecider() {
        super(GATKGenotypeGVCFsWorkflow.class);
    }

    @Override
    protected void configureDecider() {
        this.setMetaType(Arrays.asList("application/g-vcf-gz", "application/tbi"));
    }

    @Override
    protected void configureWorkflowRun(WorkflowRun wr, Set<FileAttributes> inputFileAttributes) throws AbstractGatkDecider.InvalidWorkflowRunException {

    }

    public static void main(String args[]) {

        List<String> params = new ArrayList<>();
        List<String> arguments = new ArrayList<>(Arrays.asList(args));
        params.add("--plugin");
        params.add(GenotypeGVCFsDecider.class.getCanonicalName());
        if (arguments.contains("--verbose")) {
            params.add("--verbose");
            arguments.remove("--verbose");
        }
        params.add("--");
        params.addAll(arguments);
        System.out.println("Parameters: " + Arrays.deepToString(params.toArray()));
        net.sourceforge.seqware.pipeline.runner.PluginRunner.main(params.toArray(new String[params.size()]));

    }
}
