package org.esa.cci.lc.aggregation;

import org.junit.Test;

import java.io.StringReader;

import static org.junit.Assert.*;

/**
 * @author Marco Peters
 */
public class LCCSTest {

    private final String TEST_STRING = "0 | No data\n" +
                                       "10 | Cropland, rainfed\n" +
                                       "11 | Herbaceous cover\n" +
                                       "12 | Tree or shrub cover\n" +
                                       "20 | Cropland, irrigated or post-flooding\n" +
                                       "30 | Mosaic cropland (>50%) / natural vegetation (tree, shrub, herbaceous cover) (<50%)\n" +
                                       "40 | Mosaic natural vegetation (tree, shrub, herbaceous cover) (>50%) / cropland (<50%) ";

    @Test(expected = IllegalArgumentException.class)
    public void testValuesAndDescriptionsMustBeOfSameSize() throws Exception {
        new LCCS(new int[]{1, 2, 3}, new String[]{"Failure"});
    }

    @Test()
    public void testCreationViaGetInstance() throws Exception {
        assertNotNull(LCCS.getInstance());
    }

    @Test
    public void testGetter() throws Exception {
        LCCS lccs = LCCS.load(new StringReader(TEST_STRING));

        assertEquals(7, lccs.getNumClasses());
        int[] classValues = lccs.getClassValues();
        assertEquals(7, classValues.length);
        assertEquals(0, classValues[0]);
        assertEquals(12, classValues[3]);
        assertEquals(30, classValues[5]);

        String[] classDescriptions = lccs.getClassDescriptions();
        assertEquals(7, classDescriptions.length);
        assertEquals("No data", classDescriptions[0]);
        assertEquals("Tree or shrub cover", classDescriptions[3]);
        assertEquals("Mosaic cropland (>50%) / natural vegetation (tree, shrub, herbaceous cover) (<50%)",
                     classDescriptions[5]);
    }
}
