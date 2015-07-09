package ca.on.oicr.pde.commands;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import org.apache.commons.io.FilenameUtils;

/**
 *
 * @author mlaszloffy
 */
public class SortVcf extends AbstractCommand {
    
    private String outputFile;
    
    private SortVcf(){
    }

    public String getOutputFile() {
        return outputFile;
    }
    
    public static class Builder {

        private final String outputDir;
        private String inputFile;
        private String outputFileName;

        public Builder(String outputDir) {
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

        public SortVcf build() {

            String outputFilePath;
            if (outputFileName != null) {
                outputFilePath = outputDir + outputFileName + ".vcf";
            } else {
                outputFilePath = outputDir + FilenameUtils.getBaseName(inputFile) + ".sorted.vcf";
            }

            List<String> c = new LinkedList<>();
            
            c.add("("); //start subshell

            //print vcf header
            c.add("grep --no-filename");
            c.add("'^#'");
            c.add(inputFile);
            c.add(";");

            //calls
            c.add("grep --no-filename");
            c.add("-v '^#'");
            c.add(inputFile);
            c.add("|");
            c.add("sed 's/chrX/23/'");
            c.add("|");
            c.add("sed 's/chrY/24/'");
            c.add("|");
            c.add("sed 's/chrM/25/'");
            c.add("|");
            c.add("sed 's/chr//'");
            c.add("|");
            c.add("sort -n -k1,1 -k2,2n");
            //c.add("sort -k1.4,1.5n -k2,2n");
            c.add("|");
            c.add("sed 's/^/chr/'");
            c.add("|");
            c.add("sed 's/chr23/chrX/'");
            c.add("|");
            c.add("sed 's/chr24/chrY/'");
            c.add("|");
            c.add("sed 's/chr25/chrM/'");
            c.add(";");
            
            c.add(")"); //end subshell
            
            c.add(">");
            c.add(outputFilePath);

            SortVcf cmd = new SortVcf();
            cmd.command.addAll(c);
            cmd.outputFile = outputFilePath;

            return cmd;
        }

    }

}
