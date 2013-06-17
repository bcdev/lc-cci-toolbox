package org.esa.cci.lc.conversion;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * TODO add API doc
 *
 * @author Martin Boettcher
 */
public class LcMapNetCdf4WriterPlugInTest {
    @Test
    public void testFlagMeanings() throws Exception {
        assertEquals("number of flag values and meanings",
                     LcMapNetCdf4WriterPlugIn.LCCS_CLASS_FLAG_VALUES.length,
                     LcMapNetCdf4WriterPlugIn.LCCS_CLASS_FLAG_MEANINGS.split(" ").length);
    }
}
