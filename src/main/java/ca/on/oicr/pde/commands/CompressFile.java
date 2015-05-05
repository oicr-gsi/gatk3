package ca.on.oicr.pde.commands;

import java.util.LinkedList;
import java.util.List;
import org.apache.commons.io.FilenameUtils;

/**
 *
 * @author mlaszloffy
 */
public class CompressFile extends AbstractCommand {

    private String outputFile;

    private CompressFile() {
    }

    public String getOutputFile() {
        return outputFile;
    }

    public static class Builder {

        private final String outputDir;
        private String inputFile;

        public Builder(String outputDir) {
            this.outputDir = outputDir;
        }

        public Builder setInputFile(String inputFile) {
            this.inputFile = inputFile;
            return this;
        }

        public CompressFile build() {

            String outputFilePath = outputDir + FilenameUtils.getName(inputFile) + ".gz";

            List<String> c = new LinkedList<>();
            c.add("gzip");
            c.add("-c");
            c.add(inputFile);
            c.add(">");
            c.add(outputFilePath);

            CompressFile cmd = new CompressFile();
            cmd.command.addAll(c);
            cmd.outputFile = outputFilePath;
            return cmd;
        }

    }

}
