package org.esa.cci.lc.util;

import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.cci.lc.aggregation.Lccs2PftLut;
import org.esa.cci.lc.aggregation.Lccs2PftLutBuilder;
import org.esa.cci.lc.aggregation.Lccs2PftLutException;
import org.esa.cci.lc.io.LcWriterUtils;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

public class LcHelper {

    // number of tiles per image in netcdf must be smaller then Short.MAX_VALUE
    // therefore we set 1800 as tile size, in addition it fite to the scene size and
    // no unnecessary is kept in memory
    public static final Dimension TILE_SIZE = new Dimension(1800, 1800);
    public static final String TILE_SIZE_STRING = format(TILE_SIZE);
    public static final String PROP_NAME_TILE_SIZE = "TileSize";


    // ESACCI-LC-L4-LCCS-Map-300m-P5Y-2005-v1.1.nc
    // ESACCI-LC-L4-WB-Map-300m-P5Y-2005-v1.1.nc
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
        Rectangle pixelRect = getPixelBounds(north, east, south, west, product.getGeoCoding());
        Rectangle productRect = new Rectangle(0, 0, product.getSceneRasterWidth(), product.getSceneRasterHeight());
        pixelRect = pixelRect.intersection(productRect);
        final HashMap<String, Object> parameters = new HashMap<>();
        parameters.put("region", pixelRect);
        parameters.put("copyMetadata", true);
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

    public static void addPFTTableInfoToLcProperties(HashMap<String, String> lcProperties, boolean outputPFTClasses, File userPFTConversionTable, File additionalUserMapPFTConversionTable) {
        if (outputPFTClasses) {
            try {
                // lutBuilder only used to read the comment of table.
                Lccs2PftLutBuilder lutBuilder = new Lccs2PftLutBuilder();
                String pftTableEntry;
                if (userPFTConversionTable != null) {
                    pftTableEntry = String.format("User defined PFT conversion table used (%s)",
                                                  userPFTConversionTable.getName());
                    lutBuilder = lutBuilder.useLccs2PftTable(new FileReader(userPFTConversionTable));
                } else {
                    pftTableEntry = "LC-CCI conform PFT conversion table";
                }
                if (additionalUserMapPFTConversionTable != null) {
                    pftTableEntry += String.format(" + additional user map PFT conversion table (%s)",
                                                   additionalUserMapPFTConversionTable.getName());
                    lutBuilder = lutBuilder.useAdditionalUserMap(new FileReader(additionalUserMapPFTConversionTable));
                }
                Lccs2PftLut pftLut = lutBuilder.create();
                if (pftLut.getComment() != null) {
                    lcProperties.put("pft_table_comment", pftLut.getComment());
                }
                lcProperties.put("pft_table", pftTableEntry);
            } catch (IOException | Lccs2PftLutException e) {
                throw new OperatorException("Could not read specified PFT table.", e);
            }
            if (userPFTConversionTable == null && additionalUserMapPFTConversionTable == null) {
                lcProperties.put("pft_table", "LC-CCI conform PFT conversion table used.");
            }
        } else {
            lcProperties.put("pft_table", "No PFT computed.");
        }
    }

    public static Dimension convertToDimension(String tileSizeString) {
        if (tileSizeString == null || !tileSizeString.contains(":") || tileSizeString.split(":").length != 2) {
            String msg = String.format("Not able to convert missing or invalid value (%s)", tileSizeString);
            throw new IllegalStateException(msg);
        }
        String[] tileSizes = tileSizeString.split(":");
        return new Dimension(Integer.parseInt(tileSizes[1]), Integer.parseInt(tileSizes[0]));
    }

    public static String format(Dimension tileSize) {
        return String.format("%d:%d", tileSize.width, tileSize.height);
    }
}
