package org.esa.cci.lc.aggregation;

import org.esa.beam.util.io.CsvReader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;

/**
 * A builder for setting up a {@link Lccs2PftLut Lccs2Pft-Look-Up-Table}
 */
public class Lccs2PftLutBuilder {

    static final float DEFAULT_SCALE_FACTOR = 1.0f;

    private static final String DEFAULT_LCCS2_PFT_LUT_FILENAME = "Default_LCCS2PFT_LUT.csv";

    private Reader reader;
    private float scaleFactor;

    public Lccs2PftLutBuilder() {
        this.reader = null;
        this.scaleFactor = DEFAULT_SCALE_FACTOR;
    }

    /**
     * @param lccs2PftTableReader reader which is used to read in the Lccs2Pft-Look-Up-Table.
     * @return the current builder
     */
    public Lccs2PftLutBuilder withLccs2PftTableReader(Reader lccs2PftTableReader) {
        reader = lccs2PftTableReader;
        return this;
    }

    /**
     * @param scaleFactor used to scale the values read from the table. Default is {@code 1.0}.
     * @return the current builder
     */
    public Lccs2PftLutBuilder useScaleFactor(float scaleFactor) {
        this.scaleFactor = scaleFactor;
        return this;
    }

    public Lccs2PftLut create() throws Lccs2PftLutException {
        final Reader reader = getReader();

        return PftLut.load(reader, getScaleFactor());
    }

    private Reader getReader() {
        if (this.reader == null) {
            final InputStream inputStream = Lccs2PftLutBuilder.class.getResourceAsStream(Lccs2PftLutBuilder.DEFAULT_LCCS2_PFT_LUT_FILENAME);
            return new InputStreamReader(inputStream);
        } else {
            return this.reader;
        }
    }

    private float getScaleFactor() {
        return scaleFactor;
    }

    private static class PftLut implements Lccs2PftLut {

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
                return new PftLut(pftNames, conversionFactors, comment);
            } catch (IOException e) {
                throw new Lccs2PftLutException("Error while reading Lccs2PftLut", e);
            }
        }

        private PftLut(String[] pftNames, float[][] conversionFactors, String comment) {
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
            return conversionFactors;
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

    }
}
