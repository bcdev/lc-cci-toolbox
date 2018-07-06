package org.esa.cci.lc.aggregation;

import org.esa.snap.dataio.netcdf.metadata.profiles.cf.CfNetCdfReaderPlugIn;
import org.esa.snap.core.dataio.DecodeQualification;
import org.esa.snap.core.dataio.ProductIOPlugInManager;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.cci.lc.io.LCCfNetCdfReaderPlugIn;
import org.esa.cci.lc.io.LcConditionNetCdf4WriterPlugIn;
import org.esa.cci.lc.io.LcMapNetCdf4WriterPlugIn;
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
public class LcAggregateGlobalToFileTest {


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
        tempDirectory = Files.createTempDirectory(LcAggregateGlobalToFileTest.class.getSimpleName());
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
    public void testMap_Gaussian() throws IOException {
        executeOpOnGrid(createMapAggrOp(), PlanetaryGridName.REGULAR_GAUSSIAN_GRID);
    }

    @Test
    public void testMap_WGS84() throws IOException {
        executeOpOnGrid(createMapAggrOp(), PlanetaryGridName.GEOGRAPHIC_LAT_LON);
    }

    @Test
    public void testCond_Gaussian() throws IOException {
        executeOpOnGrid(createCondAggrOp(), PlanetaryGridName.REGULAR_GAUSSIAN_GRID);
    }

    @Test
    public void testCond_WGS84() throws IOException {
        executeOpOnGrid(createCondAggrOp(), PlanetaryGridName.GEOGRAPHIC_LAT_LON);
    }

    private void executeOpOnGrid(AbstractLcAggregationOp aggrOp, PlanetaryGridName grid) throws IOException {
        aggrOp.setGridName(grid);
        aggrOp.setNumRows(80);

        aggrOp.initialize();
        checkProductWithStandardReader(tempOutputPath.toFile(), grid);
        checkProductWithLcReader(tempOutputPath.toFile(), grid);
    }

    private void checkProductWithStandardReader(File file, PlanetaryGridName grid) throws IOException {

        CfNetCdfReaderPlugIn plugIn = new CfNetCdfReaderPlugIn();
        DecodeQualification decodeQualification = plugIn.getDecodeQualification(file);
        assertEquals(DecodeQualification.SUITABLE, decodeQualification);
        Product product = plugIn.createReaderInstance().readProductNodes(file, null);
        try {
            assertNotNull(product);
            GeoCoding geoCoding = product.getSceneGeoCoding();
            GeoPos ulgp = geoCoding.getGeoPos(new PixelPos(0.5f, 0.5f), null);
            GeoPos urgp = geoCoding.getGeoPos(new PixelPos(product.getSceneRasterWidth() - 0.5f, 0.5f), null);
            if (PlanetaryGridName.REGULAR_GAUSSIAN_GRID.equals(grid)) {
                assertEquals(-180.0f, ulgp.lon, 1.0e-6f);
                assertEquals(178.875f, urgp.lon, 1.0e-6f);
            } else {
                assertEquals(-178.875, ulgp.lon, 1.0e-6f);
                assertEquals(178.875f, urgp.lon, 1.0e-6f);
            }
        } finally {
            if (product != null) {
                product.dispose();
            }
        }
    }

    private void checkProductWithLcReader(File file, PlanetaryGridName grid) throws IOException {
        CfNetCdfReaderPlugIn plugIn = new LCCfNetCdfReaderPlugIn();
        DecodeQualification decodeQualification = plugIn.getDecodeQualification(file);
        assertEquals(DecodeQualification.INTENDED, decodeQualification);
        Product product = plugIn.createReaderInstance().readProductNodes(file, null);
        try {
            assertNotNull(product);
            GeoCoding geoCoding = product.getSceneGeoCoding();
            GeoPos ulgp = geoCoding.getGeoPos(new PixelPos(0.5f, 0.5f), null);
            GeoPos urgp = geoCoding.getGeoPos(new PixelPos(product.getSceneRasterWidth() - 0.5f, 0.5f), null);
            if (PlanetaryGridName.REGULAR_GAUSSIAN_GRID.equals(grid)) {
                // the actual NetCDF data is OK [0,360], but the NetCDF reader of BEAM shifts lon data to [-180,180]
                assertEquals(0f, ulgp.lon, 1.0e-6f);
                assertEquals(358.875f, urgp.lon, 1.0e-6f);
            } else {
                assertEquals(-178.875f, ulgp.lon, 1.0e-6f);
                assertEquals(178.875f, urgp.lon, 1.0e-6f);
            }
        } finally {
            if (product != null) {
                product.dispose();
            }
        }
    }

    private LcCondAggregationOp createCondAggrOp() throws IOException {
        LcCondAggregationOp.Spi operatorSpi = new LcCondAggregationOp.Spi();
        LcCondAggregationOp aggregationOp = (LcCondAggregationOp) operatorSpi.createOperator();
        aggregationOp.setTargetDir(tempDirectory.toFile());
        aggregationOp.setOutputFile(tempOutputPath.toAbsolutePath().toString());
        aggregationOp.setOutputFormat(null);
        aggregationOp.outputTargetProduct = false;
        aggregationOp.setSourceProduct(TestProduct.createConditionSourceProduct(new Dimension(360, 180)));
        return aggregationOp;
    }

    private LcMapAggregationOp createMapAggrOp() throws IOException {
        LcMapAggregationOp.Spi operatorSpi = new LcMapAggregationOp.Spi();
        LcMapAggregationOp aggregationOp = (LcMapAggregationOp) operatorSpi.createOperator();
        aggregationOp.setTargetDir(tempDirectory.toFile());
        aggregationOp.setOutputFile(tempOutputPath.toAbsolutePath().toString());
        aggregationOp.setOutputFormat(null);
        aggregationOp.outputTargetProduct = false;
        aggregationOp.setSourceProduct(TestProduct.createMapSourceProductNetCdf(new Dimension(360, 180)));
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