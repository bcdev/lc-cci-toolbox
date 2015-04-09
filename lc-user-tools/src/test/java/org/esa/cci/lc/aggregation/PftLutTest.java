package org.esa.cci.lc.aggregation;

import org.junit.Test;

import java.io.StringReader;

import static org.junit.Assert.*;

public class PftLutTest {

    private static final String TEST_PFT_WITHOUT_COMMENT = "class|PFT_1|PFT_2|PFT_3|PFT_4\n" +
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

    private static final String TEST_PFT_WITH_COMMENT = "# This ia a comment\n" +
                                                        "class|PFT_1|PFT_2|PFT_3|PFT_4\n" +
                                                        "0|7.6| 90  |     |  2.4\n" +
                                                        "10|   |  60 |   40|     \n" +
                                                        "11|||| \n" +
                                                        "12|||| \n" +
                                                        "20|   |     | 100 |     \n" +
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

    @Test
    public void testLutWithoutComment() throws Exception {
        PftLut pftLut = PftLut.load(new StringReader(TEST_PFT_WITHOUT_COMMENT));
        checkLUT(pftLut);
    }

    @Test
    public void testLutWithComment() throws Exception {
        PftLut pftLut = PftLut.load(new StringReader(TEST_PFT_WITH_COMMENT));
        checkLUT(pftLut);
        assertEquals("This ia a comment", pftLut.getComment());
    }

    private void checkLUT(PftLut pftLut) {
        assertNotNull(pftLut);

        String[] pftNames = pftLut.getPFTNames();
        assertEquals("PFT_1", pftNames[0]);
        assertEquals("PFT_4", pftNames[3]);

        float[][] factors = pftLut.getConversionFactors();
        assertEquals(0.076f, factors[0][0], 0.0f);
        assertEquals(0.9f, factors[0][1], 0.0f);
        assertEquals(Float.NaN, factors[0][2], 0.0f);
        assertEquals(0.024f, factors[0][3], 0.0f);
        assertEquals(Float.NaN, factors[1][0], 0.0f);
        assertEquals(0.6f, factors[1][1], 0.0f);
        assertEquals(0.4f, factors[1][2], 0.0f);
        assertEquals(Float.NaN, factors[1][3], 0.0f);
        assertEquals(Float.NaN, factors[2][0], 0.0f);
        assertEquals(Float.NaN, factors[2][1], 0.0f);
        assertEquals(1.0f, factors[4][2], 0.0f);
        assertEquals(Float.NaN, factors[2][3], 0.0f);
    }


}
