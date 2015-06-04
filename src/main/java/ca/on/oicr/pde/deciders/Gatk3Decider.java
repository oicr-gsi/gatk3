package ca.on.oicr.pde.deciders;

import ca.on.oicr.pde.workflows.GATK3Workflow;
import com.google.common.collect.Iterables;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import javax.xml.parsers.ParserConfigurationException;
import net.sourceforge.seqware.common.hibernate.FindAllTheFiles.Header;
import net.sourceforge.seqware.common.model.WorkflowParam;
import net.sourceforge.seqware.common.module.ReturnValue;
import net.sourceforge.seqware.common.util.Log;
import net.sourceforge.seqware.pipeline.workflowV2.AbstractWorkflowDataModel;
import org.xml.sax.SAXException;

public class Gatk3Decider extends OicrDecider {

    private final Map<String, BeSmall> fileSwaToSmall;
    private String templateType = null;
    private String resequencingType = null;
    private List<String> tissueTypes = null;
    private List<String> tissueOrigins = null;
    private final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");
    private String workflowName;
    private String workflowVersion;
    private final Map<String, WorkflowRun> workflowRuns = new HashMap<>();

    String rsconfigXmlPath = "/.mounts/labs/PDE/data/rsconfig.xml";
    private Rsconfig rsconfig;

    public Gatk3Decider() {
        super();
        files = new HashMap<>();
        fileSwaToSmall = new HashMap<>();

        //settings
        defineArgument("stand-emit-conf", "Emission confidence threshold to pass to GATK. Default 1", false);
        defineArgument("stand-call-conf", "Calling confidence threshold to pass to GATK. Default 30.", false);
        defineArgument("chr-sizes", "Comma separated list of chromosome intervals used to parallelize indel realigning and variant calling. Default: By chromosome", false);
        parser.accepts("disable-bqsr", "Disable BQSR (BaseRecalibrator + PrintReads steps) and pass indel realigned BAMs directly to variant calling.");
        defineArgument("interval-padding", "Amount of padding to add to each interval (chr-sizes and interval-file determined by decider) in bp. Default: 100", false);
        defineArgument("id", "Override final filename prefix (eg. ID_123.haplotype_caller.raw.vcf.gz, ID_123.unified_genotyper.raw.vcf.gz). Default: gatk3", false);
        defineArgument("downsampling", "Set whether or not the variant caller should downsample the reads. Default: false for TS, true for everything else", false);
        defineArgument("rsconfig-file", "Specify location of .xml file which should be used to configure references, "
                + "will be used if resequencing-type is different from the default."
                + "Default: " + rsconfigXmlPath, false);

        //mandatory filters
        defineArgument("library-template-type", "Restrict the processing to samples of a particular template type, e.g. WG, EX, TS.", true);

        //optional filters
        defineArgument("tissue-type", "Restrict the processing to samples of particular tissue types, "
                + "e.g. P, R, X, C. Multiple values can be comma-separated. Default: no restriction", false);
        defineArgument("tissue-origin", "Restrict the processing to samples of particular tissue origin, "
                + "e.g. Ly, Pa, Pr. Multiple values can be comma-separated. Default: no restriction", false);
        defineArgument("resequencing-type", "Restrict the processing to samples of a particular resequencing type", false);

    }

    @Override
    public ReturnValue init() {
        setMetaType(Arrays.asList("application/bam", "application/bam-index"));
        setGroupBy(Group.FILE, false);

        templateType = getArgument("library-template-type");

        if (!getArgument("tissue-type").isEmpty()) {
            tissueTypes = Arrays.asList(getArgument("tissue-type").split(","));
        }

        if (!getArgument("tissue-origin").isEmpty()) {
            tissueOrigins = Arrays.asList(getArgument("tissue-origin").split(","));
        }

        if (!getArgument("resequencing-type").isEmpty()) {
            resequencingType = getArgument("resequencing-type");
        }

        //load decider properties file
        Properties p = new Properties();
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("decider.properties")) {
            p.load(is);
        } catch (IOException ioe) {
            Log.error("Unable to load decider.properties");
            throw new RuntimeException(ioe);
        }
        workflowName = p.getProperty("workflow-name");
        workflowVersion = p.getProperty("workflow-version");

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

