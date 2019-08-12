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
 * <p>
 * Contact us:
 * <p>
 * Ontario Institute for Cancer Research
 * MaRS Centre, West Tower
 * 661 University Avenue, Suite 510
 * Toronto, Ontario, Canada M5G 0A3
 * Phone: 416-977-7599
 * Toll-free: 1-866-678-6427
 * www.oicr.on.ca
 * <p>
 */
package ca.on.oicr.pde.tools;

import htsjdk.tribble.bed.BEDCodec;
import htsjdk.tribble.bed.BEDFeature;
import htsjdk.tribble.readers.AsciiLineReader;
import htsjdk.tribble.readers.LineIteratorImpl;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import org.apache.commons.io.FileUtils;

/**
 *
 * @author mlaszloffy
 */
public class BEDFileUtils {

    /**
     * Gets the set (no duplicates) of chromosomes from a collection of bed files.
     *
     * @param bedFiles A collection of bed file paths
     *
     * @return The set of chromosomes (no duplicates)
     */
    public static Set<String> getChromosomes(Collection<String> bedFiles) throws FileNotFoundException, IOException {
        Set<String> chrs = new LinkedHashSet<>();
        for (String bedFile : bedFiles) {
            chrs.addAll(BEDFileUtils.getChromosomes(bedFile));
        }
        return chrs;
    }

    /**
     * Gets the set (no duplicates) of chromosomes from a bed file.
     *
     * @param bedFilePath Path to the bed file.
     *
     * @return The set of chromosomes (no duplicates)
     */
    public static Set<String> getChromosomes(String bedFilePath) throws FileNotFoundException, IOException {
        Set<String> chrs = new LinkedHashSet<>();
        File bedFile = FileUtils.getFile(bedFilePath);
        try (InputStream is = new FileInputStream(bedFile);
                AsciiLineReader alr = new AsciiLineReader(is);
                LineIteratorImpl lineIterator = new LineIteratorImpl(alr);) {
            BEDCodec bc = new BEDCodec();
            while (!bc.isDone(lineIterator)) {
                BEDFeature bf = bc.decode(lineIterator);
                if (bf != null) {
                    //see https://github.com/samtools/htsjdk/issues/197
                    chrs.add(bf.getContig());
                }
            }
        }
        return chrs;
    }
}
