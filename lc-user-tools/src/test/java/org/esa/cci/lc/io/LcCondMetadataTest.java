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
        Matcher idMatcher = LcCondMetadata.lcConditionTypeMatcher("ESACCI-LC-L4-BA-Cond-500m-P13Y7D-2000-2012-20000604-v1.0");
        assertThat("BA", is(idMatcher.group(1)));
        assertThat("500", is(idMatcher.group(2)));
        assertThat("7", is(idMatcher.group(4)));
        assertThat(null, is(idMatcher.group(5)));
        assertThat("2000", is(idMatcher.group(6)));
        assertThat("2012", is(idMatcher.group(7)));
        assertThat("0604", is(idMatcher.group(8)));
        assertThat("1.0", is(idMatcher.group(9)));

        idMatcher = LcCondMetadata.lcConditionTypeMatcher("ESACCI-LC-L4-BA-Cond-500m-P13Y7D-aggregated-2000-2012-20000604-v1.0");
        assertThat("BA", is(idMatcher.group(1)));
        assertThat("500", is(idMatcher.group(2)));
        assertThat("7", is(idMatcher.group(4)));
        assertThat("aggregated", is(idMatcher.group(5)));
        assertThat("2000", is(idMatcher.group(6)));
        assertThat("2012", is(idMatcher.group(7)));
        assertThat("0604", is(idMatcher.group(8)));
        assertThat("1.0", is(idMatcher.group(9)));

        idMatcher = LcCondMetadata.lcConditionTypeMatcher("ESACCI-LC-L4-BA-Cond-500m-P13Y7D-20000604-v1.0");
        assertThat("BA", is(idMatcher.group(1)));
        assertThat("500", is(idMatcher.group(2)));
        assertThat("13", is(idMatcher.group(3)));
        assertThat("7", is(idMatcher.group(4)));
        assertThat(null, is(idMatcher.group(5)));
        assertThat("2000", is(idMatcher.group(6)));
        assertThat("0604", is(idMatcher.group(7)));
        assertThat("1.0", is(idMatcher.group(8)));
    }
}
