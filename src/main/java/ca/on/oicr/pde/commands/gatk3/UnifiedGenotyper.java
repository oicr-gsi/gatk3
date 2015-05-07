package ca.on.oicr.pde.commands.gatk3;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author mlaszloffy
 */
public class UnifiedGenotyper {

    private final List<String> command = new LinkedList<>();

    private String outputFile;

    private UnifiedGenotyper() {
    }

    public List<String> getCommand() {
        return Collections.unmodifiableList(command);
    }

    public String getOutputFile() {
        return outputFile;
    }

    public static class Builder extends AbstractGatkBuilder<Builder> {

        private String inputBamFile;
        private String dbsnpFilePath;
        private String standardCallConfidence;
        private String standardEmitConfidence;
        private String genotypeLikelihoodsModel;
        private String group;
        private String outputFileName;

        public Builder(String javaPath, String maxHeapSize, String tmpDir, String gatkJarPath, String gatkKey, String outputDir) {
            super(javaPath, maxHeapSize, tmpDir, gatkJarPath, gatkKey, outputDir);
        }

        public Builder setInputBamFile(String inputBamFile) {
            this.inputBamFile = inputBamFile;
            return this;
        }

        public Builder setDbsnpFilePath(String dbsnpFilePath) {
            this.dbsnpFilePath = dbsnpFilePath;
            return this;
        }

        public Builder setStandardCallConfidence(String standardCallConfidence) {
            this.standardCallConfidence = standardCallConfidence;
            return this;
        }

        public Builder setStandardEmitConfidence(String standardEmitConfidence) {
            this.standardEmitConfidence = standardEmitConfidence;
            return this;
        }

        public Builder setGenotypeLikelihoodsModel(String genotypeLikelihoodsModel) {
            this.genotypeLikelihoodsModel = genotypeLikelihoodsModel;
            return this;
        }

        public Builder setGroup(String group) {
            this.group = group;
            return this;
        }

        public Builder setOutputFileName(String outputFileName) {
            this.outputFileName = outputFileName;
            return this;
        }

        public UnifiedGenotyper build() {

            String outputFilePath;
            //String outputFilePath = outputDir + FilenameUtils.getBaseName(inputBamFile) + ".filtered.vcf";
            if (outputFileName != null) {
                outputFilePath = outputDir + outputFileName;
            } else {
                outputFilePath = outputDir + "gatk"; //FilenameUtils.getBaseName(inputBamFile);
                if (genotypeLikelihoodsModel != null) {
                    outputFilePath += "." + StringUtils.lowerCase(genotypeLikelihoodsModel) + ".raw";
                }
                if (!intervals.isEmpty()) {
                    for (String interval : intervals) {
                        outputFilePath += "." + interval.replace(":", "-");
                    }
                } else {
                    outputFilePath += "." + RandomStringUtils.randomAlphanumeric(4);
                }
                outputFilePath += ".vcf";
            }

            List<String> c = build("UnifiedGenotyper");

            c.add("--input_file");
            c.add(inputBamFile);

            c.add("--dbsnp");
            c.add(dbsnpFilePath);

            c.add("--computeSLOD");

            if (standardCallConfidence != null) {
                c.add("--standard_min_confidence_threshold_for_calling");
                c.add(standardCallConfidence);
            }

            if (standardEmitConfidence != null) {
                c.add("--standard_min_confidence_threshold_for_emitting");
                c.add(standardEmitConfidence);
            }

            if (genotypeLikelihoodsModel != null) {
                c.add("--genotype_likelihoods_model");
                c.add(genotypeLikelihoodsModel);
            }

            if (group != null) {
                c.add("--group");
                c.add(group);
            }

            c.add("--out");
            c.add(outputFilePath);

            UnifiedGenotyper filter = new UnifiedGenotyper();
            filter.command.addAll(c);
            filter.outputFile = outputFilePath;

            return filter;
        }
    }

}
