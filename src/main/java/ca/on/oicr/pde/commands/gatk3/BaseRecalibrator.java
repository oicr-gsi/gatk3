package ca.on.oicr.pde.commands.gatk3;

import ca.on.oicr.pde.commands.AbstractCommand;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 *
 * @author mlaszloffy
 */
public class BaseRecalibrator extends AbstractCommand {

    private String outputFile;

    private BaseRecalibrator() {
    }

    public String getOutputFile() {
        return outputFile;
    }

    public static class Builder extends AbstractGatkBuilder<Builder> {

        List<String> knownSites = new LinkedList<>();
        Set<String> covariates = new HashSet<>();
        List<String> inputFiles = new LinkedList<>();

        public Builder(String javaPath, String maxHeapSize, String tmpDir, String gatkJarPath, String gatkKey, String outputDir) {
            super(javaPath, maxHeapSize, tmpDir, gatkJarPath, gatkKey, outputDir);
        }

        public Builder addKnownSite(String filePath) {
            knownSites.add(filePath);
            return this;
        }

        public Builder addCovariate(String covariate) {
            covariates.add(covariate);
            return this;
        }

        public Builder setCovariates(Collection<String> covariates) {
            covariates.clear();
            covariates.addAll(covariates);
            return this;
        }

        public Builder addInputFile(String inputFile) {
            inputFiles.add(inputFile);
            return this;
        }

        public Builder addInputFiles(Collection<String> inputFiles) {
            this.inputFiles.addAll(inputFiles);
            return this;
        }

        public BaseRecalibrator build() {

            String outputFilePath = outputDir + "gatk.recalibration.csv";

            List<String> c = build("BaseRecalibrator");

            for (String inputFile : inputFiles) {
                c.add("--input_file");
                c.add(inputFile);
            }

            for (String covariate : covariates) {
                c.add("--covariate");
                c.add(covariate);
            }

            for (String knownSite : knownSites) {
                c.add("--knownSites");
                c.add(knownSite);
            }

            c.add("--out");
            c.add(outputFilePath);

            BaseRecalibrator cmd = new BaseRecalibrator();
            cmd.command.addAll(c);
            cmd.outputFile = outputFilePath;
            return cmd;
        }

    }
}
