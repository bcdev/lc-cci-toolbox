package org.esa.cci.lc.aggregation;

import org.junit.Test;

import java.io.StringReader;

import static org.junit.Assert.*;

public class PftLutTest {

    private static final String TEST_STRING = "PFT_1|PFT_2|PFT_3|PFT_4\n" +
                                              "7.6| 90  |     |  2.4\n" +
                                              "   |  60 |   40|     \n" +
                                              "   |     | 100 |     ";


    @Test
    public void testLut() throws Exception {
        PftLut pftLut = PftLut.load(new StringReader(TEST_STRING));
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
        assertEquals(1.0f, factors[2][2], 0.0f);
        assertEquals(Float.NaN, factors[2][3], 0.0f);
    }


}
