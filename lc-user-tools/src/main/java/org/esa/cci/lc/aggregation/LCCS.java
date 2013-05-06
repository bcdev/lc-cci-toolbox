package org.esa.cci.lc.aggregation;

import org.esa.beam.util.Guardian;
import org.esa.beam.util.io.CsvReader;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Marco Peters
 */
class LCCS {

    private static final String CLASS_DEFINTIONS_FILE = "LCCS_class_defintions.csv";

    private int[] classValues;
    private String[] classDescriptions;
    private int noDataClassValue;
    private Map<Integer, Integer> classValueToIndexMap;
    private Map<Integer, Integer> indexToClassValueMap;

    public static LCCS getInstance() {
        try {
            return LCCS.load(new InputStreamReader(LcAggregator.class.getResourceAsStream(CLASS_DEFINTIONS_FILE)));
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
        this.classValueToIndexMap = new HashMap<Integer, Integer>();
        this.indexToClassValueMap = new HashMap<Integer, Integer>();
        for (int i = 0; i < classValues.length; i++) {
            int classValue = classValues[i];
            classValueToIndexMap.put(classValue, i);
        }
        for (Map.Entry<Integer, Integer> entry : classValueToIndexMap.entrySet()) {
            indexToClassValueMap.put(entry.getValue(), entry.getKey());
        }

    }

    static LCCS load(Reader reader) throws IOException {
        CsvReader csvReader = new CsvReader(reader, new char[]{'|'});
        try {
            List<String[]> records = csvReader.readStringRecords();
            int[] classValues = new int[records.size()];
            String[] classDescriptions = new String[records.size()];
            for (int i = 0; i < records.size(); i++) {
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

    int getClassIndex(int classValue) {
        if (!classValueToIndexMap.containsKey(classValue)) {
            classValue = noDataClassValue;
        }
        return classValueToIndexMap.get(classValue);
    }

    int getClassValue(int classIndex) {
        return indexToClassValueMap.get(classIndex);
    }
}
