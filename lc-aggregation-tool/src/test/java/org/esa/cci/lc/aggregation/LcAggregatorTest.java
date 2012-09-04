package org.esa.cci.lc.aggregation;

import org.esa.beam.binning.BinContext;
import org.esa.beam.binning.support.VariableContextImpl;
import org.esa.beam.binning.support.VectorImpl;
import org.junit.Test;

import java.util.Arrays;

import static org.esa.cci.lc.aggregation.AggregatorTestUtils.*;
import static org.junit.Assert.*;

/**
 * @author Marco Peters
 */
public class LcAggregatorTest {

    @Test
    public void testFeatureNames() throws Exception {
        int numMajorityClasses = 3;
        LcAggregator aggregator = createAggregator(numMajorityClasses);

        String[] spatialFeatureNames = aggregator.getSpatialFeatureNames();
        String[] outputFeatureNames = aggregator.getOutputFeatureNames();

        assertEquals(LcAggregatorDescriptor.NUM_LC_CLASSES, spatialFeatureNames.length);
        assertEquals(LcAggregatorDescriptor.NUM_LC_CLASSES, aggregator.getTemporalFeatureNames().length);
        assertEquals(LcAggregatorDescriptor.NUM_LC_CLASSES + numMajorityClasses, outputFeatureNames.length);

        assertTrue(Float.isNaN(aggregator.getOutputFillValue()));

        assertEquals("class_area_1", spatialFeatureNames[0]);
        assertEquals("class_area_" + LcAggregatorDescriptor.NUM_LC_CLASSES,
                     spatialFeatureNames[LcAggregatorDescriptor.NUM_LC_CLASSES - 1]);

        assertEquals("class_area_1", outputFeatureNames[0]);
        assertEquals("class_area_" + LcAggregatorDescriptor.NUM_LC_CLASSES,
                     outputFeatureNames[LcAggregatorDescriptor.NUM_LC_CLASSES - 1]);
        assertEquals("majority_class_1", outputFeatureNames[LcAggregatorDescriptor.NUM_LC_CLASSES]);
        assertEquals("majority_class_3",
                     outputFeatureNames[LcAggregatorDescriptor.NUM_LC_CLASSES + numMajorityClasses - 1]);
    }

    @Test
    public void testInitSpatial() {
        BinContext ctx = createCtx();
        LcAggregator aggregator = createAggregator(2);

        String[] spatialFeatureNames = aggregator.getSpatialFeatureNames();
        float[] floats = new float[spatialFeatureNames.length];
        Arrays.fill(floats, 1.0f);
        VectorImpl spatialVector = vec(floats);
        aggregator.initSpatial(ctx, spatialVector);
        for (int i = 0; i < spatialVector.size(); i++) {
            assertEquals(Float.NaN, spatialVector.get(i), 1.0e-6f);
        }
    }

    @Test
    public void testAggregation() {
        BinContext ctx = createCtx();
        LcAggregator aggregator = createAggregator(2);

        String[] spatialFeatureNames = aggregator.getSpatialFeatureNames();
        float[] floats = new float[spatialFeatureNames.length];
        VectorImpl spatialVector = vec(floats);
        aggregator.initSpatial(ctx, spatialVector);

        int class8value = 80;
        int class1value = 10;
        aggregator.aggregateSpatial(ctx, obs(class8value), spatialVector);
        aggregator.aggregateSpatial(ctx, obs(class8value), spatialVector);
        aggregator.aggregateSpatial(ctx, obs(class1value), spatialVector);
        assertEquals(2.0f, spatialVector.get(7), 1.0e-6f);
        assertEquals(1.0f, spatialVector.get(0), 1.0e-6f);
    }

    private LcAggregator createAggregator(int numMajorityClasses) {
        VariableContextImpl varCtx = new VariableContextImpl();
        LcAggregatorDescriptor lcAggregatorDescriptor = new LcAggregatorDescriptor();
        LcAggregatorConfig config = new LcAggregatorConfig("classes", numMajorityClasses);
        return (LcAggregator) lcAggregatorDescriptor.createAggregator(varCtx, config);
    }
}
