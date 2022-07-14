package org.esa.cci.lc.aggregation;

import org.esa.snap.binning.*;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Product;

import java.awt.*;
import java.util.Collections;
import java.util.SortedMap;
import java.util.TreeMap;

public class LcPftAggregator extends AbstractAggregator {

    private AreaCalculator areaCalculator;
    private int numMajorityClasses;


    public LcPftAggregator( int numMajorityClasses,
                           AreaCalculator calculator,  String[] spatialFeatureNames, String[] outputFeatureNames) {
        super(LcPftAggregatorDescriptor.NAME, spatialFeatureNames, spatialFeatureNames, outputFeatureNames);
        this.numMajorityClasses = numMajorityClasses;
        this.areaCalculator = calculator;
    }


    @Override
    public void initSpatial(BinContext ctx, WritableVector vector) {
        initVector(vector, Float.NaN);
    }


    //todo : remake this using different pft bands instead of LCCS classes
    @Override
    public void aggregateSpatial(BinContext ctx, Observation observation, WritableVector spatialVector) {
        final double obsLatitude = observation.getLatitude();
        final double obsLongitude = observation.getLongitude();
        final float areaFraction = (float) areaCalculator.calculate(obsLongitude, obsLatitude, ctx.getIndex());

        final int index = (int)observation.get(0);
        final float oldValue = spatialVector.get(index);
        if (Float.isNaN(oldValue)) {
            spatialVector.set(index, areaFraction);
        } else {
            spatialVector.set(index, oldValue + areaFraction);
        }
    }

    @Override
    public void completeSpatial(BinContext ctx, int numSpatialObs, WritableVector spatialVector) {
        // normalizing the data because of aggregating with float data and the float inaccuracy
        float sum = 0;
        for (int i = 0; i < spatialVector.size(); i++) {
            float v = spatialVector.get(i);
            if (!Float.isNaN(v)) {
                sum += v;
            }
        }
        if (sum != 1.0f) {
            for (int i = 0; i < spatialVector.size(); i++) {
                spatialVector.set(i, spatialVector.get(i) / sum);
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
        initVector(outputVector, Float.NaN);
        SortedMap<Float, Integer> sortedMap = new TreeMap<>(Collections.reverseOrder());
        int outputVectorIndex = 0;
        for (short i = 0; i < temporalVector.size(); i++) {
            float classArea = temporalVector.get(i);
            if (!Float.isNaN(classArea)) {
                sortedMap.put(classArea, (int) i);
            }
        }

        Integer[] classesSortedByOccurrence = sortedMap.values().toArray(new Integer[sortedMap.size()]);
        for (int i = 0; i < numMajorityClasses; i++) {
            if (i >= classesSortedByOccurrence.length) {
                outputVector.set(outputVectorIndex++, Float.NaN);
            } else {
                outputVector.set(outputVectorIndex++, classesSortedByOccurrence[i]);
            }
        }
    }

    private void initVector(WritableVector outputVector, float initValue) {
        for (int i = 0; i < outputVector.size(); i++) {
            outputVector.set(i, initValue);
        }
    }


}
