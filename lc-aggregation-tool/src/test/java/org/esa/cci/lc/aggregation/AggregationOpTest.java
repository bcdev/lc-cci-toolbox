package org.esa.cci.lc.aggregation;

import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.OperatorException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * @author Marco Peters
 */
public class AggregationOpTest {

    private static AggregationOp.Spi aggregationSpi;

    @BeforeClass
    public static void beforeClass() {
        aggregationSpi = new AggregationOp.Spi();
        GPF.getDefaultInstance().getOperatorSpiRegistry().addOperatorSpi(aggregationSpi);
    }

    @AfterClass
    public static void afterClass() {
        GPF.getDefaultInstance().getOperatorSpiRegistry().removeOperatorSpi(aggregationSpi);
    }

    @Test
    public void testDefaultValues() {
        AggregationOp aggrOp = (AggregationOp) aggregationSpi.createOperator();
        assertEquals(ProjectionMethod.GAUSSIAN_GRID, aggrOp.getProjectionMethod());
        assertEquals(0.1, aggrOp.getPixelSizeX(), 1.0e-8);
        assertEquals(0.1, aggrOp.getPixelSizeY(), 1.0e-8);
        assertEquals(-15.0, aggrOp.getWestBound(), 1.0e-8);
        assertEquals(30.0, aggrOp.getEastBound(), 1.0e-8);
        assertEquals(75.0, aggrOp.getNorthBound(), 1.0e-8);
        assertEquals(35.0, aggrOp.getSouthBound(), 1.0e-8);
        assertTrue(aggrOp.isOutputMajorityClasses());
        assertEquals(5, aggrOp.getNumberOfMajorityClasses());
        assertTrue(aggrOp.isOutputPFTClasses());
    }

    @Test
    public void testWestEastBound() {
        AggregationOp aggrOp = (AggregationOp) aggregationSpi.createOperator();
        aggrOp.setWestBound(10);
        aggrOp.setEastBound(3);
        try {
            aggrOp.initialize();
        } catch (OperatorException oe) {
            String message = oe.getMessage().toLowerCase();
            assertTrue(message.contains("west bound"));
            assertTrue(message.contains("east bound"));
        }
    }

    @Test
    public void testNorthSouthBound() {
        AggregationOp aggrOp = (AggregationOp) aggregationSpi.createOperator();
        aggrOp.setNorthBound(30);
        aggrOp.setSouthBound(70);
        try {
            aggrOp.initialize();
        } catch (OperatorException oe) {
            String message = oe.getMessage().toLowerCase();
            assertTrue(message.contains("north bound"));
            assertTrue(message.contains("south bound"));
        }
    }

    @Test
    public void testNoOutputClassesSelected() {
        AggregationOp aggrOp = (AggregationOp) aggregationSpi.createOperator();
        aggrOp.setOutputMajorityClasses(false);
        aggrOp.setOutputPFTClasses(false);
        try {
            aggrOp.initialize();
        } catch (OperatorException oe) {
            String message = oe.getMessage().toLowerCase();
            assertTrue(message.contains("and/or"));
            assertTrue(message.contains("classes"));
        }
    }

}