package org.esa.cci.lc.aggregation;

import org.esa.beam.util.io.CsvReader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.List;

/**
 * @author Marco Peters
 */
public class PftLut {

    private static final String COMMENT_PREFIX = "#";
    private static final char[] SEPARATORS = new char[]{'|'};

    private String comment;
    private final String[] pftNames;
    private final float[][] conversionFactors;

    public static PftLut load(Reader reader) throws IOException {
        return load(reader, true, false);
    }

    public static PftLut load(Reader reader, boolean applyScaling, boolean readAllColumns) throws IOException {
        final LCCS lccs = LCCS.getInstance();
        BufferedReader bufReader = new BufferedReader(reader);
        String comment = readComment(bufReader);
        try (CsvReader csvReader = new CsvReader(bufReader, SEPARATORS, true, COMMENT_PREFIX)) {
            String[] pftNames = ensureValidNames(csvReader.readRecord());
            List<String[]> records = csvReader.readStringRecords();
            int conversionFactorCount = pftNames.length + (readAllColumns ? 1 : 0);
            float[][] conversionFactors = new float[records.size()][conversionFactorCount];
            for (int i = 0; i < records.size() && i < lccs.getClassValues().length; i++) {
                String[] record = records.get(i);
                if (! String.valueOf(lccs.getClassValue((short) i)).equals(record[0])) {
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
                        if (applyScaling) {
                            pftFactor = Float.parseFloat(stringValue) / 100.0f;
                        } else {
                            pftFactor = Float.parseFloat(stringValue);
                        }
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