        return super.init();
    }

    @Override
    public Map<String, List<ReturnValue>> separateFiles(List<ReturnValue> vals, String groupBy) {
        Map<String, ReturnValue> iusDeetsToRV = new HashMap<>();

        //Iterate through the potential files
        for (ReturnValue currentRV : vals) {

            FileAttributes fa = new FileAttributes(currentRV, currentRV.getFiles().get(0));

            //filter files by metatype
            if (!getMetaType().contains(fa.getMetatype())) {
                continue;
            }

            //filter files by template type
            String currentTemplateType = fa.getLimsValue(Lims.LIBRARY_TEMPLATE_TYPE);
            if (!templateType.equals(currentTemplateType)) {
                continue;
            }

            //filter files by resequencing type
            String currentResequencingType = fa.getLimsValue(Lims.TARGETED_RESEQUENCING);
            if (resequencingType != null && !resequencingType.equals(currentResequencingType)) {
                continue;
            }

            //filter files by tissue type
            String currentTissueType = fa.getLimsValue(Lims.TISSUE_TYPE);
            if (tissueTypes != null && !tissueTypes.contains(currentTissueType)) {
                continue;
            }

            //filter files by tissue origin
            String currentTissueOrigin = fa.getLimsValue(Lims.TISSUE_ORIGIN);
            if (tissueOrigins != null && !tissueOrigins.contains(currentTissueOrigin)) {
                continue;
            }

            //set aside information needed for subsequent processing
            BeSmall currentSmall = new BeSmall(currentRV);
            fileSwaToSmall.put(currentRV.getAttribute(groupBy), currentSmall);

            //make sure you only have the most recent single file for each
            //sequencer run + lane + barcode + meta-type
            String fileDeets = currentSmall.getIusDetails();
            Date currentDate = currentSmall.getDate();

            //if there is no entry yet, add it
            if (iusDeetsToRV.get(fileDeets) == null) {
                Log.debug("Adding file " + fileDeets + " -> \n\t" + currentSmall.getPath());
                iusDeetsToRV.put(fileDeets, currentRV);
            } //if there is an entry, compare the current value to the 'old' one in
            //the map. if the current date is newer than the 'old' date, replace
            //it in the map
            else {
                ReturnValue oldRV = iusDeetsToRV.get(fileDeets);
                BeSmall oldSmall = fileSwaToSmall.get(oldRV.getAttribute(Header.FILE_SWA.getTitle()));
                Date oldDate = oldSmall.getDate();
                if (currentDate.after(oldDate)) {
                    Log.debug("Adding file " + fileDeets + " -> \n\t" + currentSmall.getDate()
                            + "\n\t instead of file "
                            + "\n\t" + oldSmall.getDate());
                    iusDeetsToRV.put(fileDeets, currentRV);
                } else {
                    Log.debug("Disregarding file " + fileDeets + " -> \n\t" + currentSmall.getDate()
                            + "\n\tas older than duplicate sequencer run/lane/barcode in favour of "
                            + "\n\t" + oldSmall.getDate());
                    Log.debug(currentDate + " is before " + oldDate);
                }
            }
        }
        //only use those files that entered into the iusDeetsToRV
        //since it's a map, only the most recent values
        List<ReturnValue> newValues = new ArrayList<>(iusDeetsToRV.values());
        return super.separateFiles(newValues, null);
    }

    @Override
    protected boolean checkFileDetails(FileAttributes attributes) {
        return super.checkFileDetails(attributes);
    }

    @Override
    protected ReturnValue doFinalCheck(String commaSeparatedFilePaths, String commaSeparatedParentAccessions) {
        ReturnValue rv = super.doFinalCheck(commaSeparatedFilePaths, commaSeparatedParentAccessions);

        WorkflowRun wr = new WorkflowRun(null, getFileAttributes(commaSeparatedFilePaths).toArray(new FileAttributes[0]));

        Set<FileAttributes> fas = getFileAttributes(commaSeparatedFilePaths);
        Set<String> groupTemplateType = new HashSet<>();
        Set<String> groupResequencingType = new HashSet<>();
        for (FileAttributes fa : fas) {
            groupTemplateType.add(fa.getLimsValue(Lims.LIBRARY_TEMPLATE_TYPE));
            groupResequencingType.add(fa.getLimsValue(Lims.TARGETED_RESEQUENCING));
        }
        if (groupTemplateType.size() == 1 && groupResequencingType.size() == 1) {
            String file = rsconfig.get(Iterables.getOnlyElement(groupTemplateType), Iterables.getOnlyElement(groupResequencingType), "interval_file");
            if (file == null) {
                Log.error(String.format("Template type = %s and resequencing type = %s not found in rsconfig.xml", groupTemplateType, groupResequencingType));
                rv.setExitStatus(ReturnValue.FAILURE);
            }
            if (!"WG".equals(templateType)) {
                wr.addProperty("interval_files", file);
            }
        } else {
            Log.error(String.format("Unable to determine single interval file for template type = %s and resequencing type = %s.", groupTemplateType, groupResequencingType));
            rv.setExitStatus(ReturnValue.FAILURE);

        }

        wr.addProperty("input_files", commaSeparatedFilePaths);
        wr.addProperty("stand_emit_conf", getArgument("stand-emit-conf"), "1");
        wr.addProperty("stand_call_conf", getArgument("stand-call-conf"), "30");

        if (options.has("chr-sizes")) {
            wr.addProperty("chr_sizes", getArgument("chr-sizes"));
        }

        if (options.has("disable-bqsr")) {
            wr.addProperty("do_bqsr", "false");
        }

        wr.addProperty("interval_padding", getArgument("interval-padding"), "100");

        if (options.has("id")) {
            wr.addProperty("identifier", getArgument("id"));
        } else {
            wr.addProperty("identifier", "gatk3");
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

        if (!isWorkflowRunValid(wr)) {
            rv.setExitStatus(ReturnValue.FAILURE);
        } else {
            if (workflowRuns.containsKey(commaSeparatedFilePaths)) {
                throw new RuntimeException("Input file path set is expected to be unique.");
            } else {
                workflowRuns.put(commaSeparatedFilePaths, wr);
            }
        }

        return rv;
    }

    @Override
    protected Map<String, String> modifyIniFile(String commaSeparatedFilePaths, String commaSeparatedParentAccessions) {
        Map<String, String> ini = super.modifyIniFile(commaSeparatedFilePaths, commaSeparatedParentAccessions);
        ini.putAll(workflowRuns.get(commaSeparatedFilePaths).getIniFile());
        return ini;
    }

    private class BeSmall {

        private Date date = null;
        private String iusDetails = null;
        private String groupByAttribute = null;
        private String path = null;

        public BeSmall(ReturnValue rv) {
            try {
                date = format.parse(rv.getAttribute(Header.PROCESSING_DATE.getTitle()));
            } catch (ParseException ex) {
                throw new RuntimeException(ex);
            }
            FileAttributes fa = new FileAttributes(rv, rv.getFiles().get(0));
            iusDetails = fa.getSequencerRun() + fa.getLane() + fa.getBarcode() + fa.getMetatype();
            groupByAttribute = fa.getDonor() + fa.getLimsValue(Lims.LIBRARY_TEMPLATE_TYPE) + fa.getLimsValue(Lims.GROUP_ID);
            path = rv.getFiles().get(0).getFilePath() + "";
        }

        public Date getDate() {
            return date;
        }

        public void setDate(Date date) {
            this.date = date;
        }

        public String getGroupByAttribute() {
            Log.debug(groupByAttribute);
            return groupByAttribute;
        }

        public void setGroupByAttribute(String groupByAttribute) {
            this.groupByAttribute = groupByAttribute;
        }

        public String getIusDetails() {
            return iusDetails;
        }

        public void setIusDetails(String iusDetails) {
            this.iusDetails = iusDetails;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }
    }

    private Set<FileAttributes> getFileAttributes(String commaSeparatedFilePaths) {
        Set<FileAttributes> attrs = new HashSet<>();

        for (String filePath : commaSeparatedFilePaths.split(",")) {
            FileAttributes f = files.get(filePath);
            if (f == null) {
                throw new RuntimeException("An unknown file path [" + filePath + "] was encountered.");
            } else if (attrs.contains(f)) {
                throw new RuntimeException("The file path [" + filePath + "] is already known - uniqueness expectation does not hold.");
            } else {
                attrs.add(f);
            }
        }

        return attrs;
    }

    private Map<String, String> defaultWorkflowRunIni;

    private boolean isWorkflowRunValid(WorkflowRun wr) {

        //get the default workflow ini
        if (defaultWorkflowRunIni == null) {
            defaultWorkflowRunIni = new HashMap<>();
            for (WorkflowParam wp : metadata.getWorkflowParams(this.getWorkflowAccession())) {
                defaultWorkflowRunIni.put(wp.getKey(), wp.getDefaultValue() == null ? "" : wp.getDefaultValue());
            }
        }

        try {
            Map<String, String> workflowRunIni = new HashMap<>(defaultWorkflowRunIni);
            workflowRunIni.putAll(wr.getIniFile());

            GATK3Workflow wf = new GATK3Workflow();
            wf.setConfigs(workflowRunIni);

            //Build the workflow model
            AbstractWorkflowDataModel.prepare(wf);
            wf.setName(workflowName);
            wf.setVersion(workflowVersion);
            wf.setRandom("" + new Random(System.currentTimeMillis()).nextInt(100000000)); //seqware's random int method
            wf.setupDirectory();
            wf.setupFiles();
            wf.setupWorkflow();
            wf.setupEnvironment();
            wf.buildWorkflow();
            wf.wrapup();
        } catch (Exception e) {
            System.out.println("Workflow run is not valid.");
            return false;
        }
        return true;
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
