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

import org.esa.beam.util.StringUtils;
import org.esa.beam.util.io.CsvReader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>Simple main class that creates a GPT-compliant band maths graph XML file from an input CSV table.</p>
 * <p/>
 * <p>Input CSV format:</p>
 * <p/>
 * <p>&lt;source band name&gt;|&lt;target band name 1&gt;|&lt;target band name 2&gt;| ...</p>
 * <p>120|20|30| ...</p>
 * <br/>
 * <p>Meaning:</p>
 * <p>For each pixel: if the source band has the value 120, target band 1 is assigned the value 20, target band 2 the value 30.</p>
 *
 * @author thomas
 */
public class RemapGraphCreator {

    private static final String GRAPH_FILENAME = "remap_graph.xml";
    private static final String SOURCE_BAND_NAME = "lccs_class";

    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Usage:\n    RemapGraphCreator <input.csv> <outputFileName>");
            System.exit(-1);
        }

        if (!new File(args[0]).exists()) {
            System.out.println("File: " + args[0] + " does not exist. System will exit.");
            System.exit(-1);
        }

        try (Writer writer = new FileWriter(GRAPH_FILENAME)) {
            String lutFile = args[0];
            GraphWriter graphWriter = new GraphWriter(writer, lutFile);

            try (Reader fr = new FileReader(lutFile)) {
                CsvReader reader = new CsvReader(fr, new char[]{'|'});
                String[] header = reader.readRecord();
                graphWriter.init(header);
                graphWriter.writeHeader();

                String[] record;
                while ((record = reader.readRecord()) != null) {
                    graphWriter.extendExpression(SOURCE_BAND_NAME, record);
                }

                graphWriter.finishExpressions();
                graphWriter.writeTargetBands();

            }
            graphWriter.writeFooter(args[1]);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    static class GraphWriter {

        static final String GRAPH_HEAD =
                "  <graph id=\"remapClasses\">\n" +
                "    <version>1.0</version>\n" +
                "    <node id=\"remapClassesNode\">\n" +
                "      <operator>BandMaths</operator>\n" +
                "      <sources>\n" +
                "        <sourceProducts>${sourceProducts}</sourceProducts>\n" +
                "      </sources>\n" +
                "      <parameters>\n" +
                "        <targetBands>";


        static final String GRAPH_FOOT =
                "        </targetBands>\n" +
                "      </parameters>\n" +
                "    </node>\n" +
                "    <node id=\"merge\">\n" +
                "      <operator>Merge</operator>\n" +
                "      <sources>\n" +
                "        <masterProduct>${sourceProducts}</masterProduct>\n" +
                "        <sourceProducts>remapClassesNode</sourceProducts>\n" +
                "      </sources>\n" +
                "    </node>\n" +
                "    <node id=\"write\">\n" +
                "        <operator>Write</operator>\n" +
                "        <sources>\n" +
                "            <source>merge</source>\n" +
                "        </sources>\n" +
                "        <parameters>\n" +
                "            <formatName>NetCDF4-LC-Map</formatName>\n" +
                "            <writeEntireTileRows>false</writeEntireTileRows>\n" +
                "            <clearCacheAfterRowWrite>true</clearCacheAfterRowWrite>\n" +
                "            <file>%s</file>\n" +
                "        </parameters>\n" +
                "    </node>\n" +
                "  </graph>";

        private final Writer writer;
        private final String lutName;
        private Map<Integer, TargetBandSpec> targetBandSpecs;

        GraphWriter(Writer writer, String lutName) {
            this.writer = writer;
            this.lutName = lutName;
        }

        void writeHeader() throws IOException {
            writer.append(GRAPH_HEAD);
        }

        void writeFooter(String outputFileName) throws IOException {
            writer.append(String.format(GRAPH_FOOT, outputFileName));
        }

        void init(String[] header) {
            targetBandSpecs = createTargetBandSpecs(header);
        }

        void extendExpression(String sourceBand, String[] record) {
            for (int i = 1; i < record.length; i++) {
                if (StringUtils.isNullOrEmpty(record[i])) {
                    continue;
                }
                TargetBandSpec targetBandSpec = targetBandSpecs.get(i);
                targetBandSpec.expression += sourceBand + " == " + record[0] + " ? " + record[i] + " : ";
            }
        }

        void finishExpressions() {
            for (TargetBandSpec targetBandSpec : targetBandSpecs.values()) {
                targetBandSpec.expression += "0";
            }
        }

        void writeTargetBands() throws IOException {
            for (TargetBandSpec targetBandSpec : targetBandSpecs.values()) {
                writer.append(String.format(
                        "<targetBand>" +
                        "    <name>%s</name>\n" +
                        "    <expression>\n" +
                        "    %s\n" +
                        "    </expression>\n" +
                        "    <description>%s as defined in %s</description>\n" +
                        "    <type>int16</type>\n" +
                        "    <noDataValue>0</noDataValue>\n" +
                        "    <scalingFactor>0.01</scalingFactor>\n" +
                        "</targetBand>",
                        targetBandSpec.name, targetBandSpec.expression, targetBandSpec.name, lutName));
            }
        }

        private Map<Integer, TargetBandSpec> createTargetBandSpecs(String[] header) {
            HashMap<Integer, TargetBandSpec> map = new HashMap<>();
            for (int i = 1; i < header.length; i++) {
                map.put(i, new TargetBandSpec(header[i]));
            }
            return map;
        }

    }

    static class TargetBandSpec {

        String name;
        String expression = "";

        TargetBandSpec(String name) {
            this.name = escape(name);
        }

        private static String escape(String name) {
            return name.replace("/", "_");
        }
    }
}
