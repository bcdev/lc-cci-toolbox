package org.esa.cci.lc.aggregation;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;

import java.io.File;

/**
 * @author Marco Peters
 */
public abstract class AbstractLcAggregationOp extends Operator {

    @SourceProduct(description = "LC CCI map or conditions product.", optional = false)
    private Product sourceProduct;
    @Parameter(description = "The target directory.")
    private File targetDir;
    @Parameter(description = "Defines the grid for the target product.", notNull = true,
               valueSet = {"GEOGRAPHIC_LAT_LON", "REGULAR_GAUSSIAN_GRID"})
    private PlanetaryGridName gridName;
    @Parameter(defaultValue = "2160")
    private int numRows;

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
}
