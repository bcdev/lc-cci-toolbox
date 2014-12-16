package org.esa.cci.lc.aggregation;

import org.esa.beam.binning.PlanetaryGrid;
import org.esa.beam.binning.support.RegularGaussianGrid;
import org.esa.beam.binning.support.SEAGrid;
import org.junit.Test;

import java.awt.*;
import java.awt.geom.Rectangle2D;

import static org.junit.Assert.assertEquals;

/**
 * @author Marco Peters
 */
public class FractionalAreaCalculatorTest {

    @Test
    public void testCalculate_WhenGridAndMapHaveSameSize() throws Exception {
        PlanetaryGrid planetaryGrid = new SEAGrid(2160);
        AreaCalculator fractionCalculator = new FractionalAreaCalculator(planetaryGrid, 4320, 2160);
        double fraction;

        // observation has the same size as the bin
        fraction = calcFractionForLonLat(0.00001, 0.00001, planetaryGrid, fractionCalculator);
        assertEquals(1.0 / 1.0, fraction, 1.0e-6);

        // 1440 (third of 4320) observations are covered by one bin
        fraction = calcFractionForLonLat(90, 90, planetaryGrid, fractionCalculator);
        assertEquals(1.0 / 1440.0, fraction, 1.0e-6);

        // 1440 (third of 4320) observations are covered by one bin
        fraction = calcFractionForLonLat(-90, -90, planetaryGrid, fractionCalculator);

        assertEquals(1.0 / 1440.0, fraction, 1.0e-6);
    }

    @Test
    public void testCalculate_WithDifferentSize() throws Exception {
        PlanetaryGrid planetaryGrid = new SEAGrid(2160);
        AreaCalculator fractionCalculator = new FractionalAreaCalculator(planetaryGrid, 7000, 3500);
        double fraction;

        fraction = calcFractionForLonLat(0.001, 0.001, planetaryGrid, fractionCalculator);
        assertEquals(4320.0 / 7000.0 * 4320.0 / 7000.0, fraction, 1.0e-6);

        // 3 bins cover 7000 pixels in width
        // one bin height covers one-2160th of pole to pole distance and one observation height covers one-3500th of
        // pole to pole distance  --> one observation covers 2160/3500 of one bin in height
        fraction = calcFractionForLonLat(90, 90, planetaryGrid, fractionCalculator);
        assertEquals(3.0 / 7000.0 * 2160.0 / 3500.0, fraction, 1.0e-6);

        fraction = calcFractionForLonLat(-90, -90, planetaryGrid, fractionCalculator);
        assertEquals(3.0 / 7000.0 * 2160.0 / 3500.0, fraction, 1.0e-6);
    }

    @Test
    public void testThatFractionalInputPixelsAreConsidered() throws Exception {
        PlanetaryGrid planetaryGrid = new SEAGrid(10);
        AreaCalculator fractionCalculator = new FractionalAreaCalculator(planetaryGrid, 20, 10);
        // First row has 3 bins

        // retrieve lat/lon of first bin
        double[] centerLatLon = planetaryGrid.getCenterLatLon(0);
        double obsLat = centerLatLon[0];
        double obsLon = centerLatLon[1];

        double fraction = calcFractionForLonLat(obsLon, obsLat, planetaryGrid, fractionCalculator);
        assertEquals(1.0 / (20.0 / 3.0), fraction, 1.0e-6);
    }

    @Test
    public void testCalculate_RegularGaussianGrid() throws Exception {
        PlanetaryGrid planetaryGrid = new RegularGaussianGrid(160);
        AreaCalculator fractionCalculator = new FractionalAreaCalculator(planetaryGrid, 129600, 64800);
        double fraction;

        // observation is completely in bin cell
        fraction = calcFractionForLonLat(60.2, 10.4, planetaryGrid, fractionCalculator);
        assertEquals(Math.pow(320.0 / 129600.0, 2.0), fraction, 1.0e-6);
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

    @Test
    public void testFractionCalculation_WhenCrossingAntiMeridian() throws Exception {
        Rectangle2D binRect = new Rectangle.Double(179, 68, 3, 2);
        Rectangle2D obsRect = new Rectangle.Double(-179, 68, 1, 1);
        assertEquals(1.0 / 6.0, FractionalAreaCalculator.calcFraction(binRect, obsRect), 1.0e-6);

        binRect = new Rectangle.Double(-179, 68, 1, 2);
        obsRect = new Rectangle.Double(179, 68, 3, 1);
        assertEquals(1.0 / 2.0, FractionalAreaCalculator.calcFraction(binRect, obsRect), 1.0e-6);
    }

    @Test
    public void testFractionCalculation_WhenCrossingMeridian() throws Exception {
        Rectangle2D binRect = new Rectangle.Double(-0.2, 68, 0.4, 1);
        Rectangle2D obsRect = new Rectangle.Double(0.1, 68, 0.1, 1);
        assertEquals(1.0 / 4.0, FractionalAreaCalculator.calcFraction(binRect, obsRect), 1.0e-6);

        binRect = new Rectangle.Double(0.2, 68, 0.4, 1);
        obsRect = new Rectangle.Double(-0.5, 68, 2, 1);
        assertEquals(1.0, FractionalAreaCalculator.calcFraction(binRect, obsRect), 1.0e-6);
    }

    private double calcFractionForLonLat(double lon, double lat, PlanetaryGrid grid, AreaCalculator areaCalculator) {
        double fraction;
        long binIndex = grid.getBinIndex(lat, lon);
        double[] centerLatLon = grid.getCenterLatLon(binIndex);
        fraction = areaCalculator.calculate(centerLatLon[1], centerLatLon[0], binIndex);
        return fraction;
    }

}
