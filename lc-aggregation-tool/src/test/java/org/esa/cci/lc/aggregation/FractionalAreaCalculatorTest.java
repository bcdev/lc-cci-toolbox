package org.esa.cci.lc.aggregation;

import org.esa.beam.binning.support.SEAGrid;
import org.junit.Test;

import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;

import static org.junit.Assert.*;

/**
 * @author Marco Peters
 */
public class FractionalAreaCalculatorTest {

    private FractionalAreaCalculator fractionCalculator;
    private SEAGrid seaGrid;

    @Test
    public void testCalculate_WhenGridAndMapHaveSameSize() throws Exception {
        seaGrid = new SEAGrid(2160);
        fractionCalculator = new FractionalAreaCalculator(seaGrid, 4320, 2160);
        double fraction;

        // observation has the same size as the bin
        fraction = calcFractionForLonLat(0.00001, 0.00001);
        assertEquals(1.0 / 1.0, fraction, 1.0e-6);

        // 1440 (third of 4320) observations are covered by one bin
        fraction = calcFractionForLonLat(90, 90);
        assertEquals(1.0 / 1440.0, fraction, 1.0e-6);

        // 1440 (third of 4320) observations are covered by one bin
        fraction = calcFractionForLonLat(-90, -90);

        assertEquals(1.0 / 1440.0, fraction, 1.0e-6);
    }

    private double calcFractionForLonLat(double lon, double lat) {
        double fraction;
        long binIndex = seaGrid.getBinIndex(lat, lon);
        double[] centerLatLon = seaGrid.getCenterLatLon(binIndex);
        fraction = fractionCalculator.calculate(centerLatLon[1], centerLatLon[0], binIndex);
        return fraction;
    }

    @Test
    public void testCalculate_WithDifferentSize() throws Exception {
        seaGrid = new SEAGrid(2160);
        fractionCalculator = new FractionalAreaCalculator(seaGrid, 7000, 3500);
        double fraction;

        fraction = calcFractionForLonLat(0.001, 0.001);
        assertEquals(4320.0 / 7000.0 * 4320.0 / 7000.0, fraction, 1.0e-6);

        // 3 bins cover 7000 pixels in width
        // one bin height covers one-2160th of pole to pole distance and one observation height covers one-3500th of
        // pole to pole distance  --> one observation covers 2160/3500 of one bin in height
        fraction = calcFractionForLonLat(90, 90);
        assertEquals(3.0 / 7000.0 * 2160.0 / 3500.0, fraction, 1.0e-6);

        fraction = calcFractionForLonLat(-90, -90);
        assertEquals(3.0 / 7000.0 * 2160.0 / 3500.0, fraction, 1.0e-6);
    }

    @Test
    public void testThatFractionalInputPixelsAreConsidered() throws Exception {
        seaGrid = new SEAGrid(10);
        fractionCalculator = new FractionalAreaCalculator(new SEAGrid(10), 20, 10);
        // First row has 3 bins

        // retrieve lat/lon of first bin
        double obsLat = seaGrid.getCenterLat(0);
        double obsLon = seaGrid.getCenterLon(0, 0);

        double fraction = calcFractionForLonLat(obsLon, obsLat);
        assertEquals(1.0 / (20.0 / 3.0), fraction, 1.0e-6);

    }

    @Test
    public void testFractionCalculation() throws Exception {

        Rectangle2D binRect = new Rectangle.Double(118, 68, 2, 2);

        Rectangle2D obsRect = new Rectangle.Double(119, 68, 2, 2);
        assertEquals(0.5, FractionalAreaCalculator.calcFraction(binRect, obsRect), 1.0e-6);

        obsRect = new Rectangle.Double(118.5, 68, 1, 2);
        assertEquals(0.5, FractionalAreaCalculator.calcFraction(binRect, obsRect), 1.0e-6);

        obsRect = new Rectangle.Double(116.5, 68, 2, 2);
        assertEquals(0.25, FractionalAreaCalculator.calcFraction(binRect, obsRect), 1.0e-6);

        obsRect = new Rectangle.Double(117, 68, 5, 2);
        assertEquals(1.0, FractionalAreaCalculator.calcFraction(binRect, obsRect), 1.0e-6);

        obsRect = new Rectangle.Double(117, 67, 2, 2);
        assertEquals(0.25, FractionalAreaCalculator.calcFraction(binRect, obsRect), 1.0e-6);
    }
}
