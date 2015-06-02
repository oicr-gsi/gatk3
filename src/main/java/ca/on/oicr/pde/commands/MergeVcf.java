package ca.on.oicr.pde.commands;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import org.apache.commons.lang3.RandomStringUtils;

/**
 *
 * @author mlaszloffy
 */
public class MergeVcf extends AbstractCommand {

    private String outputFile;

    private MergeVcf() {
    }

    public String getOutputFile() {
        return outputFile;
    }

    public static class Builder {

        private final String perl;
        private final String mergeScriptPath;
        private final String outputDir;

        private final List<String> inputFiles = new LinkedList<>();
        private String outputFileName;

        public Builder(String perl, String mergeScriptPath, String outputDir) {
            this.perl = perl;
            this.mergeScriptPath = mergeScriptPath;
            this.outputDir = outputDir;
        }

        public Builder addInputFiles(Collection<String> inputFiles) {
            this.inputFiles.addAll(inputFiles);
            return this;
        }

        public Builder addInputFile(String inputFile) {
            this.inputFiles.add(inputFile);
            return this;
        }

        public Builder setOutputFileName(String outputFileName) {
            this.outputFileName = outputFileName;
            return this;
        }

        public MergeVcf build() {

            String outputFilePath;
            if (outputFileName != null) {
                outputFilePath = outputDir + outputFileName + ".merged.vcf";
            } else {
                outputFilePath = outputDir + RandomStringUtils.randomAlphanumeric(4) + ".merged.vcf";
            }
            
            List<String> c = new LinkedList<>();
            
            c.add(perl);
            c.add(mergeScriptPath);
            
            for(String inputFile : inputFiles){
                c.add("--vcf-input-file");
                c.add(inputFile);
            }

            c.add("--vcf-output-file");
            c.add(outputFilePath);
            
            MergeVcf cmd = new MergeVcf();
            cmd.command.addAll(c);
            cmd.outputFile = outputFilePath;
            return cmd;
        }
    }
}
