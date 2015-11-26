package org.esa.cci.lc.aggregation;

import org.junit.Test;

import java.io.StringReader;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Marco Peters
 */
public class LCCSTest {

    @Test(expected = IllegalArgumentException.class)
    public void testValuesAndDescriptionsMustBeOfSameSize() throws Exception {
        new LCCS(new int[]{1, 2, 3}, new String[]{"Failure"}, new String[]{"kaputt"});
    }

    @Test(expected = IllegalArgumentException.class)
    public void testValuesAndMeaningsMustBeOfSameSize() throws Exception {
        new LCCS(new int[]{1, 2, 3}, new String[]{"one", "two", "three"}, new String[]{"kaputt"});
    }

    @Test()
    public void testCreationViaGetInstance() throws Exception {
        assertNotNull(LCCS.getInstance());
    }

    @Test
    public void testGetter() throws Exception {
        final String TEST_STRING = "0 | No data | no_data\n" +
                                   "10 | Cropland | cropland\n" +
                                   "11 | Herbaceous | herbaceous\n" +
                                   "12 | Tree or shrub cover | tree_or_shrub_cover\n" +
                                   "110 | Cropland irrigated | cropland_irrigated\n" +
                                   "170 | Mosaic cropland (>50%) / natural vegetation (tree, shrub, herbaceous cover) (<50%) | mosaic_cropland \n" +
                                   "220 | Mosaic natural vegetation (tree, shrub, herbaceous cover) (>50%) / cropland (<50%) | mosaic_natural";
        LCCS lccs = LCCS.load(new StringReader(TEST_STRING));

        assertEquals(7, lccs.getNumClasses());
        int[] classValues = lccs.getClassValues();
        assertEquals(7, classValues.length);
        assertEquals(0, classValues[0]);
        assertEquals(12, classValues[3]);
        assertEquals(170, classValues[5]);
        assertEquals(220, classValues[6]);

        String[] classDescriptions = lccs.getClassDescriptions();
        assertEquals(7, classDescriptions.length);
        assertEquals("No data", classDescriptions[0]);
        assertEquals("Tree or shrub cover", classDescriptions[3]);
        assertEquals("Mosaic cropland (>50%) / natural vegetation (tree, shrub, herbaceous cover) (<50%)",
                     classDescriptions[5]);

        String[] flagMeanings = lccs.getFlagMeanings();
        assertEquals(7, flagMeanings.length);
        assertEquals("no_data", flagMeanings[0]);
        assertEquals("tree_or_shrub_cover", flagMeanings[3]);
        assertEquals("mosaic_cropland", flagMeanings[5]);
    }
}
