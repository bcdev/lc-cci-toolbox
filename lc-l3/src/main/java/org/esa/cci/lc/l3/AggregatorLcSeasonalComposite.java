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

package org.esa.cci.lc.l3;

import org.esa.beam.binning.AbstractAggregator;
import org.esa.beam.binning.Aggregator;
import org.esa.beam.binning.AggregatorConfig;
import org.esa.beam.binning.AggregatorDescriptor;
import org.esa.beam.binning.BinContext;
import org.esa.beam.binning.Observation;
import org.esa.beam.binning.VariableContext;
import org.esa.beam.binning.Vector;
import org.esa.beam.binning.WritableVector;
import org.esa.beam.util.logging.BeamLogManager;
import java.util.Arrays;
import java.util.logging.Logger;

/**
 * An aggregator that implements the LC seasonal compositing algorithm.
 * The priority class (land, water, snow/ice) is aggregated using the number of class observations as weights.
 */
public class AggregatorLcSeasonalComposite extends AbstractAggregator {

    protected Logger LOGGER = BeamLogManager.getSystemLogger();

    static String[] SR_VARIABLES = new String[] {
            "current_pixel_state",
            "clear_land_count",
            "clear_water_count",
            "clear_snow_ice_count",
            "cloud_count",
            "cloud_shadow_count",
            "sr_1_mean",
            "sr_2_mean",
            "sr_3_mean",
            "sr_4_mean",
            "sr_5_mean",
            "sr_6_mean",
            "sr_7_mean",
            "sr_8_mean",
            "sr_9_mean",
            "sr_10_mean",
            "sr_12_mean",
            "sr_13_mean",
            "sr_14_mean",
            "vegetation_index_mean"
    };
    static String[] SEASONAL_COMPOSITE_VARIABLES = new String[] {
            "status",
            "status_count",
            "obs_count",
            "sr_1_mean",
            "sr_2_mean",
            "sr_3_mean",
            "sr_4_mean",
            "sr_5_mean",
            "sr_6_mean",
            "sr_7_mean",
            "sr_8_mean",
            "sr_9_mean",
            "sr_10_mean",
            "sr_12_mean",
            "sr_13_mean",
            "sr_14_mean",
            "vegetation_index_mean"
    };
    final static int SR_OFFSET = SR_VARIABLES.length - SEASONAL_COMPOSITE_VARIABLES.length;

    private final int statusIndex;
    private final int[] setIndexes;
    private final int numSetFeatures;

    public AggregatorLcSeasonalComposite(VariableContext varCtx, String... setVarNames) {
        super(Descriptor.NAME, SR_VARIABLES, SEASONAL_COMPOSITE_VARIABLES, SEASONAL_COMPOSITE_VARIABLES);
        if (varCtx == null) {
            throw new NullPointerException("varCtx");
        }
        setVarNames = SR_VARIABLES;
        numSetFeatures = setVarNames.length;
        statusIndex = varCtx.getVariableIndex("current_pixel_state");
        if (statusIndex < 0) {
            throw new IllegalArgumentException("missing status variable");
        }
        setIndexes = new int[setVarNames.length];
        for (int i = 0; i < setVarNames.length; i++) {
            int varIndex = varCtx.getVariableIndex(setVarNames[i]);
            if (varIndex < 0) {
                throw new IllegalArgumentException("setIndexes[" + i + "] < 0");
            }
            setIndexes[i] = varIndex;
        }
        LOGGER.info(this.toString());
    }

    @Override
    public void initSpatial(BinContext ctx, WritableVector vector) {
        vector.set(0, 0.0f);
    }

    @Override
    public void aggregateSpatial(BinContext ctx, Observation observationVector, WritableVector spatialVector) {
        final float value = observationVector.get(statusIndex);
        if (value > 0.0) {
            for (int i = 0; i < numSetFeatures; i++) {
                spatialVector.set(i, observationVector.get(setIndexes[i]));
            }
        }
        LOGGER.info("aggregateSpatial status=" + (int)value);
    }

    @Override
    public void completeSpatial(BinContext ctx, int numObs, WritableVector spatialVector) {
    }

    @Override
    public void initTemporal(BinContext ctx, WritableVector vector) {
        vector.set(0, Float.POSITIVE_INFINITY);  // status
        vector.set(1, 0.0f);  // numStatusObs
        vector.set(2, 0.0f);  // numObs
        for (int i=3; i<SEASONAL_COMPOSITE_VARIABLES.length; ++i) {
            vector.set(i, Float.NaN);
        }
    }

