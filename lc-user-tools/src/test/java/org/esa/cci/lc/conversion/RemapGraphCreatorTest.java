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
        RemapGraphCreator.GraphWriter graphWriter = new RemapGraphCreator.GraphWriter(writer);

        graphWriter.init(new String[]{"source_band", "chl", "sst", "tsm"});
        graphWriter.writeHeader();

        graphWriter.extendExpression("source_band", new String[]{"5", "3", "9", "2"});
        graphWriter.extendExpression("source_band", new String[]{"15", "0", "", "37"});

        graphWriter.finishExpressions();

        graphWriter.writeTargetBands();
        graphWriter.writeFooter();

        String result = writer.toString().replaceAll("\\s", "");
        assertTrue(result.startsWith(RemapGraphCreator.GraphWriter.GRAPH_HEAD.replaceAll("\\s", "")));
        assertTrue(result.endsWith(RemapGraphCreator.GraphWriter.GRAPH_FOOT.replaceAll("\\s", "")));
        assertTrue(result.contains(("<name>chl</name>\n" +
                                    "    <expression>\n" +
                                    "    source_band == 5 ? 3 : source_band == 15 ? 0 : 0\n" +
                                    "    </expression>").replaceAll("\\s", "")));
        assertTrue(result.contains(("<name>sst</name>\n" +
                                    "    <expression>\n" +
                                    "    source_band == 5 ? 9 : 0\n" +
                                    "    </expression>").replaceAll("\\s", "")));
        assertTrue(result.contains(("<name>tsm</name>\n" +
                                    "    <expression>\n" +
                                    "    source_band == 5 ? 2 : source_band == 15 ? 37 : 0\n" +
                                    "    </expression>").replaceAll("\\s", "")));
    }
}
