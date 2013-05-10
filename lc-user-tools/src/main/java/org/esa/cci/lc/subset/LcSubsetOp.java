package org.esa.cci.lc.subset;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.experimental.Output;

import java.awt.Rectangle;
import java.io.File;
import java.util.HashMap;

@OperatorMetadata(
            alias = "LCCCI.Subset",
            version = "0.5",
            authors = "Marco Peters",
            copyright = "(c) 2012 by Brockmann Consult",
            description = "Allows to subset LC map and condition products.")
public class LcSubsetOp extends Operator implements Output {

    @SourceProduct(description = "LC CCI map or conditions product.", optional = false)
    private Product sourceProduct;

    @Parameter(description = "The western longitude.", interval = "[-180,180]", unit = "°")
    private Float westBound;
    @Parameter(description = "The northern latitude.", interval = "[-90,90]", unit = "°")
    private Float northBound;
    @Parameter(description = "The eastern longitude.", interval = "[-180,180]", unit = "°")
    private Float eastBound;
    @Parameter(description = "The southern latitude.", interval = "[-90,90]", unit = "°")
    private Float southBound;

    @Parameter(description = "A predefined set of north, east, south and west bounds.", valueSet = {"EUROPE", "ASIA"})
    private PredefinedRegion predefinedRegion;

    // for test cases
    Product subsetProduct;
    boolean writeProduct = true;

    @Override
    public void initialize() throws OperatorException {
        if (!predefinedRegionIsSelected() && !userDefinedRegionIsSelected()) {
            throw new OperatorException("Either predefined region or geographical bounds must be given.");
        }
        final File fileLocation = sourceProduct.getFileLocation();
        String sourceDir = fileLocation.getParent();
        if (sourceDir == null) {
            throw new OperatorException("Can not retrieve parent directory from source product");
        }
        subsetProduct = createProductSubset();
        final String formatName;
        final String fileName = fileLocation.getName();
        if (fileName.startsWith("ESACCI-LC-L4-Cond-")) {
            formatName = "NetCDF4-LC-Condition";
        } else {
            formatName = "NetCDF4-LC-Map";
        }

        final File targetFile = new File(sourceDir, getTargetFileName(fileName));
        if (writeProduct) {
            GPF.writeProduct(subsetProduct, targetFile, formatName, false, ProgressMonitor.NULL);
        }
        setTargetProduct(new Product("dummy", "dummyType", 10, 10));
    }

    private String getTargetFileName(String fileName) {
        final int splitPos = fileName.toLowerCase().lastIndexOf("v");
        final String firstPart = fileName.substring(0, splitPos);
        final String lastPart = fileName.substring(splitPos);
        if (predefinedRegionIsSelected()) {
            return firstPart + predefinedRegion + "-" + lastPart;
        } else {
            return firstPart + "USER_REGION-" + fileName;
        }
    }

    private Product createProductSubset() {
        final Rectangle pixelRect;
        if (predefinedRegionIsSelected()) {
            final PredefinedRegion r = predefinedRegion;
            pixelRect = getPixelBounds(r.getNorth(), r.getEast(), r.getSouth(), r.getWest());
        } else {
            pixelRect = getPixelBounds(northBound, eastBound, southBound, westBound);
        }
        final HashMap<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("region", pixelRect);
        return GPF.createProduct("Subset", parameters, sourceProduct);
    }

    private Rectangle getPixelBounds(float north, float east, float south, float west) {
        final GeoCoding geoCoding = sourceProduct.getGeoCoding();
        final GeoPos ulGePo = new GeoPos(north, west);
        final GeoPos lrGePo = new GeoPos(south, east);
        final PixelPos ulPiPo = geoCoding.getPixelPos(ulGePo, null);
        final PixelPos lrPiPo = geoCoding.getPixelPos(lrGePo, null);
        final int x = (int) ulPiPo.x;
        final int y = (int) ulPiPo.y;
        final int width = (int) lrPiPo.x - x + 1;
        final int height = (int) lrPiPo.y - y + 1;
        return new Rectangle(x, y, width, height);
    }

    void setWestBound(float westBound) {
        this.westBound = westBound;
    }


    void setNorthBound(float northBound) {
        this.northBound = northBound;
    }

    void setEastBound(float eastBound) {
        this.eastBound = eastBound;
    }

    void setSouthBound(float southBound) {
        this.southBound = southBound;
    }

    private boolean predefinedRegionIsSelected() {
        return predefinedRegion != null;
    }

    private boolean userDefinedRegionIsSelected() {
        final boolean valid = northBound != null && eastBound != null && southBound != null && westBound != null;
        if (valid) {
            if (westBound >= eastBound) {
                throw new OperatorException("West bound must be western of east bound.");
            }
            if (northBound <= southBound) {
                throw new OperatorException("North bound must be northern of south bound.");
            }
        }
        return valid;
    }

    /**
     * The Service Provider Interface (SPI) for the operator.
     * It provides operator meta-data and is a factory for new operator instances.
     */
    public static class Spi extends OperatorSpi {

        public Spi() {
            super(LcSubsetOp.class);
        }
    }


}
