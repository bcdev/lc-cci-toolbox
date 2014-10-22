package org.esa.cci.lc.aggregation;

import org.esa.beam.util.io.CsvReader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.List;

/**
 * @author Marco Peters
 */
class PftLut {

    private static final String COMMENT_PREFIX = "#";
    private static final char[] SEPARATORS = new char[]{'|'};

    private String comment;
    private final String[] pftNames;
    private final float[][] conversionFactors;

    public static PftLut load(Reader reader) throws IOException {
        BufferedReader bufReader = new BufferedReader(reader);
        String comment = readComment(bufReader);
        try (CsvReader csvReader = new CsvReader(bufReader, SEPARATORS, true, COMMENT_PREFIX)) {
            String[] pftNames = ensureValidNames(csvReader.readRecord());
            List<String[]> records = csvReader.readStringRecords();
            float[][] conversionFactors = new float[records.size()][pftNames.length];
            for (int i = 0; i < records.size(); i++) {
                String[] record = records.get(i);
                if (record.length - 1 != pftNames.length) {
                    final String format = String.format(
                            "Error reading the PFT conversion table. In row %d the number of conversion factors " +
                            "should be %d.", i, pftNames.length);
                    throw new IOException(format);
                }
                for (int j = 1; j < record.length; j++) {
                    float pftFactor = Float.NaN;
                    String stringValue = record[j];
                    if (!stringValue.isEmpty()) {
                        pftFactor = Float.parseFloat(stringValue) / 100.0f;
                    }
                    conversionFactors[i][j - 1] = pftFactor;
                }
            }
            return new PftLut(pftNames, conversionFactors, comment);
        }
    }

    private PftLut(String[] pftNames, float[][] conversionFactors, String comment) {
        this.pftNames = pftNames;
        this.conversionFactors = conversionFactors;
        this.comment = comment;
    }

    public String getComment() {
        return comment;
    }

    public String[] getPFTNames() {
        return pftNames;
    }

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
