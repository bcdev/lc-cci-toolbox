package org.esa.cci.lc.aggregation;

import org.esa.beam.util.io.CsvReader;
import org.esa.beam.util.math.MathUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

class AdditionalMapPftLut implements Lccs2PftLut {
    private static final String COMMENT_PREFIX = "#";
    private static final char[] SEPARATORS = new char[]{'|'};

    private final Lccs2PftLut basicPftLut;
    private final String comment;
    private final Map<Integer, Map<Integer, float[]>> mappingTable;

    public static Lccs2PftLut create(Lccs2PftLut basicPftLut, Reader additionalPftLutReader, float scaleFactor) throws Lccs2PftLutException {
        BufferedReader bufReader = new BufferedReader(additionalPftLutReader);
        try (CsvReader csvReader = new CsvReader(bufReader, SEPARATORS, true, COMMENT_PREFIX)) {
            String comment = readComment(bufReader);
            String[] pftNames = csvReader.readRecord();
            List<String[]> records = csvReader.readStringRecords();
            final Map<Integer, Map<Integer, float[]>> mappingTable = new TreeMap<>();
            for (String[] record : records) {
                final int lccsClass = Integer.parseInt(record[0]);
                ensureLccsClassIsKnown(lccsClass);
                final int userMapClass = Integer.parseInt(record[1]);
                float[] conversionFactors = new float[pftNames.length - 2];
                for (int j = 2; j < record.length; j++) {
                    float pftFactor = Float.NaN;
                    String stringValue = record[j];
                    if (!stringValue.isEmpty()) {
                        pftFactor = Float.parseFloat(stringValue) * scaleFactor;
                    }
                    conversionFactors[j - 2] = pftFactor;
                }
                ensureFactorSumIs100(lccsClass, userMapClass, conversionFactors, scaleFactor);
                addToMappingTable(mappingTable, lccsClass, userMapClass, conversionFactors);
            }
            return new AdditionalMapPftLut(basicPftLut, comment, mappingTable);
        } catch (IOException e) {
            throw new Lccs2PftLutException("Error while reading Lccs2PftLut", e);
        }

    }

    private AdditionalMapPftLut(Lccs2PftLut basicPftLut, String comment, Map<Integer, Map<Integer, float[]>> mappingTable) {
        this.basicPftLut = basicPftLut;
        this.comment = comment;
        this.mappingTable = mappingTable;
    }

    @Override
    public String getComment() {
        return basicPftLut.getComment() + " + " + comment;
    }

    @Override
    public String[] getPFTNames() {
        return basicPftLut.getPFTNames().clone();
    }

    @Override
    public float[] getConversionFactors(int lccsClass) {
        // or throw IllegalStateException?
        return basicPftLut.getConversionFactors(lccsClass);
    }

    @Override
    public float[] getConversionFactors(int lccsClass, int additionalUserClass) {
        float[] conversionFactors = null;
        if (mappingTable.containsKey(lccsClass)) {
            final Map<Integer, float[]> additionalMap = mappingTable.get(lccsClass);
            conversionFactors = additionalMap.get(additionalUserClass);
        }
        if (conversionFactors == null) {
            conversionFactors = basicPftLut.getConversionFactors(lccsClass);
        }
        return conversionFactors;
    }

    private static void addToMappingTable(Map<Integer, Map<Integer, float[]>> mappingTable,
                                          int lccsClass, int userMapClass, float[] conversionFactors) throws Lccs2PftLutException {
        Map<Integer, float[]> userMap;
        if (mappingTable.containsKey(lccsClass)) {
            userMap = mappingTable.get(lccsClass);
        } else {
            userMap = new TreeMap<>();
        }
        if (userMap.containsKey(userMapClass)) {
            throw new Lccs2PftLutException(String.format("User map class %d is duplicated for LCCS class %d", userMapClass, lccsClass));
        }
        userMap.put(userMapClass, conversionFactors);
        mappingTable.put(lccsClass, userMap);
    }

    private static String readComment(BufferedReader reader) throws IOException {
        reader.mark(512);
        String line = reader.readLine();
        reader.reset();
        String comment = null;
        if (line.startsWith(COMMENT_PREFIX)) {
            comment = line.substring(1).trim();
        }
        return comment;
    }

    private static void ensureLccsClassIsKnown(int lccsClass) throws Lccs2PftLutException {
        final LCCS lccs = LCCS.getInstance();
        if (Arrays.binarySearch(lccs.getClassValues(), lccsClass) < 0) {
            throw new Lccs2PftLutException(String.format("Unknown LCCS class (%d) used in additional user map table", lccsClass));
        }
    }

    private static void ensureFactorSumIs100(int lccsClass, int additionalMap, float[] conversionFactors, float scaleFactor) throws Lccs2PftLutException {
        float sum = 0;
        for (float conversionFactor : conversionFactors) {
            if (!Float.isNaN(conversionFactor)) {
                sum += conversionFactor;
            }
        }

        final float expectedSum = 100 * scaleFactor;
        if (!MathUtils.equalValues(expectedSum, sum, 1.0e-6)) {
            final String msg = String.format(
                    "Error reading the additional user map PFT conversion table in class [%d, %d]. Sum of factors is %.1f but expexted %.1f",
                    lccsClass, additionalMap, sum, expectedSum);
            throw new Lccs2PftLutException(msg);
        }
    }


}
