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
package ca.on.oicr.pde.tools;

import com.google.common.collect.Sets;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 *
 * @author mlaszloffy
 */
public class BEDFileUtilsTest {

    @Test
    public void singleBedFile() throws IOException {
        Set<String> expected = Sets.newHashSet("chr1", "chr2", "chr3", "chr4", "chr6", "chr7", "chr9", "chr10", "chr12", "chr14", "chr15", "chr16", "chr17", "chr19");
        Assert.assertEquals(BEDFileUtils.getChromosomes("src/test/resources/BED/test.bed"), expected);
    }

    @Test
    public void multipleBedFiles() throws IOException {
        Set<String> expected = Sets.newHashSet("chr1", "chr2", "chr3", "chr4", "chr6", "chr7", "chr9", "chr10", "chr12", "chr14", "chr15", "chr16", "chr17", "chr19", "chrX");
        Assert.assertEquals(BEDFileUtils.getChromosomes(Arrays.asList("src/test/resources/BED/test.bed", "src/test/resources/BED/chrX.bed")), expected);
    }

    @Test(expectedExceptions = Exception.class)
    public void badBedFile() throws IOException {
        BEDFileUtils.getChromosomes(Arrays.asList("src/test/resources/BED/test.bed", "src/test/resources/BED/bad.bed"));
    }

    @Test(expectedExceptions = FileNotFoundException.class)
    public void missingFile() throws IOException {
        BEDFileUtils.getChromosomes("/does/not/exist.bed");
    }

}
