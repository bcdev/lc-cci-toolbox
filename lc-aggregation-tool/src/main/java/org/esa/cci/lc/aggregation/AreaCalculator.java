package org.esa.cci.lc.aggregation;

/**
 * Interface for calculating the fractional coverage of an observation at a bin cell.
 *
 * @author Marco Peters
 */
interface AreaCalculator {

    // todo (mp) - consider the case if the pixel is only partly in the bin
    double calculate(double observationLat, double gridLat, double numGridCols);
}
