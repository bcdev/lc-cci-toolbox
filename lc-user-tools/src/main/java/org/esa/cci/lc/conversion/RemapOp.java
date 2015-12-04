package org.esa.cci.lc.conversion;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.gpf.operators.standard.WriteOp;
import org.esa.beam.util.ProductUtils;
import org.esa.beam.util.io.FileUtils;
import org.esa.cci.lc.aggregation.Lccs2PftLut;
import org.esa.cci.lc.aggregation.Lccs2PftLutBuilder;
import org.esa.cci.lc.aggregation.Lccs2PftLutException;
import org.esa.cci.lc.util.LcHelper;

import java.awt.Rectangle;
import java.awt.image.Raster;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * This operator converts the lCCS classes to PFT classes staying without resampling them onto a different grid.
 *
 * @author Marco Peters
 */
@OperatorMetadata(
        alias = "LCCCI.Remap",
        version = "3.10",
        authors = "Marco Peters",
        copyright = "(c) 2015 by Brockmann Consult",
        description = "Remaps the LCCS classes to pft classes on the same grid as the input.",
        autoWriteDisabled = true)
public class RemapOp extends Operator {

    private static final String LCCS_CLASS_BAND_NAME = "lccs_class";
    private static final String USER_MAP_BAND_NAME = "user_map";
    private static final int SCALING_FACTOR = 100;

    @SourceProduct()
    private Product sourceProduct;

    // todo - maybe it's better to have it as paramater. Then we have -PadditionalUserMap="" on the command line;
    // same as for the map aggregation
    @SourceProduct(description = "A map containing additional classes which can be used to refine " +
            "the conversion from LCCS to PFT classes", optional = true)
    private Product additionalUserMap;

    @TargetProduct(description = "The target product containing the pft classes.")
    private Product targetProduct;

    @Parameter(description = "The user defined conversion table from LCCS to PFTs. " +
            "If not given, the standard LC-CCI table is used.",
            label = "User Defined PFT Conversion Table")
    private File userPFTConversionTable;

    @Parameter(description = "The conversion table from LCCS to PFTs considering the additional user map. " +
            "This option is only applicable if the additional user map is given too.",
            label = "Additional User Map PFT Conversion Table")
    private File additionalUserMapPFTConversionTable;

    // for testing
    boolean writeProduct = true;
    private Lccs2PftLut pftLut;

    @Override
    public void initialize() throws OperatorException {
        validateSource();
        validateParameter();
        targetProduct = new Product(sourceProduct.getName(),
                                    sourceProduct.getProductType(),
                                    sourceProduct.getSceneRasterWidth(),
                                    sourceProduct.getSceneRasterHeight());
        final Band[] bands = sourceProduct.getBands();
        for (Band band : bands) {
            ProductUtils.copyBand(band.getName(), sourceProduct, targetProduct, true);
        }
        ProductUtils.copyMetadata(sourceProduct, targetProduct);
        ProductUtils.copyGeoCoding(sourceProduct, targetProduct);

        if (additionalUserMap != null) {
            validateAddionalUserMap();
            final Band mapBand = additionalUserMap.getBandAt(0);
            targetProduct.addBand(USER_MAP_BAND_NAME, mapBand.getDataType());
        }
        updateMetadata();

        pftLut = createPftLut();
        final String[] pftNames = pftLut.getPFTNames();
        for (String pftName : pftNames) {
            final Band pftBand = targetProduct.addBand(pftName, ProductData.TYPE_INT16);
            pftBand.setNoDataValue(0.0);
            pftBand.setNoDataValueUsed(true);
            pftBand.setScalingFactor(1 / SCALING_FACTOR);
        }

        writeTarget();
    }

    @Override
    public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        int lineStride = targetTile.getScanlineStride();
        int lineOffset = targetTile.getScanlineOffset();
        Band userMap = additionalUserMap.getBandAt(0);
        GeoCoding targetBandGeoCoding = targetBand.getGeoCoding();

