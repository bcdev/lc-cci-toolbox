package org.esa.cci.lc.subset;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.CrsGeoCoding;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpiRegistry;
import org.esa.beam.gpf.operators.standard.SubsetOp;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.media.jai.operator.ConstantDescriptor;
import java.io.File;

import static org.hamcrest.core.Is.*;
import static org.hamcrest.core.IsEqual.*;
import static org.junit.Assert.*;
import static org.junit.matchers.JUnitMatchers.*;

public class LcSubsetOpTest {

    private static SubsetOp.Spi subsetOpSpi;

    @BeforeClass
    public static void beforeClass() throws Exception {
        OperatorSpiRegistry spiRegistry = GPF.getDefaultInstance().getOperatorSpiRegistry();
        subsetOpSpi = new SubsetOp.Spi();
        spiRegistry.addOperatorSpi(subsetOpSpi);
    }

    @AfterClass
    public static void afterClass() throws Exception {
        OperatorSpiRegistry spiRegistry = GPF.getDefaultInstance().getOperatorSpiRegistry();
        spiRegistry.removeOperatorSpi(subsetOpSpi);
    }

    @Test
    public void testTargetProductCreation_userdefinedRegion() throws Exception {
        // preparation
        LcSubsetOp lcSubsetOp = createLcSubsetOp();
        lcSubsetOp.writeProduct = false;
        lcSubsetOp.setTargetDir(new File("."));
        lcSubsetOp.setEast(14.95f);
        lcSubsetOp.setWest(-14.95f);
        lcSubsetOp.setSouth(-14.95f);
        lcSubsetOp.setNorth(14.95f);
        final Product sourceProduct = createSourceProduct();
        final int sw = sourceProduct.getSceneRasterWidth();
        final int sh = sourceProduct.getSceneRasterHeight();
        final GeoCoding sourceGC = sourceProduct.getGeoCoding();
        assertThat(sw, is(3600));
        assertThat(sh, is(1800));
        assertThat(sourceGC.getGeoPos(new PixelPos(0, 0), null), equalTo(new GeoPos(90, -180)));
        assertThat(sourceGC.getGeoPos(new PixelPos(sw, sh), null), equalTo(new GeoPos(-90, 180)));
        lcSubsetOp.setSourceProduct(sourceProduct);

        // execution
        lcSubsetOp.getTargetProduct();

        //verification
        final Product subsetProduct = lcSubsetOp.subsetProduct;
        final int th = subsetProduct.getSceneRasterHeight();
        final int tw = subsetProduct.getSceneRasterWidth();
        final GeoCoding targetGC = subsetProduct.getGeoCoding();
        assertThat(th, is(300));
        assertThat(tw, is(300));
        assertThat(targetGC.getGeoPos(new PixelPos(0, 0), null), equalTo(new GeoPos(15.0f, -15.0f)));
        assertThat(targetGC.getGeoPos(new PixelPos(tw, th), null), equalTo(new GeoPos(-15.0f, 15.0f)));
    }

    @Test
    public void testTargetProductCreation_PredefinedRegion() throws Exception {
        // preparation
        LcSubsetOp lcSubsetOp = createLcSubsetOp();
        lcSubsetOp.writeProduct = false;
        lcSubsetOp.setTargetDir(new File("."));
        lcSubsetOp.setPredefinedRegion(PredefinedRegion.ASIA);
        final Product sourceProduct = createSourceProduct();
        lcSubsetOp.setSourceProduct(sourceProduct);

        // execution
        lcSubsetOp.getTargetProduct();

        //verification
        final Product subsetProduct = lcSubsetOp.subsetProduct;
        final int th = subsetProduct.getSceneRasterHeight();
        final int tw = subsetProduct.getSceneRasterWidth();
        assertThat(th, is(831));
        assertThat(tw, is(1271));
//        final GeoCoding targetGC = subsetProduct.getGeoCoding();
//        assertThat(targetGC.getGeoPos(new PixelPos(0, 0), null), equalTo(new GeoPos(15.0f, -15.0f)));
//        assertThat(targetGC.getGeoPos(new PixelPos(tw, th), null), equalTo(new GeoPos(-15.0f, 15.0f)));
    }

    @Test
    public void testWestEastBound() {
        LcSubsetOp aggrOp = createLcSubsetOp();
        setDefaultBounds(aggrOp);
        aggrOp.setWest(10);
        aggrOp.setEast(3);
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
        LcSubsetOp aggrOp = createLcSubsetOp();
        setDefaultBounds(aggrOp);
        aggrOp.setNorth(30);
        aggrOp.setSouth(70);
        try {
            aggrOp.initialize();
        } catch (OperatorException oe) {
            String message = oe.getMessage().toLowerCase();
            assertThat(message, containsString("north bound"));
            assertThat(message, containsString("south bound"));
        }
    }

    private LcSubsetOp createLcSubsetOp() {
        return (LcSubsetOp) new LcSubsetOp.Spi().createOperator();
    }

    private void setDefaultBounds(LcSubsetOp subsetOp) {
        subsetOp.setEast(15);
        subsetOp.setWest(-15);
        subsetOp.setSouth(-15);
        subsetOp.setNorth(15);
    }

    private Product createSourceProduct() throws Exception {
        final Integer width = 3600;
        final Integer height = 1800;
        final Product product = new Product("P", "T", width, height);
        product.setFileLocation(new File("/blah/ESACCI-LC-L4-LCCS-Map-300m-P5Y-2010-v2.nc"));
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
        product.setGeoCoding(new CrsGeoCoding(DefaultGeographicCRS.WGS84, width, height, -179.95, 89.95, 0.1, 0.1));
        MetadataElement globalAttributes = new MetadataElement("Global_Attributes");
        globalAttributes.addAttribute(new MetadataAttribute("id", ProductData.createInstance("ESACCI-LC-L4-LCCS-Map-300m-P5Y-2010-v2"), true));
        product.getMetadataRoot().addElement(globalAttributes);
        return product;
    }
}
