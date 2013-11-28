package org.esa.cci.lc.io;

import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Marco Peters
 */
public class LcCondMetadata {

    // ESACCI-LC-L4-Snow-Cond-500m-P13Y7D-2000-2012-20001224-v2.0
    private static final String LC_CONDITION_ID_PATTERN1 = "ESACCI-LC-L4-(.*)-Cond-(.*)m-P(.*)Y(.*)D-?(aggregated)?-(....)-(....)-....(....)-v(.*)";
    // ESACCI-LC-L4-Snow-Cond-500m-P13Y7D-20001224-v2.0
    private static final String LC_CONDITION_ID_PATTERN2 = "ESACCI-LC-L4-(.*)-Cond-(.*)m-P(.*)Y(.*)D-?(aggregated)?-(....)(....)-v(.*)";

    private String condition;
    private String spatialResolution;
    private String temporalResolution;
    private String startYear;
    private String endYear;
    private String startDate;
    private String version;


    public LcCondMetadata(Product product) {
        if (product.getProductReader() instanceof LcConditionTiffReader) {
            MetadataElement metadataRoot = product.getMetadataRoot();
            condition = metadataRoot.getAttributeString("condition");
            spatialResolution = metadataRoot.getAttributeString("spatialResolution");
            temporalResolution = metadataRoot.getAttributeString("temporalResolution");
            startYear = metadataRoot.getAttributeString("startYear");
            endYear = metadataRoot.getAttributeString("endYear");
            startDate = metadataRoot.getAttributeString("startDate");
            version = metadataRoot.getAttributeString("version");
        } else {
            // NetCdf
            MetadataElement globalAttributes = product.getMetadataRoot().getElement("Global_Attributes");
            Matcher idMatcher = lcConditionTypeMatcher(globalAttributes.getAttributeString("id"));
            condition = idMatcher.group(1);
            spatialResolution = idMatcher.group(2);
            temporalResolution = idMatcher.group(4);
            if (idMatcher.groupCount() == 8) {
                startYear = idMatcher.group(6);
                endYear = String.valueOf(Integer.parseInt(startYear) + Integer.parseInt(idMatcher.group(3)) - 1);
                startDate = idMatcher.group(7);
                version = idMatcher.group(8);
            } else {
                startYear = idMatcher.group(6);
                endYear = idMatcher.group(7);
                startDate = idMatcher.group(8);
                version = idMatcher.group(9);
            }
        }
    }

    static Matcher lcConditionTypeMatcher(String id) {
        Pattern p;
        Matcher m;
        p = Pattern.compile(LC_CONDITION_ID_PATTERN2);
        m = p.matcher(id);
        if (m.matches()) {
            return m;
        }
        p = Pattern.compile(LC_CONDITION_ID_PATTERN1);
        m = p.matcher(id);
        if (m.matches()) {
            return m;
        }
        throw new IllegalArgumentException("Global attribute (id=" + id + ") does not match pattern " + LC_CONDITION_ID_PATTERN2);
    }

    public String getCondition() {
        return condition;
    }

    public String getSpatialResolution() {
        return spatialResolution;
    }

    public String getTemporalResolution() {
        return temporalResolution;
    }

    public String getStartYear() {
        return startYear;
    }

    public String getEndYear() {
        return endYear;
    }

    public String getStartDate() {
        return startDate;
    }

    public String getVersion() {
        return version;
    }

}
