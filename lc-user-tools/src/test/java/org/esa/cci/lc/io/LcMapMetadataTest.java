package org.esa.cci.lc.io;

import org.esa.cci.lc.util.TestProduct;
import org.junit.Test;

import java.awt.Dimension;
import java.util.regex.Matcher;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

/**
 * @author Marco Peters
 */
public class LcMapMetadataTest {

    @Test
    public void testLcMapTypeMatcher() throws Exception {
        Matcher idMatcher = LcMapMetadata.lcMapIdMatcher("ESACCI-LC-L4-LCCS-Map-300m-P5Y-2005-v1.2");
        assertThat(idMatcher.group(1), is("300m"));
        assertThat(idMatcher.group(2), is("5"));
        assertThat(idMatcher.group(3), is("2005"));
        assertThat(idMatcher.group(4), is("1.2"));

        idMatcher = LcMapMetadata.lcMapIdMatcher("ESACCI-LC-L4-LCCS-Map-500m-P3Y-aggregated-2004-v2.0");
        assertThat(idMatcher.group(1), is("500m"));
        assertThat(idMatcher.group(2), is("3"));
        assertThat(idMatcher.group(3), is("2004"));
        assertThat(idMatcher.group(4), is("2.0"));

        idMatcher = LcMapMetadata.lcMapIdMatcher("ESACCI-LC-L4-LCCS-Map-1000m-P10Y-aggregated-N320-2002-v2.0");
        assertThat(idMatcher.group(1), is("1000m"));
        assertThat(idMatcher.group(2), is("10"));
        assertThat(idMatcher.group(3), is("2002"));
        assertThat(idMatcher.group(4), is("2.0"));

        idMatcher = LcMapMetadata.lcMapIdMatcher("ESACCI-LC-L4-LCCS-Map-1000m-P10Y-2002-v2.0_AlternativeMap_MaxBiomass");
        assertThat(idMatcher.group(1), is("1000m"));
        assertThat(idMatcher.group(2), is("10"));
        assertThat(idMatcher.group(3), is("2002"));
        assertThat(idMatcher.group(4), is("2.0"));

    }

    @Test
    public void testCreationWithNetCDF() throws Exception {
        final LcMapMetadata lcMapMetadata = new LcMapMetadata(TestProduct.createMapSourceProductNetCdf(new Dimension(10, 10)));
        checkLcMetadata(lcMapMetadata);
    }

    @Test
    public void testCreationWithGeoTiff() throws Exception {
        final LcMapMetadata lcMapMetadata = new LcMapMetadata(TestProduct.createMapSourceProductGeoTiff(new Dimension(10, 10)));
        checkLcMetadata(lcMapMetadata);
    }

    private void checkLcMetadata(LcMapMetadata lcMapMetadata) {
        assertEquals("2010", lcMapMetadata.getEpoch());
        assertEquals("ESACCI-LC-L4-LCCS-Map-300m-P5Y-2010-v2", lcMapMetadata.getId());
        assertEquals("Map", lcMapMetadata.getMapType());
        assertNull(lcMapMetadata.getPftTable());
        assertNull(lcMapMetadata.getPftTableComment());
        assertEquals("300m", lcMapMetadata.getSpatialResolution());
        assertEquals("5", lcMapMetadata.getTemporalResolution());
        assertEquals("ESACCI-LC-L4-LCCS-Map-300m-P5Y", lcMapMetadata.getType());
        assertEquals("2", lcMapMetadata.getVersion());
    }
}
