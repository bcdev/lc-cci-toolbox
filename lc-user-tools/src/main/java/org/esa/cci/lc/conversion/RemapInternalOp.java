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
import org.esa.beam.util.ProductUtils;
import org.esa.cci.lc.aggregation.Lccs2PftLut;
import org.esa.cci.lc.aggregation.Lccs2PftLutBuilder;
import org.esa.cci.lc.aggregation.Lccs2PftLutException;
import org.esa.cci.lc.util.LcHelper;

import java.awt.Rectangle;
import java.awt.image.Raster;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * This operator converts the lCCS classes to PFT classes staying without resampling them onto a different grid.
 *
 * @author Marco Peters
 */
@OperatorMetadata(
        alias = "LCCCI.RemapIntern",
        internal = true,
        version = "3.11",
        authors = "Marco Peters",
        copyright = "(c) 2015 by Brockmann Consult",
        description = "Remaps the LCCS classes to pft classes on the same grid as the input."
)
public class RemapInternalOp extends Operator {

    private static final String LCCS_CLASS_BAND_NAME = "lccs_class";
    private static final String USER_MAP_BAND_NAME = "user_map";
    private static final double SCALING_FACTOR = 100.0;

    @SourceProduct()
    private Product sourceProduct;

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

    private Lccs2PftLut pftLut;
    private Map<String, Integer> pftNameIndexMap;

    @Override
    public void initialize() throws OperatorException {
        validateSource();
        validateParameter();
        targetProduct = new Product(sourceProduct.getName(),
                                    sourceProduct.getProductType(),
                                    sourceProduct.getSceneRasterWidth(),
                                    sourceProduct.getSceneRasterHeight());
        targetProduct.setPreferredTileSize(LcHelper.TILE_SIZE);

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
            pftBand.setScalingFactor(1.0 / SCALING_FACTOR);
        }

        pftNameIndexMap = new TreeMap<>();
        for (int i = 0; i < pftNames.length; i++) {
            pftNameIndexMap.put(pftNames[i], i);
        }
    }

    @Override
    public void computeTileStack(Map<Band, Tile> targetTiles, Rectangle targetRectangle, ProgressMonitor pm) throws OperatorException {
            try {
                Collection<Band> keys = targetTiles.keySet();
                final Band proxyBand = keys.toArray(new Band[keys.size()])[0];
                Collection<Tile> values = targetTiles.values();
                final Tile proxyTile = values.toArray(new Tile[values.size()])[0];
                int lineStride = proxyTile.getScanlineStride();
                int lineOffset = proxyTile.getScanlineOffset();
                GeoCoding targetBandGeoCoding = proxyBand.getGeoCoding();

                Tile lccsTile = getSourceTile(sourceProduct.getBand(LCCS_CLASS_BAND_NAME), proxyTile.getRectangle());
                ProductData inBuffer = lccsTile.getDataBuffer();
                for (int y = proxyTile.getMinY(); y <= proxyTile.getMaxY(); y++) {
                    int index = lineOffset;
                    for (int x = proxyTile.getMinX(); x <= proxyTile.getMaxX(); x++) {
                        final int userClass = getUserMapSample(targetBandGeoCoding, x, y);
                        for (Map.Entry<Band, Tile> entry : targetTiles.entrySet()) {
                            final Tile targetTile = entry.getValue();
                            final Band targetBand = entry.getKey();
                            ProductData outBuffer = targetTile.getDataBuffer();
                            if (USER_MAP_BAND_NAME.equals(targetBand.getName())) {
                                outBuffer.setElemIntAt(index, userClass);
                            } else {
                                int pftIndex = pftNameIndexMap.get(targetBand.getName());
                                int lccsClass = inBuffer.getElemIntAt(index);
                                float[] conversionFactors = pftLut.getConversionFactors(lccsClass, userClass);
                                final double value = conversionFactors[pftIndex] * SCALING_FACTOR;
                                outBuffer.setElemIntAt(index, (int) Math.floor(Double.isNaN(value) ? 0 : value));
                            }
                        }
                        index++;
                    }
                    lineOffset += lineStride;
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }
    }

    private int getUserMapSample(GeoCoding geoCoding, int x, int y) {
        if (additionalUserMap != null) {
            Band userMap = additionalUserMap.getBandAt(0);
            final GeoPos geoPos = geoCoding.getGeoPos(new PixelPos(x, y), null);
            final PixelPos pixelPos = userMap.getGeoCoding().getPixelPos(geoPos, null);
            final Rectangle rect = new Rectangle((int) Math.floor(pixelPos.x), (int) Math.floor(pixelPos.y), 1, 1);
            final Raster source = userMap.getGeophysicalImage().getData(rect);
            return source.getSample(rect.x, rect.y, 0);
        } else {
            return -1;
        }
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
            super(RemapInternalOp.class);
        }
    }

}
