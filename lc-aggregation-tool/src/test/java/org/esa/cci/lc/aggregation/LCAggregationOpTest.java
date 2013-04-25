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
import org.hamcrest.Matcher;
import org.hamcrest.core.IsNull;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.matchers.*;

import javax.media.jai.operator.ConstantDescriptor;
import java.io.File;
import java.io.IOException;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.junit.matchers.JUnitMatchers.containsString;


public class LcAggregationOpTest {

    private static LcAggregationOp.Spi aggregationSpi;
    private static BeamNetCdf4WriterPlugIn beamNetCdf4WriterPlugIn;

    @BeforeClass
    public static void beforeClass() {
        aggregationSpi = new LcAggregationOp.Spi();
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
    public void testDefaultTargetProductCreation() throws Exception {
        LcAggregationOp aggregationOp = createAggrOp();

        aggregationOp.setSourceProduct(createSourceProduct());
        int numMajorityClasses = 2;
        aggregationOp.setNumberOfMajorityClasses(numMajorityClasses);
        aggregationOp.setNumRows(4);
        aggregationOp.formatterConfig = createFormatterConfig();
        setDefaultBounds(aggregationOp);

        Product targetProduct = aggregationOp.getTargetProduct();

        int numObsAndPasses = 2;
        int numPFTs = 14;
        int numAccuracyBands = 1;
        final int expectedNumBands = LCCS.getInstance().getNumClasses()
                                     + numMajorityClasses
                                     + numObsAndPasses
                                     + numPFTs
                                     + numAccuracyBands;
        assertThat(targetProduct.getNumBands(), is(expectedNumBands));
    }

    private void setDefaultBounds(LcAggregationOp aggregationOp) {
        aggregationOp.setEastBound(15);
        aggregationOp.setWestBound(-15);
        aggregationOp.setSouthBound(-15);
        aggregationOp.setNorthBound(15);
    }

    @Test()
    public void testTargetProductCreation_WithOnlyPFTClasses() throws Exception {
        LcAggregationOp aggregationOp = createAggrOp();
        setDefaultBounds(aggregationOp);
        aggregationOp.setSourceProduct(createSourceProduct());
        aggregationOp.setOutputLCCSClasses(false);
        int numMajorityClasses = 0;
        aggregationOp.setNumberOfMajorityClasses(numMajorityClasses);
        aggregationOp.setNumRows(4);
        aggregationOp.formatterConfig = createFormatterConfig();

        Product targetProduct = aggregationOp.getTargetProduct();

        int numObsAndPasses = 2;
        int numPFTs = 14;
        int numAccuracyBands = 1;
        final int expectedNumBands = numMajorityClasses
                                     + numObsAndPasses
                                     + numPFTs
                                     + numAccuracyBands;
        assertThat(targetProduct.getNumBands(), is(expectedNumBands));
    }

    @Test
    public void testDefaultValues() {
        LcAggregationOp aggrOp = (LcAggregationOp) aggregationSpi.createOperator();
        assertThat(aggrOp.getProjectionMethod(), isNull());
        assertThat(aggrOp.getPixelSizeX(), is(0.1));
        assertThat(aggrOp.getPixelSizeY(), is(0.1));
        assertThat(aggrOp.getWestBound(), isNull());
        assertThat(aggrOp.getEastBound(), isNull());
        assertThat(aggrOp.getNorthBound(), isNull());
        assertThat(aggrOp.getSouthBound(), isNull());
        assertThat(aggrOp.isOutputLCCSClasses(), is(true));
        assertThat(aggrOp.getNumberOfMajorityClasses(), is(5));
        assertThat(aggrOp.isOutputPFTClasses(), is(true));
        assertThat(aggrOp.getNumRows(), is(2160));
//        assertEquals(ProjectionMethod.GEOGRAPHIC_LAT_LON, aggrOp.getProjectionMethod());
//        assertEquals(0.1, aggrOp.getPixelSizeX(), 1.0e-8);
//        assertEquals(0.1, aggrOp.getPixelSizeY(), 1.0e-8);
//        assertEquals(-15.0, aggrOp.getWestBound(), 1.0e-8);
//        assertEquals(30.0, aggrOp.getEastBound(), 1.0e-8);
//        assertEquals(75.0, aggrOp.getNorthBound(), 1.0e-8);
//        assertEquals(35.0, aggrOp.getSouthBound(), 1.0e-8);
//        assertTrue(aggrOp.isOutputLCCSClasses());
//        assertEquals(5, aggrOp.getNumberOfMajorityClasses());
//        assertTrue(aggrOp.isOutputPFTClasses());
//        assertEquals(2160, aggrOp.getNumRows());

        FormatterConfig formatterConfig = aggrOp.createDefaultFormatterConfig();
        assertThat(formatterConfig.getOutputType(), is("Product"));
        assertThat(formatterConfig.getOutputFormat(), is("NetCDF4-BEAM"));
    }

    private IsNull isNull() {
        return new IsNull();
    }

    @Test
    public void testNumRows_LessThanTwo() {
        LcAggregationOp aggrOp = createAggrOp();
        setDefaultBounds(aggrOp);
        aggrOp.setNumRows(1);
        try {
            aggrOp.initialize();
        } catch (OperatorException oe) {
            String message = oe.getMessage().toLowerCase();
            assertThat(message, containsString("rows"));
        }
    }

    @Test
    public void testNumRows_OddValue() {
        LcAggregationOp aggrOp = createAggrOp();
        setDefaultBounds(aggrOp);
        aggrOp.setNumRows(23);
        try {
            aggrOp.initialize();
        } catch (OperatorException oe) {
            String message = oe.getMessage().toLowerCase();
            assertThat(message, containsString("rows"));
        }
    }

    @Test
    public void testWestEastBound() {
        LcAggregationOp aggrOp = createAggrOp();
        setDefaultBounds(aggrOp);
        aggrOp.setWestBound(10);
        aggrOp.setEastBound(3);
        try {
            aggrOp.initialize();
        } catch (OperatorException oe) {
            String message = oe.getMessage().toLowerCase();
            assertThat(message, containsString("west bound"));
            assertThat(message, containsString("east bound"));
        }
    }

    @Test
    public void testNorthSouthBound() {
        LcAggregationOp aggrOp = createAggrOp();
        setDefaultBounds(aggrOp);
        aggrOp.setNorthBound(30);
        aggrOp.setSouthBound(70);
        try {
            aggrOp.initialize();
        } catch (OperatorException oe) {
            String message = oe.getMessage().toLowerCase();
            assertThat(message, containsString("north bound"));
            assertThat(message, containsString("south bound"));
        }
    }

    @Test
    public void testNoOutputSelected() {
        LcAggregationOp aggrOp = createAggrOp();
        aggrOp.setOutputLCCSClasses(false);
        aggrOp.setOutputPFTClasses(false);
        aggrOp.setNumberOfMajorityClasses(0);
        setDefaultBounds(aggrOp);
        try {
            aggrOp.initialize();
        } catch (OperatorException oe) {
            String message = oe.getMessage();
            assertThat(message, containsString("LCCS"));
            assertThat(message, containsString("majority"));
            assertThat(message, containsString("PFT"));
        }
    }

    @Test
    public void testOnlyOneIsTrue() {
        final boolean O = false;
        final boolean I = true;

        assertThat(LcAggregationOp.onlyOneIsTrue(O, O, O), is(false));
        assertThat(LcAggregationOp.onlyOneIsTrue(O, O, I), is(true));
        assertThat(LcAggregationOp.onlyOneIsTrue(O, I, O), is(true));
        assertThat(LcAggregationOp.onlyOneIsTrue(O, I, I), is(false));
        assertThat(LcAggregationOp.onlyOneIsTrue(I, O, O), is(true));
        assertThat(LcAggregationOp.onlyOneIsTrue(I, O, I), is(false));
        assertThat(LcAggregationOp.onlyOneIsTrue(I, I, O), is(false));
        assertThat(LcAggregationOp.onlyOneIsTrue(I, I, I), is(false));
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

    private Product createSourceProduct() throws Exception {
        final Integer width = 360;
        final Integer height = 180;
        final Product product = new Product("P", "T", width, height);
        final Band classesBand = product.addBand("lccs_class", ProductData.TYPE_UINT8);
        classesBand.setSourceImage(ConstantDescriptor.create(width.floatValue(), height.floatValue(),
                                                             new Byte[]{10}, null));
        final Band confidBand = product.addBand("algorithmic_confidence_level", ProductData.TYPE_FLOAT32);
        confidBand.setSourceImage(ConstantDescriptor.create(width.floatValue(), height.floatValue(),
                                                             new Float[]{10f}, null));
        final Band processedFlag = product.addBand("processed_flag", ProductData.TYPE_UINT8);
        processedFlag.setSourceImage(ConstantDescriptor.create(width.floatValue(), height.floatValue(),
                                                             new Byte[]{1}, null));
        final Band currentPixelState = product.addBand("current_pixel_state", ProductData.TYPE_UINT8);
        currentPixelState.setSourceImage(ConstantDescriptor.create(width.floatValue(), height.floatValue(),
                                                             new Byte[]{1}, null));
        product.setGeoCoding(new CrsGeoCoding(DefaultGeographicCRS.WGS84, width, height, -180.0, 90.0, 1.0, 1.0));
        return product;
    }

    private LcAggregationOp createAggrOp() {
        LcAggregationOp aggregationOp = (LcAggregationOp) aggregationSpi.createOperator();
        aggregationOp.setTargetFile(new File("test-target.nc"));
        return aggregationOp;
    }
}