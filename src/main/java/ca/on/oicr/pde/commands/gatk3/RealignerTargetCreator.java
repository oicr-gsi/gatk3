package ca.on.oicr.pde.commands.gatk3;

import ca.on.oicr.pde.commands.AbstractCommand;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import org.apache.commons.lang3.RandomStringUtils;

/**
 *
 * @author mlaszloffy
 */
public class RealignerTargetCreator extends AbstractCommand {

    private String outputFile;

    private RealignerTargetCreator() {
    }

    public String getOutputFile() {
        return outputFile;
    }

    public static class Builder extends AbstractGatkBuilder<Builder> {

        private String knownIndelsFile;
        private final List<String> inputBamFiles = new LinkedList<>();

        public Builder(String javaPath, String maxHeapSize, String tmpDir, String gatkJarPath, String gatkKey, String outputDir) {
            super(javaPath, maxHeapSize, tmpDir, gatkJarPath, gatkKey, outputDir);
        }

        public Builder addInputBamFile(String inputBamFile) {
            this.inputBamFiles.add(inputBamFile);
            return this;
        }

        public Builder addInputBamFiles(Collection<String> inputBamFiles) {
            this.inputBamFiles.addAll(inputBamFiles);
            return this;
        }

        public Builder setKnownIndels(String filePath) {
            knownIndelsFile = filePath;
            return this;
        }

        public RealignerTargetCreator build() {

            String outputFilePath;
            if (outputFileName != null) {
                outputFilePath = outputDir + outputFileName + ".intervals";
            } else {
                outputFilePath = outputDir + "gatk";
                if (!intervals.isEmpty()) {
                    for (String interval : intervals) {
                        outputFilePath += "." + interval.replace(":", "-");
                    }
                } else {
                    outputFilePath += "." + RandomStringUtils.randomAlphanumeric(4);
                }
                outputFilePath += ".intervals";
            }

            List<String> c = build("RealignerTargetCreator");

            for (String inputFile : inputBamFiles) {
                c.add("--input_file");
                c.add(inputFile);
            }

            c.add("--known");
            c.add(knownIndelsFile);

            c.add("--out");
            c.add(outputFilePath);

            RealignerTargetCreator cmd = new RealignerTargetCreator();
            cmd.command.addAll(c);
            cmd.outputFile = outputFilePath;
            return cmd;
        }
    }

}
