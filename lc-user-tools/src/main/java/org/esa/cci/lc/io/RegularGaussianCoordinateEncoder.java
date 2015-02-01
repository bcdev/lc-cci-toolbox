package org.esa.cci.lc.io;

import org.esa.beam.binning.PlanetaryGrid;

/**
 * @author Marco Peters
 */
class RegularGaussianCoordinateEncoder extends AbstractCoordinateEncoder {

    public RegularGaussianCoordinateEncoder(PlanetaryGrid planetaryGrid) {
        super(planetaryGrid);
    }

    @Override
    protected float[] getLonValues(int sceneWidth) {
        final float[] lons = new float[sceneWidth];

        final int firstBinIndex = (int) planetaryGrid.getFirstBinIndex(0);
        for (int i = 0; i < sceneWidth; i++) {
            lons[i] = ((float) planetaryGrid.getCenterLatLon(firstBinIndex + i)[1] + 360.0f) % 360.0f;
        }
        return lons;
    }

    @Override
    protected float[] getLatValues(int sceneHeight) {
        final float[] lats = new float[sceneHeight];
        for (int i = 0; i < sceneHeight; i++) {
            lats[i] = (float) planetaryGrid.getCenterLat(i);
        }
        return lats;
    }
}
