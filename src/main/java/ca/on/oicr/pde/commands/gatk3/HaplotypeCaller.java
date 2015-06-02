package ca.on.oicr.pde.commands.gatk3;

import ca.on.oicr.pde.commands.AbstractCommand;
import java.util.List;
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

        private String inputBamFile;
        private String dbsnpFilePath;
        private String standardCallConfidence;
        private String standardEmitConfidence;
        private Integer downsamplingCoverageThreshold;
        private String downsamplingType;

        public Builder(String javaPath, String maxHeapSize, String tmpDir, String gatkJarPath, String gatkKey, String outputDir) {
            super(javaPath, maxHeapSize, tmpDir, gatkJarPath, gatkKey, outputDir);
        }

        public Builder setInputBamFile(String inputBamFile) {
            this.inputBamFile = inputBamFile;
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

        public Builder setDownsamplingCoverageThreshold(Integer downsamplingCoverageThreshold, String downsamplingType) {
            this.downsamplingCoverageThreshold = downsamplingCoverageThreshold;
            this.downsamplingType = downsamplingType;
            return this;
        }

        public HaplotypeCaller build() {

            String outputFilePath;
            if (outputFileName == null) {
                outputFileName = "gatk." + RandomStringUtils.randomAlphanumeric(4);
            }
            outputFilePath = outputDir + outputFileName + "haplotype_caller.raw.vcf";

            List<String> c = build("HaplotypeCaller");

            c.add("--input_file");
            c.add(inputBamFile);

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
                c.add("--downsampling_type");
                c.add(downsamplingType);
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
