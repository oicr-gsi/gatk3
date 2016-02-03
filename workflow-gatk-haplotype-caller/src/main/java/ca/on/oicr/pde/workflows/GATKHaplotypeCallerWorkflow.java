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

import ca.on.oicr.pde.tools.BEDFileUtils;
import ca.on.oicr.pde.tools.gatk3.CatVariants;
import ca.on.oicr.pde.tools.gatk3.HaplotypeCaller;
import ca.on.oicr.pde.utilities.workflows.OicrWorkflow;
import com.google.common.collect.Iterables;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;
import net.sourceforge.seqware.pipeline.workflowV2.model.Job;
import net.sourceforge.seqware.pipeline.workflowV2.model.SqwFile;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

public class GATKHaplotypeCallerWorkflow extends OicrWorkflow {

    private final String tmpDir = "tmp/";
    private final String dataDir = "data/";
    private final List<String> inputBamFiles = new LinkedList<>();

    public GATKHaplotypeCallerWorkflow() {
        super();
    }

    @Override
    public void setupDirectory() {
        this.addDirectory(tmpDir);
        this.addDirectory(dataDir);
    }

    @Override
    public Map<String, SqwFile> setupFiles() {

        List<String> inputFilesList = Arrays.asList(StringUtils.split(getProperty("input_files"), ","));
        Set<String> inputFilesSet = new HashSet<>(inputFilesList);

        if (inputFilesList.size() != inputFilesSet.size()) {
            throw new RuntimeException("Duplicate files detected in input_files");
        }

        if ((inputFilesSet.size() % 2) != 0) {
            throw new RuntimeException("Each bam should have a corresponding index");
        }

        Map<String, String> bams = new HashMap<>();
        Map<String, String> bais = new HashMap<>();
        for (String f : inputFilesSet) {
            String fileExtension = FilenameUtils.getExtension(f);
            String fileKey = FilenameUtils.removeExtension(f);
            if (null != fileExtension) {
                switch (fileExtension) {
                    case "bam":
                        bams.put(fileKey, f);
                        break;
                    case "bai":
                        bais.put(fileKey, f);
                        break;
                    default:
                        throw new RuntimeException("Unsupported input file type");
                }
            }
        }

        int id = 0;
        for (Entry<String, String> e : bams.entrySet()) {
            String key = e.getKey();
            String bamFilePath = e.getValue();

            String baiFilePath = bais.get(key);
            if (baiFilePath == null) {
                throw new RuntimeException("Missing index for " + FilenameUtils.getName(bamFilePath));
            }

            SqwFile bam = this.createFile("file_in_" + id++);
            bam.setSourcePath(bamFilePath);
            bam.setType("application/bam");
            bam.setIsInput(true);

            SqwFile bai = this.createFile("file_in_" + id++);
            bai.setSourcePath(baiFilePath);
            bai.setType("application/bam-index");
            bai.setIsInput(true);

            //FIXME: this seems to work for now, it would be better to be able to set the provisionedPath as
            //bai.getProvisionedPath != bai.getOutputPath ...
            //at least with seqware 1.1.0, setting output path changes where the output file will be stored,
            //but the commonly used get provisioned path will return the incorrect path to the file
            bai.setOutputPath(FilenameUtils.getPath(bam.getProvisionedPath()));

            inputBamFiles.add(bam.getProvisionedPath());
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
        final String dbsnpVcf = getProperty("gatk_dbsnp_vcf");
        final Integer intervalPadding = hasPropertyAndNotNull("interval_padding") ? Integer.parseInt(getProperty("interval_padding")) : null;
        final Integer downsamplingCoverage = hasPropertyAndNotNull("downsampling_coverage") ? Integer.parseInt(getProperty("downsampling_coverage")) : null;
        final String downsamplingType = getOptionalProperty("downsampling_type", null);
        final Integer gatkHaplotypeCallerThreads = Integer.parseInt(getProperty("gatk_haplotype_caller_threads"));
        final Integer gatkHaplotypeCallerXmx = Integer.parseInt(getProperty("gatk_haplotype_caller_xmx"));
        final Integer gatkCombineGVCFsXmx = Integer.parseInt(getProperty("gatk_combine_gvcfs_xmx"));
        final Integer gatkOverhead = Integer.parseInt(getProperty("gatk_sched_overhead_mem"));
        final String haplotypeCallerParams = getOptionalProperty("gatk_haplotype_caller_params", null);

        final List<String> intervalFilesList = Arrays.asList(StringUtils.split(getOptionalProperty("interval_files", ""), ","));
        final Set<String> intervalFiles = new HashSet<>(intervalFilesList);
        if (intervalFiles.size() != intervalFilesList.size()) {
            throw new RuntimeException("Duplicate interval_files detected");
        }

        final Set<String> chrSizes;
        if (hasProperty("chr_sizes")) {
            //chr_sizes has been set
            List<String> chrSizesList = Arrays.asList(StringUtils.split(getProperty("chr_sizes"), ","));
            chrSizes = new HashSet<>(chrSizesList);
            if (chrSizes.size() != chrSizesList.size()) {
                throw new RuntimeException("Duplicate chr_sizes detected.");
            }
        } else if (!intervalFiles.isEmpty()) {
            //chr_sizes not set, interval_files has been set - use interval files to calculate chrSizes
            try {
                chrSizes = BEDFileUtils.getChromosomes(intervalFiles);
            } catch (IOException ioe) {
                throw new RuntimeException(ioe);
            }
        } else {
            //chr_sizes and interval_files not set - can not calculate chrSizes
            chrSizes = new HashSet<>();
        }

        // one chrSize record is required, null will result in no parallelization
        if (chrSizes.isEmpty()) {
            chrSizes.add(null);
        }

        Map<String, Pair<HaplotypeCaller, Job>> gvcfs = new HashMap<>();
        for (String chrSize : chrSizes) {
            //GATK Haplotype Caller ( https://www.broadinstitute.org/gatk/gatkdocs/org_broadinstitute_gatk_tools_walkers_haplotypecaller_HaplotypeCaller.php )
            HaplotypeCaller haplotypeCallerCommand = new HaplotypeCaller.Builder(java, Integer.toString(gatkHaplotypeCallerXmx) + "g", tmpDir, gatk, gatkKey, dataDir)
                    .setInputBamFiles(inputBamFiles)
                    .setReferenceSequence(refFasta)
                    .setDbsnpFilePath(dbsnpVcf)
                    .addInterval(chrSize)
                    .addIntervalFiles(intervalFiles)
                    .setIntervalPadding(intervalPadding)
                    .setDownsamplingCoverageThreshold(downsamplingCoverage)
                    .setDownsamplingType(downsamplingType)
                    .setOutputFileName(identifier + (chrSize != null ? "." + chrSize.replace(":", "-") : ""))
                    .setNumCpuThreadsPerDataThread(gatkHaplotypeCallerThreads)
                    .setExtraParameters(haplotypeCallerParams)
                    .build();
            Job haplotypeCallerJob = this.getWorkflow().createBashJob("GATKHaplotypeCaller")
                    .setMaxMemory(Integer.toString((gatkHaplotypeCallerXmx + gatkOverhead) * 1024))
                    .setQueue(queue);
            haplotypeCallerJob.getCommand().setArguments(haplotypeCallerCommand.getCommand());

            if (gvcfs.put(chrSize, Pair.of(haplotypeCallerCommand, haplotypeCallerJob)) != null) {
                throw new RuntimeException("Unexpected state: Duplicate key.");
            }
        }

        if (gvcfs.size() > 1) {
            //GATK CatVariants ( https://www.broadinstitute.org/gatk/guide/tooldocs/org_broadinstitute_gatk_tools_CatVariants.php )
            CatVariants.Builder catVariantsBuilder = new CatVariants.Builder(java, Integer.toString(gatkCombineGVCFsXmx) + "g", tmpDir, gatk, gatkKey, dataDir)
                    .setReferenceSequence(refFasta)
                    .setOutputFileName(identifier);
            for (HaplotypeCaller hc : getLeftCollection(gvcfs.values())) {
                catVariantsBuilder.addInputFile(hc.getOutputFile());
            }
            CatVariants catvariantsCommand = catVariantsBuilder.build();
            Job combineGVCFsJob = getWorkflow().createBashJob("GATKCombineGVCFs")
                    .setMaxMemory(Integer.toString((gatkCombineGVCFsXmx + gatkOverhead) * 1024))
                    .setQueue(queue);
            combineGVCFsJob.getParents().addAll(getRightCollection(gvcfs.values()));
            combineGVCFsJob.getCommand().setArguments(catvariantsCommand.getCommand());
            combineGVCFsJob.addFile(createOutputFile(catvariantsCommand.getOutputFile(), "application/g-vcf-gz", manualOutput));
            combineGVCFsJob.addFile(createOutputFile(catvariantsCommand.getOutputIndex(), "application/tbi", manualOutput));
        } else if (gvcfs.size() == 1) {
            Pair<HaplotypeCaller, Job> p = Iterables.getOnlyElement(gvcfs.values());
            HaplotypeCaller hcCmd = p.getLeft();
            Job hcJob = p.getRight();
            hcJob.addFile(createOutputFile(hcCmd.getOutputFile(), "application/g-vcf-gz", manualOutput));
            hcJob.addFile(createOutputFile(hcCmd.getOutputIndex(), "application/tbi", manualOutput));
        } else {
            throw new RuntimeException("Unexpected state: No GVCFs");
        }
    }

    private <T, S> Set<T> getLeftCollection(Collection<Pair<T, S>> pairs) {
        Set<T> ts = new HashSet<>();
        for (Pair<T, S> p : pairs) {
            ts.add(p.getLeft());
        }
        return ts;
    }

    private <S, T> Set<T> getRightCollection(Collection<Pair<S, T>> pairs) {
        Set<T> ts = new HashSet<>();
        for (Pair<S, T> p : pairs) {
            ts.add(p.getRight());
        }
        return ts;
    }
}
