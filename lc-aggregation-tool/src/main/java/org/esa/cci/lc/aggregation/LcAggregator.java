package org.esa.cci.lc.aggregation;

import org.esa.beam.binning.AbstractAggregator;
import org.esa.beam.binning.BinContext;
import org.esa.beam.binning.Observation;
import org.esa.beam.binning.Vector;
import org.esa.beam.binning.WritableVector;
import org.esa.beam.binning.support.SEAGrid;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import static java.lang.Math.*;

/**
 * @author Marco Peters
 */
@SuppressWarnings("FieldCanBeLocal")
public class LcAggregator extends AbstractAggregator {

    private static final int NUM_GRID_ROWS = 216;
    private static final double INPUT_WIDTH = 12960.0;
    private static final double INPUT_HEIGHT = 6480.0;
//    private static final double INPUT_WIDTH = 129600.0;
//    private static final double INPUT_HEIGHT = 64800.0;

    // this Lut is for the LC example
    private static Map<Integer, Integer> classValueToVectorIndexMap;
    private final SEAGrid seaGrid;

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

    LcAggregator(String[] spatialFeatureNames, String[] outputFeatureNames) {
        super(LcAggregatorDescriptor.NAME, spatialFeatureNames, spatialFeatureNames, outputFeatureNames, null);
        seaGrid = new SEAGrid(NUM_GRID_ROWS);
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
        // todo (mp) - where to retrieve the grid from
        // todo (mp) - where to retrieve the width and height of the input from
        int rowIndex = seaGrid.getRowIndex(ctx.getIndex());
        int numCols = seaGrid.getNumCols(rowIndex);
        int numRows = seaGrid.getNumRows();
        double observationArea = computeArea(latitude, 180.0 / INPUT_HEIGHT, 360.0 / INPUT_WIDTH);
        double binArea = computeArea(seaGrid.getCenterLat(rowIndex), 180.0 / numRows, 360.0 / numCols);
        float area = (float) (observationArea / binArea);

        int index = getVectorIndex((int) observation.get(0));
        float oldValue = spatialVector.get(index);
        spatialVector.set(index, oldValue + area);
    }

    private double computeArea(double latitude, double deltaLat, double deltaLon) {
        double r2 = SEAGrid.RE * cos(toRadians(latitude));
        double a = r2 * toRadians(deltaLon);
        double b = SEAGrid.RE * toRadians(deltaLat);
        return a * b;
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
