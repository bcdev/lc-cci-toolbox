/*
 * Copyright (C) 2014 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.cci.lc.aggregation;

import org.esa.snap.binning.*;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.util.StringUtils;

import java.util.Arrays;

/**
 * An aggregator that computes an average.
 */
class LcPftAggregator extends AbstractAggregator {

    private final int varIndex;
    private final WeightFn weightFn;
    private final boolean outputCounts;
    private final boolean outputSums;
    private String icName;
    public LcPftAggregator(VariableContext varCtx, String varName, double weightCoeff) {
        this(varCtx, varName, varName, weightCoeff, false, false, null);
    }

    public LcPftAggregator(VariableContext varCtx, String varName, String targetName, double weightCoeff, boolean outputCounts,
                             boolean outputSums, AreaCalculator areaCalculator) {

        super(LcPftAggregatorDescriptor.NAME,
                createFeatureNames(varName, "sum", "sum_sq", outputCounts ? "counts" : null),
                createFeatureNames(varName, "sum", "sum_sq", "weights", outputCounts ? "counts" : null),
                //outputSums ?
                //        createFeatureNames(targetName, "sum", "sum_sq", "weights", outputCounts ? "counts" : null) :
                        //createFeatureNames(targetName, "mean-1", "sigma-1", outputCounts ? "counts" : null)
                        new String[]{targetName,targetName+"_sigma"}

        );
        if (varCtx == null) {
            throw new NullPointerException("varCtx");
        }
        if (varName == null) {
            throw new NullPointerException("varName");
        }
        if (weightCoeff < 0.0) {
            throw new IllegalArgumentException("weightCoeff < 0.0");
        }
        this.varIndex = varCtx.getVariableIndex(varName);
        this.weightFn = WeightFn.createPow(weightCoeff);
        this.outputCounts = outputCounts;
        this.outputSums = outputSums;
        this.icName = "ic." + varName;
    }

    @Override
    public void initSpatial(BinContext ctx, WritableVector vector) {
        vector.set(0, 0.0f);
        vector.set(1, 0.0f);
        if (outputCounts) {
            vector.set(2, 0.0f);
        } else {
            initNumInvalids(ctx);
        }
    }

    @Override
    public void initTemporal(BinContext ctx, WritableVector vector) {
        vector.set(0, 0.0f);
        vector.set(1, 0.0f);
        vector.set(2, 0.0f);
        if (outputCounts) {
            vector.set(3, 0.0f);
        } else {
            initNumInvalids(ctx);
        }
    }

    @Override
    public void aggregateSpatial(BinContext ctx, Observation observationVector, WritableVector spatialVector) {
        final float value = observationVector.get(varIndex);
        if (!Float.isNaN(value)) {
            spatialVector.set(0, spatialVector.get(0) + value);
            spatialVector.set(1, spatialVector.get(1) + value * value);
            if (outputCounts) {
                spatialVector.set(2, spatialVector.get(2) + 1f);
            }
        } else {
            if (!outputCounts) {
                incrementNumInvalids(ctx);
            }
        }
    }

    @Override
    public void completeSpatial(BinContext ctx, int numSpatialObs, WritableVector spatialVector) {
        int counts;
        if (outputCounts) {
            counts = (int) spatialVector.get(2);
        } else {
            counts = numSpatialObs - getNumInvalids(ctx);
        }
        if (counts > 0) {
            spatialVector.set(0, spatialVector.get(0) / counts);
            spatialVector.set(1, spatialVector.get(1) / counts);
        } else {
            spatialVector.set(0, Float.NaN);
            spatialVector.set(1, Float.NaN);
        }
    }

    @Override
    public void aggregateTemporal(BinContext ctx, Vector spatialVector, int numSpatialObs,
                                  WritableVector temporalVector) {
        float w = weightFn.eval(numSpatialObs);
        float sum = spatialVector.get(0);
        float sumSqr = spatialVector.get(1);
        if (!Float.isNaN(sum)) {
            temporalVector.set(0, temporalVector.get(0) + sum * w);
            temporalVector.set(1, temporalVector.get(1) + sumSqr * w);
            temporalVector.set(2, temporalVector.get(2) + w);
            if (outputCounts) {
                float counts = spatialVector.get(2);
                temporalVector.set(3, temporalVector.get(3) + counts);
            }
        }
    }

    @Override
    public void completeTemporal(BinContext ctx, int numTemporalObs, WritableVector temporalVector) {
    }

    @Override
    public void computeOutput(Vector temporalVector, WritableVector outputVector) {
        final double sumX = temporalVector.get(0);
        final double sumXX = temporalVector.get(1);
        final double sumW = temporalVector.get(2);

        int index = 0;
        if (outputSums) {
            if (sumW > 0.0) {
                outputVector.set(index++, (float) sumX);
                outputVector.set(index++, (float) sumXX);
                outputVector.set(index++, (float) sumW);
            } else {
                outputVector.set(index++, Float.NaN);
                outputVector.set(index++, Float.NaN);
                outputVector.set(index++, Float.NaN);
            }
        } else {
            if (sumW > 0.0) {
                // Note: sigmaSqr may be negative but not NaN.
                // If it is negative, sigma is actually a complex number of which
                // we take the real part, which is zero.  (nf)
                final double mean = sumX / sumW;
                final double sigmaSqr = sumXX / sumW - mean * mean;
                final double sigma = sigmaSqr > 0.0 ? Math.sqrt(sigmaSqr) : 0.0;
                outputVector.set(index++, (float) mean);
                outputVector.set(index++, (float) sigma);
            } else {
                outputVector.set(index++, Float.NaN);
                outputVector.set(index++, Float.NaN);
            }
        }
        if (outputCounts) {
            if (sumW > 0.0) {
                float counts = temporalVector.get(3);
                outputVector.set(index, counts);
            } else {
                outputVector.set(index, 0.0f);
            }
        }

    }

    private void initNumInvalids(BinContext ctx) {
        icName = ctx.ensureUnique(icName);
        ctx.put(icName, new int[1]);
    }

    private void incrementNumInvalids(BinContext ctx) {
        ((int[]) ctx.get(icName))[0]++;
    }

    private int getNumInvalids(BinContext ctx) {
        return ((int[]) ctx.get(icName))[0];
    }

    @Override
    public String toString() {
        return "AggregatorAverage{" +
                ", varIndex=" + varIndex +
                ", weightFn=" + weightFn +
                ", spatialFeatureNames=" + Arrays.toString(getSpatialFeatureNames()) +
                ", temporalFeatureNames=" + Arrays.toString(getTemporalFeatureNames()) +
                ", outputFeatureNames=" + Arrays.toString(getOutputFeatureNames()) +
                '}';
    }




}
