package ca.on.oicr.pde.workflows;

import ca.on.oicr.pde.tools.gatk3.CatVariants;
import ca.on.oicr.pde.tools.gatk3.CombineGVCFs;
import ca.on.oicr.pde.tools.gatk3.GenotypeGVCFs;
import ca.on.oicr.pde.utilities.workflows.OicrWorkflow;
import static com.google.common.base.Preconditions.*;
import com.google.common.collect.Iterables;
import java.util.*;
import java.util.Map.Entry;
import net.sourceforge.seqware.pipeline.workflowV2.model.Job;
import net.sourceforge.seqware.pipeline.workflowV2.model.SqwFile;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

public class GATKGenotypeGVCFsWorkflow extends OicrWorkflow {

    private final String tmpDir = "tmp/";
    private final String tmpGVCFsDir = tmpDir + "combinedGVCFs/";
    private final String dataDir = "data/";
    private final List<String> inputFiles = new LinkedList<>();

    public GATKGenotypeGVCFsWorkflow() {
        super();
    }

    @Override
    public void setupDirectory() {
        this.addDirectory(tmpDir);
        this.addDirectory(dataDir);
        this.addDirectory(tmpGVCFsDir);
    }

    @Override
    public Map<String, SqwFile> setupFiles() {

        List<String> inputFilesList = Arrays.asList(StringUtils.split(getProperty("input_files"), ","));
        Set<String> inputFilesSet = new HashSet<>(inputFilesList);

        if (inputFilesList.size() != inputFilesSet.size()) {
            throw new RuntimeException("Duplicate files detected in input_files");
        }

        if ((inputFilesSet.size() % 2) != 0) {
            throw new RuntimeException("Each GVCF should have a corresponding index");
        }

        Map<String, String> gvcfs = new HashMap<>();
        Map<String, String> gvcfIndexes = new HashMap<>();
        for (String f : inputFilesSet) {
            if (f == null || f.isEmpty()) {
            } else if (f.endsWith("g.vcf.gz")) {
                gvcfs.put(StringUtils.removeEnd(f, "g.vcf.gz"), f);
            } else if (f.endsWith("g.vcf.gz.tbi")) {
                gvcfIndexes.put(StringUtils.removeEnd(f, "g.vcf.gz.tbi"), f);
            } else {
                throw new RuntimeException("Unsupported input file: [" + f + "]");
            }
        }

        int id = 0;
        for (Entry<String, String> e : gvcfs.entrySet()) {
            String key = e.getKey();
            String gvcfFilePath = e.getValue();

            String gvcfIndexFilePath = gvcfIndexes.get(key);
            if (gvcfIndexFilePath == null) {
                throw new RuntimeException("Missing index for " + FilenameUtils.getName(gvcfFilePath));
            }

            SqwFile gvcf = this.createFile("file_in_" + id++);
            gvcf.setSourcePath(gvcfFilePath);
            gvcf.setType("application/g-vcf-gz");
            gvcf.setIsInput(true);

            SqwFile gvcfIndex = this.createFile("file_in_" + id++);
            gvcfIndex.setSourcePath(gvcfIndexFilePath);
            gvcfIndex.setType("application/tbi");
            gvcfIndex.setIsInput(true);

            //FIXME: this seems to work for now, it would be better to be able to set the provisionedPath as
            //bai.getProvisionedPath != bai.getOutputPath ...
            //at least with seqware 1.1.0, setting output path changes where the output file will be stored,
            //but the commonly used get provisioned path will return the incorrect path to the file
            gvcfIndex.setOutputPath(FilenameUtils.getPath(gvcf.getProvisionedPath()));

            inputFiles.add(gvcf.getProvisionedPath());
        }

        return this.getFiles();
    }