        if (USER_MAP_BAND_NAME.equals(targetBand.getName())) {
            ProductData dataBuffer = targetTile.getDataBuffer();
            for (int y = targetTile.getMinY(); y <= targetTile.getMaxY(); y++) {
                int index = lineOffset;
                for (int x = targetTile.getMinX(); x <= targetTile.getMaxX(); x++) {
                    dataBuffer.setElemIntAt(index++, getUserMapSample(userMap, targetBandGeoCoding, x, y));
                }
                lineOffset += lineStride;
            }
        } else {
            Band lccsBand = sourceProduct.getBand(LCCS_CLASS_BAND_NAME);
            Raster lccsData = lccsBand.getGeophysicalImage().getData(targetTile.getRectangle());
            int[] dataBufferInt = targetTile.getDataBufferInt();
            int pftIndex = Arrays.binarySearch(pftLut.getPFTNames(), targetBand.getName());
            for (int y = targetTile.getMinY(); y <= targetTile.getMaxY(); y++) {
                int index = lineOffset;
                for (int x = targetTile.getMinX(); x <= targetTile.getMaxX(); x++) {
                    int userClass = getUserMapSample(userMap, targetBand.getGeoCoding(), x, y);
                    int lccsClass = lccsData.getSample(x, y, 0);
                    float[] conversionFactors = pftLut.getConversionFactors(lccsClass, userClass);
                    dataBufferInt[index++] = (int) Math.floor(conversionFactors[pftIndex] * SCALING_FACTOR);
                }
                lineOffset += lineStride;
            }
        }

    }

    private int getUserMapSample(Band userMap, GeoCoding geoCoding, int x, int y) {
        final GeoPos geoPos = geoCoding.getGeoPos(new PixelPos(x, y), null);
        final PixelPos pixelPos = userMap.getGeoCoding().getPixelPos(geoPos, null);
        final Rectangle rect = new Rectangle((int) Math.floor(pixelPos.x), (int) Math.floor(pixelPos.y), 1, 1);
        final Raster source = userMap.getGeophysicalImage().getData(rect);
        return source.getSample(rect.x, rect.y, 0);
    }

    private void updateMetadata() {
        final HashMap<String, String> lcProperties = new HashMap<>();
        LcHelper.addPFTTableInfoToLcProperties(lcProperties, true, userPFTConversionTable,
                                               additionalUserMapPFTConversionTable);
        final MetadataElement gAttribs = targetProduct.getMetadataRoot().getElement("Global_Attributes");
        for (Map.Entry<String, String> entry : lcProperties.entrySet()) {

            gAttribs.addAttribute(new MetadataAttribute(entry.getKey(),
                                                        new ProductData.ASCII(entry.getValue()), true));
        }
    }

    private Lccs2PftLut createPftLut() {
        final Lccs2PftLutBuilder lutBuilder = new Lccs2PftLutBuilder();
        try {
            if (userPFTConversionTable != null) {
                lutBuilder.useLccs2PftTable(new FileReader(userPFTConversionTable));
            }
            if (additionalUserMapPFTConversionTable != null) {
                lutBuilder.useAdditionalUserMap(new FileReader(additionalUserMapPFTConversionTable));
            }
            return lutBuilder.create();
        } catch (FileNotFoundException | Lccs2PftLutException e) {
            throw new OperatorException("Could not create PFT look-up table.", e);
        }
    }

    private void writeTarget() {
        if (writeProduct) {
            final String targetFileName = FileUtils.getFilenameWithoutExtension(sourceProduct.getFileLocation()) + "_updated.nc";
            final String targetDir = sourceProduct.getFileLocation().getParent();
            final String formatName = "NetCDF4-LC-Map";
            File targetFile = new File(targetDir, targetFileName);
            WriteOp writeOp = new WriteOp(targetProduct, targetFile, formatName);
            writeOp.setClearCacheAfterRowWrite(true);
            // If execution order is not set to SCHEDULE_BAND_ROW_COLUMN a Java heap space error occurs multiple times
            // if only 2GB of heap space is available:
            // Exception in thread "SunTileScheduler0Standard2" java.lang.OutOfMemoryError: Java heap space
            // todo - try other setting (mp - 20151204)
            // SCHEDULE_ROW_COLUMN_BAND or SCHEDULE_ROW_BAND_COLUMN
            System.setProperty("beam.gpf.executionOrder", "SCHEDULE_BAND_ROW_COLUMN");
            writeOp.writeProduct(ProgressMonitor.NULL);
        }
    }

    private void validateSource() {
        final GeoCoding geoCoding = sourceProduct.getGeoCoding();
        if (geoCoding == null || !geoCoding.canGetGeoPos()) {
            throw new OperatorException("The source is not properly geo-referenced. " +
                                                "It must be able to provide the geo-location for a pixel position.");
        }
        if (!sourceProduct.containsBand(LCCS_CLASS_BAND_NAME)) {
            throw new OperatorException(String.format("Missing band '%s' in source product.", LCCS_CLASS_BAND_NAME));
        }
    }

    private void validateParameter() {
        if (userPFTConversionTable != null) {
            if (!isFileAndReadable(userPFTConversionTable)) {
                final String message = String.format("Path '%s' to userPFTConversionTable not valid. " +
                                                             "Please ensure that it is a file and that it is readable.",
                                                     userPFTConversionTable);
                throw new OperatorException(message);
            }
        }
        if (additionalUserMapPFTConversionTable != null) {
            if (!isFileAndReadable(additionalUserMapPFTConversionTable)) {
                final String message = String.format("Path '%s' to additionalUserMapPFTConversionTable not valid. " +
                                                             "Please ensure that it is a file and that it is readable.",
                                                     additionalUserMapPFTConversionTable);
                throw new OperatorException(message);
            }
            if (additionalUserMap == null) {
                throw new OperatorException("An additionalUserMapPFTConversionTable has been specified, " +
                                                    "but the required additionalUserMap not.");
            }
        }

    }

    private boolean isFileAndReadable(File file) {
        return file.isFile() && file.canRead();
    }

    private void validateAddionalUserMap() {
        final GeoCoding geoCoding = additionalUserMap.getGeoCoding();
        if (geoCoding == null || !geoCoding.canGetPixelPos()) {
            throw new OperatorException("The additional user map is not properly geo-referenced. " +
                                                "It must be able to provide the pixel position for a geo-location.");
        }
        if (additionalUserMap.getBands().length < 1) {
            throw new OperatorException("The additional user map must have at least one band.");
        }
    }

    /**
     * The Service Provider Interface (SPI) for the operator.
     * It provides operator meta-data and is a factory for new operator instances.
     */
    public static class Spi extends OperatorSpi {

        public Spi() {
            super(RemapOp.class);
        }
    }

}
