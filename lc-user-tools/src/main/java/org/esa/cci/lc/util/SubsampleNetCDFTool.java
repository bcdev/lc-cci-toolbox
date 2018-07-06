package org.esa.cci.lc.util;

import com.bc.ceres.core.PrintWriterProgressMonitor;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.dataio.ProductIOPlugInManager;
import org.esa.snap.core.dataio.ProductSubsetDef;
import org.esa.snap.core.datamodel.Product;
import org.esa.cci.lc.io.LcMapNetCdf4WriterPlugIn;

import java.io.File;
import java.io.IOException;

/**
 * Creates a sub-sampled LC-NetCDF file from the given LC-NetCDF file.
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
        File outputFile = new File(product.getFileLocation().getParentFile(), productName + ".nc");
        PrintWriterProgressMonitor pm = new PrintWriterProgressMonitor(System.out);
        ProductIO.writeProduct(subsetProduct, outputFile, formatName, false, pm);
    }
}
