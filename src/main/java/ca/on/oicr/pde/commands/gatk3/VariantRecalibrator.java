package ca.on.oicr.pde.commands.gatk3;

import ca.on.oicr.pde.commands.AbstractCommand;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import org.apache.commons.io.FilenameUtils;

/**
 *
 * @author mlaszloffy
 */
public class VariantRecalibrator extends AbstractCommand {

    private String recalOutputFile;
    private String tranchesOutputFile;
    private String rscriptOutputFile;
    private String plotsReportOutputFile;
    private String tranchesReportOutputFile;

    private VariantRecalibrator() {
    }

    public String getRecalOutputFile() {
        return recalOutputFile;
    }

    public String getTranchesOutputFile() {
        return tranchesOutputFile;
    }

    public String getRscriptOutputFile() {
        return rscriptOutputFile;
    }

    public String getPlotsReportOutputFile() {
        return plotsReportOutputFile;
    }

    public String getTranchesReportOutputFile() {
        return tranchesReportOutputFile;
    }

    public static class Builder extends AbstractGatkBuilder<Builder> {

        private final String rDir;
        private String inputVcfFile;
        private String outputFileName;
        private final List<String> resources = new LinkedList<>();
        private final List<String> annotations = new LinkedList<>();
        private Integer maxGaussians;

        public Builder(String javaPath, String maxHeapSize, String tmpDir, String gatkJarPath, String gatkKey, String rDir, String outputDir) {
            super(javaPath, maxHeapSize, tmpDir, gatkJarPath, gatkKey, outputDir);
            this.rDir = rDir;
        }

        public Builder setInputVcfFile(String inputVcfFile) {
            this.inputVcfFile = inputVcfFile;
            return this;
        }

        public Builder setOutputFileName(String outputFileName) {
            this.outputFileName = outputFileName;
            return this;
        }

        public Builder addResource(String resource) {
            this.resources.add(resource);
            return this;
        }

        public Builder addResources(Collection<String> resources) {
            this.resources.addAll(resources);
            return this;
        }

        public Builder addAnnotation(String annotation) {
            this.resources.add(annotation);
            return this;
        }

        public Builder addAnnotations(Collection<String> annotations) {
            this.annotations.addAll(annotations);
            return this;
        }

        public Builder setMaxGaussians(Integer maxGaussians) {
            this.maxGaussians = maxGaussians;
            return this;
        }

        public VariantRecalibrator build() {

            String recalOutputFilePath;
            String tranchesOutputFilePath;
            String rscriptOutputFilePath;
            String plotsReportOutputFilePath;
            String tranchesReportOutputFilePath;
            if (outputFileName != null) {
                recalOutputFilePath = outputDir + outputFileName + ".recal";
                tranchesOutputFilePath = outputDir + outputFileName + ".tranches";

                rscriptOutputFilePath = outputDir + outputFileName + ".plots.R";
                plotsReportOutputFilePath = outputDir + outputFileName + ".plots.R.pdf";
                tranchesReportOutputFilePath = outputDir + outputFileName + ".tranches.pdf";
            } else {
                recalOutputFilePath = outputDir + FilenameUtils.getBaseName(inputVcfFile) + ".recal";
                tranchesOutputFilePath = outputDir + FilenameUtils.getBaseName(inputVcfFile) + ".tranches";

                rscriptOutputFilePath = outputDir + FilenameUtils.getBaseName(inputVcfFile) + ".plots.R";
                plotsReportOutputFilePath = outputDir + FilenameUtils.getBaseName(inputVcfFile) + ".plots.R.pdf";
                tranchesReportOutputFilePath = outputDir + FilenameUtils.getBaseName(inputVcfFile) + ".tranches.pdf";
            }

            List<String> c = new LinkedList<>();
            c.add("PATH=" + rDir + "bin/" + "$PATH");

            c.addAll(build("VariantRecalibrator"));

            c.add("--input");
            c.add(inputVcfFile);

            for (String resource : resources) {
                String[] r = resource.split(" ");
                String descriptor = r[0];
                String filePath = r[1];
                c.add("--resource" + ":" + descriptor);
                c.add(filePath);
            }

            for (String annotation : annotations) {
                c.add("--use_annotation");
                c.add(annotation);
            }

            if (maxGaussians != null) {
                c.add("--maxGaussians");
                c.add(maxGaussians.toString());
            }

            c.add("--mode");
            c.add("SNP");

            c.add("--recal_file");
            c.add(recalOutputFilePath);

            c.add("--tranches_file");
            c.add(tranchesOutputFilePath);

            c.add("--rscript_file");
            c.add(rscriptOutputFilePath);

            VariantRecalibrator cmd = new VariantRecalibrator();
            cmd.command.addAll(c);
            cmd.recalOutputFile = recalOutputFilePath;
            cmd.tranchesOutputFile = tranchesOutputFilePath;
            cmd.rscriptOutputFile = rscriptOutputFilePath;
            cmd.plotsReportOutputFile = plotsReportOutputFilePath;
            cmd.tranchesReportOutputFile = tranchesReportOutputFilePath;
            return cmd;
        }
    }

}
