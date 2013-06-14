package org.esa.cci.lc.aggregation;

import org.esa.beam.binning.BinContext;
import org.esa.beam.binning.support.VariableContextImpl;
import org.esa.beam.binning.support.VectorImpl;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Arrays;

import static org.esa.cci.lc.aggregation.AggregatorTestUtils.*;
import static org.hamcrest.core.Is.*;
import static org.junit.Assert.*;

public class LcCondOccAggregatorTest {

    private static final String[] VAR_NAMES = new String[]{"ba_occ", "ba_nYearObs"};

    @Test
    public void testFeatureNames() throws Exception {
        LcCondOccAggregator aggregator = createAggregator(VAR_NAMES);

        String[] spatialFeatureNames = aggregator.getSpatialFeatureNames();
        String[] temporalFeatureNames = aggregator.getOutputFeatureNames();
        String[] outputFeatureNames = aggregator.getOutputFeatureNames();
        assertThat(spatialFeatureNames, is(new String[]{"ba_occ_proportion_area", "ba_occ_mean_frequency", "ba_nYearObs_sum"}));
        assertThat(temporalFeatureNames, is(spatialFeatureNames));
        assertThat(outputFeatureNames, is(spatialFeatureNames));
    }

    @Test
    public void testInitSpatial() {
        BinContext ctx = createCtx();
        LcCondOccAggregator aggregator = createAggregator(VAR_NAMES);

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
        LcCondOccAggregator aggregator = createAggregator(VAR_NAMES);

        VectorImpl spatialVector = vec(new float[aggregator.getSpatialFeatureNames().length]);
        aggregator.initSpatial(ctx, spatialVector);

        aggregator.aggregateSpatial(ctx, obs(18, 13), spatialVector);
        aggregator.aggregateSpatial(ctx, obs(45, 13), spatialVector);
        aggregator.aggregateSpatial(ctx, obs(9, 12), spatialVector);
        aggregator.aggregateSpatial(ctx, obs(45, 12), spatialVector);
        aggregator.aggregateSpatial(ctx, obs(Float.NaN, 0), spatialVector);
        int numObs = 5;
        aggregator.completeSpatial(ctx, numObs, spatialVector);
        assertEquals(80.0f, spatialVector.get(0), 1.0e-3f);
        assertEquals(29.25f, spatialVector.get(1), 1.0e-3f);
        assertEquals(50.0f, spatialVector.get(2), 1.0e-3f);

        VectorImpl temporalVector = vec(new float[aggregator.getTemporalFeatureNames().length]);
        aggregator.initTemporal(ctx, temporalVector);
        aggregator.aggregateTemporal(ctx, spatialVector, numObs, temporalVector);
        aggregator.completeTemporal(ctx, 1, temporalVector);
        assertEquals(80.0f, temporalVector.get(0), 1.0e-3f);
        assertEquals(29.25f, temporalVector.get(1), 1.0e-3f);
        assertEquals(50.f, temporalVector.get(2), 1.0e-3f);

        VectorImpl outputVector = vec(new float[aggregator.getOutputFeatureNames().length]);
        aggregator.computeOutput(temporalVector, outputVector);
        assertEquals(80.0f, outputVector.get(0), 1.0e-3f);
        assertEquals(29.25f, outputVector.get(1), 1.0e-3f);
        assertEquals(50.0f, outputVector.get(2), 1.0e-3f);

    }


    private LcCondOccAggregator createAggregator(String[] varNames) {
        VariableContextImpl varCtx = new VariableContextImpl();
        LcCondOccAggregatorDescriptor lcCondOccAggregatorDescriptor = new LcCondOccAggregatorDescriptor();

        FractionalAreaCalculator areaCalculator = Mockito.mock(FractionalAreaCalculator.class);
        Mockito.when(
                areaCalculator.calculate(Mockito.anyDouble(), Mockito.anyDouble(), Mockito.anyLong())).thenReturn(
                1.0);

        LcCondOccAggregatorConfig config = new LcCondOccAggregatorConfig(varNames);
        varCtx.defineVariable(config.getVarNames()[0]);
        varCtx.defineVariable(config.getVarNames()[1]);
        return (LcCondOccAggregator) lcCondOccAggregatorDescriptor.createAggregator(varCtx, config);
    }

}
