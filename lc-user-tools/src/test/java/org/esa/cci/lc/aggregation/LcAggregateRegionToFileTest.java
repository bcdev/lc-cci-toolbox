package org.esa.cci.lc.aggregation;

import org.esa.beam.dataio.netcdf.metadata.profiles.cf.CfNetCdfReaderPlugIn;
import org.esa.beam.framework.dataio.DecodeQualification;
import org.esa.beam.framework.dataio.ProductIOPlugInManager;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.util.ProductUtils;
import org.esa.cci.lc.io.LcConditionNetCdf4WriterPlugIn;
import org.esa.cci.lc.io.LcMapNetCdf4WriterPlugIn;
import org.esa.cci.lc.subset.PredefinedRegion;
import org.esa.cci.lc.util.PlanetaryGridName;
import org.esa.cci.lc.util.TestProduct;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


/**
 * @author Marco Peters
 */
public class LcAggregateRegionToFileTest {


    private static LcMapNetCdf4WriterPlugIn lcMapNetCdf4WriterPlugIn;
    private static LcConditionNetCdf4WriterPlugIn lcCondNetCdf4WriterPlugIn;
    private static Path tempDirectory;
    private Path tempOutputPath;

    @BeforeClass
    public static void beforeClass() throws IOException {
        Locale.setDefault(Locale.ENGLISH);
        GPF.getDefaultInstance().getOperatorSpiRegistry().loadOperatorSpis();
        lcMapNetCdf4WriterPlugIn = new LcMapNetCdf4WriterPlugIn();
        ProductIOPlugInManager.getInstance().addWriterPlugIn(lcMapNetCdf4WriterPlugIn);
        lcCondNetCdf4WriterPlugIn = new LcConditionNetCdf4WriterPlugIn();
        ProductIOPlugInManager.getInstance().addWriterPlugIn(lcCondNetCdf4WriterPlugIn);
        tempDirectory = Files.createTempDirectory(LcAggregateRegionToFileTest.class.getSimpleName());
    }

    @AfterClass
    public static void afterClass() throws IOException {
        ProductIOPlugInManager.getInstance().removeWriterPlugIn(lcMapNetCdf4WriterPlugIn);
        ProductIOPlugInManager.getInstance().removeWriterPlugIn(lcCondNetCdf4WriterPlugIn);
        for (OperatorSpi operatorSpi : GPF.getDefaultInstance().getOperatorSpiRegistry().getOperatorSpis()) {
            GPF.getDefaultInstance().getOperatorSpiRegistry().removeOperatorSpi(operatorSpi);
        }
        Files.walkFileTree(tempDirectory, new PathTreeDeleter());
    }

    @Before
    public void setUp() throws Exception {
        tempOutputPath = Files.createTempFile(tempDirectory, "BEAM-TEST", ".nc");
    }

    @Test
    public void testMap_Gaussian_WithSubset_CentralAmerica() throws IOException {
        executeOpOnGridAndRegion(createMapAggrOp(), PlanetaryGridName.REGULAR_GAUSSIAN_GRID, PredefinedRegion.CENTRAL_AMERICA);
    }

    @Test
    public void testMap_Gaussian_WithSubset_Australia() throws IOException {
        executeOpOnGridAndRegion(createMapAggrOp(), PlanetaryGridName.REGULAR_GAUSSIAN_GRID, PredefinedRegion.AUSTRALIA_AND_NEW_ZEALAND);
    }

    @Test(expected = OperatorException.class)
    public void testMap_Gaussian_WithSubset_Europe() throws IOException {
        executeOpOnGridAndRegion(createMapAggrOp(), PlanetaryGridName.REGULAR_GAUSSIAN_GRID, PredefinedRegion.WESTERN_EUROPE_AND_MEDITERRANEAN);
    }

    @Test(expected = OperatorException.class)
    public void testMap_Gaussian_WithSubset_Africa() throws IOException {
        executeOpOnGridAndRegion(createMapAggrOp(), PlanetaryGridName.REGULAR_GAUSSIAN_GRID, PredefinedRegion.AFRICA);
    }

    @Test
    public void testMap_WGS84_WithSubset_CentralAmerica() throws IOException {
        executeOpOnGridAndRegion(createMapAggrOp(), PlanetaryGridName.GEOGRAPHIC_LAT_LON, PredefinedRegion.CENTRAL_AMERICA);
    }

    @Test
    public void testMap_WGS84_WithSubset_Australia() throws IOException {
        executeOpOnGridAndRegion(createMapAggrOp(), PlanetaryGridName.GEOGRAPHIC_LAT_LON, PredefinedRegion.AUSTRALIA_AND_NEW_ZEALAND);
    }

    @Test
    public void testMap_WGS84_WithSubset_Europe() throws IOException {
        executeOpOnGridAndRegion(createMapAggrOp(), PlanetaryGridName.GEOGRAPHIC_LAT_LON, PredefinedRegion.WESTERN_EUROPE_AND_MEDITERRANEAN);
    }

    @Test
    public void testMap_WGS84_WithSubset_Africa() throws IOException {
        executeOpOnGridAndRegion(createMapAggrOp(), PlanetaryGridName.GEOGRAPHIC_LAT_LON, PredefinedRegion.AFRICA);
    }

