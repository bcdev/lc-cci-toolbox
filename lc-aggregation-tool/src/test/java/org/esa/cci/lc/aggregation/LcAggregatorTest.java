package org.esa.cci.lc.aggregation;

import org.esa.beam.binning.BinContext;
import org.esa.beam.binning.support.VariableContextImpl;
import org.esa.beam.binning.support.VectorImpl;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Arrays;

import static org.esa.cci.lc.aggregation.AggregatorTestUtils.*;
import static org.junit.Assert.*;

public class LcAggregatorTest {

    @Test
    public void testFeatureNames() throws Exception {
        int numMajorityClasses = 3;
        LcAggregator aggregator = createAggregator(true, numMajorityClasses, false);

        String[] spatialFeatureNames = aggregator.getSpatialFeatureNames();
        String[] outputFeatureNames = aggregator.getOutputFeatureNames();

        int numClasses = LCCS.getInstance().getNumClasses();
        assertEquals(numClasses, spatialFeatureNames.length);
        assertEquals(numClasses, aggregator.getTemporalFeatureNames().length);
        assertEquals(numClasses + numMajorityClasses, outputFeatureNames.length);

        assertTrue(Float.isNaN(aggregator.getOutputFillValue()));

        assertEquals("class_area_0", spatialFeatureNames[0]);
        assertEquals("class_area_220", spatialFeatureNames[numClasses - 1]);

        assertEquals("class_area_0", outputFeatureNames[0]);
        assertEquals("class_area_220", outputFeatureNames[numClasses - 1]);
        assertEquals("majority_class_1", outputFeatureNames[numClasses]);
        assertEquals("majority_class_3", outputFeatureNames[numClasses + numMajorityClasses - 1]);
    }

