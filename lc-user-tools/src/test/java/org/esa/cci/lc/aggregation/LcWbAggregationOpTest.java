package org.esa.cci.lc.aggregation;

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
import org.esa.cci.lc.io.LcWbNetCdf4WriterPlugIn;
import org.esa.cci.lc.subset.PredefinedRegion;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.hamcrest.core.IsNull;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.media.jai.operator.ConstantDescriptor;
import java.io.File;
import java.io.IOException;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;


public class LcWbAggregationOpTest {

    private static LcWbAggregationOp.Spi aggregationSpi;
    private static LcWbNetCdf4WriterPlugIn lcWbNetCdf4WriterPlugIn;

    @BeforeClass
    public static void beforeClass() {
        aggregationSpi = new LcWbAggregationOp.Spi();
        OperatorSpiRegistry spiRegistry = GPF.getDefaultInstance().getOperatorSpiRegistry();
        spiRegistry.loadOperatorSpis();
        spiRegistry.addOperatorSpi(aggregationSpi);
        lcWbNetCdf4WriterPlugIn = new LcWbNetCdf4WriterPlugIn();
        ProductIOPlugInManager.getInstance().addWriterPlugIn(lcWbNetCdf4WriterPlugIn);
    }

    @AfterClass
    public static void afterClass() {
        OperatorSpiRegistry spiRegistry = GPF.getDefaultInstance().getOperatorSpiRegistry();
        spiRegistry.removeOperatorSpi(aggregationSpi);
        ProductIOPlugInManager.getInstance().removeWriterPlugIn(lcWbNetCdf4WriterPlugIn);
    }

    @Test
    public void testTargetProductCreation_Binning() throws Exception {
        // preparation
        LcWbAggregationOp aggregationOp = createAggrOp();
        aggregationOp.setGridName(PlanetaryGridName.GEOGRAPHIC_LAT_LON);
        aggregationOp.setSourceProduct(createSourceProduct());
        boolean outputWbClasses = true;
        aggregationOp.setOutputWbClasses(outputWbClasses);
        int numMajorityClasses = 2;
        aggregationOp.setNumMajorityClasses(numMajorityClasses);
        aggregationOp.setNumRows(4);
        initOp(aggregationOp);

        // execution
        Product targetProduct = aggregationOp.getTargetProduct();

        // verification
        int numObsAndPasses = 2;
        final int expectedNumBands = (outputWbClasses ? 3 : 0)
                                     + numMajorityClasses
                                     + numObsAndPasses;
        assertThat(targetProduct.getNumBands(), is(expectedNumBands));
    }

    @Test(expected = OperatorException.class)
    public void testRegionWithGaussianGrid() throws Exception {
        LcWbAggregationOp aggrOp = createAggrOp();
        aggrOp.setSourceProduct(createSourceProduct());
        aggrOp.setGridName(PlanetaryGridName.REGULAR_GAUSSIAN_GRID);
        aggrOp.setNumRows(80);
        aggrOp.setPredefinedRegion(PredefinedRegion.WESTERN_EUROPE_AND_MEDITERRANEAN);
        aggrOp.initialize();
    }

    @Test
    public void testRegionWithGeoGrid() throws Exception {
        LcWbAggregationOp aggrOp = createAggrOp();
        aggrOp.setSourceProduct(createSourceProduct());
        aggrOp.setGridName(PlanetaryGridName.GEOGRAPHIC_LAT_LON);
        boolean outputWbClasses = true;
        aggrOp.setOutputWbClasses(outputWbClasses);
        int numMajorityClasses = 2;
        aggrOp.setNumMajorityClasses(numMajorityClasses);
        aggrOp.setNumRows(180);
        aggrOp.setPredefinedRegion(PredefinedRegion.ASIA);
        initOp(aggrOp);
    }

    @Test
    public void testDefaultValues() {
        LcWbAggregationOp aggrOp = (LcWbAggregationOp) aggregationSpi.createOperator();
        assertThat(aggrOp.getGridName(), isNull());
        assertThat(aggrOp.getNumMajorityClasses(), is(1));
        assertThat(aggrOp.getNumRows(), is(2160));
    }

    private IsNull isNull() {
        return new IsNull();
    }

    @Test
    public void testNumRows_LessThanTwo() {
        LcWbAggregationOp aggrOp = createAggrOp();
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
        LcWbAggregationOp aggrOp = createAggrOp();
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
        LcWbAggregationOp aggregationOp = createAggrOp();
        aggregationOp.setOutputWbClasses(false);
        aggregationOp.setNumMajorityClasses(0);
        try {
            aggregationOp.initialize();
        } catch (OperatorException oe) {
            String message = oe.getMessage();
            assertThat(message, containsString("WB"));
            assertThat(message, containsString("majority"));
        }
    }


