package ca.on.oicr.pde.commands.gatk3;

import ca.on.oicr.pde.commands.AbstractCommand;
import java.util.List;
import org.apache.commons.io.FilenameUtils;

/**
 *
 * @author mlaszloffy
 */
public class PrintReads extends AbstractCommand {

    private String outputFile;

    private PrintReads() {
    }

    public String getOutputFile() {
        return outputFile;
    }

    public static class Builder extends AbstractGatkBuilder<Builder> {

        private String covariatesTablesFile;
        private Integer preserveQscoresLessThan;
        private String inputFile;

        public Builder(String javaPath, String maxHeapSize, String tmpDir, String gatkJarPath, String gatkKey, String outputDir) {
            super(javaPath, maxHeapSize, tmpDir, gatkJarPath, gatkKey, outputDir);
        }

        public Builder setCovariatesTablesFile(String covariatesTablesFile) {
            this.covariatesTablesFile = covariatesTablesFile;
            return this;
        }

        public Builder setPreserveQscoresLessThan(Integer preserveQscoresLessThan) {
            this.preserveQscoresLessThan = preserveQscoresLessThan;
            return this;
        }

        public Builder setInputFile(String inputFile) {
            this.inputFile = inputFile;
            return this;
        }

        public PrintReads build() {

            String outputFilePath = outputDir + FilenameUtils.getBaseName(inputFile) + ".recal.bam";

            List<String> c = build("PrintReads");

            c.add("--BQSR");
            c.add(covariatesTablesFile);

            c.add("--input_file");
            c.add(inputFile);

            if (preserveQscoresLessThan != null) {
                c.add("--preserve_qscores_less_than");
                c.add(preserveQscoresLessThan.toString());
            }

            c.add("--out");
            c.add(outputFilePath);

            PrintReads cmd = new PrintReads();
            cmd.command.addAll(c);
            cmd.outputFile = outputFilePath;
            return cmd;
        }

    }
}
