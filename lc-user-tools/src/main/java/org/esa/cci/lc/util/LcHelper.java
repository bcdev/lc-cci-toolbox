package org.esa.cci.lc.util;

import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.cci.lc.io.LcWriterUtils;

import java.awt.Rectangle;
import java.io.File;
import java.util.HashMap;

public class LcHelper {

    // ESACCI-LC-L4-LCCS-Map-300m-P5Y-2005-v1.1.nc
    // ESACCI-LC-L4-NDVI-Cond-1000m-P14Y7D-19991224-v2.0.nc
    // ESACCI-LC-L4-NDVI-Cond-1000m-P14Y7D-aggregated-subsetted-19991224-v2.0.nc

    public static String getTargetFileName(String sourceFileName, String insertion) {
        final String sep = "-";
        final String[] strings = sourceFileName.split(sep);
        final int insertionPos = strings.length - 2;
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < strings.length - 1; i++) {
            String string = strings[i];
            if (i == insertionPos) {
                sb.append(insertion).append(sep);
            }
            sb.append(string).append(sep);
        }
        sb.append(strings[strings.length - 1]);
        return sb.toString();
    }

    public static Product createProductSubset(Product product, double north, double east, double south, double west, String regionIdentifier) {
        final Rectangle pixelRect = getPixelBounds(north, east, south, west, product.getGeoCoding());
        if (pixelRect.x < 0 || pixelRect.y < 0 || pixelRect.width < 1 || pixelRect.height < 1) {
            final String msg = "Invalid pixel region %s computed for geo-coordinates [north=%f, east=%f, south=%f, west=%f]";
            throw new OperatorException(String.format(msg, pixelRect, north, east, south, west));
        }
        final HashMap<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("region", pixelRect);
        Product subset = GPF.createProduct("Subset", parameters, product);
        if (regionIdentifier != null) {
            subset.getMetadataRoot().setAttributeString(LcWriterUtils.ATTRIBUTE_NAME_REGION_IDENTIFIER, regionIdentifier);
        }
        return subset;
    }

    private static Rectangle getPixelBounds(double north, double east, double south, double west, GeoCoding geoCoding) {
        final GeoPos ulGePo = new GeoPos((float) north, (float) west);
        final GeoPos lrGePo = new GeoPos((float) south, (float) east);
        final PixelPos ulPiPo = geoCoding.getPixelPos(ulGePo, null);
        final PixelPos lrPiPo = geoCoding.getPixelPos(lrGePo, null);
        final int x = (int) ulPiPo.x;
        final int y = (int) ulPiPo.y;
        final int width = (int) lrPiPo.x - x + 1;
        final int height = (int) lrPiPo.y - y + 1;
        return new Rectangle(x, y, width, height);
    }

    public static File ensureTargetDir(File targetDir, Product sourceProduct) {
        if (targetDir == null) {
            final File fileLocation = sourceProduct.getFileLocation();
            if (fileLocation != null) {
                targetDir = fileLocation.getParentFile();
                if (targetDir == null) {
                    throw new OperatorException("Can not retrieve parent directory from source product");
                }
            }
        } else {
            if (!targetDir.isDirectory() && targetDir.isFile()) {
                throw new OperatorException("The target directory is not a directory. Looks like a file.");
            }
            if (!targetDir.exists()) {
                if (!targetDir.mkdir()) {
                    throw new OperatorException("The target directory could not be created.");
                }
            }
        }
        return targetDir;
    }
}
