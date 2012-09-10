package org.esa.cci.lc.aggregation;

import static java.lang.Math.*;

/**
 * @author Marco Peters
 */
class FractionalAreaCalculator implements AreaCalculator {

    private final double earthRadius;
    private final double deltaGridLat;
    private final double deltaMapLat;
    private final double deltaMapLon;

    public FractionalAreaCalculator(double earthRadius, int gridRowCount, int mapWidth, int mapHeight) {
        this.earthRadius = earthRadius;
        deltaGridLat = 180.0 / gridRowCount;
        deltaMapLat = 180.0 / mapHeight;
        deltaMapLon = 360.0 / mapWidth;
    }

    // todo (mp) - consider the case if the pixel is only partly in the bin
    @Override
    public double calculate(double observationLat, double gridLat, double numGridCols) {
        double observationArea = computeArea(observationLat, deltaMapLat, deltaMapLon);
        double deltaGridLon = 360.0 / numGridCols;
        double binArea = computeArea(gridLat, deltaGridLat, deltaGridLon);
        return observationArea / binArea;
    }

    private double computeArea(double latitude, double deltaLat, double deltaLon) {
        double r2 = earthRadius * cos(toRadians(latitude));
        double a = r2 * toRadians(deltaLon);
        double b = earthRadius * toRadians(deltaLat);
        return a * b;
    }

}
