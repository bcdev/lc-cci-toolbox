package org.esa.cci.lc.aggregation;

import org.esa.beam.binning.BinContext;
import org.esa.beam.binning.support.VariableContextImpl;
import org.esa.beam.binning.support.VectorImpl;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;

import static org.esa.cci.lc.aggregation.AggregatorTestUtils.*;
import static org.junit.Assert.*;

public class LcMapAggregatorTest {

    @Test
    public void testFeatureNames() throws Exception {
        int numMajorityClasses = 3;
        LcMapAggregator aggregator = createAggregator(true, numMajorityClasses, false, null);

        String[] spatialFeatureNames = aggregator.getSpatialFeatureNames();
        String[] outputFeatureNames = aggregator.getOutputFeatureNames();

        int numClasses = LCCS.getInstance().getNumClasses();
        assertEquals(numClasses, spatialFeatureNames.length);
        assertEquals(numClasses, aggregator.getTemporalFeatureNames().length);
        assertEquals(numClasses + numMajorityClasses, outputFeatureNames.length);

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
        LcMapAggregator aggregator = createAggregator(true, 2, false, null);

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
    public void testAggregation_WithoutPFTs() {
        int numMajorityClasses = 2;
        BinContext ctx = createCtx();
        LcMapAggregator aggregator = createAggregator(true, numMajorityClasses, false, null);

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
        int class17Index = 29;

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
        LcMapAggregator aggregator = createAggregator(true, numMajorityClasses, true, null);

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
        assertEquals(14, numPFTs);
        VectorImpl outputVector = vec(new float[numSpatialFeatures + numMajorityClasses + numPFTs]);
        aggregator.computeOutput(temporalVector, outputVector);
        int startIndex = outputVector.size() - numPFTs;
        assertEquals(3, outputVector.get(startIndex + 0), 1.0e-6); // Tree Broadleaf Evergreen ( 5 * 60% class170)
        assertEquals(0.6, outputVector.get(startIndex + 3), 1.0e-6); // Tree Needleleaf Deciduous ( 2 * 30% class82)
        assertEquals(2.0, outputVector.get(startIndex + 9), 1.0e-6); // Managed Grass ( 100% class1 & class2)
        assertEquals(Float.NaN, outputVector.get(startIndex + 13), 1.0e-6); // No data
    }

    @Test
    public void testAggregation_WithUserPFTs() throws URISyntaxException {
        int numMajorityClasses = 0;
        BinContext ctx = createCtx();
        URL resource = LcMapAggregatorTest.class.getResource("Test_User_LCCS_2PFT.csv");
        File lccs2PFTFile = new File(resource.toURI());

        LcMapAggregator aggregator = createAggregator(true, numMajorityClasses, true, lccs2PFTFile);

        int numSpatialFeatures = aggregator.getSpatialFeatureNames().length;
        VectorImpl spatialVector = vec(new float[numSpatialFeatures]);
        aggregator.initSpatial(ctx, spatialVector);

        int class0 = 0;
        int class10 = 10;
        int class11 = 11;
        int class12 = 12;
        int class220 = 220;

        aggregator.aggregateSpatial(ctx, obs(class12), spatialVector);
        aggregator.aggregateSpatial(ctx, obs(class12), spatialVector);
        aggregator.aggregateSpatial(ctx, obs(class10), spatialVector);
        aggregator.aggregateSpatial(ctx, obs(class0), spatialVector);
        aggregator.aggregateSpatial(ctx, obs(class0), spatialVector);
        aggregator.aggregateSpatial(ctx, obs(class11), spatialVector);
        aggregator.aggregateSpatial(ctx, obs(class0), spatialVector);
        aggregator.aggregateSpatial(ctx, obs(class0), spatialVector);
        aggregator.aggregateSpatial(ctx, obs(class220), spatialVector);
        aggregator.aggregateSpatial(ctx, obs(class0), spatialVector);
        int numObs = 10;
        aggregator.completeSpatial(ctx, numObs, spatialVector);
        VectorImpl temporalVector = vec(new float[numSpatialFeatures]);
        aggregator.aggregateTemporal(ctx, spatialVector, numObs, temporalVector);
        aggregator.completeTemporal(ctx, 1, temporalVector);

        int numPFTs = aggregator.getNumPFTs();
        assertEquals(4, numPFTs);
        VectorImpl outputVector = vec(new float[numSpatialFeatures + numMajorityClasses + numPFTs]);
        aggregator.computeOutput(temporalVector, outputVector);
        int startIndex = outputVector.size() - numPFTs;
        assertEquals(1.1, outputVector.get(startIndex + 0), 1.0e-6); // Bare Soil: 1 * 100% class11  + 1 * 10% class220
        assertEquals(2.0, outputVector.get(
                startIndex + 1), 1.0e-6);  // Water: 5 * 10% class0 + 1 * 100% class10 + 1 * 50% class220
        assertEquals(4.4, outputVector.get(startIndex + 2), 1.0e-6);  // Snow/Ice: 5 * 52% class0 + 2 * 90% class12
        assertEquals(2.5, outputVector.get(
                startIndex + 3), 1.0e-6);  // No data: 5 * 38% class0 + 2 * 10% class12 + 1 * 40% class220

    }

    @Test
    public void testMajorityClassesWhenHavingLessClassesObserved() {
        BinContext ctx = createCtx();
        int numMajorityClasses = 4;
        LcMapAggregator aggregator = createAggregator(true, numMajorityClasses, false, null);

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

    private LcMapAggregator createAggregator(boolean outputLCCSClasses, int numMajorityClasses, boolean outputPFTClasses,
                                             File lccs2PFTFile) {
        VariableContextImpl varCtx = new VariableContextImpl();
        LcMapAggregatorDescriptor lcMapAggregatorDescriptor = new LcMapAggregatorDescriptor();

        FractionalAreaCalculator areaCalculator = Mockito.mock(FractionalAreaCalculator.class);
        Mockito.when(
                areaCalculator.calculate(Mockito.anyDouble(), Mockito.anyDouble(), Mockito.anyLong())).thenReturn(
                1.0);

        LcMapAggregatorConfig config = new LcMapAggregatorConfig(outputLCCSClasses, numMajorityClasses,
                                                                 outputPFTClasses, lccs2PFTFile, areaCalculator);
        return (LcMapAggregator) lcMapAggregatorDescriptor.createAggregator(varCtx, config);
    }

}
