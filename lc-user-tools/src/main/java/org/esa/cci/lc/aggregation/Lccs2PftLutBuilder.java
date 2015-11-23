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

    static final float DEFAULT_SCALE_FACTOR = 1 / 100.0f;

    private static final String DEFAULT_LCCS2_PFT_LUT_FILENAME = "Default_LCCS2PFT_LUT.csv";

    private Reader reader;
    private float scaleFactor;
    private boolean readAllColumns;

    public Lccs2PftLutBuilder() {
        this.reader = null;
        this.scaleFactor = DEFAULT_SCALE_FACTOR;
        this.readAllColumns = false;
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
     * @param scaleFactor used to scale the values read from the table. Default is {@code 1/100}.
     * @return the current builder
     */
    public Lccs2PftLutBuilder useScaleFactor(float scaleFactor) {
        this.scaleFactor = scaleFactor;
        return this;
    }

    /**
     * @param readAllColumns uses all columns. Default is {@code false}.
     *                       {@code True} is used in the remap case.
     * @return the current builder
     */
    public Lccs2PftLutBuilder readAllColumns(boolean readAllColumns) {
        this.readAllColumns = readAllColumns;
        return this;
    }


    public Lccs2PftLut create() throws IOException {
        final Reader reader = getReader();

        return PftLut.load(reader, isReadAllColumns(), getScaleFactor());
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

    private boolean isReadAllColumns() {
        return readAllColumns;
    }

    private static class PftLut implements Lccs2PftLut {

        private static final String COMMENT_PREFIX = "#";
        private static final char[] SEPARATORS = new char[]{'|'};

        private String comment;
        private final String[] pftNames;
        private final float[][] conversionFactors;

        public static Lccs2PftLut load(Reader lccs2PftTableReader, boolean readAllColumns, float scaleFactor) throws IOException {
            final LCCS lccs = LCCS.getInstance();
            BufferedReader bufReader = new BufferedReader(lccs2PftTableReader);
            String comment = readComment(bufReader);
            try (CsvReader csvReader = new CsvReader(bufReader, SEPARATORS, true, COMMENT_PREFIX)) {
                String[] pftNames = ensureValidNames(csvReader.readRecord());
                List<String[]> records = csvReader.readStringRecords();
                int conversionFactorCount = pftNames.length + (readAllColumns ? 1 : 0);
                float[][] conversionFactors = new float[records.size()][conversionFactorCount];
                for (int i = 0; i < records.size() && i < lccs.getClassValues().length; i++) {
                    String[] record = records.get(i);
                    if (!String.valueOf(lccs.getClassValue((short) i)).equals(record[0])) {
                        final String format = String.format(
                                "Error reading the PFT conversion table. In row %d the name %s of the LCCS class " +
                                        "should be %d.", i, record[0], lccs.getClassValue((short) i));
                        throw new IOException(format);
                    }
                    if (record.length - 1 != pftNames.length) {
                        final String format = String.format(
                                "Error reading the PFT conversion table. In row %d the number of conversion factors " +
                                        "should be %d.", i, pftNames.length);
                        throw new IOException(format);
                    }
                    int firstColumn = readAllColumns ? 0 : 1;
                    for (int j = firstColumn; j < record.length; j++) {
                        float pftFactor = Float.NaN;
                        String stringValue = record[j];
                        if (!stringValue.isEmpty()) {
                            pftFactor = Float.parseFloat(stringValue) * scaleFactor;
                        }
                        int conversionFactorIndex = readAllColumns ? j : j - 1;
                        conversionFactors[i][conversionFactorIndex] = pftFactor;
                    }
                }
                if (records.size() != lccs.getClassValues().length) {
                    final String format = String.format(
                            "Error reading the PFT conversion table. Number of rows %d does not match " +
                                    "LCCS class count %d.", records.size(), lccs.getClassValues().length);
                    throw new IOException(format);
                }
                return new PftLut(pftNames, conversionFactors, comment);
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
    }
}
