package org.esa.cci.lc.aggregation;

import org.esa.snap.core.dataio.DecodeQualification;
import org.esa.snap.core.dataio.ProductIOPlugInManager;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpiRegistry;
import org.esa.cci.lc.io.LCCfNetCdfReaderPlugIn;
import org.esa.cci.lc.io.LcMapNetCdf4WriterPlugIn;
import org.esa.cci.lc.subset.LcSubsetOp;
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


public class SubsetAfterAggregationTest {

    private static LcMapAggregationOp.Spi aggregationSpi;
    private static LcMapNetCdf4WriterPlugIn lcMapNetCdf4WriterPlugIn;
    private static LCCfNetCdfReaderPlugIn lcCfNetCdfReaderPlugIn;
    private static Path tempDirectory;
    private Path tempOutputPath;

    @BeforeClass
    public static void beforeClass() throws IOException {
        aggregationSpi = new LcMapAggregationOp.Spi();
        OperatorSpiRegistry spiRegistry = GPF.getDefaultInstance().getOperatorSpiRegistry();
        spiRegistry.loadOperatorSpis();
        spiRegistry.addOperatorSpi(aggregationSpi);
        lcMapNetCdf4WriterPlugIn = new LcMapNetCdf4WriterPlugIn();
        lcCfNetCdfReaderPlugIn = new LCCfNetCdfReaderPlugIn();
        ProductIOPlugInManager.getInstance().addWriterPlugIn(lcMapNetCdf4WriterPlugIn);
        ProductIOPlugInManager.getInstance().addReaderPlugIn(lcCfNetCdfReaderPlugIn);
        tempDirectory = Files.createTempDirectory(SubsetAfterAggregationTest.class.getSimpleName());
    }

    @AfterClass
    public static void afterClass() throws IOException {
        OperatorSpiRegistry spiRegistry = GPF.getDefaultInstance().getOperatorSpiRegistry();
        spiRegistry.removeOperatorSpi(aggregationSpi);
        ProductIOPlugInManager.getInstance().removeWriterPlugIn(lcMapNetCdf4WriterPlugIn);
        ProductIOPlugInManager.getInstance().removeReaderPlugIn(lcCfNetCdfReaderPlugIn);
        Files.walkFileTree(tempDirectory, new PathTreeDeleter());
    }

    @Before
    public void setUp() throws Exception {
        tempOutputPath = Files.createTempFile(tempDirectory, "BEAM-TEST", ".nc");
    }

    @Test
    public void testCentralAmerica() throws IOException {
        Product product = executeOpOnGrid(createMapAggrOp(), PlanetaryGridName.REGULAR_GAUSSIAN_GRID,
                                          new SubsetConfigurer() {
                                              @Override
                                              public void configure(LcSubsetOp subsetOp) {
                                                  subsetOp.setPredefinedRegion(PredefinedRegion.CENTRAL_AMERICA);
                                              }
                                          });
        try {
            GeoPos ulGp = product.getSceneGeoCoding().getGeoPos(new PixelPos(0.5f, 0.5f), null);
            GeoPos lrGp = product.getSceneGeoCoding().getGeoPos(new PixelPos(product.getSceneRasterWidth() - 0.5f,
                                                                        product.getSceneRasterHeight() - 0.5f), null);
            assertEquals(27.471285f, ulGp.getLat(), 1.0e-6f);
            assertEquals(266.625f, ulGp.getLon(), 1.0e-6f);
            assertEquals(7.2883f, lrGp.getLat(), 1.0e-6f);
            assertEquals(301.5f, lrGp.getLon(), 1.0e-6f);
        } finally {
            product.dispose();
        }
    }

    @Test(expected = OperatorException.class)
    public void testEurope() throws IOException {
        executeOpOnGrid(createMapAggrOp(), PlanetaryGridName.REGULAR_GAUSSIAN_GRID,
                        new SubsetConfigurer() {
                            @Override
                            public void configure(LcSubsetOp subsetOp) {
                                subsetOp.setPredefinedRegion(PredefinedRegion.WESTERN_EUROPE_AND_MEDITERRANEAN);
                            }
                        });
    }

    @Test
    public void testUserDefinedEurope() throws IOException {
        Product eastProduct = null;
        Product westProduct = null;
        try {
            // Eastern part of Europe
            eastProduct = executeOpOnGrid(createMapAggrOp(), PlanetaryGridName.REGULAR_GAUSSIAN_GRID,
                                          new SubsetConfigurer() {
                                              @Override
                                              public void configure(LcSubsetOp subsetOp) {
                                                  subsetOp.setEast(53);
                                                  subsetOp.setNorth(83);
                                                  subsetOp.setWest(0);
                                                  subsetOp.setSouth(25);
                                              }
                                          });
            // Western part of Europe
            westProduct = executeOpOnGrid(createMapAggrOp(), PlanetaryGridName.REGULAR_GAUSSIAN_GRID,
                                          new SubsetConfigurer() {
                                              @Override
                                              public void configure(LcSubsetOp subsetOp) {
                                                  subsetOp.setEast(360);
                                                  subsetOp.setNorth(83);
                                                  subsetOp.setWest(334);
                                                  subsetOp.setSouth(25);
                                              }
                                          });
        } finally {
            if (eastProduct != null) {
                eastProduct.dispose();
            }
            if (westProduct != null) {
                westProduct.dispose();
            }
        }
    }

    private Product executeOpOnGrid(AbstractLcAggregationOp aggrOp, PlanetaryGridName grid, SubsetConfigurer configurer) throws IOException {
        Product tempProduct = null;
        try {
            aggrOp.setGridName(grid);
            aggrOp.setNumRows(80);
            aggrOp.initialize();
            LcSubsetOp subsetOp = (LcSubsetOp) new LcSubsetOp.Spi().createOperator();
            subsetOp.setTargetDir(tempDirectory.toFile());
            File targetFile = Files.createTempFile(tempDirectory, "BEAM-TEST_subset", ".nc").toFile();
            subsetOp.setTargetFile(targetFile);
            configurer.configure(subsetOp);
            tempProduct = readProduct(tempOutputPath.toFile());
            subsetOp.setSourceProduct(tempProduct);
            subsetOp.initialize();
            Product subsetProduct = readProduct(targetFile);
            assertNotNull(subsetProduct.getSceneGeoCoding());
            return subsetProduct;
        } finally {
            aggrOp.dispose();
            if (tempProduct != null) {
                tempProduct.dispose();
            }
        }
    }

    private Product readProduct(File file) throws IOException {
        LCCfNetCdfReaderPlugIn plugIn = new LCCfNetCdfReaderPlugIn();
        DecodeQualification decodeQualification = plugIn.getDecodeQualification(file);
        assertEquals(DecodeQualification.INTENDED, decodeQualification);
        return plugIn.createReaderInstance().readProductNodes(file, null);
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

    private static interface SubsetConfigurer {
        void configure(LcSubsetOp subsetOp);
    }
}