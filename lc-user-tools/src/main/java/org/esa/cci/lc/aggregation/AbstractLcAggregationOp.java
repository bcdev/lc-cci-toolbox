package org.esa.cci.lc.aggregation;

import org.esa.beam.binning.support.PlateCarreeGrid;
import org.esa.beam.binning.support.RegularGaussianGrid;
import org.esa.beam.binning.support.SEAGrid;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.cci.lc.subset.PredefinedRegion;
import org.esa.cci.lc.util.LcHelper;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.crs.DefaultGeographicCRS;

import java.io.File;
import java.util.HashMap;

/**
 * @author Marco Peters
 */
public abstract class AbstractLcAggregationOp extends Operator {

    private static final int METER_PER_DEGREE_At_EQUATOR = 111300;

    @SourceProduct(description = "LC CCI map or conditions product.", optional = false)
    private Product sourceProduct;
    @Parameter(description = "The target directory.")
    private File targetDir;
    @Parameter(description = "Defines the grid for the target product.", notNull = true,
               valueSet = {"GEOGRAPHIC_LAT_LON", "REGULAR_GAUSSIAN_GRID"})
    private PlanetaryGridName gridName;
    @Parameter(defaultValue = "2160")
    private int numRows;

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
                       "WESTERN_EUROPE_AND_MEDITERRANEAN", "ASIA", "AFRICA",
                       "SOUTH_EAST_ASIA", "AUSTRALIA_AND_NEW_ZEALAND", "GREENLAND"
               })
    private PredefinedRegion predefinedRegion;


    private final HashMap<String, String> lcProperties;

    protected AbstractLcAggregationOp() {
        this.lcProperties = new HashMap<>();
    }

    @Override
    public void initialize() throws OperatorException {
        targetDir = LcHelper.ensureTargetDir(targetDir, getSourceProduct());
    }

    public File getTargetDir() {
        return targetDir;
    }

    public void setTargetDir(File targetDir) {
        this.targetDir = targetDir;
    }

    int getNumRows() {
        return numRows;
    }

    void setNumRows(int numRows) {
        this.numRows = numRows;
    }

    PlanetaryGridName getGridName() {
        return gridName;
    }

    void setGridName(PlanetaryGridName gridName) {
        this.gridName = gridName;
    }

    PredefinedRegion getPredefinedRegion() {
        return predefinedRegion;
    }

    void setPredefinedRegion(PredefinedRegion predefinedRegion) {
        this.predefinedRegion = predefinedRegion;
    }

    Float getWest() {
        return west;
    }

    void setWest(Float west) {
        this.west = west;
    }

    Float getNorth() {
        return north;
    }

    void setNorth(Float north) {
        this.north = north;
    }

    Float getEast() {
        return east;
    }

    void setEast(Float east) {
        this.east = east;
    }

    Float getSouth() {
        return south;
    }

    void setSouth(Float south) {
        this.south = south;
    }

    String getRegionIdentifier() {
        if (isPredefinedRegionSet()) {
            return predefinedRegion.toString();
        } else if (isUserDefinedRegionSet()) {
            return "USER_REGION";
        } else {
            return null;
        }
    }

    public ReferencedEnvelope getRegionEnvelope() {
        if (isPredefinedRegionSet()) {
            return createEnvelope(predefinedRegion.getNorth(), predefinedRegion.getEast(),
                                  predefinedRegion.getSouth(), predefinedRegion.getWest());
        } else if (isUserDefinedRegionSet()) {
            return createEnvelope(north, east, south, west);
        }
        return null;
    }

    private ReferencedEnvelope createEnvelope(float north, float east, float south, float west) {
        return new ReferencedEnvelope(east, west, north, south, DefaultGeographicCRS.WGS84);
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
        }
        return valid;
    }


    public HashMap<String, String> getLcProperties() {
        return lcProperties;
    }

    protected void validateInputSettings() {
        if (targetDir == null) {
            throw new OperatorException("The parameter 'targetDir' must be given.");
        }
        if (!targetDir.isDirectory()) {
            throw new OperatorException("The target directory does not exist or is not a directory.");
        }
        int numRows = getNumRows();
        if (numRows < 2 || numRows % 2 != 0) {
            throw new OperatorException("Number of rows must be greater than 2 and must be an even number.");
        }
        if (PlanetaryGridName.REGULAR_GAUSSIAN_GRID.equals(getGridName())) {
            setNumRows(numRows * 2);
        }
    }

    protected void addMetadataToLcProperties(MetadataElement globalAttributes) {
        String timeCoverageDuration = globalAttributes.getAttributeString("time_coverage_duration");
        String timeCoverageResolution = globalAttributes.getAttributeString("time_coverage_resolution");
        lcProperties.put("temporalCoverageYears", timeCoverageDuration.substring(1, timeCoverageDuration.length() - 1));
        lcProperties.put("spatialResolutionNominal", globalAttributes.getAttributeString("spatial_resolution"));
        lcProperties.put("temporalResolution", timeCoverageResolution.substring(1, timeCoverageResolution.length() - 1));
        lcProperties.put("startTime", globalAttributes.getAttributeString("time_coverage_start"));
        lcProperties.put("endTime", globalAttributes.getAttributeString("time_coverage_end"));
        lcProperties.put("version", globalAttributes.getAttributeString("product_version"));
        lcProperties.put("source", globalAttributes.getAttributeString("source"));
        lcProperties.put("history", globalAttributes.getAttributeString("history"));
        float resolutionDegree = getTargetSpatialResolution();
        lcProperties.put("spatialResolutionDegrees", String.format("%.6f", resolutionDegree));
        lcProperties.put("spatialResolution", String.valueOf((int) (METER_PER_DEGREE_At_EQUATOR * resolutionDegree)));
        ReferencedEnvelope regionEnvelope = getRegionEnvelope();
        if (PlanetaryGridName.REGULAR_GAUSSIAN_GRID.equals(getGridName())) {
            double newEast = (regionEnvelope.getMaximum(0) + 360) % 360;
            double newWest = (regionEnvelope.getMinimum(0) + 360) % 360;
            double south = regionEnvelope.getMinimum(1);
            double north = regionEnvelope.getMaximum(1);
            regionEnvelope = new ReferencedEnvelope(newEast, newWest, north, south, DefaultGeographicCRS.WGS84);
        }
        if (regionEnvelope != null) {
            lcProperties.put("latMin", String.valueOf(regionEnvelope.getMinimum(1)));
            lcProperties.put("latMax", String.valueOf(regionEnvelope.getMaximum(1)));
            lcProperties.put("lonMin", String.valueOf(regionEnvelope.getMinimum(0)));
            lcProperties.put("lonMax", String.valueOf(regionEnvelope.getMaximum(0)));

        } else {
            lcProperties.put("latMin", globalAttributes.getAttributeString("geospatial_lat_min"));
            lcProperties.put("latMax", globalAttributes.getAttributeString("geospatial_lat_max"));
            lcProperties.put("lonMin", globalAttributes.getAttributeString("geospatial_lon_min"));
            lcProperties.put("lonMax", globalAttributes.getAttributeString("geospatial_lon_max"));
        }
    }

    protected void addGridNameToLcProperties(String planetaryGridClassName) {
        final String gridName;
        int numRows = getNumRows();
        if (planetaryGridClassName.equals(RegularGaussianGrid.class.getName())) {
            gridName = "Regular gaussian grid (N" + numRows / 2 + ")";
            getLcProperties().put("grid_name", gridName);
        } else if (planetaryGridClassName.equals(PlateCarreeGrid.class.getName())) {
            getLcProperties().put("grid_name", String.format("Geographic lat lon grid (cell size: %.6f degree)", 180.0 / numRows));
        } else {
            throw new OperatorException("The grid '" + planetaryGridClassName + "' is not a valid grid.");
        }
    }

    protected void addAggregationTypeToLcProperties(String type) {
        lcProperties.put("aggregationType", type);
    }

    protected String getPlanetaryGridClassName() {
        PlanetaryGridName gridName = getGridName();
        if (PlanetaryGridName.GEOGRAPHIC_LAT_LON.equals(gridName)) {
            return PlateCarreeGrid.class.getName();
        } else if (PlanetaryGridName.REGULAR_GAUSSIAN_GRID.equals(gridName)) {
            return RegularGaussianGrid.class.getName();  // using this -180 to 180 grid instead of the one in BEAM which is from 0 to 360
//            return RegularGaussianGrid.class.getName();
//        } else if (PlanetaryGridName.REDUCED_GAUSSIAN_GRID.equals(gridName)) { // not supported yet
//            return ReducedGaussianGrid.class.getName();
        } else {
            return SEAGrid.class.getName();
        }
    }

    protected Product createSubset(Product source, ReferencedEnvelope regionEnvelope) {
        ReferencedEnvelope envelopeCopy = new ReferencedEnvelope(regionEnvelope);
        // work on the copy to prevent altering the original envelope
        envelopeCopy.expandBy(getTargetSpatialResolution() * 5);
        double north = envelopeCopy.getMaximum(1);
        double east = envelopeCopy.getMaximum(0);
        double south = envelopeCopy.getMinimum(1);
        double west = envelopeCopy.getMinimum(0);
        source = LcHelper.createProductSubset(source, north, east, south, west, getRegionIdentifier());
        return source;
    }

    private float getTargetSpatialResolution() {
        return 180.0f / getNumRows();
    }

}
