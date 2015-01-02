package org.esa.cci.lc.aggregation;

import org.esa.beam.binning.PlanetaryGrid;
import org.esa.beam.binning.support.RegularGaussianGrid;

import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Marco Peters
 */
class FractionalAreaCalculator implements AreaCalculator {

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
        binRectanglesMap = new HashMap<>();
        deltaMapLat = mapResolutionX;
        deltaMapLon = mapResolutionY;
    }


    @Override
    public double calculate(double longitude, double latitude, long binIndex) {
        Rectangle2D.Double binRect = getBinRect(binIndex);
        Rectangle2D.Double obsRect = new Rectangle2D.Double();
        obsRect.setFrameFromDiagonal(longitude - deltaMapLon / 2.0,
                                     latitude + deltaMapLat / 2.0,
                                     longitude + deltaMapLon / 2.0,
                                     latitude - deltaMapLat / 2.0);

        return calcFraction(binRect, obsRect);
    }

    private Rectangle2D.Double getBinRect(long binIndex) {
        clearMapIfToBig(binRectanglesMap);
        Rectangle2D.Double binRect = binRectanglesMap.get(binIndex);
        if (binRect == null) {
            double[] binCenterLatLon = planetaryGrid.getCenterLatLon(binIndex);
            double binCenterLon = binCenterLatLon[1];
            double binCenterLat = binCenterLatLon[0];
            double maxLat;
            double minLat;
            int rowIndex = planetaryGrid.getRowIndex(binIndex);
            double deltaGridLon = 360.0 / planetaryGrid.getNumCols(rowIndex);
            if (planetaryGrid instanceof RegularGaussianGrid) {
                if (rowIndex == 0) {
                    maxLat = 90;
                } else {
                    maxLat = (planetaryGrid.getCenterLat(rowIndex - 1) + binCenterLat) / 2;
                }
                if (rowIndex == planetaryGrid.getNumRows() - 1) {
                    minLat = -90;
                } else {
                    minLat = (planetaryGrid.getCenterLat(rowIndex + 1) + binCenterLat) / 2;
                }
                binRect = new Rectangle2D.Double();
                binRect.setFrameFromDiagonal(binCenterLon - deltaGridLon / 2.0,
                                             maxLat,
                                             binCenterLon + deltaGridLon / 2.0,
                                             minLat);
            } else {
                binRect = new Rectangle2D.Double();
                binRect.setFrameFromDiagonal(binCenterLon - deltaGridLon / 2.0,
                                             binCenterLat + deltaGridLat / 2.0,
                                             binCenterLon + deltaGridLon / 2.0,
                                             binCenterLat - deltaGridLat / 2.0);
            }
            binRectanglesMap.put(binIndex, binRect);
        }
        return binRect;
    }

    private void clearMapIfToBig(Map<Long, Rectangle2D.Double> binRectanglesMap) {
        if (binRectanglesMap.size() > 5000) { // this test could be cleverer, but works right now
            binRectanglesMap.clear();
        }
    }

    static double calcFraction(Rectangle2D binRect, Rectangle2D obsRect) {
        Rectangle2D binRectangle;
        Rectangle2D obsRectangle;
        if (crossesAntiMeridian(binRect) || crossesAntiMeridian(obsRect)) {
            binRectangle = normalize(binRect);
            obsRectangle = normalize(obsRect);
        } else {
            binRectangle = binRect;
            obsRectangle = obsRect;
        }
        if (binRectangle.intersects(obsRectangle)) {
            Rectangle2D intersection = binRectangle.createIntersection(obsRectangle);
            double intersectionArea = intersection.getWidth() * intersection.getHeight();
            double binArea = binRectangle.getWidth() * binRectangle.getHeight();
            return intersectionArea / binArea;
        } else {
            return 0;
        }
    }

    private static boolean crossesAntiMeridian(Rectangle2D rect) {
        return rect.intersectsLine(180, 90, 180, -90) || rect.intersectsLine(-180, 90, -180, -90);
    }

    private static Rectangle2D normalize(Rectangle2D rect) {
        double rectMinX = rect.getMinX();
        double minX = (rectMinX + 360) % 360;
        double maxX = minX + rect.getWidth();
        double minY = rect.getMinY();
        double maxY = rect.getMaxY();
        Rectangle2D.Double targetRect = new Rectangle2D.Double();
        targetRect.setFrameFromDiagonal(minX, minY, maxX, maxY);
        return targetRect;
    }

}
