package org.esa.cci.lc.aggregation;

import org.esa.beam.binning.BinContext;
import org.esa.beam.binning.Observation;
import org.esa.beam.binning.PlanetaryGrid;
import org.esa.beam.binning.SpatialBin;
import org.esa.beam.binning.support.PlateCarreeGrid;
import org.esa.beam.binning.support.RegularGaussianGrid;
import org.esa.beam.binning.support.VariableContextImpl;
import org.esa.beam.binning.support.VectorImpl;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;

import static org.esa.cci.lc.aggregation.AggregatorTestUtils.createCtx;
import static org.esa.cci.lc.aggregation.AggregatorTestUtils.obs;
import static org.esa.cci.lc.aggregation.AggregatorTestUtils.vec;
import static org.junit.Assert.assertEquals;

public class LcMapAggregatorTest {

    @Test
    public void testFeatureNames() throws Exception {
        int numMajorityClasses = 3;
        LcMapAggregator aggregator = createAggregator(true, numMajorityClasses, false, null, createMockedAreaCalculator(0));

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
        LcMapAggregator aggregator = createAggregator(true, 2, false, null, createMockedAreaCalculator(0));

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
        int numObs = 9;
        LcMapAggregator aggregator = createAggregator(true, numMajorityClasses, false, null,
                                                      createMockedAreaCalculator(numObs));

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
        aggregator.completeSpatial(ctx, numObs, spatialVector);
        assertEquals(1.0f / numObs, spatialVector.get(class1Index), 1.0e-6f);
        assertEquals(1.0f / numObs, spatialVector.get(class2Index), 1.0e-6f);
        assertEquals(2.0f / numObs, spatialVector.get(class8Index), 1.0e-6f);
        assertEquals(5.0f / numObs, spatialVector.get(class17Index), 1.0e-6f);

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
        int numObs = 9;
        LcMapAggregator aggregator = createAggregator(true, numMajorityClasses, true, null,
                                                      createMockedAreaCalculator(numObs));

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
        aggregator.completeSpatial(ctx, numObs, spatialVector);
        VectorImpl temporalVector = vec(new float[numSpatialFeatures]);
        aggregator.aggregateTemporal(ctx, spatialVector, numObs, temporalVector);
        aggregator.completeTemporal(ctx, 1, temporalVector);

        int numPFTs = aggregator.getNumPFTs();
        assertEquals(14, numPFTs);
        VectorImpl outputVector = vec(new float[numSpatialFeatures + numMajorityClasses + numPFTs]);
        aggregator.computeOutput(temporalVector, outputVector);
        int startIndex = outputVector.size() - numPFTs;
        assertEquals(3.0f / numObs, outputVector.get(startIndex + 0), 1.0e-6); // Tree Broadleaf Evergreen ( 5/numObs * 60% class170)
        assertEquals(0.6 / numObs, outputVector.get(startIndex + 3), 1.0e-6); // Tree Needleleaf Deciduous ( 2/numObs * 30% class82)
        assertEquals(2.0 / numObs, outputVector.get(startIndex + 9), 1.0e-6); // Managed Grass ( 1//numObs * 100% class1 & class2)
        assertEquals(Float.NaN, outputVector.get(startIndex + 13), 1.0e-6); // No data
    }

    @Test
    public void testAggregation_WithUserPFTs() throws URISyntaxException {
        int numMajorityClasses = 0;
        BinContext ctx = createCtx();
        URL resource = LcMapAggregatorTest.class.getResource("Test_User_LCCS_2PFT.csv");
        File lccs2PFTFile = new File(resource.toURI());

        int numObs = 10;
        LcMapAggregator aggregator = createAggregator(true, numMajorityClasses, true, lccs2PFTFile,
                                                      createMockedAreaCalculator(numObs));

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
        aggregator.completeSpatial(ctx, numObs, spatialVector);
        VectorImpl temporalVector = vec(new float[numSpatialFeatures]);
        aggregator.aggregateTemporal(ctx, spatialVector, numObs, temporalVector);
        aggregator.completeTemporal(ctx, 1, temporalVector);

        int numPFTs = aggregator.getNumPFTs();
        assertEquals(4, numPFTs);
        VectorImpl outputVector = vec(new float[numSpatialFeatures + numMajorityClasses + numPFTs]);
        aggregator.computeOutput(temporalVector, outputVector);
        int startIndex = outputVector.size() - numPFTs;
        assertEquals(1.1f / numObs, outputVector.get(startIndex + 0), 1.0e-6); // Bare Soil: 1 * 100% class11  + 1 * 10% class220
        assertEquals(2.0 / numObs, outputVector.get(
                startIndex + 1), 1.0e-6);  // Water: 5 * 10% class0 + 1 * 100% class10 + 1 * 50% class220
        assertEquals(4.4 / numObs, outputVector.get(startIndex + 2), 1.0e-6);  // Snow/Ice: 5 * 52% class0 + 2 * 90% class12
        assertEquals(2.5 / numObs, outputVector.get(
                startIndex + 3), 1.0e-6);  // No data: 5 * 38% class0 + 2 * 10% class12 + 1 * 40% class220

    }