    @Override
    public void buildWorkflow() {

        final String binDir = this.getWorkflowBaseDir() + "/bin/";
        final Boolean manualOutput = BooleanUtils.toBoolean(getProperty("manual_output"), "true", "false");
        final String queue = getOptionalProperty("queue", "");
        final String java = getProperty("java");
        final String gatk = getOptionalProperty("gatk_jar", binDir);
        final String gatkKey = getProperty("gatk_key");
        final String identifier = getProperty("identifier");
        final String refFasta = getProperty("ref_fasta");
        final Integer intervalPadding = hasPropertyAndNotNull("interval_padding") ? Integer.parseInt(getProperty("interval_padding")) : null;
        final Integer gatkGenotypeGvcfsXmx = Integer.parseInt(getProperty("gatk_genotype_gvcfs_xmx"));
        final String gatkGenotypeGvcfsParams = getOptionalProperty("gatk_genotype_gvcfs_params", null);
        final Integer gatkCombineGVCFsMem = Integer.parseInt(getProperty("gatk_combine_gvcfs_mem"));
        final Integer gatkOverhead = Integer.parseInt(getProperty("gatk_sched_overhead_mem"));
        final Integer maxGenotypeGVCFsInputFiles = Integer.parseInt(getProperty("gatk_genotype_gvcfs_max_input_files"));
        final Integer maxCombineGVCFsInputFiles = Integer.parseInt(getProperty("gatk_combine_gvcfs_max_input_files"));
        final List<String> chrSizesList = Arrays.asList(StringUtils.split(getProperty("chr_sizes"), ","));
        final Set<String> chrSizes = new LinkedHashSet<>(chrSizesList);

        if (chrSizes.size() != chrSizesList.size()) {
            throw new RuntimeException("Duplicate chr_sizes detected.");
        }

        // one chrSize record is required, null will result in no parallelization
        if (chrSizes.isEmpty()) {
            chrSizes.add(null);
        }

        List<Pair<String, Job>> combineGvcfs = batchGVCFs(inputFiles, maxGenotypeGVCFsInputFiles, maxCombineGVCFsInputFiles,
                java, gatkCombineGVCFsMem, gatkOverhead, tmpDir, gatk, gatkKey, tmpGVCFsDir, refFasta, queue);

        //use linked hashmap to keep "pairs" in sort order determined by chr_sizes
        LinkedHashMap<String, Pair<GenotypeGVCFs, Job>> vcfs = new LinkedHashMap<>();
        for (String chrSize : chrSizes) {

            //GATK Genotype VCFs( https://www.broadinstitute.org/gatk/gatkdocs/org_broadinstitute_gatk_tools_walkers_variantutils_GenotypeGVCFs.php )
            GenotypeGVCFs.Builder genotypeGvcfsBuilder = new GenotypeGVCFs.Builder(java, gatkGenotypeGvcfsXmx + "g", tmpDir, gatk, gatkKey, dataDir)
                    .setReferenceSequence(refFasta)
                    .setOutputFileName(identifier + (chrSize != null ? "." + chrSize.replace(":", "-") : "") + ".raw")
                    .addInterval(chrSize)
                    .setExtraParameters(gatkGenotypeGvcfsParams);
            for (String f : getLeftCollection(combineGvcfs)) {
                genotypeGvcfsBuilder.addInputFile(f);
            }
            GenotypeGVCFs genotypeGvcfsCommand = genotypeGvcfsBuilder.build();

            Job genotypeGvcfsJob = getWorkflow().createBashJob("GATKGenotypeGVCFs")
                    .setMaxMemory(Integer.toString((gatkGenotypeGvcfsXmx + gatkOverhead) * 1024))
                    .setQueue(queue);
            genotypeGvcfsJob.getCommand().setArguments(genotypeGvcfsCommand.getCommand());

            // add parents, null if provision file in, not null if parent is a combine gvcf job
            for (Job j : getRightCollection(combineGvcfs)) {
                if (j != null) {
                    genotypeGvcfsJob.addParent(j);
                }
            }

            if (vcfs.put(chrSize, Pair.of(genotypeGvcfsCommand, genotypeGvcfsJob)) != null) {
                throw new RuntimeException("Unexpected state: duplicate vcf.");
            }
        }

        if (vcfs.size() > 1) {
            //GATK CatVariants ( https://www.broadinstitute.org/gatk/guide/tooldocs/org_broadinstitute_gatk_tools_CatVariants.php )
            CatVariants.Builder catVariantsBuilder = new CatVariants.Builder(java, gatkCombineGVCFsMem + "g", tmpDir, gatk, gatkKey, dataDir)
                    .setReferenceSequence(refFasta)
                    //individual vcf files sorted by genotype gvcfs; order of input vcf concatenation is determined by chr_sizes order (assumed to be sorted)
                    .disableSorting()
                    .setOutputFileName(identifier + ".raw");
            for (GenotypeGVCFs cmd : getLeftCollection(vcfs.values())) {
                catVariantsBuilder.addInputFile(cmd.getOutputFile());
            }
            CatVariants catVariantsCommand = catVariantsBuilder.build();

            Job combineGVCFsJob = getWorkflow().createBashJob("GATKCombineGVCFs")
                    .setMaxMemory(Integer.toString((gatkCombineGVCFsMem + gatkOverhead) * 1024))
                    .setQueue(queue);
            combineGVCFsJob.getParents().addAll(getRightCollection(vcfs.values()));
            combineGVCFsJob.getCommand().setArguments(catVariantsCommand.getCommand());
            combineGVCFsJob.addFile(createOutputFile(catVariantsCommand.getOutputFile(), "application/vcf-gz", manualOutput));
            combineGVCFsJob.addFile(createOutputFile(catVariantsCommand.getOutputIndex(), "application/tbi", manualOutput));
        } else if (vcfs.size() == 1) {
            Pair<GenotypeGVCFs, Job> p = Iterables.getOnlyElement(vcfs.values());
            GenotypeGVCFs cmd = p.getLeft();
            Job genotypeGvcfsJob = p.getRight();
            genotypeGvcfsJob.addFile(createOutputFile(cmd.getOutputFile(), "application/vcf-gz", manualOutput));
            genotypeGvcfsJob.addFile(createOutputFile(cmd.getOutputIndex(), "application/tbi", manualOutput));
        } else {
            throw new RuntimeException("Unexpected state: No VCFs");
        }
    }

