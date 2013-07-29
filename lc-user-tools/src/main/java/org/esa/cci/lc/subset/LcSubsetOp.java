package org.esa.cci.lc.subset;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.experimental.Output;
import org.esa.cci.lc.io.LcConditionNetCdf4WriterPlugIn;
import org.esa.cci.lc.io.LcMapNetCdf4WriterPlugIn;
import org.esa.cci.lc.util.LcHelper;

import java.io.File;

@OperatorMetadata(
        alias = "LCCCI.Subset",
        version = "0.8",
        authors = "Marco Peters",
        copyright = "(c) 2013 by Brockmann Consult",
        description = "Allows to subset LC map and condition products.")
public class LcSubsetOp extends Operator implements Output {

    @SourceProduct(description = "LC CCI map or conditions product.", optional = false)
    private Product sourceProduct;

    @Parameter(description = "The target directory.")
    private File targetDir;

    @Parameter(description = "The western longitude.", interval = "[-180,180]", unit = "°")
    private Float west;
    @Parameter(description = "The northern latitude.", interval = "[-90,90]", unit = "°")
    private Float north;
    @Parameter(description = "The eastern longitude.", interval = "[-180,180]", unit = "°")
    private Float east;
    @Parameter(description = "The southern latitude.", interval = "[-90,90]", unit = "°")
    private Float south;

    @Parameter(description = "A predefined set of north, east, south and west bounds.",
               valueSet = {
                       "NORTH_AMERICA", "CENTRAL_AMERICA", "SOUTH_AMERICA",
                       "WESTERN_EUROPE_AND_MEDITERRANEAN_BASIS", "ASIA", "AFRICA",
                       "SOUTH_EAST_ASIA", "AUSTRALIA_AND_NEW_ZEALAND", "GREENLAND"
               })
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
        targetDir = LcHelper.ensureTargetDir(targetDir, getSourceProduct());

        if (predefinedRegionIsSelected()) {
            final PredefinedRegion r = predefinedRegion;
            subsetProduct = LcHelper.createProductSubset(sourceProduct, r.getNorth(), r.getEast(), r.getSouth(), r.getWest(), getRegionIdentifier());
        } else {
            subsetProduct = LcHelper.createProductSubset(sourceProduct, north, east, south, west, getRegionIdentifier());
        }
        final String formatName;
        final String fileName = fileLocation.getName();
        if (fileName.startsWith("ESACCI-LC-L4-LCCS-Map-")) {
            formatName = LcMapNetCdf4WriterPlugIn.FORMAT_NAME;
        } else {
            formatName = LcConditionNetCdf4WriterPlugIn.FORMAT_NAME;
        }

        final File targetFile = new File(targetDir, getTargetFileName(fileName));
        if (writeProduct) {
            GPF.writeProduct(subsetProduct, targetFile, formatName, false, ProgressMonitor.NULL);
        }
        setTargetProduct(new Product("dummy", "dummyType", 10, 10));
    }

    private String getTargetFileName(String fileName) {
        return LcHelper.getTargetFileName(fileName, getRegionIdentifier());
    }

    private String getRegionIdentifier() {
        if (predefinedRegionIsSelected()) {
            return predefinedRegion.toString();
        } else {
            return "USER_REGION";
        }
    }

    void setWest(float west) {
        this.west = west;
    }

    void setNorth(float north) {
        this.north = north;
    }

    void setEast(float east) {
        this.east = east;
    }

    void setSouth(float south) {
        this.south = south;
    }

    void setTargetDir(File targetDir) {
        this.targetDir = targetDir;
    }

    private boolean predefinedRegionIsSelected() {
        return predefinedRegion != null;
    }

    private boolean userDefinedRegionIsSelected() {
        final boolean valid = north != null && east != null && south != null && west != null;
        if (valid) {
            if (west >= east) {
                throw new OperatorException("West bound must be western of east bound.");
            }
            if (north <= south) {
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