    @Test
    public void testAggregationWithGaussianGrid() {
        final PlanetaryGrid planetaryGrid = new RegularGaussianGrid(160);
        BinContext ctx = new SpatialBin(8329, 0);
        AreaCalculator fractionCalculator = new FractionalAreaCalculator(planetaryGrid, 129600, 64800);
        PlateCarreeGrid plateCarreeGrid = new PlateCarreeGrid(64800);
        LcMapAggregator aggregator = createAggregator(true, 0, false, null, fractionCalculator);

        int numSpatialFeatures = aggregator.getSpatialFeatureNames().length;
        VectorImpl spatialVector = vec(new float[numSpatialFeatures]);
        aggregator.initSpatial(ctx, spatialVector);

        int class81 = 81;
        int class81Index = 15;
        int class82 = 82;
        int class82Index = 16;
        int colStartIndex = plateCarreeGrid.getColIndex(10.4 - 1.5);
        int rowStartIndex = plateCarreeGrid.getRowIndex(60.2 + 1.5);
        int colStopIndex = plateCarreeGrid.getColIndex(10.4 + 1.5);
        int rowStopIndex = plateCarreeGrid.getRowIndex(60.2 - 1.5);
        for (int y = rowStartIndex; y < rowStopIndex; y++) {
            for (int x = colStartIndex; x < colStopIndex; x++) {
                long binIndex = plateCarreeGrid.getFirstBinIndex(y) + x;
                double[] centerLatLon = plateCarreeGrid.getCenterLatLon(binIndex);
                Observation obs;
                if (x % 2 == 0) {
                    obs = obs(centerLatLon[0], centerLatLon[1], class81);
                } else {
                    obs = obs(centerLatLon[0], centerLatLon[1], class82);
                }
                aggregator.aggregateSpatial(ctx, obs, spatialVector);
            }
        }
        aggregator.completeSpatial(ctx, -1, spatialVector);
        float actual82Value = spatialVector.get(class82Index);
        float actual81Value = spatialVector.get(class81Index);

        float actualValue = actual81Value + actual82Value;
        assertEquals(1.0f, actualValue, 1.0e-6);
    }

    @Test
    public void testMajorityClassesWhenHavingLessClassesObserved() {
        BinContext ctx = createCtx();
        int numMajorityClasses = 4;
        int numObs = 2;
        LcMapAggregator aggregator = createAggregator(true, numMajorityClasses, false, null,
                                                      createMockedAreaCalculator(numObs));

        int numSpatialFeatures = aggregator.getSpatialFeatureNames().length;
        VectorImpl spatialVector = vec(new float[numSpatialFeatures]);
        aggregator.initSpatial(ctx, spatialVector);

        int class80 = 80;
        int class80Index = 14;
        aggregator.aggregateSpatial(ctx, obs(class80), spatialVector);
        aggregator.aggregateSpatial(ctx, obs(class80), spatialVector);
        assertEquals(2.0f / numObs, spatialVector.get(class80Index), 1.0e-6f);
        aggregator.completeSpatial(ctx, numObs, spatialVector);

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
                                             File lccs2PFTFile, AreaCalculator areaCalculator) {
        VariableContextImpl varCtx = new VariableContextImpl();
        LcMapAggregatorDescriptor lcMapAggregatorDescriptor = new LcMapAggregatorDescriptor();

        LcMapAggregatorConfig config = new LcMapAggregatorConfig(outputLCCSClasses, numMajorityClasses,
                                                                 outputPFTClasses, lccs2PFTFile,
                                                                 null, false, null,
                                                                 areaCalculator);
        return (LcMapAggregator) lcMapAggregatorDescriptor.createAggregator(varCtx, config);
    }

    private AreaCalculator createMockedAreaCalculator(int numObs) {
        AreaCalculator areaCalculator = Mockito.mock(AreaCalculator.class);
        Mockito.when(
                areaCalculator.calculate(Mockito.anyDouble(), Mockito.anyDouble(), Mockito.anyLong())).thenReturn(1.0 / numObs);
        return areaCalculator;
    }

}
