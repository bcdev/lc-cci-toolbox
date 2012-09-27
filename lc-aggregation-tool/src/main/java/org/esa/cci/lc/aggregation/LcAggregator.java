package org.esa.cci.lc.aggregation;

import org.esa.beam.binning.AbstractAggregator;
import org.esa.beam.binning.BinContext;
import org.esa.beam.binning.Observation;
import org.esa.beam.binning.PlanetaryGrid;
import org.esa.beam.binning.Vector;
import org.esa.beam.binning.WritableVector;
import org.esa.beam.binning.support.SEAGrid;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * @author Marco Peters
 */
@SuppressWarnings("FieldCanBeLocal")
class LcAggregator extends AbstractAggregator {

    // this Lut is for the LC example
    private static final Map<Integer, Integer> classValueToVectorIndexMap;
    private static final Map<Integer, Integer> vectorIndexToClassValueMap;
    private static final LCCS LCCS_CLASSES = LCCS.getInstance();
    private final PlanetaryGrid grid;
    private FractionalAreaCalculator areaCalculator;

    static {
        classValueToVectorIndexMap = new HashMap<Integer, Integer>();
        vectorIndexToClassValueMap = new HashMap<Integer, Integer>();
        int[] classValues = LCCS_CLASSES.getClassValues();
        for (int i = 0; i < classValues.length; i++) {
            int classValue = classValues[i];
            classValueToVectorIndexMap.put(classValue, i);
        }
        for (Map.Entry<Integer, Integer> entry : classValueToVectorIndexMap.entrySet()) {
            vectorIndexToClassValueMap.put(entry.getValue(), entry.getKey());
        }
    }

    public LcAggregator(int numMajorityClasses, int numGridRows, FractionalAreaCalculator areaCalculator) {
        this(createSpatialFeatureNames(), numMajorityClasses, numGridRows, areaCalculator);

    }

    private LcAggregator(String[] spatialFeatureNames, int numMajorityClasses, int numGridRows,
                         FractionalAreaCalculator areaCalculator) {
        super(LcAggregatorDescriptor.NAME, spatialFeatureNames, spatialFeatureNames,
              createOutputFeatureNames(numMajorityClasses, spatialFeatureNames), null);
        this.areaCalculator = areaCalculator;
        this.grid = new SEAGrid(numGridRows);

    }

    private static String[] createOutputFeatureNames(int numMajorityClasses, String[] spatialFeatureNames) {
        String[] outputFeatureNames = new String[spatialFeatureNames.length + numMajorityClasses + 1];
        System.arraycopy(spatialFeatureNames, 0, outputFeatureNames, 0, spatialFeatureNames.length);
        for (int i = 0; i < numMajorityClasses; i++) {
            outputFeatureNames[spatialFeatureNames.length + i] = "majority_class_" + (i + 1);
        }
        outputFeatureNames[outputFeatureNames.length - 1] = "class_area_sum";
        return outputFeatureNames;
    }

    private static String[] createSpatialFeatureNames() {
        String[] spatialFeatureNames = new String[LCCS_CLASSES.getNumClasses()];
        int[] classValues = LCCS_CLASSES.getClassValues();
        for (int i = 0; i < spatialFeatureNames.length; i++) {
            spatialFeatureNames[i] = "class_area_" + classValues[i];
        }
        return spatialFeatureNames;
    }

    @Override
    public void initSpatial(BinContext ctx, WritableVector vector) {
        for (int i = 0; i < vector.size(); i++) {
            vector.set(i, 0.0f);
        }
    }

    @Override
    public void aggregateSpatial(BinContext ctx, Vector observationVector, WritableVector spatialVector) {
        Observation observation = (Observation) observationVector;
        double latitude = observation.getLatitude();
        int rowIndex = grid.getRowIndex(ctx.getIndex());
        int numCols = grid.getNumCols(rowIndex);
        float arealFraction = (float) areaCalculator.calculate(latitude, grid.getCenterLat(rowIndex), numCols);

        int index = getVectorIndex((int) observation.get(0));
        float oldValue = spatialVector.get(index);
        spatialVector.set(index, oldValue + arealFraction);
    }

    @Override
    public void completeSpatial(BinContext ctx, int numSpatialObs, WritableVector spatialVector) {
        for (int i = 0; i < spatialVector.size(); i++) {
            float spatialValue = spatialVector.get(i);
            if (Float.compare(spatialValue, 0.0f) != 0) {
                spatialVector.set(i, spatialValue);
            } else {
                spatialVector.set(i, Float.NaN);
            }
        }
    }

    @Override
    public void initTemporal(BinContext ctx, WritableVector vector) {
        // Nothing to be done here
    }

    @Override
    public void aggregateTemporal(BinContext ctx, Vector spatialVector, int numSpatialObs,
                                  WritableVector temporalVector) {
        // simply copy the data; no temporal aggregation needed
        for (int i = 0; i < spatialVector.size(); i++) {
            temporalVector.set(i, spatialVector.get(i));
        }
    }

    @Override
    public void completeTemporal(BinContext ctx, int numTemporalObs, WritableVector temporalVector) {
        // Nothing to be done here
    }

    @Override
    public void computeOutput(Vector temporalVector, WritableVector outputVector) {
        SortedMap<Float, Integer> sortedMap = new TreeMap<Float, Integer>(Collections.reverseOrder());
        float sum = 0.0f;
        for (int i = 0; i < temporalVector.size(); i++) {
            float classArea = temporalVector.get(i);
            if (!Float.isNaN(classArea)) {
                sortedMap.put(classArea, getClassValue(i));
                sum += classArea;
            }
            outputVector.set(i, classArea);
        }

        Integer[] classesSortedByOccurrence = sortedMap.values().toArray(new Integer[sortedMap.size()]);
        for (int i = 0; i < outputVector.size() - 1 - LCCS_CLASSES.getNumClasses(); i++) {
            Float majorityClass;
            if (i >= classesSortedByOccurrence.length) {
                majorityClass = Float.NaN;
            } else {
                majorityClass = classesSortedByOccurrence[i].floatValue();
            }
            outputVector.set(i + LCCS_CLASSES.getNumClasses(), majorityClass);
        }
        outputVector.set(outputVector.size() - 1, sum);
    }

    private int getVectorIndex(int classValue) {
        // map classValue to spatial vector index i
        if (!classValueToVectorIndexMap.containsKey(classValue)) {
            classValue = 0;
        }
        return classValueToVectorIndexMap.get(classValue);
    }

    private int getClassValue(int vectorIndex) {
        // map classValue to spatial vector index i
        return vectorIndexToClassValueMap.get(vectorIndex);
    }

}
