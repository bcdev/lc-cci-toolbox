package org.esa.cci.lc.aggregation;

import com.bc.ceres.binding.PropertyContainer;
import org.esa.beam.binning.Aggregator;
import org.esa.beam.binning.support.VariableContextImpl;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Marco Peters
 */
public class LcClassAggregatorTest {

    public static class Data {

        int numMajorityClasses;
    }

    @Test
    public void testFeatureNames() throws Exception {
        Data param = new Data();
        param.numMajorityClasses = 3;
        PropertyContainer propertyContainer = PropertyContainer.createObjectBacked(param);
        VariableContextImpl varCtx = new VariableContextImpl();
        Aggregator aggregator = new LcAggregatorDescriptor().createAggregator(varCtx, propertyContainer);

        String[] spatialFeatureNames = aggregator.getSpatialFeatureNames();
        String[] outputFeatureNames = aggregator.getOutputFeatureNames();

        assertEquals(LcAggregatorDescriptor.NUM_LC_CLASSES, spatialFeatureNames.length);
        assertEquals(LcAggregatorDescriptor.NUM_LC_CLASSES, aggregator.getTemporalFeatureNames().length);
        assertEquals(LcAggregatorDescriptor.NUM_LC_CLASSES + param.numMajorityClasses, outputFeatureNames.length);

        assertTrue(Float.isNaN(aggregator.getOutputFillValue()));

        assertEquals("class_area_1", spatialFeatureNames[0]);
        assertEquals("class_area_24", spatialFeatureNames[LcAggregatorDescriptor.NUM_LC_CLASSES - 1]);

        assertEquals("class_area_1", outputFeatureNames[0]);
        assertEquals("class_area_24", outputFeatureNames[LcAggregatorDescriptor.NUM_LC_CLASSES - 1]);
        assertEquals("majority_class_1", outputFeatureNames[LcAggregatorDescriptor.NUM_LC_CLASSES]);
        assertEquals("majority_class_3",
                     outputFeatureNames[LcAggregatorDescriptor.NUM_LC_CLASSES + param.numMajorityClasses - 1]);
    }

}
