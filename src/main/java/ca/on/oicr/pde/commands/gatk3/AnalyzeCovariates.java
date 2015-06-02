package ca.on.oicr.pde.commands.gatk3;

import ca.on.oicr.pde.commands.AbstractCommand;
import java.util.LinkedList;
import java.util.List;
import org.apache.commons.io.FilenameUtils;

/**
 *
 * @author mlaszloffy
 */
public class AnalyzeCovariates extends AbstractCommand {

    private String plotsReportFile;

    private AnalyzeCovariates() {
    }

    public String getPlotsReportFile() {
        return plotsReportFile;
    }

    public static class Builder extends AbstractGatkBuilder<Builder> {

        private final String rDir;
        private String recalibrationTableFile;

        public Builder(String javaPath, String maxHeapSize, String tmpDir, String gatkJarPath, String gatkKey, String rDir, String outputDir) {
            super(javaPath, maxHeapSize, tmpDir, gatkJarPath, gatkKey, outputDir);
            this.rDir = rDir;
        }

        public Builder setRecalibrationTable(String recalibrationTableFile) {
            this.recalibrationTableFile = recalibrationTableFile;
            return this;
        }

        public AnalyzeCovariates build() {

            String outputFilePath;
            if (outputFileName == null) {
                outputFileName = FilenameUtils.getBaseName(recalibrationTableFile);
            }
            outputFilePath = outputDir + outputFileName + ".bqsr.pdf";

            List<String> c = new LinkedList<>();
            c.add("PATH=" + rDir + "bin/" + ":" + "$PATH");

            c.addAll(build("AnalyzeCovariates"));

            c.add("--BQSR");
            c.add(recalibrationTableFile);

            c.add("--plotsReportFile");
            c.add(outputFilePath);

            AnalyzeCovariates cmd = new AnalyzeCovariates();
            cmd.command.addAll(c);
            cmd.plotsReportFile = outputFilePath;
            return cmd;
        }

    }

}
