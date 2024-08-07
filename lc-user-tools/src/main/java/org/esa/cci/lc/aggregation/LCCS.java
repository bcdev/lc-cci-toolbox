package org.esa.cci.lc.aggregation;

import org.esa.snap.core.util.Guardian;
import org.esa.snap.core.util.io.CsvReader;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author Marco Peters
 */
public class LCCS {

    /*
    classValue | description | flag meaning
     */
    private static final String CLASS_DEFINTIONS_FILE = "LCCS_class_defintions.csv";
    private static LCCS singleton = null;
    private final int[] classValues;
    private final String[] classDescriptions;
    private final String[] flagMeanings;
    private final int noDataClassValue;
    private final Map<Integer, Integer> classValueToIndexMap;
    private final Map<Integer, Integer> indexToClassValueMap;

    public static LCCS getInstance() {
        if (singleton == null) {
            try {
                singleton = LCCS.load(new InputStreamReader(LcMapAggregator.class.getResourceAsStream(CLASS_DEFINTIONS_FILE)));
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
        return singleton;
    }

    LCCS(int[] classValues, String[] classDescriptions, String[] flagMeanings) {
        Guardian.assertEquals("classValues.length == classDescriptions.length",
                              classValues.length == classDescriptions.length, true);
        Guardian.assertEquals("classValues.length == flagMeaning.length",
                              classValues.length == flagMeanings.length, true);
        this.classValues = classValues;
        this.classDescriptions = classDescriptions;
        this.flagMeanings = flagMeanings;
        this.noDataClassValue = classValues[0];
        this.classValueToIndexMap = new TreeMap<>();
        this.indexToClassValueMap = new TreeMap<>();
        for (int i = 0; i < classValues.length; i++) {
            int classValue = classValues[i];
            classValueToIndexMap.put(classValue, i);
        }
        for (Map.Entry<Integer, Integer> entry : classValueToIndexMap.entrySet()) {
            indexToClassValueMap.put(entry.getValue(), entry.getKey());
        }

    }

    static LCCS load(Reader reader) throws IOException {
        try (CsvReader csvReader = new CsvReader(reader, new char[]{'|'})) {
            List<String[]> records = csvReader.readStringRecords();
            int[] classValues = new int[records.size()];
            String[] classDescriptions = new String[records.size()];
            String[] flagMeaning = new String[records.size()];
            for (int i = 0; i < records.size(); i++) {
                String[] record = records.get(i);
                classValues[i] = Short.parseShort(record[0]);
                classDescriptions[i] = record[1];
                flagMeaning[i] = record[2];
            }
            return new LCCS(classValues, classDescriptions, flagMeaning);
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

    public String[] getFlagMeanings() {
        return flagMeanings;
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
