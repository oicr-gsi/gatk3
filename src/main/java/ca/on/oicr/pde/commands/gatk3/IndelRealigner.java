package ca.on.oicr.pde.commands.gatk3;

import ca.on.oicr.pde.commands.AbstractCommand;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import org.apache.commons.lang3.RandomStringUtils;

/**
 *
 * @author mlaszloffy
 */
public class IndelRealigner extends AbstractCommand {

    private String outputFile;

    private IndelRealigner() {
    }

    public String getOutputFile() {
        return outputFile;
    }

    public static class Builder extends AbstractGatkBuilder<Builder> {

        private String targetIntervalFile;
        private String outputFileName;
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

        public Builder setTargetIntervalFile(String targetIntervalFile) {
            this.targetIntervalFile = targetIntervalFile;
            return this;
        }

        public Builder setOutputFileName(String outputFileName) {
            this.outputFileName = outputFileName;
            return this;
        }
        
        private String targetIntervalsDescriptor;
        @Override
        public Builder addInterval(String intervals) {
            // "IR will only try to realign the regions output from RealignerTargetCreator, 
            // so there is nothing to be gained by providing the capture targets."
            // quote from http://gatkforums.broadinstitute.org/discussion/4133/when-should-i-use-l-to-pass-in-a-list-of-intervals
            this.targetIntervalsDescriptor = intervals;
            return this;
        }

        public IndelRealigner build() {

            String outputFilePath;
            if (outputFileName != null) {
                outputFilePath = outputDir + outputFileName + ".bam";
            } else {
                outputFilePath = outputDir + "gatk.realigned";
                if (intervals != null) {
                    for (String interval : intervals) {
                        outputFilePath += "." + interval.replace(":", "-");
                    }
                } else {
                    outputFilePath += "." + RandomStringUtils.randomAlphanumeric(4);
                }
                outputFilePath += ".bam";
            }

            List<String> c = build("IndelRealigner");

            for (String inputFile : inputBamFiles) {
                c.add("--input_file");
                c.add(inputFile);
            }

            c.add("--targetIntervals");
            c.add(targetIntervalFile);

            c.add("--bam_compression"); //aka -compress
            c.add("0");

            c.add("--out");
            c.add(outputFilePath);

            IndelRealigner cmd = new IndelRealigner();
            cmd.command.addAll(c);
            cmd.outputFile = outputFilePath;
            return cmd;
        }
    }

}
