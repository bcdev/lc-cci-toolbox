package org.esa.cci.lc.conversion;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.CrsGeoCoding;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.cci.lc.aggregation.Lccs2PftLut;
import org.esa.cci.lc.aggregation.Lccs2PftLutBuilder;
import org.esa.cci.lc.util.TestProduct;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;

import java.awt.Dimension;
import java.io.File;
import java.io.InputStreamReader;
import java.net.URL;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class RemapOpTest {

    private static final URL RESOURCE_PATH_ADDITIONAL_MAP = RemapOpTest.class.getResource("/org/esa/cci/lc/aggregation/TEST_LCCS2PFT_KG_ADDITIONAL.csv");
    private Product testSource;
    private Product testUserMap;

    @Before
    public void setUp() throws Exception {
        testSource = TestProduct.createMapSourceProduct(new Dimension(360, 180));
        testUserMap = TestProduct.createAdditionalUserMapProduct(new Dimension(36, 18));
    }

    @After
    public void tearDown() throws Exception {
        testSource.dispose();
        testUserMap.dispose();
    }

    @Test(expected = OperatorException.class)
    public void testSource_MissingBand() throws Exception {
        final RemapOp remapOp = new RemapOp();
        testSource.removeBand(testSource.getBand("lccs_class"));
        remapOp.setSourceProduct(testSource);

        remapOp.getTargetProduct();
    }

    @Test(expected = OperatorException.class)
    public void testSource_MissingGeoCoding() throws Exception {
        final RemapOp remapOp = new RemapOp();
        testSource.setGeoCoding(null);
        remapOp.setSourceProduct(testSource);

        remapOp.getTargetProduct();
    }

    @Test(expected = OperatorException.class)
    public void testSource_GeoCodingNotProvidingGeoPos() throws Exception {
        final RemapOp remapOp = new RemapOp();
        final int width = testSource.getSceneRasterWidth();
        final int height = testSource.getSceneRasterHeight();
        testSource.setGeoCoding(new NotGeoPosProvidingGeoCoding(width, height));
        remapOp.setSourceProduct(testSource);

        remapOp.getTargetProduct();
    }


    @Test(expected = OperatorException.class)
    public void testUserMap_hasOneBand() throws Exception {
        final RemapOp remapOp = new RemapOp();
        remapOp.setSourceProduct(testSource);
        testUserMap.removeBand(testUserMap.getBandAt(0));
        remapOp.setSourceProduct("additionalUserMap", testUserMap);

        remapOp.getTargetProduct();
    }

    @Test(expected = OperatorException.class)
    public void testUserMap_GeoCodingNotProvidingPixelPos() throws Exception {
        final RemapOp remapOp = new RemapOp();
        remapOp.setSourceProduct(testSource);
        final int width = testUserMap.getSceneRasterWidth();
        final int height = testUserMap.getSceneRasterHeight();
        testUserMap.setGeoCoding(new NotPixelPosProvidingGeoCoding(width, height));
        remapOp.setSourceProduct("additionalUserMap", testUserMap);

        remapOp.getTargetProduct();
    }

    @Test()
    public void testTargetProduct_DefaultSignature() throws Exception {
        final RemapOp remapOp = new RemapOp();
        remapOp.setSourceProduct(testSource);

        final Product targetProduct = remapOp.getTargetProduct();
        assertNotNull(targetProduct.getGeoCoding());
        final Band[] sourceBands = testSource.getBands();
        checkSourceBandsAreCopied(targetProduct, sourceBands);
        final Lccs2PftLut lut = new Lccs2PftLutBuilder().create();
        checkPftBandsArePresent(targetProduct, lut);
    }

    @Test()
    public void testTargetProduct_AdditionalMapSignature() throws Exception {
        final RemapOp remapOp = new RemapOp();
        remapOp.setSourceProduct(testSource);
        remapOp.setSourceProduct("additionalUserMap", testUserMap);
        final File additionalMapFile = new File(RESOURCE_PATH_ADDITIONAL_MAP.toURI());
        remapOp.setParameter("additionalUserMapPFTConversionTable", additionalMapFile);

        final Product targetProduct = remapOp.getTargetProduct();

        assertNotNull(targetProduct.getGeoCoding());
        final Band[] sourceBands = testSource.getBands();
        checkSourceBandsAreCopied(targetProduct, sourceBands);
        final MetadataElement globAttribs = targetProduct.getMetadataRoot().getElement("Global_Attributes");
        final String pftTable = globAttribs.getAttributeString("pft_table");
        assertTrue(pftTable.contains(additionalMapFile.getName()));
        final String pftTableComment = globAttribs.getAttributeString("pft_table_comment");
        assertTrue(pftTableComment.contains("Default LCCS to PFT lookup table"));
        assertTrue(pftTableComment.contains("Koeppen-Geiger Map"));

        final Band userMapBand = targetProduct.getBand("user_map");
        assertNotNull(userMapBand);
        assertEquals(userMapBand.getDataType(), testUserMap.getBandAt(0).getDataType());

        final Lccs2PftLutBuilder lutBuilder = new Lccs2PftLutBuilder();
        lutBuilder.useAdditionalUserMap(new InputStreamReader(RESOURCE_PATH_ADDITIONAL_MAP.openStream()));
        final Lccs2PftLut lut = lutBuilder.create();
        checkPftBandsArePresent(targetProduct, lut);
    }


    private void checkPftBandsArePresent(Product targetProduct, Lccs2PftLut lut) {
        final String[] pftNames = lut.getPFTNames();
        for (String pftName : pftNames) {
            final Band pftBand = targetProduct.getBand(pftName);
            assertEquals(ProductData.TYPE_INT16, pftBand.getDataType());
            assertEquals(0.01, pftBand.getScalingFactor(), 1.0e-2);
            assertTrue(pftBand.isNoDataValueSet());
            assertTrue(pftBand.isNoDataValueUsed());
            assertEquals(0, pftBand.getNoDataValue(), 0.0);
        }
    }

    private void checkSourceBandsAreCopied(Product targetProduct, Band[] sourceBands) {
        for (Band sourceBand : sourceBands) {
            final Band targetBand = targetProduct.getBand(sourceBand.getName());
            assertNotNull(targetBand);
            assertEquals(sourceBand.getDescription(), targetBand.getDescription());
            assertEquals(sourceBand.getDataType(), targetBand.getDataType());
        }
    }


    private static class NotGeoPosProvidingGeoCoding extends CrsGeoCoding {
        public NotGeoPosProvidingGeoCoding(int width, int height) throws FactoryException, TransformException {
            super(DefaultGeographicCRS.WGS84, width, height, -179.95, 89.95, 0.1, 0.1);
        }

        @Override
        public boolean canGetGeoPos() {
            return false;
        }
    }

    private static class NotPixelPosProvidingGeoCoding extends CrsGeoCoding {
        public NotPixelPosProvidingGeoCoding(int width, int height) throws FactoryException, TransformException {
            super(DefaultGeographicCRS.WGS84, width, height, -179.95, 89.95, 0.1, 0.1);
        }

        @Override
        public boolean canGetPixelPos() {
            return false;
        }
    }
}