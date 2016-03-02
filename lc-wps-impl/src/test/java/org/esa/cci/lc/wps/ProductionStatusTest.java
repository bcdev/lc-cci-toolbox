package org.esa.cci.lc.wps;

import static org.junit.Assert.*;

import org.junit.*;

/**
 * @author hans
 */
public class ProductionStatusTest {

    @Test
    public void testNewProductionStatus() throws Exception {
        ProductionStatus productionStatus = new ProductionStatus("jobId", ProductionState.SUCCESSFUL, 1, "no message", null);

        System.out.println(productionStatus.getState().toString());

    }
}