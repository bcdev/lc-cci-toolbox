package org.esa.cci.lc.aggregation;

import org.esa.beam.binning.operator.FormatterConfig;
import org.esa.beam.dataio.netcdf.metadata.profiles.beam.BeamNetCdf4WriterPlugIn;
import org.esa.beam.framework.dataio.ProductIOPlugInManager;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.CrsGeoCoding;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.OperatorException;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.media.jai.operator.ConstantDescriptor;
import java.io.File;
import java.io.IOException;

import static org.junit.Assert.*;


/**
 * @author Marco Peters
 */
public class AggregationOpTest {

    private static AggregationOp.Spi aggregationSpi;
    private static BeamNetCdf4WriterPlugIn beamNetCdf4WriterPlugIn;

    @BeforeClass
    public static void beforeClass() {
        aggregationSpi = new AggregationOp.Spi();
        GPF.getDefaultInstance().getOperatorSpiRegistry().addOperatorSpi(aggregationSpi);
        beamNetCdf4WriterPlugIn = new BeamNetCdf4WriterPlugIn();
        ProductIOPlugInManager.getInstance().addWriterPlugIn(beamNetCdf4WriterPlugIn);
    }

    @AfterClass
    public static void afterClass() {
        GPF.getDefaultInstance().getOperatorSpiRegistry().removeOperatorSpi(aggregationSpi);
        ProductIOPlugInManager.getInstance().removeWriterPlugIn(beamNetCdf4WriterPlugIn);
    }


    @Test()
    public void testProcessing() throws Exception {
        AggregationOp aggregationOp = new AggregationOp();
        aggregationOp.setSourceProduct(createSourceProduct());
        int numMajorityClasses = 2;
        aggregationOp.setNumberOfMajorityClasses(numMajorityClasses);
        aggregationOp.numRows = 4;
        aggregationOp.formatterConfig = createFormatterConfig();

        Product targetProduct = aggregationOp.getTargetProduct();

        int numObsAndPasses = 2;
        assertEquals(LcAggregatorDescriptor.NUM_LC_CLASSES + numMajorityClasses + numObsAndPasses,
                     targetProduct.getNumBands());
    }

    private FormatterConfig createFormatterConfig() throws IOException {
        final FormatterConfig formatterConfig = new FormatterConfig();
        formatterConfig.setOutputFormat("NetCDF4-BEAM");
        File tempFile = File.createTempFile("BEAM-TEST_", ".nc");
        tempFile.deleteOnExit();
        formatterConfig.setOutputFile(tempFile.getAbsolutePath());
        formatterConfig.setOutputType("Product");
        return formatterConfig;
    }


    @Test
    public void testDefaultValues() {
        AggregationOp aggrOp = (AggregationOp) aggregationSpi.createOperator();
        assertEquals(ProjectionMethod.GAUSSIAN_GRID, aggrOp.getProjectionMethod());
        assertEquals(0.1, aggrOp.getPixelSizeX(), 1.0e-8);
        assertEquals(0.1, aggrOp.getPixelSizeY(), 1.0e-8);
        assertEquals(-15.0, aggrOp.getWestBound(), 1.0e-8);
        assertEquals(30.0, aggrOp.getEastBound(), 1.0e-8);
        assertEquals(75.0, aggrOp.getNorthBound(), 1.0e-8);
        assertEquals(35.0, aggrOp.getSouthBound(), 1.0e-8);
        assertTrue(aggrOp.isOutputMajorityClasses());
        assertEquals(5, aggrOp.getNumberOfMajorityClasses());
        assertTrue(aggrOp.isOutputPFTClasses());

        FormatterConfig formatterConfig = aggrOp.getFormatterConfig();
        assertEquals("Product", formatterConfig.getOutputType());
        assertEquals("NetCDF4-BEAM", formatterConfig.getOutputFormat());
        assertEquals("target.nc", formatterConfig.getOutputFile());
    }

    @Test
    public void testWestEastBound() {
        AggregationOp aggrOp = (AggregationOp) aggregationSpi.createOperator();
        aggrOp.setWestBound(10);
        aggrOp.setEastBound(3);
        try {
            aggrOp.initialize();
        } catch (OperatorException oe) {
            String message = oe.getMessage().toLowerCase();
            assertTrue(message.contains("west bound"));
            assertTrue(message.contains("east bound"));
        }
    }

    @Test
    public void testNorthSouthBound() {
        AggregationOp aggrOp = (AggregationOp) aggregationSpi.createOperator();
        aggrOp.setNorthBound(30);
        aggrOp.setSouthBound(70);
        try {
            aggrOp.initialize();
        } catch (OperatorException oe) {
            String message = oe.getMessage().toLowerCase();
            assertTrue(message.contains("north bound"));
            assertTrue(message.contains("south bound"));
        }
    }

    @Test
    public void testNoOutputClassesSelected() {
        AggregationOp aggrOp = (AggregationOp) aggregationSpi.createOperator();
        aggrOp.setOutputMajorityClasses(false);
        aggrOp.setOutputPFTClasses(false);
        try {
            aggrOp.initialize();
        } catch (OperatorException oe) {
            String message = oe.getMessage().toLowerCase();
            assertTrue(message.contains("and/or"));
            assertTrue(message.contains("classes"));
        }
    }

    private Product createSourceProduct() throws Exception {
        Integer width = 360;
        Integer height = 180;
        Product product = new Product("P", "T", width, height);
        Band classesBand = product.addBand("classes", ProductData.TYPE_UINT8);
        classesBand.setSourceImage(ConstantDescriptor.create(width.floatValue(), height.floatValue(),
                                                             new Byte[]{10}, null));
        product.setGeoCoding(new CrsGeoCoding(DefaultGeographicCRS.WGS84, width, height, -180.0, 90.0, 1.0, 1.0));
        return product;
    }


}