package ca.on.oicr.pde.commands.gatk3;

import ca.on.oicr.pde.commands.AbstractCommand;
import com.google.common.base.Joiner;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.RandomStringUtils;

/**
 *
 * @author mlaszloffy
 */
public class IndelRealigner extends AbstractCommand {

    private final List<String> outputFiles = new LinkedList<>();

    private IndelRealigner() {
    }

    public Collection<String> getOutputFiles() {
        return Collections.unmodifiableList(outputFiles);
    }

    public static class Builder extends AbstractGatkBuilder<Builder> {

        private String targetIntervalFile;
        private final List<String> knownIndelFiles = new LinkedList<>();
        private final List<String> inputBamFiles = new LinkedList<>();

        public Builder(String javaPath, String maxHeapSize, String tmpDir, String gatkJarPath, String gatkKey, String outputDir) {
            super(javaPath, maxHeapSize, tmpDir, gatkJarPath, gatkKey, outputDir);
        }

        public Builder addInputBamFile(String inputBamFile) {
            this.inputBamFiles.add(inputBamFile);
            return this;
        }

        public Builder addInputBamFiles(Collection<String> inputBamFiles) {
            this.inputBamFiles.addAll(inputBamFiles);
            return this;
        }

        public Builder addKnownIndelFile(String knownIndelFile) {
            knownIndelFiles.add(knownIndelFile);
            return this;
        }

        public Builder setTargetIntervalFile(String targetIntervalFile) {
            this.targetIntervalFile = targetIntervalFile;
            return this;
        }

        public IndelRealigner build() {

            if (outputFileName != null) {
                throw new RuntimeException("setOutputFileName is not supported");
            }

            String intervalString;
            if (!intervals.isEmpty()) {
                intervalString = Joiner.on("_").join(intervals).replace(":", "-");
            } else {
                intervalString = RandomStringUtils.randomAlphanumeric(4);
            }

            Map<String, String> inputOutputFilePathMap = new HashMap<>();
            for (String inputFile : inputBamFiles) {
                String inputFileName = FilenameUtils.getName(inputFile);
                String outputFilePath;
                if (intervalString == null) {
                    outputFilePath = outputDir + FilenameUtils.getBaseName(inputFile) + ".realigned.bam";
                } else {
                    outputFilePath = outputDir + FilenameUtils.getBaseName(inputFile) + "_" + intervalString + ".realigned.bam";
                }

                if (inputOutputFilePathMap.put(inputFileName, outputFilePath) != null) {
                    throw new RuntimeException("Expected unique input files");
                }
            }

            List<String> mapCommand = new LinkedList<>();
            String inputOutputMapFile = outputDir + "input_output_" + intervalString + ".map";
            mapCommand.add("echo \"");
            mapCommand.add(Joiner.on("\n").withKeyValueSeparator("\t").join(inputOutputFilePathMap));
            mapCommand.add("\" > " + inputOutputMapFile + ";\n");

            List<String> c = build("IndelRealigner");

            for (String inputFile : inputBamFiles) {
                c.add("--input_file");
                c.add(inputFile);
            }

            c.add("--targetIntervals");
            c.add(targetIntervalFile);

            for (String knownIndelFile : knownIndelFiles) {
                c.add("--knownAlleles");
                c.add(knownIndelFile);
            }

            c.add("--bam_compression"); //aka -compress
            c.add("0");

            c.add("--nWayOut");
            c.add(inputOutputMapFile);

            IndelRealigner cmd = new IndelRealigner();
            cmd.command.addAll(mapCommand);
            cmd.command.addAll(c);
            cmd.outputFiles.addAll(inputOutputFilePathMap.values());
            return cmd;
        }
    }

}
