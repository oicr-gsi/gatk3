package ca.on.oicr.pde.commands.gatk3;

import ca.on.oicr.pde.commands.AbstractCommand;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.RandomStringUtils;

/**
 *
 * @author mlaszloffy
 */
public class HaplotypeCaller extends AbstractCommand {

    private String outputFile;

    private HaplotypeCaller() {
    }

    public String getOutputFile() {
        return outputFile;
    }

    public static class Builder extends AbstractGatkBuilder<Builder> {

        private final List<String> inputBamFiles = new LinkedList<>();
        private String dbsnpFilePath;
        private String standardCallConfidence;
        private String standardEmitConfidence;
        private Integer downsamplingCoverageThreshold;
        private String downsamplingType;
        private String genotypingMode;
        private String outputMode;

        public Builder(String javaPath, String maxHeapSize, String tmpDir, String gatkJarPath, String gatkKey, String outputDir) {
            super(javaPath, maxHeapSize, tmpDir, gatkJarPath, gatkKey, outputDir);
        }

        public Builder setInputBamFile(String inputBamFile) {
            this.inputBamFiles.clear();
            this.inputBamFiles.add(inputBamFile);
            return this;
        }

        public Builder setInputBamFiles(Collection<String> inputBamFiles) {
            this.inputBamFiles.clear();
            this.inputBamFiles.addAll(inputBamFiles);
            return this;
        }

        public Builder setDbsnpFilePath(String dbsnpFilePath) {
            this.dbsnpFilePath = dbsnpFilePath;
            return this;
        }

        public Builder setStandardCallConfidence(String standardCallConfidence) {
            this.standardCallConfidence = standardCallConfidence;
            return this;
        }

        public Builder setStandardEmitConfidence(String standardEmitConfidence) {
            this.standardEmitConfidence = standardEmitConfidence;
            return this;
        }

        public Builder setDownsamplingCoverageThreshold(Integer downsamplingCoverageThreshold) {
            this.downsamplingCoverageThreshold = downsamplingCoverageThreshold;
            return this;
        }

        public Builder setDownsamplingType(String downsamplingType) {
            this.downsamplingType = downsamplingType;
            return this;
        }

        public Builder setGenotypingMode(String genotypingMode) {
            this.genotypingMode = genotypingMode;
            return this;
        }

        public Builder setOutputMode(String outputMode) {
            this.outputMode = outputMode;
            return this;
        }

        public HaplotypeCaller build() {

            String outputFilePath;
            if (outputFileName == null) {
                outputFileName = "gatk." + RandomStringUtils.randomAlphanumeric(4);
            }
            outputFilePath = outputDir + outputFileName + ".haplotype_caller.raw.vcf";

            List<String> c = build("HaplotypeCaller");

            for (String inputBamFile : inputBamFiles) {
                c.add("--input_file");
                c.add(inputBamFile);
            }

            c.add("--dbsnp");
            c.add(dbsnpFilePath);

            if (standardCallConfidence != null) {
                c.add("--standard_min_confidence_threshold_for_calling");
                c.add(standardCallConfidence);
            }

            if (standardEmitConfidence != null) {
                c.add("--standard_min_confidence_threshold_for_emitting");
                c.add(standardEmitConfidence);
            }

            if (downsamplingCoverageThreshold != null) {
                c.add("--downsample_to_coverage");
                c.add(downsamplingCoverageThreshold.toString());
            }

            if (downsamplingType != null) {
                c.add("--downsampling_type");
                c.add(downsamplingType);
            }

            if (genotypingMode != null) {
                c.add("--genotyping_mode");
                c.add(StringUtils.upperCase(genotypingMode));
            }

            if (outputMode != null) {
                c.add("--output_mode");
                c.add(StringUtils.upperCase(outputMode));
            }

            c.add("--out");
            c.add(outputFilePath);

            HaplotypeCaller cmd = new HaplotypeCaller();
            cmd.command.addAll(c);
            cmd.outputFile = outputFilePath;

            return cmd;
        }
    }

}
