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
        FractionalAreaCalculator areaCalculator = new FractionalAreaCalculator(SEAGrid.RE, 2160, 4320, 2160);
        double area;

        area = areaCalculator.calculate(0, 0, 4320);
        assertEquals(4320.0 / 4320.0, area, 1.0e-6);

        area = areaCalculator.calculate(90, 90, 3);
        assertEquals(3.0 / 4320.0, area, 1.0e-6);

        area = areaCalculator.calculate(-90, -90, 3);
        assertEquals(3.0 / 4320.0, area, 1.0e-6);
    }

    @Test
    public void testCalculate_WithDifferentSize() throws Exception {
        FractionalAreaCalculator areaCalculator = new FractionalAreaCalculator(SEAGrid.RE, 2160, 7000, 3500);
        double area;

        area = areaCalculator.calculate(0, 0, 4320);
        assertEquals(4320.0 / 7000.0 * 4320.0 / 7000.0, area, 1.0e-6);

        area = areaCalculator.calculate(90, 90, 3);
        assertEquals(4320.0 / 7000.0 * 3.0 / 7000.0, area, 1.0e-6);

        area = areaCalculator.calculate(-90, -90, 3);
        assertEquals(4320.0 / 7000.0 * 3.0 / 7000.0, area, 1.0e-6);
    }
}
