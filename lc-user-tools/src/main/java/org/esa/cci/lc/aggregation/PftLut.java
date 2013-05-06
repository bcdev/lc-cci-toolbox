package org.esa.cci.lc.aggregation;

import org.esa.beam.util.io.CsvReader;

import java.io.IOException;
import java.io.Reader;
import java.util.List;

/**
 * @author Marco Peters
 */
class PftLut {

    private final String[] pftNames;
    private final float[][] conversionFactors;

    public static PftLut load(Reader reader) throws IOException {
        CsvReader csvReader = new CsvReader(reader, new char[]{'|'});
        try {
            String[] pftNames = ensureValidNames(csvReader.readRecord());
            List<String[]> records = csvReader.readStringRecords();
            float[][] conversionFactors = new float[records.size()][pftNames.length];
            for (int i = 0; i < records.size(); i++) {
                String[] record = records.get(i);
                for (int j = 0; j < record.length; j++) {
                    float pftFactor = Float.NaN;
                    String stringValue = record[j];
                    if (!stringValue.isEmpty()) {
                        pftFactor = Float.parseFloat(stringValue) / 100.0f;
                    }
                    conversionFactors[i][j] = pftFactor;
                }
            }
            return new PftLut(pftNames, conversionFactors);
        } finally {
            csvReader.close();
        }
    }

    private PftLut(String[] pftNames, float[][] conversionFactors) {
        this.pftNames = pftNames;
        this.conversionFactors = conversionFactors;
    }

    private static String[] ensureValidNames(String[] pftNames) {
        for (int i = 0; i < pftNames.length; i++) {
            pftNames[i] = pftNames[i].replaceAll("[ /]", "_");
        }
        return pftNames;
    }

    public String[] getPFTNames() {
        return pftNames;
    }

    public float[][] getConversionFactors() {
        return conversionFactors;
    }
}
