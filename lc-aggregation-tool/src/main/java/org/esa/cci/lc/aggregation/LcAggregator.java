package org.esa.cci.lc.aggregation;

import org.esa.beam.binning.AbstractAggregator;
import org.esa.beam.binning.BinContext;
import org.esa.beam.binning.Observation;
import org.esa.beam.binning.PlanetaryGrid;
import org.esa.beam.binning.Vector;
import org.esa.beam.binning.WritableVector;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * @author Marco Peters
 */
@SuppressWarnings("FieldCanBeLocal")
public class LcAggregator extends AbstractAggregator {

    // this Lut is for the LC example
    private static Map<Integer, Integer> classValueToVectorIndexMap;
    private final PlanetaryGrid grid;
    private AreaCalculator areaCalculator;

    static {
        classValueToVectorIndexMap = new HashMap<Integer, Integer>();
        classValueToVectorIndexMap.put(10, 0);
        classValueToVectorIndexMap.put(20, 1);
        classValueToVectorIndexMap.put(30, 2);
        classValueToVectorIndexMap.put(40, 3);
        classValueToVectorIndexMap.put(50, 4);
        classValueToVectorIndexMap.put(60, 5);
        classValueToVectorIndexMap.put(70, 6);
        classValueToVectorIndexMap.put(80, 7);
        classValueToVectorIndexMap.put(90, 8);
        classValueToVectorIndexMap.put(100, 9);
        classValueToVectorIndexMap.put(110, 10);
        classValueToVectorIndexMap.put(120, 11);
        classValueToVectorIndexMap.put(130, 12);
        classValueToVectorIndexMap.put(140, 13);
        classValueToVectorIndexMap.put(150, 14);
        classValueToVectorIndexMap.put(160, 15);
        classValueToVectorIndexMap.put(170, 16);
        classValueToVectorIndexMap.put(180, 17);
        classValueToVectorIndexMap.put(190, 18);
        classValueToVectorIndexMap.put(200, 19);
        classValueToVectorIndexMap.put(210, 20);
        classValueToVectorIndexMap.put(220, 21);
        classValueToVectorIndexMap.put(230, 22);
        classValueToVectorIndexMap.put(255, 23);
    }

    LcAggregator(String[] spatialFeatureNames, String[] outputFeatureNames,
                 AreaCalculator areaCalculator, PlanetaryGrid grid) {
        super(LcAggregatorDescriptor.NAME, spatialFeatureNames, spatialFeatureNames, outputFeatureNames, null);
        this.areaCalculator = areaCalculator;
        this.grid = grid;
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
        for (int i = 0; i < temporalVector.size(); i++) {
            float classArea = temporalVector.get(i);
            if (!Float.isNaN(classArea)) {
                sortedMap.put(classArea, i);
            }
            outputVector.set(i, classArea);
        }

        Integer[] classesSortedByOccurrence = sortedMap.values().toArray(new Integer[sortedMap.size()]);
        for (int i = 0; i < outputVector.size() - LcAggregatorDescriptor.NUM_LC_CLASSES; i++) {
            Float majorityClass;
            if (i >= classesSortedByOccurrence.length) {
                majorityClass = Float.NaN;
            } else {
                majorityClass = classesSortedByOccurrence[i].floatValue();
            }
            outputVector.set(i + LcAggregatorDescriptor.NUM_LC_CLASSES, majorityClass + 1);
        }

        // todo: Generate overall area sum
    }

    private int getVectorIndex(int classValue) {
        // map classValue to spatial vector index i
        Integer vectorIndex = classValueToVectorIndexMap.get(classValue);
        if (vectorIndex == null) { // how to handle a value which is not assigned to a class
            vectorIndex = 1;
        }
        return vectorIndex % LcAggregatorDescriptor.NUM_LC_CLASSES;
    }


}
