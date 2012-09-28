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
    private final double[][] conversionFactors;

    public PftLut(String[] pftNames, double[][] conversionFactors) {
        this.pftNames = pftNames;
        this.conversionFactors = conversionFactors;
    }

    public static PftLut load(Reader reader) throws IOException {
        CsvReader csvReader = new CsvReader(reader, new char[]{'|'});
        try {
            String[] pftNames = csvReader.readRecord();
            List<String[]> records = csvReader.readStringRecords();
            double[][] conversionFactors = new double[records.size()][pftNames.length];
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
            return new PftLut(pftNames, conversionFactors);
        } finally {
            csvReader.close();
        }
    }

    public String[] getPFTNames() {
        return pftNames;
    }

    public double[][] getConversionFactors() {
        return conversionFactors;
    }
}
