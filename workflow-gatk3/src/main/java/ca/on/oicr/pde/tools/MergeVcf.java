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
