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
