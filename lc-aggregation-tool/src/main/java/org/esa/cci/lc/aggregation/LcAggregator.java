package org.esa.cci.lc.aggregation;

import org.esa.beam.binning.AbstractAggregator;
import org.esa.beam.binning.BinContext;
import org.esa.beam.binning.Observation;
import org.esa.beam.binning.Vector;
import org.esa.beam.binning.WritableVector;

/**
 * @author Marco Peters
 */
public class LcAggregator extends AbstractAggregator {

    private final int[] classToSpatialVectorIndex;

    LcAggregator(String[] spatialFeatureNames, String[] outputFeatureNames) {
        super(LcAggregatorDescriptor.LC_AGGR, spatialFeatureNames, spatialFeatureNames, outputFeatureNames, null);

        // this Lut is for the LC example
        classToSpatialVectorIndex = new int[]{
                -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 0, -1, -1, -1, -1, -1, -1, -1, -1, -1, 1, -1, -1, -1, -1, -1,
                -1, -1, -1, -1, 2, -1, -1, -1, -1, -1, -1, -1, -1, -1, 3, -1, -1, -1, -1, -1, -1, -1, -1, -1, 4, -1,
                -1, -1, -1, -1, -1, -1, -1, -1, 5, -1, -1, -1, -1, -1, -1, -1, -1, -1, 6, -1, -1, -1, -1, -1, -1, -1,
                -1, -1, 7, -1, -1, -1, -1, -1, -1, -1, -1, -1, 8, -1, -1, -1, -1, -1, -1, -1, -1, -1, 9, -1, -1, -1,
                -1, -1, -1, -1, -1, -1, 10, -1, -1, -1, -1, -1, -1, -1, -1, -1, 11, -1, -1, -1, -1, -1, -1, -1, -1,
                -1, 12, -1, -1, -1, -1, -1, -1, -1, -1, -1, 13, -1, -1, -1, -1, -1, -1, -1, -1, -1, 14, -1, -1, -1,
                -1, -1, -1, -1, -1, -1, 15, -1, -1, -1, -1, -1, -1, -1, -1, -1, 16, -1, -1, -1, -1, -1, -1, -1, -1,
                -1, 17, -1, -1, -1, -1, -1, -1, -1, -1, -1, 18, -1, -1, -1, -1, -1, -1, -1, -1, -1, 19, -1, -1, -1,
                -1, -1, -1, -1, -1, -1, 20, -1, -1, -1, -1, -1, -1, -1, -1, -1, 21, -1, -1, -1, -1, -1, -1, -1, -1,
                -1, 22, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
                -1, 23
        };
    }

    @Override
    public void initSpatial(BinContext ctx, WritableVector vector) {
        for (int i = 0; i < vector.size(); i++) {
            vector.set(i, Float.NaN);
        }
    }

    @Override
    public void aggregateSpatial(BinContext ctx, Vector observationVector, WritableVector spatialVector) {
        Observation observation = (Observation) observationVector;
        // todo: estimate area for input observation latitude assuming it is the center of a LC-map pixel
        float area = 1.0F;
        int classIndex = (int) observationVector.get(0);
        // map classIndex to spatial vector index i
        int i = classToSpatialVectorIndex[classIndex];
        spatialVector.set(i, spatialVector.get(i) + area);
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
        // todo: Copy areal classes
        for (int i = 0; i < temporalVector.size(); i++) {
            outputVector.set(i, temporalVector.get(i));
        }

        // todo: Generate majority classes
        for (int i = LcAggregatorDescriptor.NUM_LC_CLASSES; i < outputVector.size(); i++) {
            outputVector.set(i, 0);
        }

        // todo: Generate overall area sum
    }


}
