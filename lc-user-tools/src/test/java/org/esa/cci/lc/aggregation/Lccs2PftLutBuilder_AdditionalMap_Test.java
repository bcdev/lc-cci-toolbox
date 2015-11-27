package org.esa.cci.lc.aggregation;

import org.junit.Test;

import java.io.InputStream;
import java.io.InputStreamReader;

import static org.junit.Assert.assertEquals;


public class Lccs2PftLutBuilder_AdditionalMap_Test {

    @Test
    public void testLutWithAdditionalUserMap() throws Exception {
        final InputStream additional = this.getClass().getResourceAsStream("TEST_LCCS2PFT_KG_ADDITIONAL.csv");
        Lccs2PftLutBuilder lutBuilder = new Lccs2PftLutBuilder();
        lutBuilder = lutBuilder.useAdditionalUserMap(new InputStreamReader(additional));
        final Lccs2PftLut lccs2PftLut = lutBuilder.create();
        assertEquals("Default LCCS to PFT lookup table + Koeppen-Geiger Map", lccs2PftLut.getComment());

        float[] conversionFactors;

//        10|21| | | | | | | | |6|94| | | |
        conversionFactors = lccs2PftLut.getConversionFactors(10, 21);
        assertEquals(Float.NaN, conversionFactors[0], 1.0e-6f);
        assertEquals(Float.NaN, conversionFactors[1], 1.0e-6f);
        assertEquals(Float.NaN, conversionFactors[2], 1.0e-6f);
        assertEquals(Float.NaN, conversionFactors[3], 1.0e-6f);
        assertEquals(Float.NaN, conversionFactors[4], 1.0e-6f);
        assertEquals(Float.NaN, conversionFactors[5], 1.0e-6f);
        assertEquals(Float.NaN, conversionFactors[6], 1.0e-6f);
        assertEquals(Float.NaN, conversionFactors[7], 1.0e-6f);
        assertEquals(6.0f, conversionFactors[8], 1.0e-6f);
        assertEquals(94.0f, conversionFactors[9], 1.0e-6f);
        assertEquals(Float.NaN, conversionFactors[10], 1.0e-6f);
        assertEquals(Float.NaN, conversionFactors[11], 1.0e-6f);
        assertEquals(Float.NaN, conversionFactors[12], 1.0e-6f);

//        150|31|2|4|1| |2|1|1| |5| |84| | |
        conversionFactors = lccs2PftLut.getConversionFactors(150, 31);
        assertEquals(2.0f, conversionFactors[0], 1.0e-6f);
        assertEquals(4.0f, conversionFactors[1], 1.0e-6f);
        assertEquals(1.0f, conversionFactors[2], 1.0e-6f);
        assertEquals(Float.NaN, conversionFactors[3], 1.0e-6f);
        assertEquals(2.0f, conversionFactors[4], 1.0e-6f);
        assertEquals(1.0f, conversionFactors[5], 1.0e-6f);
        assertEquals(1.0f, conversionFactors[6], 1.0e-6f);
        assertEquals(Float.NaN, conversionFactors[7], 1.0e-6f);
        assertEquals(5.0f, conversionFactors[8], 1.0e-6f);
        assertEquals(Float.NaN, conversionFactors[9], 1.0e-6f);
        assertEquals(84.0f, conversionFactors[10], 1.0e-6f);
        assertEquals(Float.NaN, conversionFactors[11], 1.0e-6f);
        assertEquals(Float.NaN, conversionFactors[12], 1.0e-6f);

    }

    @Test
    public void testLutWithAdditionalUserMap_Incomplete() throws Exception {
        Lccs2PftLutBuilder lutBuilder = new Lccs2PftLutBuilder();
        final InputStream userDefault = this.getClass().getResourceAsStream("TEST_LCCS2PFT_KG_DEFAULT.csv");
        lutBuilder = lutBuilder.useAdditionalUserMap(new InputStreamReader(userDefault));
        final InputStream additional = this.getClass().getResourceAsStream("TEST_LCCS2PFT_KG_ADDITIONAL_INCOMPLETE.csv");
        lutBuilder = lutBuilder.useAdditionalUserMap(new InputStreamReader(additional));
        final Lccs2PftLut lccs2PftLut = lutBuilder.create();
        assertEquals("Default LCCS to PFT lookup table + Koeppen-Geiger Map", lccs2PftLut.getComment());

        float[] conversionFactors;

//       50|12|96| | | |3|1| | | | | | | |
        conversionFactors = lccs2PftLut.getConversionFactors(50, 12);
        assertEquals(96.0f, conversionFactors[0], 1.0e-6f);
        assertEquals(Float.NaN, conversionFactors[1], 1.0e-6f);
        assertEquals(Float.NaN, conversionFactors[2], 1.0e-6f);
        assertEquals(Float.NaN, conversionFactors[3], 1.0e-6f);
        assertEquals(3.0f, conversionFactors[4], 1.0e-6f);
        assertEquals(1.0f, conversionFactors[5], 1.0e-6f);
        assertEquals(Float.NaN, conversionFactors[6], 1.0e-6f);
        assertEquals(Float.NaN, conversionFactors[7], 1.0e-6f);
        assertEquals(Float.NaN, conversionFactors[8], 1.0e-6f);
        assertEquals(Float.NaN, conversionFactors[9], 1.0e-6f);
        assertEquals(Float.NaN, conversionFactors[10], 1.0e-6f);
        assertEquals(Float.NaN, conversionFactors[11], 1.0e-6f);
        assertEquals(Float.NaN, conversionFactors[12], 1.0e-6f);

//      [60, 50] Not defined in additional - shall fall back to default
//      60| |70| | | |15| | |15| | | | |
        conversionFactors = lccs2PftLut.getConversionFactors(60, 50);
        assertEquals(Float.NaN, conversionFactors[0], 1.0e-6f);
        assertEquals(70.0f, conversionFactors[1], 1.0e-6f);
        assertEquals(Float.NaN, conversionFactors[2], 1.0e-6f);
        assertEquals(Float.NaN, conversionFactors[3], 1.0e-6f);
        assertEquals(Float.NaN, conversionFactors[4], 1.0e-6f);
        assertEquals(15.0f, conversionFactors[5], 1.0e-6f);
        assertEquals(Float.NaN, conversionFactors[6], 1.0e-6f);
        assertEquals(Float.NaN, conversionFactors[7], 1.0e-6f);
        assertEquals(15.0f, conversionFactors[8], 1.0e-6f);
        assertEquals(Float.NaN, conversionFactors[9], 1.0e-6f);
        assertEquals(Float.NaN, conversionFactors[10], 1.0e-6f);
        assertEquals(Float.NaN, conversionFactors[11], 1.0e-6f);
        assertEquals(Float.NaN, conversionFactors[12], 1.0e-6f);

    }

}