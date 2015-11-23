package org.esa.cci.lc.aggregation;

import org.junit.Test;

import java.io.IOException;
import java.io.StringReader;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class Lccs2PftLutBuilderTest {

    private static final String DEFAULT_TEST_TABLE =
            "class|PFT_1|PFT_2|PFT_3|PFT_4\n" +
                    "0|7.6| 90  |     |  2.4\n" +
                    "10|   |  60 |   40|     \n" +
                    "11||||\n" +
                    "12||||\n" +
                    "20|   |     | 100 |      \n" +
                    "30||||\n" +
                    "40||||\n" +
                    "50||||\n" +
                    "60||||\n" +
                    "61||||\n" +
                    "62||||\n" +
                    "70||||\n" +
                    "71||||\n" +
                    "72||||\n" +
                    "80||||\n" +
                    "81||||\n" +
                    "82||||\n" +
                    "90||||\n" +
                    "100||||\n" +
                    "110||||\n" +
                    "120||||\n" +
                    "121||||\n" +
                    "122||||\n" +
                    "130||||\n" +
                    "140||||\n" +
                    "150||||\n" +
                    "152||||\n" +
                    "153||||\n" +
                    "160||||\n" +
                    "170||||\n" +
                    "180||||\n" +
                    "190||||\n" +
                    "200||||\n" +
                    "201||||\n" +
                    "202||||\n" +
                    "210||||\n" +
                    "220||||\n";

    private static final String TEST_PFT_WITH_COMMENT =
            "# This ia a comment\n" +
                    DEFAULT_TEST_TABLE;

    @Test
    public void testLutWithoutComment() throws Exception {
        testPftLut(DEFAULT_TEST_TABLE, Lccs2PftLutBuilder.DEFAULT_SCALE_FACTOR);
    }

    @Test
    public void testLutWithComment() throws Exception {
        final Lccs2PftLut pftLut = testPftLut(TEST_PFT_WITH_COMMENT, Lccs2PftLutBuilder.DEFAULT_SCALE_FACTOR);
        assertEquals("This ia a comment", pftLut.getComment());
    }

    @Test
    public void testLutWithScaleFactor() throws Exception {
        testPftLut(DEFAULT_TEST_TABLE, 12.0f);
    }

    private Lccs2PftLut testPftLut(String conversionTable, float scaleFactor) throws IOException {
        Lccs2PftLutBuilder lutBuilder = new Lccs2PftLutBuilder();
        lutBuilder = lutBuilder.withLccs2PftTableReader(new StringReader(conversionTable));
        lutBuilder = lutBuilder.useScaleFactor(scaleFactor);
        Lccs2PftLut pftLut = lutBuilder.create();
        checkLUT(pftLut, scaleFactor);
        return pftLut;
    }

    private void checkLUT(Lccs2PftLut pftLut, float scaleFactor) {
        assertNotNull(pftLut);

        String[] pftNames = pftLut.getPFTNames();
        assertEquals("PFT_1", pftNames[0]);
        assertEquals("PFT_4", pftNames[3]);

        float[][] factors = pftLut.getConversionFactors();
        assertEquals(scaleFactor * 7.6f, factors[0][0], 01.0e-6);
        assertEquals(scaleFactor * 90f, factors[0][1], 01.0e-6);
        assertEquals(scaleFactor * Double.NaN, factors[0][2], 01.0e-6);
        assertEquals(scaleFactor * 2.4f, factors[0][3], 01.0e-6);
        assertEquals(scaleFactor * Double.NaN, factors[1][0], 01.0e-6);
        assertEquals(scaleFactor * 60.0f, factors[1][1], 01.0e-6);
        assertEquals(scaleFactor * 40.0, factors[1][2], 01.0e-6);
        assertEquals(scaleFactor * Double.NaN, factors[1][3], 01.0e-6);
        assertEquals(scaleFactor * Double.NaN, factors[2][0], 01.0e-6);
        assertEquals(scaleFactor * Double.NaN, factors[2][1], 01.0e-6);
        assertEquals(scaleFactor * 100.0f, factors[4][2], 01.0e-6);
        assertEquals(scaleFactor * Float.NaN, factors[2][3], 01.0e-6);
    }


}
