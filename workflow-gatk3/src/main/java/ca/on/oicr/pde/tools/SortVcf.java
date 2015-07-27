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
package ca.on.oicr.pde.tools;

import ca.on.oicr.pde.tools.common.AbstractCommand;
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
