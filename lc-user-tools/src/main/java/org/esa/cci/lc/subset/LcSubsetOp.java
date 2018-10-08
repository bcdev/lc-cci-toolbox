package org.esa.cci.lc.subset;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.cci.lc.io.*;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.cci.lc.util.LcHelper;
import org.esa.cci.lc.util.PlanetaryGridName;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.crs.DefaultGeographicCRS;

import java.io.File;

@OperatorMetadata(
        alias = "LCCCI.Subset",
        version = "3.10",
        authors = "Marco Peters",
        copyright = "(c) 2013 by Brockmann Consult",
        description = "Allows to subset LC map and condition products.",
        autoWriteDisabled = true
)
public class LcSubsetOp extends Operator {

    @SourceProduct(description = "LC CCI map or conditions product.", optional = false)
    private Product sourceProduct;

    @Parameter(description = "The target directory.")
    private File targetDir;

    @Parameter(description = "The western longitude.", interval = "[-180,360]", unit = "°")
    private Float west;
    @Parameter(description = "The northern latitude.", interval = "[-90,90]", unit = "°")
    private Float north;
    @Parameter(description = "The eastern longitude.", interval = "[-180,360]", unit = "°")
    private Float east;
    @Parameter(description = "The southern latitude.", interval = "[-90,90]", unit = "°")
    private Float south;
    @Parameter(description = "Format of the output file: lccci,lccds",defaultValue = "lccci")
    private String format;

    @Parameter(description = "A predefined set of north, east, south and west bounds.",
            valueSet = {
                    "NORTH_AMERICA", "CENTRAL_AMERICA", "SOUTH_AMERICA",
                    "WESTERN_EUROPE_AND_MEDITERRANEAN", "ASIA", "AFRICA",
                    "SOUTH_EAST_ASIA", "AUSTRALIA_AND_NEW_ZEALAND", "GREENLAND"
            })
    private PredefinedRegion predefinedRegion;


    // for test cases
    Product subsetProduct;
    boolean writeProduct = true;
    private File targetFile;

    @Override
    public void initialize() throws OperatorException {
        validateInputSettings();
        targetDir = LcHelper.ensureTargetDir(targetDir, getSourceProduct());

        String id = createId();

        if (isPredefinedRegionSet()) {
            final PredefinedRegion r = predefinedRegion;
            north = r.getNorth();
            east = r.getEast();
            south = r.getSouth();
            west = r.getWest();
            MetadataElement globalAttributes = getSourceProduct().getMetadataRoot().getElement(LcMapMetadata.GLOBAL_ATTRIBUTES_ELEMENT_NAME);
            if (globalAttributes.getAttributeString("grid_name", "").startsWith("Regular gaussian grid")) {
                // shift only for predefined regions
                east = (east + 360) % 360;
                west = (west + 360) % 360;
            }
        }

        subsetProduct = LcHelper.createProductSubset(getSourceProduct(), north, east, south, west, getRegionIdentifier());
        subsetProduct.setPreferredTileSize(LcHelper.TILE_SIZE);

        updateIdMetadataAttribute(id);
        String formatName;

        if (id.startsWith("ESACCI-LC-L4-LCCS-Map-") || id.startsWith("ESACCI-LC-L4-LCCS-AlternativeMap")) {
            formatName = LcMapNetCdf4WriterPlugIn.FORMAT_NAME;
        } else if (id.startsWith("ESACCI-LC-L4-WB-Map-")) {
            formatName = LcWbNetCdf4WriterPlugIn.FORMAT_NAME;
        } else {
            formatName = LcConditionNetCdf4WriterPlugIn.FORMAT_NAME;
        }
        if (format.equals("lccds")){
            formatName= LcCdsNetCDF4WriterPlugin.FORMAT_NAME;
            subsetProduct.getMetadataRoot().getElement("global_attributes").setAttributeString("parent_path", sourceProduct.getFileLocation().getAbsolutePath());
            subsetProduct.getMetadataRoot().getElement("global_attributes").setAttributeString("geospatial_lat_min", south.toString());
            subsetProduct.getMetadataRoot().getElement("global_attributes").setAttributeString("geospatial_lat_max", north.toString());
            subsetProduct.getMetadataRoot().getElement("global_attributes").setAttributeString("geospatial_lon_min", west.toString());
            subsetProduct.getMetadataRoot().getElement("global_attributes").setAttributeString("geospatial_lon_max", east.toString());
            subsetProduct.getMetadataRoot().getElement("global_attributes").setAttributeString("subsetted", "true");

        }

        if (targetFile == null) {
            targetFile = new File(targetDir, id + ".nc");
        }

        if (writeProduct) {
            GPF.writeProduct(subsetProduct, targetFile, formatName, false, ProgressMonitor.NULL);
        }
        setTargetProduct(new Product("dummy", "dummyType", 10, 10));
    }

