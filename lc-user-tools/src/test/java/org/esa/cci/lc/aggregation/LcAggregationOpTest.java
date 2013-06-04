package org.esa.cci.lc.aggregation;

import org.esa.beam.binning.operator.FormatterConfig;
import org.esa.beam.dataio.netcdf.metadata.profiles.beam.BeamNetCdf4WriterPlugIn;
import org.esa.beam.framework.dataio.ProductIOPlugInManager;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.CrsGeoCoding;
import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpiRegistry;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.hamcrest.core.IsNull;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.media.jai.operator.ConstantDescriptor;
import java.io.File;
import java.io.IOException;

import static org.hamcrest.core.Is.*;
import static org.junit.Assert.*;
import static org.junit.matchers.JUnitMatchers.*;


public class LcAggregationOpTest {

    private static LcAggregationOp.Spi aggregationSpi;
    private static BeamNetCdf4WriterPlugIn beamNetCdf4WriterPlugIn;

    @BeforeClass
    public static void beforeClass() {
        aggregationSpi = new LcAggregationOp.Spi();
        OperatorSpiRegistry spiRegistry = GPF.getDefaultInstance().getOperatorSpiRegistry();
        spiRegistry.addOperatorSpi(aggregationSpi);
        beamNetCdf4WriterPlugIn = new BeamNetCdf4WriterPlugIn();
        ProductIOPlugInManager.getInstance().addWriterPlugIn(beamNetCdf4WriterPlugIn);
    }

    @AfterClass
    public static void afterClass() {
        OperatorSpiRegistry spiRegistry = GPF.getDefaultInstance().getOperatorSpiRegistry();
        spiRegistry.removeOperatorSpi(aggregationSpi);
        ProductIOPlugInManager.getInstance().removeWriterPlugIn(beamNetCdf4WriterPlugIn);
    }

    @Test()
    public void testTargetProductCreation_Binning() throws Exception {
        // preparation
        LcAggregationOp aggregationOp = createAggrOp();
        aggregationOp.setGridName(PlanetaryGridName.GEOGRAPHIC_LAT_LON);
        aggregationOp.setSourceProduct(createSourceProduct());
        int numMajorityClasses = 2;
        aggregationOp.setNumMajorityClasses(numMajorityClasses);
        aggregationOp.setNumRows(4);
        aggregationOp.formatterConfig = createFormatterConfig();
        aggregationOp.outputTargetProduct = true;

        // execution
        Product targetProduct = aggregationOp.getTargetProduct();

        // verification
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

    @Test()
    public void testTargetProductCreation_WithOnlyPFTClasses() throws Exception {
        // preparation
        LcAggregationOp aggregationOp = createAggrOp();
        aggregationOp.setGridName(PlanetaryGridName.GEOGRAPHIC_LAT_LON);
        aggregationOp.setSourceProduct(createSourceProduct());
        aggregationOp.setOutputLCCSClasses(false);
        int numMajorityClasses = 0;
        aggregationOp.setNumMajorityClasses(numMajorityClasses);
        aggregationOp.setNumRows(4);
        aggregationOp.formatterConfig = createFormatterConfig();
        aggregationOp.outputTargetProduct = true;

        // execution
        Product targetProduct = aggregationOp.getTargetProduct();

        // verification
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
        assertThat(aggrOp.getGridName(), isNull());
//        assertThat(aggrOp.getPixelSizeX(), is(0.1));
//        assertThat(aggrOp.getPixelSizeY(), is(0.1));
        assertThat(aggrOp.isOutputLCCSClasses(), is(true));
        assertThat(aggrOp.getNumMajorityClasses(), is(5));
        assertThat(aggrOp.isOutputPFTClasses(), is(true));
        assertThat(aggrOp.getNumRows(), is(2160));

        final Product sourceProduct = new Product("dummy", "t", 20, 10);
        sourceProduct.setFileLocation(new File(".", "a-b-c-d-e-f-g-h.nc"));
        aggrOp.setSourceProd(sourceProduct);
        aggrOp.setGridName(PlanetaryGridName.GEOGRAPHIC_LAT_LON);
        aggrOp.ensureTargetDir();
        FormatterConfig formatterConfig = aggrOp.createDefaultFormatterConfig();
        assertThat(formatterConfig.getOutputType(), is("Product"));
        assertThat(formatterConfig.getOutputFile(), is("." + File.separator + "a-b-c-d-aggregated-0.083333Deg-e-f-g-h.nc"));
    }

    private IsNull isNull() {
        return new IsNull();
    }

    @Test
    public void testNumRows_LessThanTwo() {
        LcAggregationOp aggrOp = createAggrOp();
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
        aggrOp.setNumRows(23);
        try {
            aggrOp.initialize();
        } catch (OperatorException oe) {
            String message = oe.getMessage().toLowerCase();
            assertThat(message, containsString("rows"));
        }
    }

    @Test
    public void testNoOutputSelected() {
        LcAggregationOp aggrOp = createAggrOp();
        aggrOp.setOutputLCCSClasses(false);
        aggrOp.setOutputPFTClasses(false);
        aggrOp.setNumMajorityClasses(0);
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
        final Integer width = 3600;
        final Integer height = 1800;
        final Product product = new Product("P", "T", width, height);
        product.setFileLocation(new File("/blah/ESACCI-LC-L4-LCCS-Map-300m-P5Y-2010-v2.nc"));
        final Band classesBand = product.addBand("lccs_class", ProductData.TYPE_UINT8);
        classesBand.setSourceImage(ConstantDescriptor.create(width.floatValue(), height.floatValue(),
                                                             new Byte[]{10}, null));
        final Band processedFlag = product.addBand("processed_flag", ProductData.TYPE_INT8);
        processedFlag.setSourceImage(ConstantDescriptor.create(width.floatValue(), height.floatValue(),
                                                               new Byte[]{1}, null));
        final Band currentPixelState = product.addBand("current_pixel_state", ProductData.TYPE_INT8);
        currentPixelState.setSourceImage(ConstantDescriptor.create(width.floatValue(), height.floatValue(),
                                                                   new Byte[]{1}, null));
        final Band observationBand = product.addBand("observation_count", ProductData.TYPE_INT8);
        observationBand.setSourceImage(ConstantDescriptor.create(width.floatValue(), height.floatValue(),
                                                                 new Float[]{10f}, null));
        final Band algoConfidBand = product.addBand("algorithmic_confidence_level", ProductData.TYPE_FLOAT32);
        algoConfidBand.setSourceImage(ConstantDescriptor.create(width.floatValue(), height.floatValue(),
                                                                new Float[]{10f}, null));
        final Band overallConfidBand = product.addBand("overall_confidence_level", ProductData.TYPE_INT8);
        overallConfidBand.setSourceImage(ConstantDescriptor.create(width.floatValue(), height.floatValue(),
                                                                   new Float[]{10f}, null));
        product.setGeoCoding(new CrsGeoCoding(DefaultGeographicCRS.WGS84, width, height, -179.95, 89.95, 0.1, 0.1));
        MetadataElement globalAttributes = new MetadataElement("Global_Attributes");
        globalAttributes.addAttribute(new MetadataAttribute("id", ProductData.createInstance("ESACCI-LC-L4-LCCS-Map-300m-P5Y-2010-v2"), true));
        product.getMetadataRoot().addElement(globalAttributes);
        return product;
    }

    private LcAggregationOp createAggrOp() {
        LcAggregationOp aggregationOp = (LcAggregationOp) aggregationSpi.createOperator();
        aggregationOp.setTargetDir(new File("."));
        return aggregationOp;
    }
}