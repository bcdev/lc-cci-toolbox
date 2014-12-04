package org.esa.cci.lc;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.Tile;

import java.awt.Rectangle;
import java.awt.image.Raster;

class MerisFlagDetector implements FlagDetector {

    private int[] data;
    private int roiWidth;

    MerisFlagDetector(Tile flagTile, Rectangle roi) {
        data = flagTile.getSamplesInt();
        roiWidth = roi.width;
    }

    @Override
    public boolean isLand(int x, int y) {
        final int sample = data[y * roiWidth + x];
        return (sample & 128) != 0;
    }


    @Override
    public boolean isClearLand(int x, int y) {
        final int sample = data[y * roiWidth + x];
        return (sample & 16) != 0 && (sample & 64) == 0;
    }


    @Override
    public boolean isInvalid(int x, int y) {
        final int sample = data[y * roiWidth + x];
        return (sample & 1) != 0;
    }


}
