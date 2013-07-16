package org.esa.cci.lc.aggregation;

import org.esa.beam.binning.PlanetaryGrid;
import org.esa.beam.binning.support.PlateCarreeGrid;
import org.esa.beam.binning.support.RegularGaussianGrid;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.opengis.referencing.crs.GeographicCRS;

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

    private final HashMap<String, String> lcProperties;

    protected AbstractLcAggregationOp() {
        this.lcProperties = new HashMap<String, String>();
    }

    void ensureTargetDir() {
        if (targetDir == null) {
            final File fileLocation = getSourceProduct().getFileLocation();
            if (fileLocation != null) {
                targetDir = fileLocation.getParentFile();
            }
        }
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

    public HashMap<String, String> getLcProperties() {
        return lcProperties;
    }

    protected void validateInputSettings() {
        final GeoCoding sourceGC = sourceProduct.getGeoCoding();
        if (sourceGC == null) {
            throw new OperatorException("The source product must have a geo-coding.");
        }
        if (!(sourceGC.getMapCRS() instanceof GeographicCRS)) {
            throw new OperatorException("The geo-coding of the source product must be of type geographic.");
        }
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
        float resolutionDegree = 180.0f / getNumRows();
        String timeCoverageDuration = globalAttributes.getAttributeString("time_coverage_duration");
        String timeCoverageResolution = globalAttributes.getAttributeString("time_coverage_resolution");
        lcProperties.put("spatialResolutionDegrees", String.format("%.6f", resolutionDegree));
        lcProperties.put("spatialResolution", String.valueOf((int) (METER_PER_DEGREE_At_EQUATOR * resolutionDegree)));
        lcProperties.put("temporalCoverageYears", timeCoverageDuration.substring(1, timeCoverageDuration.length() - 1));
        lcProperties.put("temporalResolution", timeCoverageResolution.substring(1, timeCoverageResolution.length() - 1));
        lcProperties.put("startTime", globalAttributes.getAttributeString("time_coverage_start"));
        lcProperties.put("endTime", globalAttributes.getAttributeString("time_coverage_end"));
        lcProperties.put("version", globalAttributes.getAttributeString("product_version"));
        lcProperties.put("latMin", globalAttributes.getAttributeString("geospatial_lat_min"));
        lcProperties.put("latMax", globalAttributes.getAttributeString("geospatial_lat_max"));
        lcProperties.put("lonMin", globalAttributes.getAttributeString("geospatial_lon_min"));
        lcProperties.put("lonMax", globalAttributes.getAttributeString("geospatial_lon_max"));
    }

    protected void addGridNameToLcProperties(PlanetaryGrid planetaryGrid) {
        final String gridName;
        int numRows = getNumRows();
        if (planetaryGrid instanceof RegularGaussianGrid) {
            gridName = "Regular gaussian grid (N" + numRows / 2 + ")";
            getLcProperties().put("grid_name", gridName);
        } else if (planetaryGrid instanceof PlateCarreeGrid) {
            getLcProperties().put("grid_name", String.format("Geographic lat lon grid (cell size: %.6f degree)", 180.0 / numRows));
        } else {
            throw new OperatorException("The grid '" + planetaryGrid.getClass().getName() + "' is not a valid grid.");
        }
    }

    protected void addAggregationTypeToLcProperties(String type) {
        lcProperties.put("aggregationType", type);
    }
}
