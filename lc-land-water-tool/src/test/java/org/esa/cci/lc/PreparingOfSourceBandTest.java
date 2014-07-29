package org.esa.cci.lc;

import org.junit.Assert;

/**
 * Created with IntelliJ IDEA.
 * User: grit
 * Date: 30.01.13
 * Time: 11:21
 * To change this template use File | Settings | File Templates.
 */
public class PreparingOfSourceBandTest {

    @org.junit.Test
    public void testPreparedOfSourceBand() throws Exception {
        PreparingOfSourceBand preparingOfSourceBand = new PreparingOfSourceBand();
        int width = 2;
        int height = 3;
        int[] sourceData1 = {0, 256, 512, 0, 512, 512};
        int[] sourceData2 = {0, 127, 100, 100, 0, 0};
        preparingOfSourceBand.preparedOfSourceBand(sourceData1, sourceData2, width, height);

        Assert.assertEquals(5, sourceData1[0]);
        Assert.assertEquals(10, sourceData1[1]);
        Assert.assertEquals(5, sourceData1[2]);
        Assert.assertEquals(5, sourceData1[3]);
        Assert.assertEquals(5, sourceData1[4]);
        Assert.assertEquals(5, sourceData1[5]);

        Assert.assertEquals(10, sourceData2[0]);
        Assert.assertEquals(5, sourceData2[1]);
        Assert.assertEquals(5, sourceData2[2]);
        Assert.assertEquals(5, sourceData2[3]);
        Assert.assertEquals(10, sourceData2[4]);
        Assert.assertEquals(10, sourceData2[5]);
    }
}
