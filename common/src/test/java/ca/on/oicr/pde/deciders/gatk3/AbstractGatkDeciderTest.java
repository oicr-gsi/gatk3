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
package ca.on.oicr.pde.deciders.gatk3;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 *
 * @author mlaszloffy
 */
public class AbstractGatkDeciderTest {

    public AbstractGatkDeciderTest() {
    }

    @Test
    public void testSanitize() {
        Assert.assertEquals(AbstractGatkDecider.sanitize("ID+ID../~!@#$%^&*()=$(ls /)`ls /`"), "ID+ID..lsls");
        Assert.assertEquals(AbstractGatkDecider.sanitize("TEST_001+TEST_002_R+P_Ly+Pr"), "TEST_001+TEST_002_R+P_Ly+Pr");
    }

}
