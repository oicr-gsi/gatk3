package ca.on.oicr.pde.commands.picard;

import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author mlaszloffy
 */
public abstract class AbstractPicardBuilder<T> {

    protected final String javaPath;
    protected final String maxHeapSize;
    protected final String tmpDir;
    protected final String picardDir;
    protected final String outputDir;

    protected String inputFile;

    public AbstractPicardBuilder(String javaPath, String maxHeapSize, String tmpDir, String picardDir, String outputDir) {
        this.javaPath = javaPath;
        this.maxHeapSize = maxHeapSize;
        this.tmpDir = tmpDir;
        this.picardDir = picardDir;
        this.outputDir = outputDir;
    }

    public T setInputFile(String inputFile) {
        this.inputFile = inputFile;
        return (T) this;
    }

    public List<String> build(String jar) {
        List<String> c = new LinkedList<>();

        c.add(javaPath);
        c.add("-Xmx" + maxHeapSize);
        c.add("-Djava.io.tmpdir=" + tmpDir);

        c.add("-jar");
        c.add(picardDir + jar);

        c.add("VALIDATION_STRINGENCY=SILENT");

        c.add("TMP_DIR=" + tmpDir);

        return c;
    }
}
