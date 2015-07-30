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
package ca.on.oicr.pde.deciders.gatk3;

import ca.on.oicr.pde.deciders.FileAttributes;
import ca.on.oicr.pde.deciders.Group;
import ca.on.oicr.pde.deciders.Lims;
import ca.on.oicr.pde.deciders.OicrDecider;
import ca.on.oicr.pde.deciders.WorkflowRun;
import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import net.sourceforge.seqware.common.hibernate.FindAllTheFiles.Header;
import net.sourceforge.seqware.common.model.WorkflowParam;
import net.sourceforge.seqware.common.module.ReturnValue;
import net.sourceforge.seqware.common.util.Log;
import net.sourceforge.seqware.pipeline.plugins.fileprovenance.ProvenanceUtility;
import net.sourceforge.seqware.pipeline.workflowV2.AbstractWorkflowDataModel;

/**
 *
 * @author mlaszloffy
 */
public abstract class AbstractGatkDecider<T extends AbstractWorkflowDataModel> extends OicrDecider {

    protected String templateType = null;
    protected List<String> resequencingTypes = null;
    protected List<String> tissueTypes = null;
    protected List<String> tissueOrigins = null;
    protected List<String> groupIds = null;
    protected final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");
    protected String workflowName;
    protected String workflowVersion;
    protected final Map<String, WorkflowRun> workflowRuns = new HashMap<>();
    protected String identifierFromFilters;
    private final Map<String, BeSmall> fileSwaToSmall = new HashMap<>();
    private final Class<T> workflowClass;

    public AbstractGatkDecider(Class<T> workflowClass) {
        super();

        //the workflow class is used during validation
        this.workflowClass = workflowClass;

        //settings
        defineArgument("id", "Override final filename prefix (eg. ID_123.haplotype_caller.raw.vcf.gz, ID_123.unified_genotyper.raw.vcf.gz).", false);

        //mandatory filters
        defineArgument("library-template-type", "Restrict the processing to samples of a particular template type, e.g. WG, EX, TS.", true);

        //optional filters
        defineArgument("tissue-type", "Restrict the processing to samples of particular tissue types, "
                + "e.g. P, R, X, C. Multiple values can be comma-separated. Default: no restriction", false);
        defineArgument("tissue-origin", "Restrict the processing to samples of particular tissue origin, "
                + "e.g. Ly, Pa, Pr. Multiple values can be comma-separated. Default: no restriction", false);
        defineArgument("resequencing-type", "Restrict the processing to samples of a particular resequencing type. Multiple values can be comma-separated. Default: no restriction", false);
        defineArgument("group-id", "Restrict the processing to samples of a particular group-id. Multiple values can be comma-separated. Default: no restriction", false);

    }

    protected abstract void configureDecider();

    protected abstract void configureWorkflowRun(WorkflowRun wr, Set<FileAttributes> inputFileAttributes) throws InvalidWorkflowRunException;

    //protected abstract boolean validate(WorkflowRun wr);
    @Override
    public ReturnValue init() {
        List<String> filters = new LinkedList<>();

        if (options.hasArgument("all")) {
            filters.add("ALL");
        }

        if (options.hasArgument(ProvenanceUtility.HumanProvenanceFilters.STUDY_NAME.toString())) {
            List<?> vs = options.valuesOf(ProvenanceUtility.HumanProvenanceFilters.STUDY_NAME.toString());
            filters.add(Joiner.on("+").join(vs));
        }

        if (options.hasArgument(ProvenanceUtility.HumanProvenanceFilters.ROOT_SAMPLE_NAME.toString())) {
            List<?> vs = options.valuesOf(ProvenanceUtility.HumanProvenanceFilters.ROOT_SAMPLE_NAME.toString());
            filters.add(Joiner.on("+").join(vs));
        }

        if (options.hasArgument(ProvenanceUtility.HumanProvenanceFilters.SAMPLE_NAME.toString())) {
            List<?> vs = options.valuesOf(ProvenanceUtility.HumanProvenanceFilters.SAMPLE_NAME.toString());
            filters.add(Joiner.on("+").join(vs));
        }

        if (!getArgument("tissue-origin").isEmpty()) {
            tissueOrigins = Arrays.asList(getArgument("tissue-origin").split(","));
            filters.add(Joiner.on("+").join(tissueOrigins));
        }

        if (!getArgument("tissue-type").isEmpty()) {
            tissueTypes = Arrays.asList(getArgument("tissue-type").split(","));
            filters.add(Joiner.on("+").join(tissueTypes));
        }

        if (!getArgument("resequencing-type").isEmpty()) {
            resequencingTypes = Arrays.asList(getArgument("resequencing-type").split(","));
            filters.add(Joiner.on("+").join(resequencingTypes));
        }

        if (!getArgument("group-id").isEmpty()) {
            groupIds = Arrays.asList(getArgument("group-id"));
            filters.add(Joiner.on("+").join(groupIds));
        }

        templateType = getArgument("library-template-type");
        filters.add(templateType);

        identifierFromFilters = Joiner.on("_").join(filters);

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

        //call the implemented configureDecider method
        configureDecider();

        return super.init();
    }

