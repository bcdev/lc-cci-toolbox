package org.esa.cci.lc.aggregation;

import org.esa.snap.binning.AbstractAggregator;
import org.esa.snap.binning.BinContext;
import org.esa.snap.binning.Observation;
import org.esa.snap.binning.VariableContext;
import org.esa.snap.binning.Vector;
import org.esa.snap.binning.WritableVector;
import org.esa.snap.binning.support.GrowableVector;

import java.util.Arrays;

//import org.esa.snap.util.logging.BeamLogManager;

/**
 * This class implements a median average to aggregate the accuracy.
 */
class LcAccuracyAggregator extends AbstractAggregator {

    private final int varIndex;
    private final String contextNameSpace;

    LcAccuracyAggregator(VariableContext varCtx, String[] sourceVarNames, String[] targetVarNames) {
        super(LcAccuracyAggregatorDescriptor.NAME,
              targetVarNames,
              targetVarNames,
              targetVarNames);
        varIndex = varCtx.getVariableIndex(sourceVarNames[0]);
        contextNameSpace = targetVarNames[0] + hashCode();
    }

    @Override
    public void initSpatial(BinContext ctx, WritableVector vector) {
        ctx.put(contextNameSpace, new GrowableVector(128));
    }

    @Override
    public void aggregateSpatial(BinContext ctx, Observation observationVector, WritableVector spatialVector) {
        final GrowableVector growableVector = ctx.get(contextNameSpace);
        growableVector.add(observationVector.get(varIndex));
    }

    @Override
    public void completeSpatial(BinContext ctx, int numSpatialObs, WritableVector spatialVector) {
        final GrowableVector growableVector = ctx.get(contextNameSpace);
        final float[] elements = growableVector.getElements();
        Arrays.sort(elements);
        final float lcMedian;
        final int length = elements.length;
        if (length == 0) {
            lcMedian = Float.NaN;
        } else if (length == 1) {
            lcMedian = elements[0];
        } else if (length % 2 == 0) {
            final float lowerMedian = elements[length / 2 - 1];
            final float higherMedian = elements[length / 2];
            lcMedian = lowerMedian + ((higherMedian - lowerMedian) / 2);
        } else {
            lcMedian = elements[length / 2];
        }
        spatialVector.set(0, lcMedian);
        ctx.put(contextNameSpace, null);
    }

    @Override
    public void initTemporal(BinContext ctx, WritableVector vector) {
        vector.set(0, 0f);
    }

    @Override
    public void aggregateTemporal(BinContext ctx, Vector spatialVector, int numSpatialObs, WritableVector temporalVector) {
        temporalVector.set(0, spatialVector.get(0));
    }

    @Override
    public void completeTemporal(BinContext ctx, int numTemporalObs, WritableVector temporalVector) {
    }

    @Override
    public void computeOutput(Vector temporalVector, WritableVector outputVector) {
        outputVector.set(0, temporalVector.get(0));
    }
}
