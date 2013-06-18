package org.esa.cci.lc.io;

import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Marco Peters
 */
public class LcCondMetadata {

    public static final String LC_CONDITION_TYPE_PATTERN = "ESACCI-LC-L4-(.*)-Cond-(.*)m-P(.*)D-(....)-(....)-(....)-v(.*)";

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
            startDate = metadataRoot.getAttributeString("weekNumber");
            version = metadataRoot.getAttributeString("version");
        } else {
            // NetCdf
            MetadataElement globalAttributes = product.getMetadataRoot().getElement("Global_Attributes");
            Matcher idMatcher = lcConditionTypeMatcher(globalAttributes.getAttributeString("id"));
            condition = idMatcher.group(1);
            spatialResolution = idMatcher.group(2);
            temporalResolution = idMatcher.group(3);
            startYear = idMatcher.group(4);
            endYear = idMatcher.group(5);
            startDate = idMatcher.group(6);
            version = idMatcher.group(7);
        }
    }

    public static Matcher lcConditionTypeMatcher(String lcConditionFilename) {
        Pattern p = Pattern.compile(LC_CONDITION_TYPE_PATTERN);
        final Matcher m = p.matcher(lcConditionFilename);
        if (!m.matches()) {
            throw new IllegalArgumentException("input file name " + lcConditionFilename + " does not match pattern " + LC_CONDITION_TYPE_PATTERN);
        }
        return m;
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
