package org.esa.cci.lc.aggregation;

import org.esa.beam.binning.PlanetaryGrid;

import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Marco Peters
 */
class FractionalAreaCalculator {

    private final double deltaGridLat;
    private final double deltaMapLat;
    private final double deltaMapLon;
    private final PlanetaryGrid planetaryGrid;
    private final Map<Long, Rectangle2D.Double> binRectanglesMap;

    public FractionalAreaCalculator(PlanetaryGrid planetaryGrid, int mapWidth, int mapHeight) {
        this(planetaryGrid, 180.0 / mapHeight, 360.0 / mapWidth);
    }

    public FractionalAreaCalculator(PlanetaryGrid planetaryGrid, double mapResolutionX, double mapResolutionY) {
        this.planetaryGrid = planetaryGrid;
        deltaGridLat = 180.0 / planetaryGrid.getNumRows();
        binRectanglesMap = new HashMap<Long, Rectangle2D.Double>();
        deltaMapLat = mapResolutionX;
        deltaMapLon = mapResolutionY;
    }


    public double calculate(double longitude, double latitude, long binIndex) {
        Rectangle2D.Double binRect = getBinRect(binIndex);
        Rectangle2D.Double obsRect = createRect(longitude, latitude, deltaMapLon, deltaMapLat);
        return calcFraction(binRect, obsRect);
    }

    private Rectangle2D.Double getBinRect(long binIndex) {
        clearMapIfToBig(binRectanglesMap);
        Rectangle2D.Double binRect = binRectanglesMap.get(binIndex);
        if (binRect == null) {
            double[] binCenterLatLon = planetaryGrid.getCenterLatLon(binIndex);
            double binCenterLon = binCenterLatLon[1];
            double binCenterLat = binCenterLatLon[0];
            int rowIndex = planetaryGrid.getRowIndex(binIndex);
            double deltaGridLon = 360.0 / planetaryGrid.getNumCols(rowIndex);
            binRect = createRect(binCenterLon, binCenterLat, deltaGridLon, deltaGridLat);
            binRectanglesMap.put(binIndex, binRect);
        }
        return binRect;
    }

    private void clearMapIfToBig(Map<Long, Rectangle2D.Double> binRectanglesMap) {
        if (binRectanglesMap.size() > 5000) { // this test could be cleverer, but works right now
            binRectanglesMap.clear();
        }
    }

    private Rectangle2D.Double createRect(double binCenterLon, double binCenterLat, double deltaGridLon,
                                          double deltaGridLat) {
        Rectangle2D.Double binRect = new Rectangle2D.Double();
        binRect.setFrameFromCenter(binCenterLon, binCenterLat, binCenterLon + deltaGridLon / 2.0,
                                   binCenterLat + deltaGridLat / 2.0);
        return binRect;
    }

    public static double calcFraction(Rectangle2D binRect, Rectangle2D obsRect) {
        Rectangle2D intersection = binRect.createIntersection(obsRect);
        double intersectionArea = intersection.getWidth() * intersection.getHeight();
        double binArea = binRect.getWidth() * binRect.getHeight();
        return intersectionArea / binArea;
    }
}
