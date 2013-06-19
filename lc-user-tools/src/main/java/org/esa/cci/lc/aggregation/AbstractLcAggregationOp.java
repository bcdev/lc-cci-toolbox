package org.esa.cci.lc.aggregation;

import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;

import java.io.File;
import java.util.HashMap;

/**
 * @author Marco Peters
 */
public abstract class AbstractLcAggregationOp extends Operator {

    private static final int METER_PER_DEGREE = 111300;

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
        lcProperties.put("spatialResolutionDegrees", String.format("%.6f", resolutionDegree));
        lcProperties.put("spatialResolution", String.valueOf((int) (METER_PER_DEGREE * resolutionDegree)));
        lcProperties.put("temporalCoverageYears", String.valueOf(globalAttributes.getAttributeString("time_coverage_duration").charAt(1)));
        lcProperties.put("temporalResolution", String.valueOf(globalAttributes.getAttributeString("time_coverage_resolution").charAt(1)));
        lcProperties.put("startTime", globalAttributes.getAttributeString("time_coverage_start"));
        lcProperties.put("endTime", globalAttributes.getAttributeString("time_coverage_end"));
        lcProperties.put("version", globalAttributes.getAttributeString("product_version"));
        lcProperties.put("latMin", globalAttributes.getAttributeString("geospatial_lat_min"));
        lcProperties.put("latMax", globalAttributes.getAttributeString("geospatial_lat_max"));
        lcProperties.put("lonMin", globalAttributes.getAttributeString("geospatial_lon_min"));
        lcProperties.put("lonMax", globalAttributes.getAttributeString("geospatial_lon_max"));
    }
}
