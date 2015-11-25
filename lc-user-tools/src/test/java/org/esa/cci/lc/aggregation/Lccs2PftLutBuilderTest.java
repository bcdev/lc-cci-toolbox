package org.esa.cci.lc.aggregation;

import org.junit.Test;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;

import static org.esa.cci.lc.aggregation.Lccs2PftLutBuilder.DEFAULT_SCALE_FACTOR;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class Lccs2PftLutBuilderTest {

    private static final URL PFT_TEST_TABLE_DEFAULT = Lccs2PftLutBuilderTest.class.getResource("PFT_TEST_TABLE_DEFAULT.csv");

    @Test
    public void testLutWithoutComment() throws Exception {
        testPftLut(DEFAULT_SCALE_FACTOR, createStream(PFT_TEST_TABLE_DEFAULT));
    }

    private Reader createStream(URL resourceUrl) throws IOException {
        return new InputStreamReader(resourceUrl.openStream());
    }

    @Test
    public void testLutWithComment() throws Exception {
        URL withComment = Lccs2PftLutBuilderTest.class.getResource("PFT_TEST_TABLE_WITH_COMMENT.csv");
        final Lccs2PftLut pftLut = testPftLut(DEFAULT_SCALE_FACTOR, createStream(withComment));
        assertEquals("This ia a comment", pftLut.getComment());
    }

    @Test
    public void testLutWithScaleFactor() throws Exception {
        testPftLut(12.0f, createStream(PFT_TEST_TABLE_DEFAULT));
    }

    @Test
    public void testLutMissingClasses() throws Exception {
        URL missingClasses = Lccs2PftLutBuilderTest.class.getResource("PFT_TEST_TABLE_MISSING_CLASS.csv");
        try {
            testPftLut(1.0f, createStream(missingClasses));
            fail("Expected Exception. Class 72 is missing.");
        } catch (Lccs2PftLutException e) {
            assertTrue(e.getMessage().contains("72"));
        }
    }

    @Test
    public void testLutWRONGClasses() throws Exception {
        URL missingClasses = Lccs2PftLutBuilderTest.class.getResource("PFT_TEST_TABLE_WRONG_CLASS.csv");
        try {
            testPftLut(1.0f, createStream(missingClasses));
            fail("Expected Exception. Class 154 is unknown.");
        } catch (Lccs2PftLutException e) {
            assertTrue(e.getMessage().contains("154"));
        }
    }

    private Lccs2PftLut testPftLut(float scaleFactor, Reader reader) throws IOException, Lccs2PftLutException {
        Lccs2PftLutBuilder lutBuilder = new Lccs2PftLutBuilder();
        lutBuilder = lutBuilder.withLccs2PftTableReader(reader);
        lutBuilder = lutBuilder.useScaleFactor(scaleFactor);
        Lccs2PftLut pftLut = lutBuilder.create();
        checkDefaultLUT(pftLut, scaleFactor);
        return pftLut;
    }

    private void checkDefaultLUT(Lccs2PftLut pftLut, float scaleFactor) {
        assertNotNull(pftLut);

        String[] pftNames = pftLut.getPFTNames();
        assertEquals(4, pftNames.length);
        assertEquals("PFT_1", pftNames[0]);
        assertEquals("PFT_4", pftNames[3]);

        float[][] factors = pftLut.getConversionFactors();
        assertEquals(37, factors.length);
        assertEquals(4, factors[0].length);
        assertEquals(scaleFactor * 7.6f, factors[0][0], 01.0e-6);
        assertEquals(scaleFactor * 90f, factors[0][1], 01.0e-6);
        assertEquals(scaleFactor * Float.NaN, factors[0][2], 01.0e-6);
        assertEquals(scaleFactor * 2.4f, factors[0][3], 01.0e-6);
        assertEquals(scaleFactor * Float.NaN, factors[1][0], 01.0e-6);
        assertEquals(scaleFactor * 60.0f, factors[1][1], 01.0e-6);
        assertEquals(scaleFactor * 40.0, factors[1][2], 01.0e-6);
        assertEquals(scaleFactor * Float.NaN, factors[1][3], 01.0e-6);
        assertEquals(scaleFactor * Float.NaN, factors[2][0], 01.0e-6);
        assertEquals(scaleFactor * Float.NaN, factors[2][1], 01.0e-6);
        assertEquals(scaleFactor * Float.NaN, factors[2][2], 01.0e-6);
        assertEquals(scaleFactor * Float.NaN, factors[2][3], 01.0e-6);
        assertEquals(scaleFactor * 100.0f, factors[4][2], 01.0e-6);
    }


}
