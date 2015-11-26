package org.esa.cci.lc.aggregation;

import org.junit.Test;

import java.io.InputStream;
import java.io.InputStreamReader;


public class Lccs2PftLutBuilder_WithAdditionalUserMap_Test {

    @Test
    public void testLutWithAdditionalUsermap() throws Exception {
        final InputStream resource = this.getClass().getResourceAsStream("TEST-LCCStoPFT_KG_Additional.csv");
        Lccs2PftLutBuilder lutBuilder = new Lccs2PftLutBuilder();
        lutBuilder = lutBuilder.useAdditionalUserMap(new InputStreamReader(resource));

    }

}