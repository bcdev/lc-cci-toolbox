package org.esa.cci.lc;

import org.esa.beam.framework.datamodel.Product;

import java.awt.Rectangle;
import java.awt.image.Raster;

class MerisFlagDetector implements FlagDetector {

    private Raster data;

    MerisFlagDetector(Product product, Rectangle roi) {
        data = product.getBand("l1_flags").getSourceImage().getData(roi);
    }

    @Override
    public boolean isLand(int x, int y) {
        return (data.getSample(x, y, 0) & 16) != 0;
    }

    @Override
    public boolean isInvalid(int x, int y) {
        return (data.getSample(x, y, 0) & 128) != 0;
    }

}