    @Test
    public void testInitSpatial() {
        BinContext ctx = createCtx();
        LcAggregator aggregator = createAggregator(true, 2, false);

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
    public void testAggregation_WithoutPFTs() {
        int numMajorityClasses = 2;
        BinContext ctx = createCtx();
        LcAggregator aggregator = createAggregator(true, numMajorityClasses, false);

        int numSpatialFeatures = aggregator.getSpatialFeatureNames().length;
        VectorImpl spatialVector = vec(new float[numSpatialFeatures]);
        aggregator.initSpatial(ctx, spatialVector);

        int class1 = 10;
        int class1Index = 1;
        int class2 = 20;
        int class2Index = 4;
        int class82 = 82;
        int class8Index = 16;
        int class170 = 170;
        int class17Index = 30;

        aggregator.aggregateSpatial(ctx, obs(class82), spatialVector);
        aggregator.aggregateSpatial(ctx, obs(class82), spatialVector);
        aggregator.aggregateSpatial(ctx, obs(class1), spatialVector);
        aggregator.aggregateSpatial(ctx, obs(class170), spatialVector);
        aggregator.aggregateSpatial(ctx, obs(class170), spatialVector);
        aggregator.aggregateSpatial(ctx, obs(class2), spatialVector);
        aggregator.aggregateSpatial(ctx, obs(class170), spatialVector);
        aggregator.aggregateSpatial(ctx, obs(class170), spatialVector);
        aggregator.aggregateSpatial(ctx, obs(class170), spatialVector);
        int numObs = 9;
        aggregator.completeSpatial(ctx, numObs, spatialVector);
        assertEquals(1.0f, spatialVector.get(class1Index), 1.0e-6f);
        assertEquals(1.0f, spatialVector.get(class2Index), 1.0e-6f);
        assertEquals(2.0f, spatialVector.get(class8Index), 1.0e-6f);
        assertEquals(5.0f, spatialVector.get(class17Index), 1.0e-6f);

        VectorImpl temporalVector = vec(new float[numSpatialFeatures]);
        aggregator.aggregateTemporal(ctx, spatialVector, numObs, temporalVector);
        aggregator.completeTemporal(ctx, 1, temporalVector);
        for (int i = 0; i < numSpatialFeatures; i++) {
            assertEquals(spatialVector.get(i), temporalVector.get(i), 1.0e-6);
        }

        VectorImpl outputVector = vec(new float[numSpatialFeatures + numMajorityClasses]);
        aggregator.computeOutput(temporalVector, outputVector);
        for (int i = 0; i < temporalVector.size(); i++) {
            assertEquals(temporalVector.get(i), outputVector.get(i), 1.0e-6);
        }
        assertEquals(170, outputVector.get(outputVector.size() - 2), 0.0f); // majority class 1
        assertEquals(82, outputVector.get(outputVector.size() - 1), 0.0f);  // majority class 2

    }

    @Test
    public void testAggregation_WithPFTs() {
        int numMajorityClasses = 0;
        BinContext ctx = createCtx();
        LcAggregator aggregator = createAggregator(true, numMajorityClasses, true);

        int numSpatialFeatures = aggregator.getSpatialFeatureNames().length;
        VectorImpl spatialVector = vec(new float[numSpatialFeatures]);
        aggregator.initSpatial(ctx, spatialVector);

        int class1 = 10;
        int class2 = 20;
        int class82 = 82;
        int class170 = 170;

        aggregator.aggregateSpatial(ctx, obs(class82), spatialVector);
        aggregator.aggregateSpatial(ctx, obs(class82), spatialVector);
        aggregator.aggregateSpatial(ctx, obs(class1), spatialVector);
        aggregator.aggregateSpatial(ctx, obs(class170), spatialVector);
        aggregator.aggregateSpatial(ctx, obs(class170), spatialVector);
        aggregator.aggregateSpatial(ctx, obs(class2), spatialVector);
        aggregator.aggregateSpatial(ctx, obs(class170), spatialVector);
        aggregator.aggregateSpatial(ctx, obs(class170), spatialVector);
        aggregator.aggregateSpatial(ctx, obs(class170), spatialVector);
        int numObs = 9;
        aggregator.completeSpatial(ctx, numObs, spatialVector);
        VectorImpl temporalVector = vec(new float[numSpatialFeatures]);
        aggregator.aggregateTemporal(ctx, spatialVector, numObs, temporalVector);
        aggregator.completeTemporal(ctx, 1, temporalVector);

        int numPFTs = aggregator.getNumPFTs();
        VectorImpl outputVector = vec(new float[numSpatialFeatures + numMajorityClasses + numPFTs]);
        aggregator.computeOutput(temporalVector, outputVector);
        int startIndex = outputVector.size() - numPFTs;
        assertEquals(outputVector.get(startIndex + 0), 3, 1.0e-6); // Tree Broadleaf Evergreen ( 5 * 60% class170)
        assertEquals(outputVector.get(startIndex + 3), 0.6, 1.0e-6); // Tree Needleleaf Deciduous ( 2 * 30% class82)
        assertEquals(outputVector.get(startIndex + 7), 0.5, 1.0e-6); // Shrub Needleleaf Deciduous ( 2 * 25% class82)
        assertEquals(outputVector.get(startIndex + 9), 2.0, 1.0e-6); // Managed Grass ( 100% class1 & class2)
        assertEquals(outputVector.get(startIndex + 13), Float.NaN, 1.0e-6); // No data

    }

    @Test
    public void testMajorityClassesWhenHavingLessClassesObserved() {
        BinContext ctx = createCtx();
        int numMajorityClasses = 4;
        LcAggregator aggregator = createAggregator(true, numMajorityClasses, false);

        int numSpatialFeatures = aggregator.getSpatialFeatureNames().length;
        VectorImpl spatialVector = vec(new float[numSpatialFeatures]);
        aggregator.initSpatial(ctx, spatialVector);

        int class80 = 80;
        int class80Index = 14;
        aggregator.aggregateSpatial(ctx, obs(class80), spatialVector);
        aggregator.aggregateSpatial(ctx, obs(class80), spatialVector);
        assertEquals(2.0f, spatialVector.get(class80Index), 1.0e-6f);
        aggregator.completeSpatial(ctx, 2, spatialVector);

        VectorImpl temporalVector = vec(new float[numSpatialFeatures]);
        aggregator.aggregateTemporal(ctx, spatialVector, 2, temporalVector);
        aggregator.completeTemporal(ctx, 1, temporalVector);
        VectorImpl outputVector = vec(new float[numSpatialFeatures + numMajorityClasses]);
        aggregator.computeOutput(temporalVector, outputVector);
        assertEquals(80.0f, outputVector.get(outputVector.size() - 4), 0.0f); // majority_1
        assertEquals(Float.NaN, outputVector.get(outputVector.size() - 3), 0.0f); // majority_2
        assertEquals(Float.NaN, outputVector.get(outputVector.size() - 2), 0.0f); // majority_3
        assertEquals(Float.NaN, outputVector.get(outputVector.size() - 1), 0.0f); // majority_4
    }

    private LcAggregator createAggregator(boolean outputLCCSClasses, int numMajorityClasses, boolean outputPFTClasses) {
        VariableContextImpl varCtx = new VariableContextImpl();
        LcAggregatorDescriptor lcAggregatorDescriptor = new LcAggregatorDescriptor();

        FractionalAreaCalculator areaCalculator = Mockito.mock(FractionalAreaCalculator.class);
        Mockito.when(
                areaCalculator.calculate(Mockito.anyDouble(), Mockito.anyDouble(), Mockito.anyLong())).thenReturn(
                1.0);

        LcAggregatorConfig config = new LcAggregatorConfig("classes", outputLCCSClasses, numMajorityClasses, 10,
                                                           outputPFTClasses, areaCalculator);
        return (LcAggregator) lcAggregatorDescriptor.createAggregator(varCtx, config);
    }

}
