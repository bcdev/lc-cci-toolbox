package org.esa.cci.lc.aggregation;

import org.esa.beam.binning.PlanetaryGrid;

import java.awt.geom.Rectangle2D;

/**
 * @author Marco Peters
 */
class FractionalAreaCalculator {

    private final double deltaGridLat;
    private final double deltaMapLat;
    private final double deltaMapLon;
    private final PlanetaryGrid planetaryGrid;

    public FractionalAreaCalculator(PlanetaryGrid planetaryGrid, int mapWidth, int mapHeight) {
        this.planetaryGrid = planetaryGrid;
        deltaGridLat = 180.0 / planetaryGrid.getNumRows();
        deltaMapLat = 180.0 / mapHeight;
        deltaMapLon = 360.0 / mapWidth;
    }

    public double calculate(double longitude, double latitude, long binIndex) {
        int rowIndex = planetaryGrid.getRowIndex(binIndex);
        double[] binCenterLatLon = planetaryGrid.getCenterLatLon(binIndex);
        double binCenterLon = binCenterLatLon[1];
        double binCenterLat = binCenterLatLon[0];
        double deltaGridLon = 360.0 / planetaryGrid.getNumCols(rowIndex);
        Rectangle2D.Double binRect = createRect(binCenterLon, binCenterLat, deltaGridLon, deltaGridLat);
        Rectangle2D.Double obsRect = createRect(longitude, latitude, deltaMapLon, deltaMapLat);
        return calcFraction(binRect, obsRect);
    }

    private Rectangle2D.Double createRect(double binCenterLon, double binCenterLat, double deltaGridLon,
                                          double deltaGridLat1) {
        Rectangle2D.Double binRect = new Rectangle2D.Double();
        binRect.setFrameFromCenter(binCenterLon, binCenterLat, binCenterLon + deltaGridLon / 2.0,
                                   binCenterLat + deltaGridLat1 / 2.0);
        return binRect;
    }

    public static double calcFraction(Rectangle2D binRect, Rectangle2D obsRect) {
        Rectangle2D intersection = binRect.createIntersection(obsRect);
        double intersectionArea = intersection.getWidth() * intersection.getHeight();
        double binArea = binRect.getWidth() * binRect.getHeight();
        return intersectionArea / binArea;
    }
}
