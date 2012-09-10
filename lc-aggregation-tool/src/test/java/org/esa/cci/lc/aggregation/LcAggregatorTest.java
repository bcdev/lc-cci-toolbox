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
            assertEquals(0.0f, spatialVector.get(i), 1.0e-6f);
        }
    }

    @Test
    public void testAggregation() {
        int numMajorityClasses = 2;
        BinContext ctx = createCtx();
        LcAggregator aggregator = createAggregator(numMajorityClasses);

        int numSpatialFeatures = aggregator.getSpatialFeatureNames().length;
        VectorImpl spatialVector = vec(new float[numSpatialFeatures]);
        aggregator.initSpatial(ctx, spatialVector);

        int class1 = 10;
        int class2 = 20;
        int class8 = 80;
        int class17 = 170;
        aggregator.aggregateSpatial(ctx, obs(class8), spatialVector);
        aggregator.aggregateSpatial(ctx, obs(class8), spatialVector);
        aggregator.aggregateSpatial(ctx, obs(class1), spatialVector);
        aggregator.aggregateSpatial(ctx, obs(class17), spatialVector);
        aggregator.aggregateSpatial(ctx, obs(class17), spatialVector);
        aggregator.aggregateSpatial(ctx, obs(class2), spatialVector);
        aggregator.aggregateSpatial(ctx, obs(class17), spatialVector);
        aggregator.aggregateSpatial(ctx, obs(class17), spatialVector);
        aggregator.aggregateSpatial(ctx, obs(class17), spatialVector);
        aggregator.completeSpatial(ctx, 9, spatialVector);
        assertEquals(2.0f, spatialVector.get(7), 1.0e-6f);
        assertEquals(1.0f, spatialVector.get(0), 1.0e-6f);
        assertEquals(1.0f, spatialVector.get(1), 1.0e-6f);
        assertEquals(5.0f, spatialVector.get(16), 1.0e-6f);

        VectorImpl temporalVector = vec(new float[numSpatialFeatures]);
        aggregator.aggregateTemporal(ctx, spatialVector, 9, temporalVector);
        aggregator.completeTemporal(ctx, 1, temporalVector);
        for (int i = 0; i < numSpatialFeatures; i++) {
            assertEquals(spatialVector.get(i), temporalVector.get(i), 1.0e-6);
        }

        VectorImpl outputVector = vec(new float[numSpatialFeatures + numMajorityClasses]);
        aggregator.computeOutput(temporalVector, outputVector);
        for (int i = 0; i < temporalVector.size(); i++) {
            assertEquals(temporalVector.get(i), outputVector.get(i), 1.0e-6);
        }
        assertEquals(17, outputVector.get(outputVector.size() - 2), 0.0f);
        assertEquals(8, outputVector.get(outputVector.size() - 1), 0.0f);
    }

    @Test
    public void testMajorityClassesWhenHavingLessClassesObserved() {
        BinContext ctx = createCtx();
        int numMajorityClasses = 4;
        LcAggregator aggregator = createAggregator(numMajorityClasses);

        int numSpatialFeatures = aggregator.getSpatialFeatureNames().length;
        VectorImpl spatialVector = vec(new float[numSpatialFeatures]);
        aggregator.initSpatial(ctx, spatialVector);

        int class8 = 80;
        aggregator.aggregateSpatial(ctx, obs(class8), spatialVector);
        aggregator.aggregateSpatial(ctx, obs(class8), spatialVector);
        assertEquals(2.0f, spatialVector.get(7), 1.0e-6f);
        aggregator.completeSpatial(ctx, 2, spatialVector);

        VectorImpl temporalVector = vec(new float[numSpatialFeatures]);
        aggregator.aggregateTemporal(ctx, spatialVector, 2, temporalVector);
        aggregator.completeTemporal(ctx, 1, temporalVector);
        VectorImpl outputVector = vec(new float[numSpatialFeatures + numMajorityClasses]);
        aggregator.computeOutput(temporalVector, outputVector);
        assertEquals(8, outputVector.get(outputVector.size() - 4), 0.0f);
        assertEquals(Float.NaN, outputVector.get(outputVector.size() - 3), 0.0f);
        assertEquals(Float.NaN, outputVector.get(outputVector.size() - 2), 0.0f);
        assertEquals(Float.NaN, outputVector.get(outputVector.size() - 1), 0.0f);
    }

    private LcAggregator createAggregator(int numMajorityClasses) {
        VariableContextImpl varCtx = new VariableContextImpl();
        LcAggregatorDescriptor lcAggregatorDescriptor = new LcAggregatorDescriptor();
        AreaCalculator areaCalculator = new ConstAreaCalculator();
        LcAggregatorConfig config = new LcAggregatorConfig("classes", numMajorityClasses, 10, areaCalculator);
        return (LcAggregator) lcAggregatorDescriptor.createAggregator(varCtx, config);
    }

    private static class ConstAreaCalculator implements AreaCalculator {

        @Override
        public double calculate(double observationLat, double gridLat, double numGridCols) {
            return 1.0;
        }
    }
}
