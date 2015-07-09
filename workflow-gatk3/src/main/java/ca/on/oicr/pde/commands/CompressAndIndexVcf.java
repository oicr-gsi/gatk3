package ca.on.oicr.pde.commands;

import java.util.LinkedList;
import java.util.List;
import org.apache.commons.io.FilenameUtils;

/**
 *
 * @author mlaszloffy
 */
public class CompressAndIndexVcf extends AbstractCommand {

    private String outputVcfFile;
    private String outputTabixFile;

    private CompressAndIndexVcf() {
    }

    public String getOutputVcfFile() {
        return outputVcfFile;
    }

    public String getOutputTabixFile() {
        return outputTabixFile;
    }

    public static class Builder {

        private final String tabixDir;
        private final String outputDir;
        private String inputFile;
        private String outputFileName;

        public Builder(String tabixDir, String outputDir) {
            this.tabixDir = tabixDir;
            this.outputDir = outputDir;
        }

        public Builder setInputFile(String inputFile) {
            this.inputFile = inputFile;
            return this;
        }

        public Builder setOutputFileName(String outputFileName) {
            this.outputFileName = outputFileName;
            return this;
        }

        public CompressAndIndexVcf build() {
            
            String outputVcfFilePath;
            if (outputFileName != null) {
                outputVcfFilePath = outputDir + outputFileName + ".vcf.gz";
            } else {
                outputVcfFilePath = outputDir + FilenameUtils.getBaseName(inputFile) + ".vcf.gz";
            }
            
            String outputTabixFilePath = outputVcfFilePath + ".tbi";

            List<String> c = new LinkedList<>();
            c.add(tabixDir + "bgzip");
            c.add("-c"); //write to stdout
            c.add(inputFile);
            c.add(">");
            c.add(outputVcfFilePath);

            c.add("&&");

            c.add(tabixDir + "tabix");
            c.add("-p vcf");
            c.add(outputVcfFilePath);

            CompressAndIndexVcf cmd = new CompressAndIndexVcf();
            cmd.command.addAll(c);
            cmd.outputVcfFile = outputVcfFilePath;
            cmd.outputTabixFile = outputTabixFilePath;
            return cmd;
        }

    }

}
