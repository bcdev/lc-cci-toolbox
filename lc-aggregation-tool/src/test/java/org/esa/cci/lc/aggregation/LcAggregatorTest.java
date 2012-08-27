package org.esa.cci.lc.aggregation;

import org.esa.beam.binning.support.VariableContextImpl;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.CrsGeoCoding;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.Test;

import javax.media.jai.operator.ConstantDescriptor;

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
        assertEquals("class_area_24", spatialFeatureNames[LcAggregatorDescriptor.NUM_LC_CLASSES - 1]);

        assertEquals("class_area_1", outputFeatureNames[0]);
        assertEquals("class_area_24", outputFeatureNames[LcAggregatorDescriptor.NUM_LC_CLASSES - 1]);
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

    private Product createSourceProduct() throws Exception {
        Product product = new Product("P", "T", 360, 180);
        Band classesBand = product.addBand("classes", ProductData.TYPE_UINT8);
        classesBand.setSourceImage(ConstantDescriptor.create(360f, 180f, new Byte[]{10}, null));
        product.setGeoCoding(new CrsGeoCoding(DefaultGeographicCRS.WGS84, 360, 180, -180.0, 90.0, 1.0, 1.0));
        return product;
    }
}
