package org.esa.cci.lc.aggregation;

import org.esa.beam.binning.AbstractAggregator;
import org.esa.beam.binning.BinContext;
import org.esa.beam.binning.Observation;
import org.esa.beam.binning.Vector;
import org.esa.beam.binning.WritableVector;

import java.util.ArrayList;
import java.util.List;

/**
 * This class implements the aggregation of LC-CCI Condition products.
 *
 * @author Marco Peters
 */
@SuppressWarnings("FieldCanBeLocal")
class LcCondAggregator extends AbstractAggregator {

    private FractionalAreaCalculator areaCalculator;

    public LcCondAggregator(FractionalAreaCalculator calculator) {
        this(createSpatialFeatureNames(), calculator);
    }

    private LcCondAggregator(String[] spatialFeatureNames, FractionalAreaCalculator calculator) {
        super(LcMapAggregatorDescriptor.NAME, spatialFeatureNames, spatialFeatureNames,
              createOutputFeatureNames());
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
    }

    private void initVector(WritableVector outputVector, float initValue) {
        for (int i = 0; i < outputVector.size(); i++) {
            outputVector.set(i, initValue);
        }
    }

    private static String[] createSpatialFeatureNames() {
        String[] spatialFeatureNames = new String[0];
        return spatialFeatureNames;
    }

    private static String[] createOutputFeatureNames() {
        List<String> outputFeatureNames = new ArrayList<String>();
        return outputFeatureNames.toArray(new String[outputFeatureNames.size()]);
    }

}
