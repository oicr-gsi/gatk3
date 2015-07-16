package ca.on.oicr.pde.deciders;

import ca.on.oicr.pde.deciders.gatk3.AbstractGatkDecider;
import ca.on.oicr.pde.workflows.GATK3Workflow;
import com.google.common.collect.Iterables;
import java.io.File;
import java.io.IOException;
import java.util.*;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;

public class Gatk3Decider extends AbstractGatkDecider<GATK3Workflow>  {

    private String rsconfigXmlPath = "/.mounts/labs/PDE/data/rsconfig.xml";
    private Rsconfig rsconfig;

    public Gatk3Decider() {
        super(GATK3Workflow.class);
    }

    @Override
    protected void configureDecider() {
        setMetaType(Arrays.asList("application/bam", "application/bam-index"));

        //settings
        defineArgument("stand-emit-conf", "Emission confidence threshold to pass to GATK. Default 1", false);
        defineArgument("stand-call-conf", "Calling confidence threshold to pass to GATK. Default 30.", false);
        parser.accepts("disable-bqsr", "Disable BQSR (BaseRecalibrator + PrintReads steps) and pass indel realigned BAMs directly to variant calling.");
        defineArgument("downsampling", "Set whether or not the variant caller should downsample the reads. Default: false for TS, true for everything else", false);
        defineArgument("rsconfig-file", "Specify location of .xml file which should be used to configure references, "
                + "will be used if resequencing-type is different from the default."
                + "Default: " + rsconfigXmlPath, false);

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
    }

    @Override
    protected void configureWorkflowRun(WorkflowRun wr, Set<FileAttributes> inputFileAttributes) throws InvalidWorkflowRunException {

        Set<String> groupTemplateType = new HashSet<>();
        Set<String> groupResequencingType = new HashSet<>();
        for (FileAttributes fa : inputFileAttributes) {
            groupTemplateType.add(fa.getLimsValue(Lims.LIBRARY_TEMPLATE_TYPE));
            groupResequencingType.add(fa.getLimsValue(Lims.TARGETED_RESEQUENCING));
        }
        if (groupTemplateType.size() == 1 && groupResequencingType.size() == 1) {
            String file = rsconfig.get(Iterables.getOnlyElement(groupTemplateType), Iterables.getOnlyElement(groupResequencingType), "interval_file");
            if (file == null) {
                throw new InvalidWorkflowRunException(String.format("Template type = %s and resequencing type = %s not found in rsconfig.xml",
                        groupTemplateType, groupResequencingType));
            }
            if (!"WG".equals(templateType)) {
                wr.addProperty("interval_files", file);
            }
        } else {
            throw new InvalidWorkflowRunException(String.format("Unable to determine single interval file for template type = %s and resequencing type = %s.",
                    groupTemplateType, groupResequencingType));
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
