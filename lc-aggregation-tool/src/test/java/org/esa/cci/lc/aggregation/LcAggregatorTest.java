package org.esa.cci.lc.aggregation;

import com.bc.ceres.binding.PropertyContainer;
import org.esa.beam.binning.operator.BinningConfig;
import org.esa.beam.binning.operator.BinningOp;
import org.esa.beam.binning.operator.FormatterConfig;
import org.esa.beam.binning.operator.VariableConfig;
import org.esa.beam.binning.support.SEAGrid;
import org.esa.beam.binning.support.VariableContextImpl;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.CrsGeoCoding;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.OperatorException;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.Test;

import javax.media.jai.operator.ConstantDescriptor;
import java.io.File;
import java.io.IOException;

import static org.junit.Assert.*;

/**
 * @author Marco Peters
 */
public class LcAggregatorTest {

    public static class Data {

        int numMajorityClasses;
    }

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


    @Test
    public void testAggregate() throws Exception {
        LcAggregator aggregator = createAggregator(2);

    }

    @Test
    public void testBinning() throws Exception {
        // todo remove this method
        // replaced by test in AggregationOpTest
        BinningConfig binningConfig = new BinningConfig();
        binningConfig.setMaskExpr("true");
        binningConfig.setNumRows(SEAGrid.DEFAULT_NUM_ROWS);
        int numMajorityClasses = 2;

        LcAggregatorConfig lcAggregatorConfig = new LcAggregatorConfig(numMajorityClasses);
        lcAggregatorConfig.setVarName("classes");
        binningConfig.setAggregatorConfigs(lcAggregatorConfig);
        binningConfig.setVariableConfigs(new VariableConfig("classes", null));

        BinningOp binningOp = new BinningOp();
        binningOp.setSourceProduct(createSourceProduct());
        binningOp.setBinningConfig(binningConfig);
        binningOp.setParameter("outputBinnedData", false);
        final FormatterConfig formatterConfig = new FormatterConfig();
        formatterConfig.setOutputFormat("BEAM-DIMAP");
        try {
            File tempFile = File.createTempFile("BEAM_", "_LC_AGGR.dim");
            tempFile.deleteOnExit();
            formatterConfig.setOutputFile(tempFile.getAbsolutePath());
            formatterConfig.setOutputFormat(ProductIO.DEFAULT_FORMAT_NAME);
            formatterConfig.setOutputType("Product");
        } catch (IOException e) {
            throw new OperatorException(e);
        }

        binningOp.setFormatterConfig(formatterConfig);

        Product targetProduct = binningOp.getTargetProduct();

        int numObsAndPasses = 2;
        assertEquals(LcAggregatorDescriptor.NUM_LC_CLASSES + numMajorityClasses + numObsAndPasses,
                     targetProduct.getNumBands());
    }

    private LcAggregator createAggregator(int numMajorityClasses) {
        Data param = new Data();
        param.numMajorityClasses = numMajorityClasses;
        PropertyContainer propertyContainer = PropertyContainer.createObjectBacked(param);
        VariableContextImpl varCtx = new VariableContextImpl();
        return (LcAggregator) new LcAggregatorDescriptor().createAggregator(varCtx, propertyContainer);
    }

    private Product createSourceProduct() throws Exception {
        Product product = new Product("P", "T", 360, 180);
        Band classesBand = product.addBand("classes", ProductData.TYPE_UINT8);
        classesBand.setSourceImage(ConstantDescriptor.create(360f, 180f, new Byte[]{10}, null));
        product.setGeoCoding(new CrsGeoCoding(DefaultGeographicCRS.WGS84, 360, 180, -180.0, 90.0, 1.0, 1.0));
        return product;
    }
}
