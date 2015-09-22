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
import ca.on.oicr.pde.workflows.GATK3Workflow;
import com.google.common.collect.Iterables;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;

public class Gatk3Decider extends AbstractGatkDecider<GATK3Workflow> {

    private String rsconfigXmlPath = "/.mounts/labs/PDE/data/rsconfig.xml";
    private Rsconfig rsconfig;

    public Gatk3Decider() {
        super(GATK3Workflow.class);

        //settings
        defineArgument("chr-sizes", "Comma separated list of chromosome intervals used to parallelize indel realigning and variant calling. Default: By chromosome", false);
        defineArgument("interval-padding", "Amount of padding to add to each interval (chr-sizes and interval-file determined by decider) in bp. Default: 100", false);
        defineArgument("stand-emit-conf", "Emission confidence threshold to pass to GATK. Default 1", false);
        defineArgument("stand-call-conf", "Calling confidence threshold to pass to GATK. Default 30.", false);
        parser.accepts("disable-bqsr", "Disable BQSR (BaseRecalibrator + PrintReads steps) and pass indel realigned BAMs directly to variant calling.");
        defineArgument("downsampling", "Set whether or not the variant caller should downsample the reads. Default: false for TS, true for everything else", false);
        defineArgument("rsconfig-file", "Specify location of .xml file which should be used to configure references, "
                + "will be used if resequencing-type is different from the default."
                + "Default: " + rsconfigXmlPath, false);
        defineArgument("dbsnp", "Specify the absolute path to the dbSNP vcf.", true);
    }

    @Override
    protected void configureDecider() {
        setMetaType(Arrays.asList("application/bam", "application/bam-index"));

        //rsconfig
        if (options.has("rsconfig-file")) {
            if (!options.hasArgument("rsconfig-file")) {
                throw new RuntimeException("--rsconfig-file requires a file argument.");
            }
            rsconfigXmlPath = options.valueOf("rsconfig-file").toString();
        }
        File rsconfigFile = new File(rsconfigXmlPath);
        if (!rsconfigFile.exists() || !rsconfigFile.canRead()) {
            throw new RuntimeException("The rsconfig-file is not accessible.");
        }
        try {
            rsconfig = new Rsconfig(rsconfigFile);
        } catch (ParserConfigurationException | SAXException | IOException | Rsconfig.InvalidFileFormatException e) {
            throw new RuntimeException("Rsconfig file did not load properly, exeception stack trace:\n" + Arrays.toString(e.getStackTrace()));
        }

        //check user defined input files
        if (options.hasArgument("check-files-exist")) {
            if (!Files.exists(Paths.get(getArgument("dbsnp"))) || !Files.isReadable(Paths.get(getArgument("dbsnp")))) {
                throw new RuntimeException("The dbsnp file [ " + getArgument("dbsnp") + "] is not accessible.");
            }
        }
    }

    @Override
    protected void configureWorkflowRun(WorkflowRun wr, Set<FileAttributes> inputFileAttributes) throws InvalidWorkflowRunException {

        String intervalFileChromosomes = null;
        Set<String> intervalFiles = new HashSet<>();
        for (FileAttributes fa : inputFileAttributes) {
            String groupTemplateType = fa.getLimsValue(Lims.LIBRARY_TEMPLATE_TYPE);
            String groupResequencingType = fa.getLimsValue(Lims.TARGETED_RESEQUENCING);

            String intervalFile = rsconfig.get(groupTemplateType, groupResequencingType, "interval_file");
            if (intervalFile == null) {
                throw new InvalidWorkflowRunException(String.format("Template type = %s and resequencing type = %s not found in rsconfig.xml",
                        groupTemplateType, groupResequencingType));
            } else {
                intervalFiles.add(intervalFile);
            }

            intervalFileChromosomes = rsconfig.get(groupTemplateType, groupResequencingType, "chromosomes");
        }

        if ("WG".equals(templateType)) {
            // do not use interval file
            wr.addProperty("chr_sizes", "chr1,chr2,chr3,chr4,chr5,chr6,chr7,chr8,chr9,chr10,chr11,chr12,chr13,chr14,chr15,chr16,chr17,chr18,chr19,chr20,chr21,chr22,chrX,chrY,chrM");
        } else {
            if (intervalFiles.size() == 1) {
                wr.addProperty("interval_files", Iterables.getOnlyElement(intervalFiles));
            } else {
                throw new InvalidWorkflowRunException(String.format("Found [%s] interval files, expected one.", intervalFiles.size()));
            }

            if (intervalFileChromosomes != null) {
                wr.addProperty("chr_sizes", intervalFileChromosomes);
            }
        }

        wr.addProperty("gatk_dbsnp_vcf", getArgument("dbsnp"));

        if (options.has("chr-sizes")) {
            wr.addProperty("chr_sizes", getArgument("chr-sizes"));
        }

        if (options.has("interval-padding")) {
            wr.addProperty("interval_padding", getArgument("interval-padding"));
        }

        wr.addProperty("stand_emit_conf", getArgument("stand-emit-conf"), "1");

        wr.addProperty("stand_call_conf", getArgument("stand-call-conf"), "30");

        if (options.has("disable-bqsr")) {
            wr.addProperty("do_bqsr", "false");
        }

        if (options.has("downsampling")) {
            if (getArgument("downsampling").equalsIgnoreCase("false")) {
                wr.addProperty("downsampling_type", "NONE");
            } else if (getArgument("downsampling").equalsIgnoreCase("true")) {
                //do nothing, downsampling is performed by default
            } else {
                throw new RuntimeException("--downsampling parameter expects true/false.");
            }
        } else {
            switch (templateType) {
                case "WG":
                    //do nothing, downsampling is performed by default
                    break;
                case "EX":
                    //do nothing, downsampling is performed by default
                    break;
                case "TS":
                    wr.addProperty("downsampling_type", "NONE");  //TS, do not downsample
                    break;
                default:
                    throw new RuntimeException("Unsupported template type = [" + templateType + "]");
            }
        }
    }

    public static void main(String args[]) {

        List<String> params = new ArrayList<>();
        List<String> arguments = new ArrayList<>(Arrays.asList(args));
        params.add("--plugin");
        params.add(Gatk3Decider.class.getCanonicalName());
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
