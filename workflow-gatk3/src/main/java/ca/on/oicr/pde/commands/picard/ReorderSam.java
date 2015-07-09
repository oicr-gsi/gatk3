package ca.on.oicr.pde.commands.picard;

import ca.on.oicr.pde.commands.AbstractCommand;
import java.util.List;
import org.apache.commons.io.FilenameUtils;

/**
 *
 * @author mlaszloffy
 */
public class ReorderSam extends AbstractCommand {

    private String outputFile;

    private ReorderSam() {
    }

    public String getOutputFile() {
        return outputFile;
    }

    public static class Builder extends AbstractPicardBuilder<Builder> {

        private String referenceSequence;
        private boolean createIndex;

        public Builder(String javaPath, String maxHeapSize, String tmpDir, String picardDir, String outputDir) {
            super(javaPath, maxHeapSize, tmpDir, picardDir, outputDir);
        }

        public Builder setReferenceSequence(String referenceSequence) {
            this.referenceSequence = referenceSequence;
            return this;
        }

        public Builder setCreateIndex(boolean createIndex) {
            this.createIndex = true;
            return this;
        }

        public ReorderSam build() {

            String outputFilePath = outputDir + FilenameUtils.getBaseName(inputFile) + ".reordered.bam";

            List<String> c = build("ReorderSam.jar");

            c.add("INPUT=" + inputFile);

            c.add("OUTPUT=" + outputFilePath);

            c.add("REFERENCE=" + referenceSequence);

            if (createIndex) {
                c.add("CREATE_INDEX=true");
            }

            ReorderSam cmd = new ReorderSam();
            cmd.command.addAll(c);
            cmd.outputFile = outputFilePath;
            return cmd;
        }
    }

}
