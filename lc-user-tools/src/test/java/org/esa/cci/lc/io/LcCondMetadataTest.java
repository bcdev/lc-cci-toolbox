package org.esa.cci.lc.io;

import org.junit.Test;

import java.util.regex.Matcher;

import static org.hamcrest.core.Is.*;
import static org.junit.Assert.*;

/**
 * @author Marco Peters
 */
public class LcCondMetadataTest {

    @Test
    public void testParseTypeAttribute() throws Exception {
        Matcher idMatcher = LcCondMetadata.lcConditionTypeMatcher("ESACCI-LC-L4-BA-Cond-500m-P13Y7D-20000604-v2.0");
        assertThat(idMatcher.group(1), is("BA"));
        assertThat(idMatcher.group(2), is("500m"));
        assertThat(idMatcher.group(3), is("13"));
        assertThat(idMatcher.group(4), is("7"));
        assertThat(idMatcher.group(5), is("2000"));
        assertThat(idMatcher.group(6), is("0604"));
        assertThat(idMatcher.group(7), is("2.0"));

        idMatcher = LcCondMetadata.lcConditionTypeMatcher("ESACCI-LC-L4-Snow-Cond-500m-P13Y7D-aggregated-20010101-v2.0");
        assertThat(idMatcher.group(1), is("Snow"));
        assertThat(idMatcher.group(2), is("500m"));
        assertThat(idMatcher.group(3), is("13"));
        assertThat(idMatcher.group(4), is("7"));
        assertThat(idMatcher.group(5), is("2001"));
        assertThat(idMatcher.group(6), is("0101"));
        assertThat(idMatcher.group(7), is("2.0"));

        idMatcher = LcCondMetadata.lcConditionTypeMatcher("ESACCI-LC-L4-NDVI-Cond-500m-P10Y5D-aggregated-N320-20020210-v2.0");
        assertThat(idMatcher.group(1), is("NDVI"));
        assertThat(idMatcher.group(2), is("500m"));
        assertThat(idMatcher.group(3), is("10"));
        assertThat(idMatcher.group(4), is("5"));
        assertThat(idMatcher.group(5), is("2002"));
        assertThat(idMatcher.group(6), is("0210"));
        assertThat(idMatcher.group(7), is("2.0"));
    }
}
