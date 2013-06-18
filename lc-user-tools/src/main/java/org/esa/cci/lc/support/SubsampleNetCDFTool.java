package org.esa.cci.lc.support;

import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.dataio.ProductIOPlugInManager;
import org.esa.beam.framework.dataio.ProductSubsetDef;
import org.esa.beam.framework.datamodel.Product;
import org.esa.cci.lc.io.LcMapNetCdf4WriterPlugIn;

import java.io.IOException;

/**
 * Creates a subsampled LC-NetCDF file from the given LC-NetCDF file.
 * Just a tool used during development.
 *
 * @author Marco Peters
 */
public class SubsampleNetCDFTool {

    public static void main(String[] args) throws IOException {
        Product product = ProductIO.readProduct(args[0]);
        ProductSubsetDef productSubsetDef = new ProductSubsetDef();
        int subSamplingY = product.getSceneRasterHeight() / Integer.parseInt(args[1]);
        int subSamplingX = subSamplingY;
        productSubsetDef.setSubSampling(subSamplingX, subSamplingY);
        String productName = product.getName() + "_subset_" + args[1];
        productSubsetDef.setSubsetName(productName);
        Product subsetProduct = product.createSubset(productSubsetDef, productName, product.getDescription());
        LcMapNetCdf4WriterPlugIn lcMapNetCdf4WriterPlugIn = new LcMapNetCdf4WriterPlugIn();
        ProductIOPlugInManager.getInstance().addWriterPlugIn(lcMapNetCdf4WriterPlugIn);
        String formatName = lcMapNetCdf4WriterPlugIn.getFormatNames()[0];
        ProductIO.writeProduct(subsetProduct, "./" + productName, formatName);
    }
}
