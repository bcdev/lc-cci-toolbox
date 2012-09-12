package org.esa.cci.lc.aggregation;

import org.esa.beam.binning.support.SEAGrid;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Marco Peters
 */
public class FractionalAreaCalculatorTest {

    @Test
    public void testCalculate_WhenGridAndMapHaveSameSize() throws Exception {
        FractionalAreaCalculator fractionCalculator = new FractionalAreaCalculator(SEAGrid.RE, 2160, 4320, 2160);
        double area;

        // observation has the same size as the bin
        area = fractionCalculator.calculate(0, 0, 4320);
        assertEquals(1.0 / 1.0, area, 1.0e-6);

        // 1440 (third of 4320) observations are covered by one bin
        area = fractionCalculator.calculate(90, 90, 3);
        assertEquals(1.0 / 1440.0, area, 1.0e-6);

        // 1440 (third of 4320) observations are covered by one bin
        area = fractionCalculator.calculate(-90, -90, 3);
        assertEquals(1.0 / 1440.0, area, 1.0e-6);
    }

    @Test
    public void testCalculate_WithDifferentSize() throws Exception {
        FractionalAreaCalculator areaCalculator = new FractionalAreaCalculator(SEAGrid.RE, 2160, 7000, 3500);
        double area;

        area = areaCalculator.calculate(0, 0, 4320);
        assertEquals(4320.0 / 7000.0 * 4320.0 / 7000.0, area, 1.0e-6);

        // 3 bins cover 7000 pixels in width
        // one bin height covers one-2160th of pole to pole distance and one observation height covers one-3500th of
        // pole to pole distance  --> one observation covers 2160/3500 of one bin in height
        area = areaCalculator.calculate(90, 90, 3);
        assertEquals(3.0 / 7000.0 * 2160.0 / 3500.0, area, 1.0e-6);

        area = areaCalculator.calculate(-90, -90, 3);
        assertEquals(3.0 / 7000.0 * 2160.0 / 3500.0, area, 1.0e-6);
    }

    @Test
    public void testThatFractionalInputPixelsAreConsidered() throws Exception {
        FractionalAreaCalculator fractionCalculator = new FractionalAreaCalculator(SEAGrid.RE, 10, 20, 10);
        // First row has 3 bins
        SEAGrid seaGrid = new SEAGrid(10);

        // retrieve lat/lon of first bin
        double binLat = seaGrid.getCenterLat(0);
        double binLon = seaGrid.getCenterLon(0, 0);

        // todo - longitude of the observation is missing
        double fraction = fractionCalculator.calculate(binLat, binLat, 3);
        assertEquals(1.0 / (20.0 / 3.0), fraction, 1.0e-6);

    }
}
