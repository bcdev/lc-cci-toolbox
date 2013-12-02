package org.esa.cci.lc.aggregation;

import org.esa.beam.binning.BinContext;
import org.esa.beam.binning.support.VariableContextImpl;
import org.esa.beam.binning.support.VectorImpl;
import org.junit.Test;

import java.util.Arrays;

import static org.esa.cci.lc.aggregation.AggregatorTestUtils.*;
import static org.hamcrest.core.Is.*;
import static org.junit.Assert.*;

public class LcNDVIAggregatorTest {

    @Test
    public void testFeatureNames() throws Exception {
        LcNDVIAggregator aggregator = createAggregator();

        String[] spatialFeatureNames = aggregator.getSpatialFeatureNames();
        String[] temporalFeatureNames = aggregator.getOutputFeatureNames();
        String[] outputFeatureNames = aggregator.getOutputFeatureNames();
        assertThat(spatialFeatureNames, is(new String[]{"ndvi_mean_mean", "ndvi_nYearObs_sum"}));
        assertThat(temporalFeatureNames, is(spatialFeatureNames));
        assertThat(outputFeatureNames, is(spatialFeatureNames));
    }

    @Test
    public void testInitSpatial() {
        BinContext ctx = createCtx();
        LcNDVIAggregator aggregator = createAggregator();

        String[] spatialFeatureNames = aggregator.getSpatialFeatureNames();
        float[] floats = new float[spatialFeatureNames.length];
        Arrays.fill(floats, 1.0f);
        VectorImpl spatialVector = vec(floats);
        aggregator.initSpatial(ctx, spatialVector);
        for (int i = 0; i < spatialVector.size(); i++) {
            assertEquals(0.0f, spatialVector.get(i), 1.0e-6f);
        }
    }

    @Test
    public void testAggregation() {
        BinContext ctx = createCtx();
        LcNDVIAggregator aggregator = createAggregator();

        VectorImpl spatialVector = vec(new float[aggregator.getSpatialFeatureNames().length]);
        aggregator.initSpatial(ctx, spatialVector);

        aggregator.aggregateSpatial(ctx, obs(0.48f, 13), spatialVector);
        aggregator.aggregateSpatial(ctx, obs(0.52f, 13), spatialVector);
        aggregator.aggregateSpatial(ctx, obs(Float.NaN, 12), spatialVector);
        aggregator.aggregateSpatial(ctx, obs(0.31f, Float.NaN), spatialVector);
        aggregator.aggregateSpatial(ctx, obs(0.39f, Float.NaN), spatialVector);
        int numObs = 5;
        aggregator.completeSpatial(ctx, numObs, spatialVector);
        assertEquals(0.425f, spatialVector.get(0), 1.0e-3f);
        assertEquals(12.67f, spatialVector.get(1), 1.0e-2f);

        VectorImpl temporalVector = vec(new float[aggregator.getTemporalFeatureNames().length]);
        aggregator.initTemporal(ctx, temporalVector);
        aggregator.aggregateTemporal(ctx, spatialVector, numObs, temporalVector);
        aggregator.completeTemporal(ctx, 1, temporalVector);
        assertEquals(0.425f, temporalVector.get(0), 1.0e-3f);
        assertEquals(12.67f, temporalVector.get(1), 1.0e-2f);

        VectorImpl outputVector = vec(new float[aggregator.getOutputFeatureNames().length]);
        aggregator.computeOutput(temporalVector, outputVector);
        assertEquals(0.425f, outputVector.get(0), 1.0e-3f);
        assertEquals(12.67f, outputVector.get(1), 1.0e-2f);

    }


    private LcNDVIAggregator createAggregator() {
        VariableContextImpl varCtx = new VariableContextImpl();
        LcNDVIAggregatorDescriptor lcNDVIAggregatorDescriptor = new LcNDVIAggregatorDescriptor();

        LcNDVIAggregatorConfig config = new LcNDVIAggregatorConfig(new String[]{"ndvi_mean", "ndvi_nYearObs"});
        varCtx.defineVariable(config.getVarNames()[0]);
        varCtx.defineVariable(config.getVarNames()[1]);
        return (LcNDVIAggregator) lcNDVIAggregatorDescriptor.createAggregator(varCtx, config);
    }

}
