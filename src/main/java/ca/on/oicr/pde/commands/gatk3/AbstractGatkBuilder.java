package ca.on.oicr.pde.commands.gatk3;

import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author mlaszloffy
 */
public abstract class AbstractGatkBuilder<T> {

    protected final String javaPath;
    protected final String maxHeapSize;
    protected final String tmpDir;
    protected final String gatkJarPath;
    protected final String gatkKey;
    protected final String outputDir;

    protected String referenceSequence;
    protected String intervals;
    protected String numThreads;

    public AbstractGatkBuilder(String javaPath, String maxHeapSize, String tmpDir, String gatkJarPath, String gatkKey, String outputDir) {
        this.javaPath = javaPath;
        this.maxHeapSize = maxHeapSize;
        this.tmpDir = tmpDir;
        this.gatkJarPath = gatkJarPath;
        this.gatkKey = gatkKey;
        this.outputDir = outputDir;
    }

    public T setReferenceSequence(String referenceSequence) {
        this.referenceSequence = referenceSequence;
        return (T) this;
    }

    public T setIntervals(String intervals) {
        this.intervals = intervals;
        return (T) this;
    }

    public T setNumThreads(String numThreads) {
        this.numThreads = numThreads;
        return (T) this;
    }

    protected List<String> build(String type) {
        List<String> c = new LinkedList<>();
        
        c.add(javaPath);
        c.add("-Xmx" + maxHeapSize);
        c.add("-Djava.io.tmpdir=" + tmpDir);

        c.add("-jar");
        c.add(gatkJarPath);

        c.add("--analysis_type");
        c.add(type);

        c.add("--phone_home");
        c.add("NO_ET");
        c.add("--gatk_key");
        c.add(gatkKey);

        c.add("--reference_sequence");
        c.add(referenceSequence);

        if (intervals != null) {
            c.add("--intervals"); //aka -L
            c.add(intervals);
        }

        if (numThreads != null) {
            c.add("--num_threads");
            c.add(numThreads);
        }

        return c;
    }

}
