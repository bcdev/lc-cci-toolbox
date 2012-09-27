package org.esa.cci.lc.aggregation;

import org.esa.beam.util.Guardian;
import org.esa.beam.util.io.CsvReader;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;

/**
 * @author Marco Peters
 */
public class LCCS {

    private int[] classValues;
    private String[] classDescriptions;
    private int noDataClassValue;

    public static LCCS getInstance() {
        try {
            return LCCS.load(new InputStreamReader(LcAggregator.class.getResourceAsStream("lccs_classes.csv")));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    LCCS(int[] classValues, String[] classDescriptions) {
        Guardian.assertEquals("classValues.length == classDescriptions.length",
                              classValues.length == classDescriptions.length, true);
        this.classValues = classValues;
        this.classDescriptions = classDescriptions;
        this.noDataClassValue = classValues[0];
    }

    static LCCS load(Reader reader) throws IOException {
        CsvReader csvReader = new CsvReader(reader, new char[]{'|'});
        try {
            List<String[]> records = csvReader.readStringRecords();
            int[] classValues = new int[records.size()];
            String[] classDescriptions = new String[records.size()];
            for (int i = 0, recordsSize = records.size(); i < recordsSize; i++) {
                String[] record = records.get(i);
                classValues[i] = Integer.parseInt(record[0]);
                classDescriptions[i] = record[1];
            }
            return new LCCS(classValues, classDescriptions);
        } finally {
            csvReader.close();
        }
    }

    public int getNumClasses() {
        return classValues.length;
    }

    public int[] getClassValues() {
        return classValues;
    }

    public String[] getClassDescriptions() {
        return classDescriptions;
    }

    public int getNoDataClassValue() {
        return noDataClassValue;
    }

}