    @Test
    public void testCond_Gaussian_WithSubset_CentralAmerica() throws IOException {
        executeOpOnGridAndRegion(createCondAggrOp(), PlanetaryGridName.REGULAR_GAUSSIAN_GRID, PredefinedRegion.CENTRAL_AMERICA);
    }

    @Test
    public void testCond_Gaussian_WithSubset_Australia() throws IOException {
        executeOpOnGridAndRegion(createCondAggrOp(), PlanetaryGridName.REGULAR_GAUSSIAN_GRID, PredefinedRegion.AUSTRALIA_AND_NEW_ZEALAND);
    }

    @Test(expected = OperatorException.class)
    public void testCond_Gaussian_WithSubset_Europe() throws IOException {
        executeOpOnGridAndRegion(createCondAggrOp(), PlanetaryGridName.REGULAR_GAUSSIAN_GRID, PredefinedRegion.WESTERN_EUROPE_AND_MEDITERRANEAN);
    }

    @Test(expected = OperatorException.class)
    public void testCond_Gaussian_WithSubset_Africa() throws IOException {
        executeOpOnGridAndRegion(createCondAggrOp(), PlanetaryGridName.REGULAR_GAUSSIAN_GRID, PredefinedRegion.AFRICA);
    }

    @Test
    public void testCond_WGS84_WithSubset_CentralAmerica() throws IOException {
        executeOpOnGridAndRegion(createCondAggrOp(), PlanetaryGridName.GEOGRAPHIC_LAT_LON, PredefinedRegion.CENTRAL_AMERICA);
    }

    @Test
    public void testCond_WGS84_WithSubset_Australia() throws IOException {
        executeOpOnGridAndRegion(createCondAggrOp(), PlanetaryGridName.GEOGRAPHIC_LAT_LON, PredefinedRegion.AUSTRALIA_AND_NEW_ZEALAND);
    }

    @Test
    public void testCond_WGS84_WithSubset_Europe() throws IOException {
        executeOpOnGridAndRegion(createCondAggrOp(), PlanetaryGridName.GEOGRAPHIC_LAT_LON, PredefinedRegion.WESTERN_EUROPE_AND_MEDITERRANEAN);
    }

    @Test
    public void testCond_WGS84_WithSubset_Africa() throws IOException {
        executeOpOnGridAndRegion(createCondAggrOp(), PlanetaryGridName.GEOGRAPHIC_LAT_LON, PredefinedRegion.AFRICA);
    }

    private void executeOpOnGridAndRegion(AbstractLcAggregationOp aggrOp, PlanetaryGridName grid, PredefinedRegion region) throws IOException {
        aggrOp.setGridName(grid);
        aggrOp.setNumRows(80);
        aggrOp.setPredefinedRegion(region);

        aggrOp.initialize();
        Product product = null;
        try {
            File file = tempOutputPath.toFile();
            product = readProduct(file);
            assertNotNull(product);
//            debugOutput(grid, region, product);
        } finally {
            aggrOp.dispose();
            if (product != null) {
                product.dispose();
            }
        }
    }

    private void debugOutput(PlanetaryGridName grid, PredefinedRegion region, Product product) {
        GeoPos[] geoBoundary = ProductUtils.createGeoBoundary(product, 500);
        System.out.println("region = " + region.name());
        System.out.println("grid = " + grid.name());
        System.out.print("\t");
        for (GeoPos geoPos : geoBoundary) {
            System.out.printf("{%.3f, %.3f}", geoPos.getLat(), geoPos.getLon());
        }
        System.out.println();
    }

    private Product readProduct(File file) throws IOException {
        CfNetCdfReaderPlugIn plugIn = new CfNetCdfReaderPlugIn();
        DecodeQualification decodeQualification = plugIn.getDecodeQualification(file);
        assertEquals(DecodeQualification.SUITABLE, decodeQualification);
        return plugIn.createReaderInstance().readProductNodes(file, null);
    }

    private LcCondAggregationOp createCondAggrOp() throws IOException {
        LcCondAggregationOp.Spi operatorSpi = new LcCondAggregationOp.Spi();
        LcCondAggregationOp aggregationOp = (LcCondAggregationOp) operatorSpi.createOperator();
        aggregationOp.setTargetDir(tempDirectory.toFile());
        aggregationOp.setOutputFile(tempOutputPath.toAbsolutePath().toString());
        aggregationOp.setOutputFormat(null);
        aggregationOp.outputTargetProduct = false;
        aggregationOp.setSourceProduct(TestProduct.createConditionSourceProduct(new Dimension(3600, 1800)));
        return aggregationOp;
    }

    private LcMapAggregationOp createMapAggrOp() throws IOException {
        LcMapAggregationOp.Spi operatorSpi = new LcMapAggregationOp.Spi();
        LcMapAggregationOp aggregationOp = (LcMapAggregationOp) operatorSpi.createOperator();
        aggregationOp.setTargetDir(tempDirectory.toFile());
        aggregationOp.setOutputFile(tempOutputPath.toAbsolutePath().toString());
        aggregationOp.setOutputFormat(null);
        aggregationOp.outputTargetProduct = false;
        aggregationOp.setSourceProduct(TestProduct.createMapSourceProduct(new Dimension(3600, 1800)));
        return aggregationOp;
    }

    private static class PathTreeDeleter extends SimpleFileVisitor<Path> {

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
            Files.delete(dir);
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            Files.delete(file);
            return FileVisitResult.CONTINUE;
        }
    }

}