    @Test
    public void testRegionEnvelope_WithPredefinedRegion() throws Exception {
        LcWbAggregationOp aggrOp = createAggrOp();
        aggrOp.setPredefinedRegion(PredefinedRegion.GREENLAND);

        final ReferencedEnvelope region = aggrOp.getRegionEnvelope();
        assertEquals(PredefinedRegion.GREENLAND.getEast(), region.getMaximum(0), 1.0e-6);
        assertEquals(PredefinedRegion.GREENLAND.getNorth(), region.getMaximum(1), 1.0e-6);
        assertEquals(PredefinedRegion.GREENLAND.getWest(), region.getMinimum(0), 1.0e-6);
        assertEquals(PredefinedRegion.GREENLAND.getSouth(), region.getMinimum(1), 1.0e-6);
    }

    @Test
    public void testRegionEnvelope_WithUserdefinedRegion() throws Exception {
        LcWbAggregationOp aggrOp = createAggrOp();
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

    private void initOp(LcWbAggregationOp aggregationOp) throws IOException {
        aggregationOp.setOutputFormat("NetCDF4-BEAM");
        File tempFile = File.createTempFile("BEAM-TEST_", ".nc");
        tempFile.deleteOnExit();
        aggregationOp.setOutputFile(tempFile.getAbsolutePath());
        aggregationOp.setOutputType("Product");
        aggregationOp.outputTargetProduct = true;
    }

    private Product createSourceProduct() throws Exception {
        final Integer width = 3600;
        final Integer height = 1800;
        final Product product = new Product("P", "T", width, height);
        product.setFileLocation(new File("/blah/ESACCI-LC-L4-WB-Map-300m-P5Y-2010-v2.nc"));
        final Band classesBand = product.addBand("wb_class", ProductData.TYPE_UINT8);
        classesBand.setSourceImage(ConstantDescriptor.create(width.floatValue(), height.floatValue(),
                                                             new Float[]{2f}, null));
        final Band wsObservationBand = product.addBand("ws_observation_count", ProductData.TYPE_INT16);
        wsObservationBand.setSourceImage(ConstantDescriptor.create(width.floatValue(), height.floatValue(),
                                                                 new Float[]{10f}, null));
        final Band gmObservationBand = product.addBand("gm_observation_count", ProductData.TYPE_INT16);
        gmObservationBand.setSourceImage(ConstantDescriptor.create(width.floatValue(), height.floatValue(),
                                                                 new Float[]{10f}, null));
        product.setGeoCoding(new CrsGeoCoding(DefaultGeographicCRS.WGS84, width, height, -179.95, 89.95, 0.1, 0.1));
        MetadataElement globalAttributes = new MetadataElement("Global_Attributes");
        globalAttributes.addAttribute(new MetadataAttribute("id", ProductData.createInstance("ESACCI-LC-L4-WB-Map-300m-P5Y-2010-v2"), true));
        globalAttributes.addAttribute(new MetadataAttribute("time_coverage_duration", ProductData.createInstance("P5Y"), true));
        globalAttributes.addAttribute(new MetadataAttribute("time_coverage_resolution", ProductData.createInstance("P7D"), true));
        globalAttributes.addAttribute(new MetadataAttribute("time_coverage_start", ProductData.createInstance("2000"), true));
        globalAttributes.addAttribute(new MetadataAttribute("time_coverage_end", ProductData.createInstance("2012"), true));
        globalAttributes.addAttribute(new MetadataAttribute("product_version", ProductData.createInstance("1.0"), true));
        globalAttributes.addAttribute(new MetadataAttribute("spatial_resolution", ProductData.createInstance("300m"), true));
        globalAttributes.addAttribute(new MetadataAttribute("geospatial_lat_min", ProductData.createInstance("-90"), true));
        globalAttributes.addAttribute(new MetadataAttribute("geospatial_lat_max", ProductData.createInstance("90"), true));
        globalAttributes.addAttribute(new MetadataAttribute("geospatial_lon_min", ProductData.createInstance("-180"), true));
        globalAttributes.addAttribute(new MetadataAttribute("geospatial_lon_max", ProductData.createInstance("180"), true));
        globalAttributes.addAttribute(new MetadataAttribute("source", ProductData.createInstance("ASAR"), true));
        globalAttributes.addAttribute(new MetadataAttribute("history", ProductData.createInstance("LC tool tests"), true));
        product.getMetadataRoot().addElement(globalAttributes);
        return product;
    }

    private LcWbAggregationOp createAggrOp() {
        LcWbAggregationOp aggregationOp = (LcWbAggregationOp) aggregationSpi.createOperator();
        aggregationOp.setTargetDir(new File("."));
        return aggregationOp;
    }
}