    @Override
    public Map<String, List<ReturnValue>> separateFiles(List<ReturnValue> vals, String groupBy) {

        Map<String, ReturnValue> iusDeetsToRV = new HashMap<>();

        //Iterate and filter the potential files
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
            if (resequencingTypes != null && !resequencingTypes.contains(currentResequencingType)) {
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

            //filter files by group id
            String currentGroupId = fa.getLimsValue(Lims.GROUP_ID);
            if (groupIds != null && !groupIds.contains(currentGroupId)) {
                continue;
            }

            //set aside information needed for subsequent processing
            BeSmall currentSmall = new BeSmall(currentRV);
            fileSwaToSmall.put(currentRV.getAttribute(Header.FILE_SWA.getTitle()), currentSmall);

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

        Map<String, List<ReturnValue>> groupedFiles;
        if (options.hasArgument("group-by") || getGroupBy() != null) {
            groupedFiles = super.separateFiles(newValues, groupBy);
        } else {
            groupedFiles = super.separateFiles(newValues, null); //do not group files
        }

        return groupedFiles;
    }

    @Override
    protected String handleGroupByAttribute(String attribute) {
        String a = super.handleGroupByAttribute(attribute);
        BeSmall small = fileSwaToSmall.get(a);
        if (small != null) {
            return small.getGroupByAttribute();
        }
        return attribute;
    }

    @Override
    protected boolean checkFileDetails(FileAttributes attributes) {
        return super.checkFileDetails(attributes);
    }

    @Override
    protected ReturnValue doFinalCheck(String commaSeparatedFilePaths, String commaSeparatedParentAccessions) {
        ReturnValue rv = super.doFinalCheck(commaSeparatedFilePaths, commaSeparatedParentAccessions);

        Set<FileAttributes> fas = getFileAttributes(commaSeparatedFilePaths);

        WorkflowRun wr = new WorkflowRun(null, getFileAttributes(commaSeparatedFilePaths).toArray(new FileAttributes[0]));

        wr.addProperty("input_files", commaSeparatedFilePaths);

        Set<String> groupByValues = new HashSet<>();
        for (FileAttributes fa : fas) {
            String key = fa.getOtherAttribute(getGroupingStrategy());
            String group = null;
            BeSmall bs = fileSwaToSmall.get(key);
            if (bs != null) {
                group = bs.getGroupName();
            }
            if (group != null) {
                groupByValues.add(group);
            } else {
                groupByValues.add(key);
            }
        }

        if (options.has("id")) {
            //set user requested identifier
            wr.addProperty("identifier", sanitize(getArgument("id")));
        } else if (Group.FILE == getGroupBy()) {
            //when using group by file, file to BeSmall grouping is used - so use BeSmall's group name
            wr.addProperty("identifier", sanitize(Iterables.getOnlyElement(groupByValues)));
        } else if (options.hasArgument("group-by") || getGroupBy() != null) {
            //use the user defined "group-by" value (eg, --group-by study -> id=STUDY1_{templateType})
            wr.addProperty("identifier", sanitize(Iterables.getOnlyElement(groupByValues) + "_" + templateType));
        } else {
            //use the aggregation of the user provided filters as the identifier
            wr.addProperty("identifier", sanitize(identifierFromFilters));
        }

        try {
            configureWorkflowRun(wr, fas);
        } catch (InvalidWorkflowRunException iwre) {
            Log.error(iwre.getMessage());
            rv.setExitStatus(ReturnValue.FAILURE);
        }

        if (!validate(wr)) {
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
        private String groupName = null;
        private String path = null;

        public BeSmall(ReturnValue rv) {
            try {
                date = format.parse(rv.getAttribute(Header.PROCESSING_DATE.getTitle()));
            } catch (ParseException ex) {
                throw new RuntimeException(ex);
            }
            FileAttributes fa = new FileAttributes(rv, rv.getFiles().get(0));
            this.iusDetails = fa.getSequencerRun() + fa.getLane() + fa.getBarcode() + fa.getMetatype();

            List<String> groupByAttributes = new LinkedList<>();
            List<String> groupNames = new LinkedList<>();

            if (fa.getOtherAttribute("Root Sample Name") != null) {
                groupByAttributes.add(fa.getOtherAttribute("Root Sample Name"));
                groupNames.add(fa.getOtherAttribute("Root Sample Name"));
            } else {
                throw new RuntimeException("Missing required attribute " + "Root Sample Name");
            }

            if (fa.getLimsValue(Lims.TISSUE_ORIGIN) != null) {
                groupByAttributes.add(fa.getLimsValue(Lims.TISSUE_ORIGIN));
                groupNames.add(fa.getLimsValue(Lims.TISSUE_ORIGIN));
            } else {
                throw new RuntimeException("Missing required attribute " + Lims.TISSUE_ORIGIN);
            }

            if (fa.getLimsValue(Lims.TISSUE_TYPE) != null) {
                groupByAttributes.add(fa.getLimsValue(Lims.TISSUE_TYPE));
                groupNames.add(fa.getLimsValue(Lims.TISSUE_TYPE));
            } else {
                throw new RuntimeException("Missing required attribute " + Lims.TISSUE_TYPE);
            }

            //Used in BFMC grouping, but not in group name
            if (fa.getLimsValue(Lims.TISSUE_PREP) != null) {
                groupByAttributes.add(fa.getLimsValue(Lims.TISSUE_PREP));
            }

            if (fa.getLimsValue(Lims.TISSUE_REGION) != null) {
                groupByAttributes.add(fa.getLimsValue(Lims.TISSUE_REGION));
            }

            // library type and library size are not currently used in BFMC grouping
            //if (fa.getLimsValue(Lims.LIBRARY_TYPE) != null) {
            //    groupByAttributes.add(fa.getLimsValue(Lims.LIBRARY_TYPE));
            //}
            //if (fa.getLimsValue(Lims.LIBRARY_SIZE) != null) {
            //    groupByAttributes.add(fa.getLimsValue(Lims.LIBRARY_SIZE));
            //}
            if (fa.getLimsValue(Lims.LIBRARY_TEMPLATE_TYPE) != null) {
                groupByAttributes.add(fa.getLimsValue(Lims.LIBRARY_TEMPLATE_TYPE));
                groupNames.add(fa.getLimsValue(Lims.LIBRARY_TEMPLATE_TYPE));
            } else {
                throw new RuntimeException("Missing required attribute " + Lims.LIBRARY_TEMPLATE_TYPE);
            }

            if (fa.getLimsValue(Lims.GROUP_ID) != null) {
                groupByAttributes.add(fa.getLimsValue(Lims.GROUP_ID));
                groupNames.add(fa.getLimsValue(Lims.GROUP_ID));
            }

            this.groupByAttribute = Joiner.on("_").join(groupByAttributes);
            this.groupName = Joiner.on("_").join(groupNames);

            this.path = rv.getFiles().get(0).getFilePath() + "";
        }

        public Date getDate() {
            return date;
        }

        public void setDate(Date date) {
            this.date = date;
        }

        public String getGroupName() {
            return groupName;
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

    private boolean validate(WorkflowRun wr) {
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

            T wf = workflowClass.newInstance();
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
            Log.error("Workflow run is not valid: " + e.getMessage());
            return false;
        }
        return true;
    }
    
    public static String sanitize(String s){
        return s.replaceAll("[^a-zA-Z0-9.+_-]", "");
    }

    public static class InvalidWorkflowRunException extends Exception {

        public InvalidWorkflowRunException(String message) {
            super(message);
        }

        public InvalidWorkflowRunException(String message, Throwable throwable) {
            super(message, throwable);
        }
    }

}
