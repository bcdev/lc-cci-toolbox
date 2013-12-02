package org.esa.cci.lc.io;

import org.junit.Test;

import java.util.regex.Matcher;

import static org.hamcrest.core.Is.*;
import static org.junit.Assert.*;

/**
 * @author Marco Peters
 */
public class LcMapMetadataTest {

    @Test
    public void testLcMapTypeMatcher() throws Exception {
        Matcher idMatcher = LcMapMetadata.lcMapTypeMatcher("ESACCI-LC-L4-LCCS-Map-300m-P5Y-2005-v1.2");
        assertThat(idMatcher.group(1), is("300m"));
        assertThat(idMatcher.group(2), is("5"));
        assertThat(idMatcher.group(3), is("2005"));
        assertThat(idMatcher.group(4), is("1.2"));

        idMatcher = LcMapMetadata.lcMapTypeMatcher("ESACCI-LC-L4-LCCS-Map-500m-P3Y-aggregated-2004-v2.0");
        assertThat(idMatcher.group(1), is("500m"));
        assertThat(idMatcher.group(2), is("3"));
        assertThat(idMatcher.group(3), is("2004"));
        assertThat(idMatcher.group(4), is("2.0"));

        idMatcher = LcMapMetadata.lcMapTypeMatcher("ESACCI-LC-L4-LCCS-Map-1000m-P10Y-aggregated-N320-2002-v2.0");
        assertThat(idMatcher.group(1), is("1000m"));
        assertThat(idMatcher.group(2), is("10"));
        assertThat(idMatcher.group(3), is("2002"));
        assertThat(idMatcher.group(4), is("2.0"));

    }
}