    @Override
    public void aggregateTemporal(BinContext ctx, Vector spatialVector, int numSpatialObs,
                                  WritableVector temporalVector) {
        //final float mjd = spatialVector.get(0);
        final float status = spatialVector.get(0);
        final float aggregatedStatus = temporalVector.get(0);
        final int numStatusObs = numObsOf(status, spatialVector);
        final int numObs = numObsOf(spatialVector);
        // same status as before, aggregating
        if (status == aggregatedStatus) {
            temporalVector.set(1, temporalVector.get(1) + numStatusObs);
            temporalVector.set(2, temporalVector.get(2) + numObs);
            for (int i=3; i<SEASONAL_COMPOSITE_VARIABLES.length; ++i) {
                temporalVector.set(i, temporalVector.get(i) + spatialVector.get(i+SR_OFFSET) * numStatusObs);
            }
            //LOGGER.info("aggregateSpatial n " + temporalVector.get(1) + " status=" + (int)status);
        }
        else if (status > 0.0f) {
            // we found a better status and start aggregating again
            if (status < aggregatedStatus) {
                temporalVector.set(0, status);
                temporalVector.set(1, numStatusObs);
                for (int i=3; i<SEASONAL_COMPOSITE_VARIABLES.length; ++i) {
                    temporalVector.set(i, spatialVector.get(i+SR_OFFSET) * numStatusObs);
                }
                //LOGGER.info("aggregateTemporal 0 " + temporalVector.get(1) + " status=" + (int)status);
            }
            temporalVector.set(2, temporalVector.get(2) + numObs);
        }
    }

    @Override
    public void completeTemporal(BinContext ctx, int numTemporalObs, WritableVector temporalVector) {
        int numStatusObs = (int)temporalVector.get(1);
        if (numStatusObs != 0) {
            for (int i=3; i<SEASONAL_COMPOSITE_VARIABLES.length; ++i) {
                temporalVector.set(i, temporalVector.get(i) / numStatusObs);
            }
        }
        if (Float.isInfinite(temporalVector.get(0))) {
            temporalVector.set(0, 0.0f);
        }
    }

    @Override
    public void computeOutput(Vector temporalVector, WritableVector outputVector) {
        for (int i = 0; i < SEASONAL_COMPOSITE_VARIABLES.length; ++i) {
            outputVector.set(i, temporalVector.get(i));
        }
    }

    private static int numObsOf(float status, Vector spatialVector) {
        if (status == 1.0f) {
            return (int)spatialVector.get(1);
        } else if (status == 2.0f) {
            return (int)spatialVector.get(2);
        } else if (status == 3.0f) {
            return (int)spatialVector.get(3);
        } else if (status == 4.0f) {
            return (int)spatialVector.get(4);
        } else if (status == 5.0f) {
            return (int)spatialVector.get(5);
        } else {
            return 0;
        }
    }

    private static int numObsOf(Vector spatialVector) {
        return (int)spatialVector.get(1) + (int)spatialVector.get(2) + (int)spatialVector.get(3) + (int)spatialVector.get(4) + (int)spatialVector.get(5);
    }

    @Override
    public String toString() {
        return "AggregatorLcSeasonalComposite{" +
               "statusIndex=" + statusIndex +
               ", setIndexes=" + Arrays.toString(setIndexes) +
               ", spatialFeatureNames=" + Arrays.toString(getSpatialFeatureNames()) +
               ", temporalFeatureNames=" + Arrays.toString(getTemporalFeatureNames()) +
               ", outputFeatureNames=" + Arrays.toString(getOutputFeatureNames()) +
               '}';
    }

    public static class Config extends AggregatorConfig {
        /*
        @Parameter(label = "Status band name", notEmpty = true, notNull = true,
                           description = "Status flag, used as filter")
        String statusVarName;
        @Parameter(label = "Num passes band name", notEmpty = true, notNull = true,
                           description = "Number of passes, used as weight for averaging the SR values")
        String numPassesVarName;
        @Parameter(label = "Num obs band name", notEmpty = true, notNull = false,
                           description = "Number of observations, aggregated for status > 0")
        String numObsVarName;
        @Parameter(label = "Source band names", notNull = true, description = "The source bands to be aggregated")
        String[] setVarNames;
        */
        public Config() {
            super(Descriptor.NAME);
        }
    }

    public static class Descriptor implements AggregatorDescriptor {

        public static final String NAME = "LC_SEASONAL_COMPOSITE";

        @Override
        public String getName() {
            return NAME;
        }

        @Override
        public AggregatorConfig createConfig() {
            return new Config();
        }

        @Override
        public Aggregator createAggregator(VariableContext varCtx, AggregatorConfig aggregatorConfig) {
            Config config = (Config) aggregatorConfig;
            return new AggregatorLcSeasonalComposite(varCtx);
        }

        @Override
        public String[] getSourceVarNames(AggregatorConfig aggregatorConfig) {
            return SR_VARIABLES;
        }

        @Override
        public String[] getTargetVarNames(AggregatorConfig aggregatorConfig) {
            return SEASONAL_COMPOSITE_VARIABLES;
        }
    }
}
