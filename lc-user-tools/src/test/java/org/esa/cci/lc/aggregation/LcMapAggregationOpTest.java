package org.esa.cci.lc.aggregation;

import org.esa.beam.framework.dataio.ProductIOPlugInManager;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpiRegistry;
import org.esa.cci.lc.io.LcMapNetCdf4WriterPlugIn;
import org.esa.cci.lc.subset.PredefinedRegion;
import org.esa.cci.lc.util.PlanetaryGridName;
import org.esa.cci.lc.util.TestProduct;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.hamcrest.core.IsNull;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.awt.Dimension;
import java.io.File;
import java.io.IOException;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;


public class LcMapAggregationOpTest {

    private static LcMapAggregationOp.Spi aggregationSpi;
    private static LcMapNetCdf4WriterPlugIn lcMapNetCdf4WriterPlugIn;

    @BeforeClass
    public static void beforeClass() {
        aggregationSpi = new LcMapAggregationOp.Spi();
        OperatorSpiRegistry spiRegistry = GPF.getDefaultInstance().getOperatorSpiRegistry();
        spiRegistry.loadOperatorSpis();
        spiRegistry.addOperatorSpi(aggregationSpi);
        lcMapNetCdf4WriterPlugIn = new LcMapNetCdf4WriterPlugIn();
        ProductIOPlugInManager.getInstance().addWriterPlugIn(lcMapNetCdf4WriterPlugIn);
    }

    @AfterClass
    public static void afterClass() {
        OperatorSpiRegistry spiRegistry = GPF.getDefaultInstance().getOperatorSpiRegistry();
        spiRegistry.removeOperatorSpi(aggregationSpi);
        ProductIOPlugInManager.getInstance().removeWriterPlugIn(lcMapNetCdf4WriterPlugIn);
    }

    @Test
    public void testTargetProductCreation_Binning() throws Exception {
        // preparation
        LcMapAggregationOp aggregationOp = createAggrOp();
        aggregationOp.setGridName(PlanetaryGridName.GEOGRAPHIC_LAT_LON);
        aggregationOp.setSourceProduct(TestProduct.createMapSourceProduct(new Dimension(3600, 1800)));
        int numMajorityClasses = 2;
        aggregationOp.setNumMajorityClasses(numMajorityClasses);
        aggregationOp.setNumRows(4);
        initOp(aggregationOp);

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

    @Test
    public void testRegionWithGaussianGrid() throws Exception {
        LcMapAggregationOp aggrOp = createAggrOp();
        aggrOp.setSourceProduct(TestProduct.createMapSourceProduct(new Dimension(3600, 1800)));
        aggrOp.setGridName(PlanetaryGridName.REGULAR_GAUSSIAN_GRID);
        aggrOp.setNumRows(80);
        aggrOp.setPredefinedRegion(PredefinedRegion.GREENLAND);
        initOp(aggrOp);
    }

    @Test
    public void testTargetProductCreation_WithOnlyPFTClasses() throws Exception {
        // preparation
        LcMapAggregationOp aggregationOp = createAggrOp();
        aggregationOp.setGridName(PlanetaryGridName.GEOGRAPHIC_LAT_LON);
        aggregationOp.setSourceProduct(TestProduct.createMapSourceProduct(new Dimension(3600, 1800)));
        aggregationOp.setOutputLCCSClasses(false);
        int numMajorityClasses = 0;
        aggregationOp.setNumMajorityClasses(numMajorityClasses);
        aggregationOp.setNumRows(4);
        initOp(aggregationOp);

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
        LcMapAggregationOp aggrOp = (LcMapAggregationOp) aggregationSpi.createOperator();
        assertThat(aggrOp.getGridName(), new IsNull());
        assertThat(aggrOp.isOutputLCCSClasses(), is(true));
        assertThat(aggrOp.getNumMajorityClasses(), is(5));
        assertThat(aggrOp.isOutputPFTClasses(), is(true));
        assertThat(aggrOp.getNumRows(), is(2160));
        assertThat(aggrOp.isOutputUserMapClasses(), is(false));
    }

    @Test
    public void testNumRows_LessThanTwo() throws IOException {
        LcMapAggregationOp aggrOp = createAggrOp();
        aggrOp.setNumRows(1);
        try {
            initOp(aggrOp);
        } catch (OperatorException oe) {
            String message = oe.getMessage().toLowerCase();
            assertThat(message, containsString("rows"));
        }
    }

    @Test
    public void testNumRows_OddValue() throws IOException {
        LcMapAggregationOp aggrOp = createAggrOp();
        aggrOp.setNumRows(23);
        try {
            initOp(aggrOp);
        } catch (OperatorException oe) {
            String message = oe.getMessage().toLowerCase();
            assertThat(message, containsString("rows"));
        }
    }

    @Test
    public void testNoOutputSelected() throws IOException {
        LcMapAggregationOp aggrOp = createAggrOp();
        aggrOp.setOutputLCCSClasses(false);
        aggrOp.setOutputPFTClasses(false);
        aggrOp.setNumMajorityClasses(0);
        try {
            initOp(aggrOp);
        } catch (OperatorException oe) {
            String message = oe.getMessage();
            assertThat(message, containsString("LCCS"));
            assertThat(message, containsString("majority"));
            assertThat(message, containsString("PFT"));
        }
    }


    @Test
    public void testRegionEnvelope_WithPredefinedRegion() throws Exception {
        LcMapAggregationOp aggrOp = createAggrOp();
        aggrOp.setPredefinedRegion(PredefinedRegion.GREENLAND);

        final ReferencedEnvelope region = aggrOp.getRegionEnvelope();
        assertEquals(PredefinedRegion.GREENLAND.getEast(), region.getMaximum(0), 1.0e-6);
        assertEquals(PredefinedRegion.GREENLAND.getNorth(), region.getMaximum(1), 1.0e-6);
        assertEquals(PredefinedRegion.GREENLAND.getWest(), region.getMinimum(0), 1.0e-6);
        assertEquals(PredefinedRegion.GREENLAND.getSouth(), region.getMinimum(1), 1.0e-6);
    }

    @Test
    public void testRegionEnvelope_WithUserdefinedRegion() throws Exception {
        LcMapAggregationOp aggrOp = createAggrOp();
        aggrOp.setNorth(88.8f);
        aggrOp.setEast(10.4f);
        aggrOp.setSouth(54.4f);
        aggrOp.setWest(-36.63f);

        final ReferencedEnvelope region = aggrOp.getRegionEnvelope();
        assertEquals(-36.63f, region.getMinimum(0), 1.0e-6);
        assertEquals(88.8f, region.getMaximum(1), 1.0e-6);
        assertEquals(10.4f, region.getMaximum(0), 1.0e-6);
        assertEquals(54.4f, region.getMinimum(1), 1.0e-6);
    }

    private void initOp(LcMapAggregationOp aggregationOp) throws IOException {
        aggregationOp.setOutputFormat("NetCDF4-BEAM");
        File tempFile = File.createTempFile("BEAM-TEST_", ".nc");
        tempFile.deleteOnExit();
        aggregationOp.setOutputFile(tempFile.getAbsolutePath());
        aggregationOp.setOutputType("Product");
        aggregationOp.outputTargetProduct = true;
    }

    private LcMapAggregationOp createAggrOp() {
        LcMapAggregationOp aggregationOp = (LcMapAggregationOp) aggregationSpi.createOperator();
        aggregationOp.setTargetDir(new File("."));
        return aggregationOp;
    }
}