    private void validateInputSettings() {
        if (!isPredefinedRegionSet() && !isUserDefinedRegionSet()) {
            throw new OperatorException("Either predefined region or geographical bounds must be given.");
        }

        if (isRegularGuassianGrid() && getRegionIdentifier() != null) {
            ReferencedEnvelope regionEnvelope = getRegionEnvelope();
            double maxLon = regionEnvelope.getMaximum(0);
            double minLon = regionEnvelope.getMinimum(0);
            if (maxLon > 0 && minLon < 0) {
                String msg = "The planetary grid '%s' can not be used in combination with a region " +
                        "which crosses the prime meridian.";
                throw new OperatorException(String.format(msg, PlanetaryGridName.REGULAR_GAUSSIAN_GRID));
            }
        }

    }

    private boolean isRegularGuassianGrid() {
        MetadataElement globalAttributes = getSourceProduct().getMetadataRoot().getElement(LcMapMetadata.GLOBAL_ATTRIBUTES_ELEMENT_NAME);
        return globalAttributes.getAttributeString("grid_name", "").startsWith("Regular gaussian grid");
    }

    private ReferencedEnvelope getRegionEnvelope() {
        if (isPredefinedRegionSet()) {
            return new ReferencedEnvelope(predefinedRegion.getEast(), predefinedRegion.getWest(),
                                          predefinedRegion.getNorth(), predefinedRegion.getSouth(),
                                          DefaultGeographicCRS.WGS84);
        } else if (isUserDefinedRegionSet()) {
            return new ReferencedEnvelope(east, west, north, south, DefaultGeographicCRS.WGS84);
        }
        return null;
    }

    private void updateIdMetadataAttribute(String id) {
        MetadataElement metadataRoot = getSourceProduct().getMetadataRoot();
        if (metadataRoot.containsElement(LcMapMetadata.GLOBAL_ATTRIBUTES_ELEMENT_NAME)) {
            MetadataElement globalAttributes = metadataRoot.getElement(LcMapMetadata.GLOBAL_ATTRIBUTES_ELEMENT_NAME);
            globalAttributes.setAttributeString("id", id);
        }
    }

    private String createId() {
        MetadataElement metadataRoot = getSourceProduct().getMetadataRoot();
        if (metadataRoot.containsElement(LcMapMetadata.GLOBAL_ATTRIBUTES_ELEMENT_NAME)) {
            MetadataElement globalAttributes = metadataRoot.getElement(LcMapMetadata.GLOBAL_ATTRIBUTES_ELEMENT_NAME);
            final String id = globalAttributes.getAttributeString("id");
            int p1 = id.lastIndexOf("-");
            int p2 = id.lastIndexOf("-", p1);
            return id.substring(0, p2) + "-" + getRegionIdentifier() + id.substring(p2);
        } else {
            throw new IllegalStateException("Missing metadata element " + LcMapMetadata.GLOBAL_ATTRIBUTES_ELEMENT_NAME);
        }
    }

    private String getRegionIdentifier() {
        if (isPredefinedRegionSet()) {
            return predefinedRegion.toString();
        } else {
            return "USER_REGION";
        }
    }

    public void setWest(float west) {
        this.west = west;
    }

    public void setNorth(float north) {
        this.north = north;
    }

    public void setEast(float east) {
        this.east = east;
    }

    public void setSouth(float south) {
        this.south = south;
    }

    public void setTargetDir(File targetDir) {
        this.targetDir = targetDir;
    }

    public void setTargetFile(File targetFile) {
        this.targetFile = targetFile;
    }

    public void setPredefinedRegion(PredefinedRegion predefinedRegion) {
        this.predefinedRegion = predefinedRegion;
    }

    private boolean isPredefinedRegionSet() {
        return predefinedRegion != null;
    }

    private boolean isUserDefinedRegionSet() {
        final boolean valid = north != null && east != null && south != null && west != null;
        if (valid) {
            if (west >= east) {
                throw new OperatorException("West bound must be western of east bound.");
            }
            if (north <= south) {
                throw new OperatorException("North bound must be northern of south bound.");
            }
            if (isRegularGuassianGrid()) {
                if (west < 0 || east < 0) {
                    throw new OperatorException("West and east bound must be between " +
                                                        "0 and 360 for regular gaussian grid.");
                }
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
