package ca.on.oicr.pde.commands.gatk3;

import ca.on.oicr.pde.commands.AbstractCommand;
import java.util.List;
import org.apache.commons.io.FilenameUtils;

/**
 *
 * @author mlaszloffy
 */
public class ApplyRecalibration extends AbstractCommand {

    private String outputFile;

    private ApplyRecalibration() {
    }

    public String getOutputFile() {
        return outputFile;
    }

    public static class Builder extends AbstractGatkBuilder<Builder> {

        private String inputVcfFile;
        private String outputFileName;
        private String recalFile;
        private String tranchesFile;
        private Double truthSensitivityLevel;

        public Builder(String javaPath, String maxHeapSize, String tmpDir, String gatkJarPath, String gatkKey, String outputDir) {
            super(javaPath, maxHeapSize, tmpDir, gatkJarPath, gatkKey, outputDir);
        }

        public Builder setInputVcfFile(String inputVcfFile) {
            this.inputVcfFile = inputVcfFile;
            return this;
        }

        public Builder setOutputFileName(String outputFileName) {
            this.outputFileName = outputFileName;
            return this;
        }

        public Builder setRecalFile(String recalFile) {
            this.recalFile = recalFile;
            return this;
        }

        public Builder setTranchesFile(String tranchesFile) {
            this.tranchesFile = tranchesFile;
            return this;
        }

        public Builder setTruthSensitivityLevel(Double truthSensitivityLevel) {
            this.truthSensitivityLevel = truthSensitivityLevel;
            return this;
        }

        public ApplyRecalibration build() {

            String outputFilePath;
            if (outputFileName != null) {
                outputFilePath = outputDir + outputFileName + ".vcf";
            } else {
                outputFilePath = outputDir + FilenameUtils.getBaseName(inputVcfFile) + ".recalibrated.filtered.vcf";
            }

            List<String> c = build("ApplyRecalibration");

            c.add("--input");
            c.add(inputVcfFile);

            c.add("--recal_file");
            c.add(recalFile);

            c.add("--tranches_file");
            c.add(tranchesFile);

            c.add("--ts_filter_level");
            c.add(truthSensitivityLevel.toString());

            c.add("--mode");
            c.add("SNP");

            c.add("--out");
            c.add(outputFilePath);

            ApplyRecalibration cmd = new ApplyRecalibration();
            cmd.command.addAll(c);
            cmd.outputFile = outputFilePath;
            return cmd;
        }
    }

}
