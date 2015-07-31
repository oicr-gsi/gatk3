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
package ca.on.oicr.pde.tools.picard;

import ca.on.oicr.pde.tools.common.AbstractCommand;
import java.util.List;
import org.apache.commons.io.FilenameUtils;

/**
 *
 * @author mlaszloffy
 */
public class SortSam extends AbstractCommand {

    private String outputFile;
    
    private SortSam() {
    }

    public String getOutputFile() {
        return outputFile;
    }
    
    public static class Builder extends AbstractPicardBuilder<Builder> {

        private String sortOrder;
        private boolean createIndex;

        public Builder(String javaPath, String maxHeapSize, String tmpDir, String picardDir, String outputDir) {
            super(javaPath, maxHeapSize, tmpDir, picardDir, outputDir);
        }

        public Builder setSortOrder(String sortOrder) {
            this.sortOrder = sortOrder;
            return this;
        }
        
        public Builder setCreateIndex(boolean createIndex){
            this.createIndex = createIndex;
            return this;
        }

        public SortSam build() {

            String outputFilePath = outputDir + FilenameUtils.getBaseName(inputFile) + ".sorted.bam";

            List<String> c = build("SortSam.jar");

            c.add("INPUT=" + inputFile);

            c.add("OUTPUT=" + outputFilePath);

            c.add("SORT_ORDER=" + sortOrder);
            
            if(createIndex){
                c.add("CREATE_INDEX=true");
            }

            SortSam cmd = new SortSam();
            cmd.command.addAll(c);
            cmd.outputFile = outputFilePath;
            return cmd;
        }
    }
}
