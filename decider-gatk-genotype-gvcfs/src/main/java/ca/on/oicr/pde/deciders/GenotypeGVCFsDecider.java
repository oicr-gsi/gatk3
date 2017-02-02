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
package ca.on.oicr.pde.deciders;

import ca.on.oicr.pde.deciders.gatk3.AbstractGatkDecider;
import ca.on.oicr.pde.workflows.GATKGenotypeGVCFsWorkflow;
import java.util.*;

public class GenotypeGVCFsDecider extends AbstractGatkDecider<GATKGenotypeGVCFsWorkflow> {

    public GenotypeGVCFsDecider() {
        super(GATKGenotypeGVCFsWorkflow.class);
        defineArgument("stand-emit-conf", "Emission confidence threshold to pass to GATK. Default 1", false);
        defineArgument("stand-call-conf", "Calling confidence threshold to pass to GATK. Default 30.", false);
        defineArgument("dbsnp", "Specify the absolute path to the dbSNP vcf.", true);
    }

    @Override
    protected void configureDecider() {
        this.setMetaType(Arrays.asList("application/g-vcf-gz", "application/tbi"));
    }

    @Override
    protected void configureWorkflowRun(WorkflowRun wr, Set<FileAttributes> inputFileAttributes) throws AbstractGatkDecider.InvalidWorkflowRunException {
        if (options.has("stand-emit-conf")) {
            wr.addProperty("stand_emit_conf", getArgument("stand-emit-conf"));
        }
        if (options.has("stand-call-conf")) {
            wr.addProperty("stand_call_conf", getArgument("stand-call-conf"));
        }
        if (options.has("dbsnp")) {
            wr.addProperty("gatk_dbsnp_vcf", getArgument("dbsnp"));
        }
    }

    public static void main(String args[]) {

        List<String> params = new ArrayList<>();
        List<String> arguments = new ArrayList<>(Arrays.asList(args));
        params.add("--plugin");
        params.add(GenotypeGVCFsDecider.class.getCanonicalName());
        params.add("--");
        params.addAll(arguments);
        System.out.println("Parameters: " + Arrays.deepToString(params.toArray()));
        net.sourceforge.seqware.pipeline.runner.PluginRunner.main(params.toArray(new String[params.size()]));

    }
}
