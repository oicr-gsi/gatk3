package ca.on.oicr.pde.commands.gatk3;

import ca.on.oicr.pde.commands.AbstractCommand;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.commons.io.FilenameUtils;

/**
 *
 * @author mlaszloffy
 */
public class VariantFiltration extends AbstractCommand {

    private String outputFile;

    private VariantFiltration() {
    }

    public String getOutputFile() {
        return outputFile;
    }

    public static class Builder extends AbstractGatkBuilder<Builder> {

        private final Map<String, String> filters = new LinkedHashMap<>();
        private final Map<String, String> masks = new LinkedHashMap<>();

        private String inputVcfFile;
        private String clusterWindowSize;
        private String clusterSize;

        public Builder(String javaPath, String maxHeapSize, String tmpDir, String gatkJarPath, String gatkKey, String outputDir) {
            super(javaPath, maxHeapSize, tmpDir, gatkJarPath, gatkKey, outputDir);
        }

        public Builder setInputVcfFile(String inputVcfFile) {
            this.inputVcfFile = inputVcfFile;
            return this;
        }

        public Builder setClusterWindowSize(String clusterWindowSize) {
            this.clusterWindowSize = clusterWindowSize;
            return this;
        }

        public Builder setClusterSize(String clusterSize) {
            this.clusterSize = clusterSize;
            return this;
        }

        public Builder addFilter(String name, String expression) {
            String previous = this.filters.put(name, expression);
            if (previous != null) {
                throw new RuntimeException(String.format("duplicate filter name = [%s], previous = [%s], new = [%s]", name, previous, expression));
            }
            return this;
        }

        public Builder addMask(String name, String path) {
            String previous = this.masks.put(name, path);
            if (previous != null) {
                throw new RuntimeException(String.format("duplicate mask name = [%s], previous = [%s], new = [%s]", name, previous, path));
            }
            return this;
        }

        public VariantFiltration build() {

            String outputFilePath = outputDir + FilenameUtils.getBaseName(inputVcfFile) + ".filtered.vcf";

            List<String> c = build("VariantFiltration");

            c.add("--variant");
            c.add(inputVcfFile);

            c.add("--out");
            c.add(outputFilePath);

            if (clusterWindowSize != null) {
                c.add("--clusterWindowSize");
                c.add(clusterWindowSize);
            }

            if (clusterSize != null) {
                c.add("--clusterSize");
                c.add(clusterSize);
            }

            for (Entry<String, String> e : filters.entrySet()) {
                c.add("--filterExpression");
                c.add("\"" + e.getValue() + "\"");
                c.add("--filterName");
                c.add("\"" + e.getKey() + "\"");
            }

            for (Entry<String, String> e : masks.entrySet()) {
                c.add("--mask");
                c.add(e.getValue());
                c.add("--maskName");
                c.add(e.getKey());
            }

            VariantFiltration filter = new VariantFiltration();
            filter.command.addAll(c);
            filter.outputFile = outputFilePath;

            return filter;
        }
    }

}
