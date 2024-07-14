package org.esa.cci.lc.aggregation;

import org.esa.snap.binning.AbstractAggregator;
import org.esa.snap.binning.BinContext;
import org.esa.snap.binning.Observation;
import org.esa.snap.binning.VariableContext;
import org.esa.snap.binning.Vector;
import org.esa.snap.binning.WritableVector;
import org.esa.snap.binning.support.GrowableVector;

import java.awt.geom.FlatteningPathIterator;
import java.util.Arrays;

/**
 * This class implements a majority selection to aggregate the change count.
 */
class LcMajorityAggregator extends AbstractAggregator {

    private final int varIndex;
    private final String contextNameSpace;
    private final int rowRatio;

    LcMajorityAggregator(VariableContext varCtx, String[] sourceVarNames, String[] targetVarNames, int rowRatio) {
        super(LcMajorityAggregatorDescriptor.NAME,
              targetVarNames,
              targetVarNames,
              targetVarNames);
        varIndex = varCtx.getVariableIndex(sourceVarNames[0]);
        contextNameSpace = targetVarNames[0] + hashCode();
        this.rowRatio = rowRatio;
    }

    @Override
    public void initSpatial(BinContext ctx, WritableVector vector) {
        ctx.put(contextNameSpace, new GrowableVector(rowRatio*rowRatio));
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
        float majorityValue;
        if (elements.length > 0) {
            majorityValue = elements[0];
            int majorityCount = 1;
            int i0 = 0;
            for (int i = 1; ; ++i) {
                if (i >= elements.length || elements[i] != elements[i0]) {
                    if (i - i0 > majorityCount) {
                        majorityValue = elements[i0];
                        majorityCount = i-i0;
                    }
                    if (i >= elements.length) {
                        break;
                    }
                    i0 = i;
                }
            }
        } else {
            majorityValue = Float.NaN;
        }

        spatialVector.set(0, majorityValue);
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
