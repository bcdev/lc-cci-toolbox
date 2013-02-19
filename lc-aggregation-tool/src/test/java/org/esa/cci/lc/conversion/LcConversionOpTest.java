package org.esa.cci.lc.conversion;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.dataio.ProductIOPlugInManager;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.CrsGeoCoding;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.gpf.operators.standard.WriteOp;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.media.jai.operator.ConstantDescriptor;
import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


public class LcConversionOpTest {

    private static WriteOp.Spi writeOpSpI;
    private static LcMapNetCdf4WriterPlugIn writerPlugIn;

    @BeforeClass
    public static void beforeClass() {
        writeOpSpI = new WriteOp.Spi();
        GPF.getDefaultInstance().getOperatorSpiRegistry().addOperatorSpi(writeOpSpI);
        writerPlugIn = new LcMapNetCdf4WriterPlugIn();
        ProductIOPlugInManager.getInstance().addWriterPlugIn(writerPlugIn);
    }

    @AfterClass
    public static void afterClass() {
        GPF.getDefaultInstance().getOperatorSpiRegistry().removeOperatorSpi(writeOpSpI);
        ProductIOPlugInManager.getInstance().removeWriterPlugIn(writerPlugIn);
    }

    @Test()
    public void testTargetProductCreation() throws Exception {
        WriteOp op = createOp();
        Product targetProduct = op.getTargetProduct();
        op.writeProduct(ProgressMonitor.NULL);

        final File output = new File("/tmp/ESACCI-LC-L4-Map-10000m-P5Y-2010-v0.nc");
        assertTrue("check output exists", output.exists());
        output.delete();
    }

    private Product createSourceProduct() throws Exception {
        Integer width = 3600;
        Integer height = 1800;
        Product product = new Product("P", "T", width, height);
        Band classesBand = product.addBand("lccs_class", ProductData.TYPE_UINT8);
        classesBand.setSourceImage(ConstantDescriptor.create(width.floatValue(), height.floatValue(),
                                                             new Byte[]{10}, null));
        product.setGeoCoding(new CrsGeoCoding(DefaultGeographicCRS.WGS84, width, height, -180.0, 90.0, 1.0, 1.0));
        product.getMetadataRoot().setAttributeString("epoch", "2010");
        product.getMetadataRoot().setAttributeString("version", "0");
        product.getMetadataRoot().setAttributeString("spatialResolution", "10000");
        product.getMetadataRoot().setAttributeString("temporalResolution", "5");
        product.setFileLocation(new File("/tmp/dummy"));
        return product;
    }

    private WriteOp createOp() throws Exception {
        WriteOp writeOp = new WriteOp(createSourceProduct(), new File("/tmp/dummy"), "NetCDF4-LC-Map");
        writeOp.setClearCacheAfterRowWrite(true);
        return writeOp;
    }


}