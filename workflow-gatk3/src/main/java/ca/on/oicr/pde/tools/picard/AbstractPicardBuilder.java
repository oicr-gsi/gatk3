/**
 * Copyright (C) 2015 Ontario Institute of Cancer Research
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * Contact us:
 *
 * Ontario Institute for Cancer Research
 * MaRS Centre, West Tower
 * 661 University Avenue, Suite 510
 * Toronto, Ontario, Canada M5G 0A3
 * Phone: 416-977-7599
 * Toll-free: 1-866-678-6427
 * www.oicr.on.ca
 *
 */
package ca.on.oicr.pde.tools.picard;

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
