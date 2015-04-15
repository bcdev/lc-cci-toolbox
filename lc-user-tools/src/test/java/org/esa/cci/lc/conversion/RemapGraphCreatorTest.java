/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
 * 
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 * 
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.cci.lc.conversion;

import org.apache.commons.lang.StringUtils;
import org.junit.Test;

import java.io.StringWriter;

import static org.junit.Assert.*;

/**
 * @author thomas
 */
public class RemapGraphCreatorTest {

    @Test
    public void testCreateGraph() throws Exception {
        StringWriter writer = new StringWriter();
        RemapGraphCreator.GraphWriter graphWriter = new RemapGraphCreator.GraphWriter(writer, "any_lut.csv");

        graphWriter.init(new String[]{"chl", "sst", "tsm"});
        graphWriter.writeHeader();

        graphWriter.extendExpression("source_band", new float[]{5F, 3F, 9F, 2F});
        graphWriter.extendExpression("source_band", new float[]{15F, 0F, Float.NaN, 37F});

        graphWriter.finishExpressions();

        graphWriter.writeTargetBands();
        graphWriter.writeFooter("schlumpf");

        String result = removeSpaces(writer.toString());
        assertTrue(result.startsWith(removeSpaces(RemapGraphCreator.GraphWriter.GRAPH_HEAD)));
        assertTrue(result.endsWith(removeSpaces(String.format(RemapGraphCreator.GraphWriter.GRAPH_FOOT, "schlumpf"))));
        assertTrue(result.contains(removeSpaces("<name>chl</name>\n" +
                                                "    <expression>\n" +
                                                "    source_band == 5 ? 3 : source_band == 15 ? 0 : 0\n" +
                                                "    </expression>\n" +
                                                "    <description>chl as defined in any_lut.csv</description>")));
        assertTrue(result.contains(removeSpaces("<name>sst</name>\n" +
                                                "    <expression>\n" +
                                                "    source_band == 5 ? 9 : 0\n" +
                                                "    </expression>\n" +
                                                "    <description>sst as defined in any_lut.csv</description>")));
        assertTrue(result.contains(removeSpaces("<name>tsm</name>\n" +
                                                "    <expression>\n" +
                                                "    source_band == 5 ? 2 : source_band == 15 ? 37 : 0\n" +
                                                "    </expression>\n" +
                                                "    <description>tsm as defined in any_lut.csv</description>")));
        assertEquals(3, StringUtils.countMatches(result, "<scalingFactor>0.01</scalingFactor>"));
    }

    private static String removeSpaces(String string) {
        return string.replaceAll("\\s", "");
    }
}
