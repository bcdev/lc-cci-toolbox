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

import org.esa.beam.binning.BinContext;
import org.esa.beam.binning.support.VectorImpl;
import org.junit.Before;
import org.junit.Test;

import static java.lang.Float.NaN;
import static org.esa.cci.lc.l3.AggregatorTestUtils.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AggregatorLcSeasonalCompositeTest {

    private BinContext ctx;
    private AggregatorLcSeasonalComposite agg;

    @Before
    public void setUp() throws Exception {
        ctx = createCtx();
        agg = new AggregatorLcSeasonalComposite(new MyVariableContext("current_pixel_state",
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
                    "vegetation_index_mean"));
    }

    @Test
    public void testMetadata() {
        assertEquals("LC_SEASONAL_COMPOSITE", agg.getName());

        assertEquals(20, agg.getSpatialFeatureNames().length);
        assertEquals("current_pixel_state", agg.getSpatialFeatureNames()[0]);
        assertEquals("clear_land_count", agg.getSpatialFeatureNames()[1]);
        assertEquals("clear_water_count", agg.getSpatialFeatureNames()[2]);
        assertEquals("sr_1_mean", agg.getSpatialFeatureNames()[6]);

        assertEquals(17, agg.getTemporalFeatureNames().length);
        assertEquals("status", agg.getTemporalFeatureNames()[0]);
        assertEquals("status_count", agg.getTemporalFeatureNames()[1]);
        assertEquals("obs_count", agg.getTemporalFeatureNames()[2]);
        assertEquals("sr_1_mean", agg.getTemporalFeatureNames()[3]);

        assertEquals(17, agg.getOutputFeatureNames().length);
        assertEquals("status", agg.getOutputFeatureNames()[0]);
        assertEquals("status_count", agg.getOutputFeatureNames()[1]);
        assertEquals("obs_count", agg.getOutputFeatureNames()[2]);
        assertEquals("sr_1_mean", agg.getOutputFeatureNames()[3]);

    }

    @Test
    public void testAggregatorLcSeasonalComposite() {
        VectorImpl svec = vec(NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN);
        VectorImpl tvec = vec(NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN);
        VectorImpl out = vec(NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN);

        agg.initSpatial(ctx, svec);
        assertEquals(0.0f, svec.get(0), 0.0f);
        assertEquals(NaN, svec.get(1), 0.0f);
        assertEquals(NaN, svec.get(2), 0.0f);
        assertEquals(NaN, svec.get(3), 0.0f);
        assertEquals(NaN, svec.get(4), 0.0f);
        assertEquals(NaN, svec.get(5), 0.0f);
        assertEquals(NaN, svec.get(6), 0.0f);

        agg.aggregateSpatial(ctx, obs(999, 3, 9,8,7,6,5, 0.6f,0.5f,0.5f,0.5f,0.5f,0.5f,0.5f,0.5f,0.5f,0.5f,0.5f,0.5f,0.5f, 1.1f), svec);
        assertEquals(3.0f, svec.get(0), 0.0f);
        assertEquals(9f, svec.get(1), 1e-5f);
        assertEquals(8f, svec.get(2), 1e-5f);
        assertEquals(7f, svec.get(3), 1e-5f);
        assertEquals(6f, svec.get(4), 1e-5f);
        assertEquals(5f, svec.get(5), 1e-5f);
        assertEquals(0.6f, svec.get(6), 1e-5f);
        assertEquals(0.5f, svec.get(7), 1e-5f);

        agg.completeSpatial(ctx, 1, svec);

        agg.initTemporal(ctx, tvec);
        assertEquals(Float.POSITIVE_INFINITY, tvec.get(0), 0.0f);
        assertEquals(0.0f, tvec.get(1), 0.0f);
        assertEquals(0.0f, tvec.get(2), 0.0f);
        assertTrue(Float.isNaN(tvec.get(3)));
        assertTrue(Float.isNaN(tvec.get(4)));

        agg.aggregateTemporal(ctx, svec, 99, tvec);
        assertEquals(3.0f, tvec.get(0), 0.0f);
        assertEquals(7f, tvec.get(1), 0.0f);
        assertEquals(35f, tvec.get(2), 0.0f);
        assertEquals(7*0.6f, tvec.get(3), 1e-5f);
        assertEquals(7*0.5f, tvec.get(4), 1e-5f);

        agg.completeTemporal(ctx, -1, tvec);
        agg.computeOutput(tvec, out);
        assertEquals(3.0f, out.get(0), 0.0f);
        assertEquals(7f, out.get(1), 0.0f);
        assertEquals(35f, out.get(2), 0.0f);
        assertEquals(0.6f, out.get(3), 1e-5f);
        assertEquals(0.5f, out.get(4), 1e-5f);
    }

}
