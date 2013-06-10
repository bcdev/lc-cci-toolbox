package org.esa.cci.lc.aggregation;

import org.esa.beam.binning.AbstractAggregator;
import org.esa.beam.binning.BinContext;
import org.esa.beam.binning.Observation;
import org.esa.beam.binning.VariableContext;
import org.esa.beam.binning.Vector;
import org.esa.beam.binning.WritableVector;

/**
 * This class implements the aggregation of LC-CCI NDVI Condition products.
 * Input variables are: ndvi_mean, ndvi_std, ndvi_nYearObs
 *
 * @author Marco Peters
 */
class LcNDVIAggregator extends AbstractAggregator {

    private static final String[] featureNames = {"mean_ndvi_mean", "mean_ndvi_std", "sum_ndvi_nYearObs"};

    private final int ndviMeanIndex;
    private final int ndviStdIndex;
    private final int nYearObsIndex;
    private final String ndviMeanInvCountName;
    private final String ndviStdInvCountName;
    private final String nYearObsInvCountName;

    LcNDVIAggregator(VariableContext varCtx, String[] varNames) {
        super(LcMapAggregatorDescriptor.NAME, featureNames, featureNames, featureNames);

        ndviMeanIndex = varCtx.getVariableIndex(varNames[0]);
        ndviMeanInvCountName = "invCount." + varNames[0];
        ndviStdIndex = varCtx.getVariableIndex(varNames[1]);
        ndviStdInvCountName = "invCount." + varNames[1];
        nYearObsIndex = varCtx.getVariableIndex((varNames[2]));
        nYearObsInvCountName = "invCount." + varNames[2];
    }

    @Override
    public void initSpatial(BinContext ctx, WritableVector vector) {
        initVector(vector, 0.0f);
        ctx.put(ndviMeanInvCountName, new int[1]);
        ctx.put(ndviStdInvCountName, new int[1]);
        ctx.put(nYearObsInvCountName, new int[1]);
    }

    @Override
    public void aggregateSpatial(BinContext ctx, Observation observationVector, WritableVector spatialVector) {
        aggregateSpatialVar(ctx, observationVector, spatialVector, ndviMeanIndex, 0, ndviMeanInvCountName);
        aggregateSpatialVar(ctx, observationVector, spatialVector, ndviStdIndex, 1, ndviStdInvCountName);
        aggregateSpatialVar(ctx, observationVector, spatialVector, nYearObsIndex, 2, nYearObsInvCountName);
    }

    private void aggregateSpatialVar(BinContext ctx, Observation observationVector, WritableVector spatialVector,
                                     int observationIndex, int spatialVectorIndex, String invCountName) {
        final float value = observationVector.get(observationIndex);
        if (!Float.isNaN(value)) {
            spatialVector.set(spatialVectorIndex, spatialVector.get(spatialVectorIndex) + value);
        } else {
            ((int[]) ctx.get(invCountName))[0]++;
        }
    }

    @Override
    public void completeSpatial(BinContext ctx, int numSpatialObs, WritableVector spatialVector) {
        completeSpatialVar(ctx, numSpatialObs, spatialVector, 0, ndviMeanInvCountName);
        completeSpatialVar(ctx, numSpatialObs, spatialVector, 1, ndviStdInvCountName);
        completeSpatialVar(ctx, numSpatialObs, spatialVector, 2, nYearObsInvCountName);
    }

    private void completeSpatialVar(BinContext ctx, int numSpatialObs, WritableVector spatialVector, int spatialVectorIndex, String invCountName) {
        int invalidNumObs = ((int[]) ctx.get(invCountName))[0];
        if (invalidNumObs == numSpatialObs) {
            spatialVector.set(spatialVectorIndex, Float.NaN);
        } else {
            int effectiveValidNumObs = numSpatialObs - invalidNumObs;
            spatialVector.set(spatialVectorIndex, spatialVector.get(spatialVectorIndex) / effectiveValidNumObs);
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
        // simply copy the data; no extra computation needed
        for (int i = 0; i < temporalVector.size(); i++) {
            outputVector.set(i, temporalVector.get(i));
        }

    }

    private void initVector(WritableVector outputVector, float initValue) {
        for (int i = 0; i < outputVector.size(); i++) {
            outputVector.set(i, initValue);
        }
    }

}