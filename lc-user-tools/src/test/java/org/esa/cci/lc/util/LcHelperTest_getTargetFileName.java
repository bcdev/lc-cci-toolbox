package org.esa.cci.lc.util;

import org.hamcrest.core.Is;
import org.junit.*;

public class LcHelperTest_getTargetFileName {

    @Test
    public void testGetTargetFileName_MapFile() throws Exception {
        final String sourceFileName = "ESACCI-LC-L4-LCCS-Map-300m-P5Y-2016-v4.nc";
        final String insertion = "TextToBeInserted";

        final String targetFileName = LcHelper.getTargetFileName(insertion, sourceFileName);

        Assert.assertThat(targetFileName, Is.is("ESACCI-LC-L4-LCCS-Map-300m-P5Y-TextToBeInserted-2016-v4.nc"));
    }

    @Test
    public void testGetTargetFileName_ConditionFile() throws Exception {
        final String sourceFileName = "ESACCI-LC-L4-NDVI-Cond-300m-P7D-2001-2009-0101-v4.nc";
        final String insertion = "TextToBeInserted";

        final String targetFileName = LcHelper.getTargetFileName(insertion, sourceFileName);

        Assert.assertThat(targetFileName, Is.is("ESACCI-LC-L4-NDVI-Cond-300m-P7D-TextToBeInserted-2001-2009-0101-v4.nc"));
    }

    @Test
    public void testGetTargetFileName_AggregatedFile_Subset() {
        final String sourceFileName = "ESACCI-LC-L4-LCCS-Map-300m-P5Y-aggregated-0.083333Deg-2016-v4.nc";
        final String insertion = "Subset";

        final String targetFileName = LcHelper.getTargetFileName(insertion, sourceFileName);

        Assert.assertThat(targetFileName, Is.is("ESACCI-LC-L4-LCCS-Map-300m-P5Y-aggregated-0.083333Deg-Subset-2016-v4.nc"));
    }
}