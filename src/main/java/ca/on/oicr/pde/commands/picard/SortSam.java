package ca.on.oicr.pde.commands.picard;

import ca.on.oicr.pde.commands.AbstractCommand;
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
