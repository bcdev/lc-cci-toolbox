package org.esa.cci.lc.aggregation;

import org.esa.beam.util.io.CsvReader;

import java.io.IOException;
import java.io.Reader;
import java.util.List;

/**
 * @author Marco Peters
 */
public class PftLut {

    private final String[] pftnames;
    private final double[][] conversionFactors;

    private Object PFTNames;

    public PftLut(String[] pftnames, double[][] conversionFactors) {
        this.pftnames = pftnames;
        this.conversionFactors = conversionFactors;
    }

    public static PftLut load(Reader reader) throws IOException {
        CsvReader csvReader = new CsvReader(reader, new char[]{'|'});
        try {
            String[] pftnames = csvReader.readRecord();
            List<String[]> records = csvReader.readStringRecords();
            double[][] conversionFactors = new double[records.size()][pftnames.length];
            for (int i = 0; i < records.size(); i++) {
                String[] record = records.get(i);
                for (int j = 0; j < record.length; j++) {
                    double pftFactor = 0.0;
                    String stringValue = record[j];
                    if (!stringValue.isEmpty()) {
                        pftFactor = Double.parseDouble(stringValue);
                    }
                    conversionFactors[i][j] = pftFactor;
                }
            }
            return new PftLut(pftnames, conversionFactors);
        } finally {
            csvReader.close();
        }
    }

    public String[] getPFTNames() {
        return pftnames;
    }

    public double[][] getConversionFactors() {
        return conversionFactors;
    }
}
