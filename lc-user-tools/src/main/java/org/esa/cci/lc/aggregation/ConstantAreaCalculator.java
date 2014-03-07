package org.esa.cci.lc.aggregation;

/**
 * @author Marco Peters
 */
public class ConstantAreaCalculator implements AreaCalculator {

    @Override
    public double calculate(double longitude, double latitude, long binIndex) {
        return 1;
    }
}
