package org.esa.cci.lc.aggregation;

import org.esa.beam.binning.support.SEAGrid;

import java.awt.geom.Rectangle2D;

/**
 * @author Marco Peters
 */
class FractionalAreaCalculator {

    private final double deltaGridLat;
    private final double deltaMapLat;
    private final double deltaMapLon;
    private final SEAGrid seaGrid;

    public FractionalAreaCalculator(SEAGrid seaGrid, int mapWidth, int mapHeight) {
        this.seaGrid = seaGrid;
        deltaGridLat = 180.0 / seaGrid.getNumRows();
        deltaMapLat = 180.0 / mapHeight;
        deltaMapLon = 360.0 / mapWidth;
    }

    public double calculate(double longitude, double latitude, long binIndex) {
        int rowIndex = seaGrid.getRowIndex(binIndex);
        long firstBinIndex = seaGrid.getFirstBinIndex(rowIndex);
        int colInRowIndex = (int) (binIndex - firstBinIndex);
        double binCenterLon = seaGrid.getCenterLon(rowIndex, colInRowIndex);
        double binCenterLat = seaGrid.getCenterLat(rowIndex);
        double deltaGridLon = 360.0 / seaGrid.getNumCols(rowIndex);
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
