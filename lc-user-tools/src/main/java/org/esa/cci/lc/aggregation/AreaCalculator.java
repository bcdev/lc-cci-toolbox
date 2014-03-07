package org.esa.cci.lc.aggregation;

/**
 * @author Marco Peters
 */
public interface AreaCalculator {

    double calculate(double longitude, double latitude, long binIndex);
}
