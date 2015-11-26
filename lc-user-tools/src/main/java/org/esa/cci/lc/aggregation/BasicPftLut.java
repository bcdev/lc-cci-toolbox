package org.esa.cci.lc.aggregation;

import org.esa.beam.util.io.CsvReader;
import org.esa.beam.util.math.MathUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.List;

class BasicPftLut implements Lccs2PftLut {

    private static final String COMMENT_PREFIX = "#";
    private static final char[] SEPARATORS = new char[]{'|'};

    private String comment;
    private final String[] pftNames;
    private final float[][] conversionFactors;

    public static Lccs2PftLut load(Reader lccs2PftTableReader, float scaleFactor) throws Lccs2PftLutException {
        final LCCS lccs = LCCS.getInstance();
        BufferedReader bufReader = new BufferedReader(lccs2PftTableReader);
        try (CsvReader csvReader = new CsvReader(bufReader, SEPARATORS, true, COMMENT_PREFIX)) {
            String comment = readComment(bufReader);
            String[] pftNames = ensureValidNames(csvReader.readRecord());
            List<String[]> records = csvReader.readStringRecords();
            float[][] conversionFactors = new float[records.size()][pftNames.length];
            for (int i = 0; i < records.size() && i < lccs.getClassValues().length; i++) {
                String[] record = records.get(i);
                ensureCorrectClasses(i, lccs.getClassValue((short) i), record[0]);
                ensureCorrectNumFactors(i, pftNames, record);
                for (int j = 1; j < record.length; j++) {
                    float pftFactor = Float.NaN;
                    String stringValue = record[j];
                    if (!stringValue.isEmpty()) {
                        pftFactor = Float.parseFloat(stringValue) * scaleFactor;
                    }
                    conversionFactors[i][(j - 1)] = pftFactor;
                }
            }
            ensureExpectedClassCount(lccs, conversionFactors);
            ensureFactorSumIs100(conversionFactors, scaleFactor);
            return new BasicPftLut(pftNames, conversionFactors, comment);
        } catch (IOException e) {
            throw new Lccs2PftLutException("Error while reading Lccs2PftLut", e);
        }
    }

    private BasicPftLut(String[] pftNames, float[][] conversionFactors, String comment) {
        this.pftNames = pftNames;
        this.conversionFactors = conversionFactors;
        this.comment = comment;
    }

    @Override
    public String getComment() {
        return comment;
    }

    @Override
    public String[] getPFTNames() {
        return pftNames;
    }

    @Override
    public float[][] getConversionFactors() {
        return conversionFactors.clone();
    }

    @Override
    public float[] getConversionFactors(int lccsClass) {
        final int classIndex = LCCS.getInstance().getClassIndex(lccsClass);
        return conversionFactors[classIndex].clone();
    }

    @Override
    public float[] getConversionFactors(int lccsClass, int additionalUserClass) {
        throw new IllegalStateException("Not implemented yet!");
    }

    private static String[] ensureValidNames(String[] pftNames) {
        String[] newPftNames = new String[pftNames.length - 1];
        // start at 1 to skip lccci class column
        for (int i = 1; i < pftNames.length; i++) {
            newPftNames[i - 1] = pftNames[i].replaceAll("[ /]", "_");
        }
        return newPftNames;
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

    private static void ensureExpectedClassCount(LCCS lccs, float[][] conversionFactors) throws Lccs2PftLutException {
        if (conversionFactors.length != lccs.getClassValues().length) {
            final String msg = String.format(
                    "Error reading the PFT conversion table. Number of rows %d does not match " +
                            "LCCS class count %d.", conversionFactors.length, lccs.getClassValues().length);
            throw new Lccs2PftLutException(msg);
        }
    }

    private static void ensureCorrectNumFactors(int i, String[] pftNames, String[] record) throws Lccs2PftLutException {
        if (record.length - 1 != pftNames.length) {
            final String msg = String.format(
                    "Error reading the PFT conversion table in row %d. Found %d conversion factors " +
                            "but expected %d.", i, record.length - 1, pftNames.length);
            throw new Lccs2PftLutException(msg);
        }
    }

    private static void ensureCorrectClasses(int rowIndex, int expectedClass, String actualClass) throws Lccs2PftLutException {
        if (!String.valueOf(expectedClass).equals(actualClass)) {
            final String msg = String.format(
                    "Error reading the PFT conversion table in row %d. Found %s but expected %d",
                    rowIndex, actualClass, expectedClass);
            throw new Lccs2PftLutException(msg);
        }
    }

    private static void ensureFactorSumIs100(float[][] conversionFactorsArray, float scaleFactor) throws Lccs2PftLutException {
        for (int i = 0; i < conversionFactorsArray.length; i++) {
            float[] conversionFactors = conversionFactorsArray[i];
            float sum = 0;
            for (float conversionFactor : conversionFactors) {
                if (!Float.isNaN(conversionFactor)) {
                    sum += conversionFactor;
                }
            }

            final float expectedSum = 100 * scaleFactor;
            if (!MathUtils.equalValues(expectedSum, sum, 1.0e-6)) {
                final String msg = String.format(
                        "Error reading the PFT conversion table in row %d. Sum of factors is %.1f but expexted %.1f",
                        i + 1, sum, expectedSum);
                throw new Lccs2PftLutException(msg);
            }
        }

    }


}
