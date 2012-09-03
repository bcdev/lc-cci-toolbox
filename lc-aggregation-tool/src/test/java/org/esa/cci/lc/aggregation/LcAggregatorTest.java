package org.esa.cci.lc.aggregation;

import org.esa.beam.binning.support.VariableContextImpl;
import org.junit.Test;

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

    private LcAggregator createAggregator(int numMajorityClasses) {
        VariableContextImpl varCtx = new VariableContextImpl();
        LcAggregatorDescriptor lcAggregatorDescriptor = new LcAggregatorDescriptor();
        LcAggregatorConfig config = new LcAggregatorConfig("classes", numMajorityClasses);
        return (LcAggregator) lcAggregatorDescriptor.createAggregator(varCtx, config);
    }
}
