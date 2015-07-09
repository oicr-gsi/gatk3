package ca.on.oicr.pde.commands.picard;

import ca.on.oicr.pde.commands.AbstractCommand;
import java.util.List;
import org.apache.commons.io.FilenameUtils;

/**
 *
 * @author mlaszloffy
 */
public class BuildBamIndex extends AbstractCommand {

    private String outputFile;

    private BuildBamIndex() {
    }

    public String getOutputFile() {
        return outputFile;
    }

    public static class Builder extends AbstractPicardBuilder<Builder>{

        public Builder(String javaPath, String maxHeapSize, String tmpDir, String picardDir) {
            super(javaPath, maxHeapSize, tmpDir, picardDir, null);
        }

        public BuildBamIndex build() {

            String outputFilePath = FilenameUtils.getPath(inputFile) + FilenameUtils.getBaseName(inputFile) + ".bai";

            List<String> c = build("BuildBamIndex.jar");
            
            c.add("INPUT=" + inputFile);

            BuildBamIndex cmd = new BuildBamIndex();
            cmd.command.addAll(c);
            cmd.outputFile = outputFilePath;
            return cmd;
        }
    }
}
