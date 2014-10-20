package org.esa.cci.lc.aggregation;

import org.esa.beam.binning.AbstractAggregator;
import org.esa.beam.binning.BinContext;
import org.esa.beam.binning.Observation;
import org.esa.beam.binning.VariableContext;
import org.esa.beam.binning.Vector;
import org.esa.beam.binning.WritableVector;

/**
 * This class implements the aggregation of LC-CCI burnt area and snow condition products.
 * Input variables are: ba_occ, ba_nYearObs
 *
 * @author Marco Peters
 */
class LcCondOccAggregator extends AbstractAggregator {

    private final int condOccIndex;
    private final int condNYearObsIndex;

    LcCondOccAggregator(VariableContext varCtx, String[] varNames, String[] targetVarNames) {
        super(LcMapAggregatorDescriptor.NAME, targetVarNames, targetVarNames, targetVarNames);
        condOccIndex = varCtx.getVariableIndex(varNames[0]);
        condNYearObsIndex = varCtx.getVariableIndex(varNames[1]);
    }

    @Override
    public void initSpatial(BinContext ctx, WritableVector vector) {
        initVector(vector, 0.0f);
    }

    @Override
    public void aggregateSpatial(BinContext ctx, Observation observation, WritableVector spatialVector) {
        float occurrence = observation.get(condOccIndex);

        if (!Float.isNaN(occurrence) && occurrence > 0) {
            spatialVector.set(0, spatialVector.get(0) + 1); // count valid occurrences
            spatialVector.set(1, spatialVector.get(1) + occurrence); // sum valid occurrences
            spatialVector.set(2, spatialVector.get(2) + observation.get(condNYearObsIndex)); // sum nYearObs
        }
    }

    @Override
    public void completeSpatial(BinContext ctx, int numSpatialObs, WritableVector spatialVector) {
        float numValidObs = spatialVector.get(0);

        if (numValidObs > 0) {
            spatialVector.set(0, (numValidObs / (float) numSpatialObs) * 100); // percentage of valid observations
            spatialVector.set(1, (spatialVector.get(1) / numValidObs)); // average of occurrences
        } else {
            spatialVector.set(0, Float.NaN);
            spatialVector.set(1, Float.NaN);
            spatialVector.set(2, 0);
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
