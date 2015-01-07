package org.esa.cci.lc.aggregation;

import org.esa.beam.binning.AbstractAggregator;
import org.esa.beam.binning.BinContext;
import org.esa.beam.binning.Observation;
import org.esa.beam.binning.Vector;
import org.esa.beam.binning.WritableVector;

import java.util.Collections;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * This class implements the aggregation of LC-CCI WB products. The aggregation includes the computation
 * of the fractional area covered by the different WB classes.
 *
 * @author Marco Peters, Martin Boettcher
 */
@SuppressWarnings("FieldCanBeLocal")
class LcWbAggregator extends AbstractAggregator {

    private AreaCalculator areaCalculator;
    private boolean outputWbClasses;
    private int numMajorityClasses;

    public LcWbAggregator(int numMajorityClasses, boolean outputWbClasses,
                          AreaCalculator calculator, String[] spatialFeatureNames, String[] outputFeatureNames) {
        super(LcMapAggregatorDescriptor.NAME, spatialFeatureNames, spatialFeatureNames, outputFeatureNames);
        this.outputWbClasses = outputWbClasses;
        this.numMajorityClasses = numMajorityClasses;
        this.areaCalculator = calculator;
    }

    @Override
    public void initSpatial(BinContext ctx, WritableVector vector) {
        initVector(vector, Float.NaN);
    }

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
        // Nothing to be done here
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
            if (outputWbClasses) {
                outputVector.set(outputVectorIndex++, classArea);
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
