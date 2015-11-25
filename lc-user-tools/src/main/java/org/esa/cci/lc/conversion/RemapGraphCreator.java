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

import org.esa.cci.lc.aggregation.LCCS;
import org.esa.cci.lc.aggregation.Lccs2PftLut;
import org.esa.cci.lc.aggregation.Lccs2PftLutBuilder;
import org.esa.cci.lc.aggregation.Lccs2PftLutException;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

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

        String lutFile = args[0];
        String outputFileName = args[1];

        createGraphFile(lutFile, outputFileName);
    }

    static File createGraphFile(String lutFile, String outputFileName) {
        File outputFile = new File(GRAPH_FILENAME);
        try (Writer writer = new FileWriter(outputFile)) {
            GraphWriter graphWriter = new GraphWriter(writer, lutFile);

            try (Reader fr = new FileReader(lutFile)) {
                final Lccs2PftLutBuilder lutBuilder = new Lccs2PftLutBuilder().useLccs2PftTable(fr);
                Lccs2PftLut lut = lutBuilder.create();
                graphWriter.init(lut.getPFTNames());
                graphWriter.writeHeader();

                final LCCS lccs = LCCS.getInstance();
                final short[] classValues = lccs.getClassValues();
                float[][] conversionFactors = lut.getConversionFactors();
                for (int i = 0; i < conversionFactors.length; i++) {
                    float[] conversionFactorsRecord = conversionFactors[i];
                    graphWriter.extendExpression(SOURCE_BAND_NAME, classValues[i], conversionFactorsRecord);
                }

                graphWriter.finishExpressions();
                graphWriter.writeTargetBands();

            }
            graphWriter.writeFooter(outputFileName);
        } catch (IOException | Lccs2PftLutException e) {
            throw new IllegalStateException(e);
        }
        return outputFile;
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
        private List<TargetBandSpec> targetBandSpecs;

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

        void extendExpression(String sourceBandName, int lccsClass, float[] record) {
            for (int i = 0; i < record.length; i++) {
                if (Float.isNaN(record[i])) {
                    continue;
                }
                TargetBandSpec targetBandSpec = targetBandSpecs.get(i);
                targetBandSpec.expression +=
                        sourceBandName + " == " + format(lccsClass) + " ? " + format(record[i]) + " : ";
            }
        }

        private static String format(float value) {
            int i = (int) value;
            return value == i ? String.valueOf(i) : String.valueOf(value);
        }

        void finishExpressions() {
            for (TargetBandSpec targetBandSpec : targetBandSpecs) {
                targetBandSpec.expression += "0";
            }
        }

        void writeTargetBands() throws IOException {
            for (TargetBandSpec targetBandSpec : targetBandSpecs) {
                writer.append(String.format(
                        "<targetBand>\n" +
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

        private List<TargetBandSpec> createTargetBandSpecs(String[] header) {
            List<TargetBandSpec> list = new ArrayList<>();
            for (String aHeader : header) {
                list.add(new TargetBandSpec(aHeader));
            }
            return list;
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