    List<Pair<String, Job>> batchGVCFs(final List<String> inputFiles, final int maxBins, final int maxFilesPerBin,
            final String javaPath, final Integer maxHeapSize, final Integer gatkOverhead, final String tmpDir, final String gatkJarPath,
            final String gatkKey, final String outputDir, final String referenceSequence, final String queue) {

        checkArgument(inputFiles != null && !inputFiles.isEmpty(), "no input files");
        checkArgument(maxBins > 0, "maxBins must be greater than 0");
        checkArgument(maxFilesPerBin > 1, "maxFilesPerBin must be greater than 1");

        // create initial list of file + job pairs
        List<Pair<String, Job>> currentInputFiles = new ArrayList<>();
        for (String f : inputFiles) {
            currentInputFiles.add(Pair.of(f, (Job) null));
        }

        while (currentInputFiles.size() > maxBins) {

            // calculate the number of files to include in bin/job
            // for example, if max bins = 200 and max files per bin = 50
            // case 1: 400 input files -> add 50 files to new bin -> 351 input files (350 original + 1 new bin)
            // case 2: 230 input files -> add 31 files to new bin -> 200 input files (199 original + 1 new bin)
            int len = Math.min(currentInputFiles.size() - maxBins + 1, maxFilesPerBin);

            CombineGVCFs.Builder combineGVCFsBuilder = new CombineGVCFs.Builder(javaPath, maxHeapSize + "g", tmpDir, gatkJarPath, gatkKey, outputDir)
                    .setReferenceSequence(referenceSequence);
            for (String file : getLeftCollection(currentInputFiles.subList(0, len))) {
                combineGVCFsBuilder.addInputFile(file);
            }
            CombineGVCFs combineGVCFsCommand = combineGVCFsBuilder.build();
            Job combineGVCFsJob = getWorkflow().createBashJob("GATKCombineGVCFs")
                    .setMaxMemory(Integer.toString((maxHeapSize + gatkOverhead) * 1024))
                    .setQueue(queue);
            combineGVCFsJob.getCommand().setArguments(combineGVCFsCommand.getCommand());
            for (Job job : getRightCollection(currentInputFiles.subList(0, len))) {
                if (job != null) { // only add parent if there is one (initial input files do not have parents)
                    combineGVCFsJob.addParent(job);
                }
            }

            List<Pair<String, Job>> nextInputFiles = currentInputFiles.subList(len, currentInputFiles.size());
            nextInputFiles.add(Pair.of(combineGVCFsCommand.getOutputFile(), combineGVCFsJob));
            currentInputFiles = nextInputFiles;
        }

        return currentInputFiles;
    }

    private <T, S> Set<T> getLeftCollection(Collection<Pair<T, S>> pairs) {
        LinkedHashSet<T> ts = new LinkedHashSet<>();
        for (Pair<T, S> p : pairs) {
            ts.add(p.getLeft());
        }
        return ts;
    }

    private <S, T> Set<T> getRightCollection(Collection<Pair<S, T>> pairs) {
        LinkedHashSet<T> ts = new LinkedHashSet<>();
        for (Pair<S, T> p : pairs) {
            ts.add(p.getRight());
        }
        return ts;
    }
}
