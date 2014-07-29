package org.esa.cci.lc;

import org.junit.Test;

import static org.junit.Assert.*;

public class EdgeOperatorTest {

    @Test
    public void testComputeEdge() throws Exception {
        EdgeOperator edgeOperator = new EdgeOperator();
        int[] sourceData = {5, 5, 5, 5, 5, 10};
        byte[] result = edgeOperator.computeEdge(sourceData, 2, 3);

        assertEquals(0, result[0]);
        assertEquals(0, result[1]);
        assertEquals(1, result[2]);
        assertEquals(1, result[3]);
        assertEquals(1, result[4]);
        assertEquals(1, result[5]);
    }
}
