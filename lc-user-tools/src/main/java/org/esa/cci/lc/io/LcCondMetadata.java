package org.esa.cci.lc.io;

import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Marco Peters
 */
public class LcCondMetadata {

    // ESACCI-LC-L4-Snow-Cond-500m-P13Y7D-(aggregated)(-)(N640)(-)20001224-v2.0
    private static final String LC_CONDITION_ID_PATTERN = "ESACCI-LC-L4-(.*)-Cond-(.*m)-P(.*)Y(.*)D-[aggregated]?-?.*?-?(....)(....)-v(.*)";
    //private static final String LC_ALTERNATIVE_CONDITION_ID_PATTERN = "C3S-LC-L4-LCCS-Map-(.*m)-P(.*)Y-(....)-v(.*)";
    private static final String LC_ALTERNATIVE_CONDITION_ID_PATTERN = "C3S-LC-L4-(.*)-Map-(.*m)-P(.*)Y-(.*)-v(.*)";

    private String type;
    private String id;
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
            if (metadataRoot.containsAttribute("type")) {
                type = metadataRoot.getAttributeString("type");
            }
            if (metadataRoot.containsAttribute("id")) {
                id = metadataRoot.getAttributeString("id");
            }
            condition = metadataRoot.getAttributeString("condition");
            spatialResolution = metadataRoot.getAttributeString("spatialResolution");
            temporalResolution = metadataRoot.getAttributeString("temporalResolution");
            startYear = metadataRoot.getAttributeString("startYear");
            endYear = metadataRoot.getAttributeString("endYear");
            startDate = metadataRoot.getAttributeString("startDate");
            version = metadataRoot.getAttributeString("version");

        } else if (product.getName().contains("C3S-LC-L4")) {
         //C3S LC
            MetadataElement globalAttributes = product.getMetadataRoot().getElement("Global_Attributes");
            type = globalAttributes.getAttributeString("type");
            id = globalAttributes.getAttributeString("id");
            Matcher idMatcher = lcConditionTypeMatcher(id);
            condition = " ";
            spatialResolution = globalAttributes.getAttributeString("spatial_resolution");
            temporalResolution = globalAttributes.getAttributeString("time_coverage_resolution");
            startYear = idMatcher.group(3);
            endYear = idMatcher.group(3);
            startDate = startYear+"0101";
            version = idMatcher.group(4);
        }
        else {
            // Old NetCdf
            MetadataElement globalAttributes = product.getMetadataRoot().getElement("Global_Attributes");
            type = globalAttributes.getAttributeString("type");
            id = globalAttributes.getAttributeString("id");
            Matcher idMatcher = lcConditionTypeMatcher(id);
            condition = idMatcher.group(1);
            spatialResolution = idMatcher.group(2);
            int temporalCoverage = Integer.parseInt(idMatcher.group(3));
            temporalResolution = idMatcher.group(4);
            startYear = idMatcher.group(5);
            endYear = String.valueOf(Integer.parseInt(startYear) + temporalCoverage - 1);
            startDate = startYear + idMatcher.group(6);
            version = idMatcher.group(7);
        }
    }

    static Matcher lcConditionTypeMatcher(String id) {
        final String regexp =
                id.contains("ESACCI") ? LC_CONDITION_ID_PATTERN : LC_ALTERNATIVE_CONDITION_ID_PATTERN;
        Pattern p = Pattern.compile(regexp);
        Matcher m = p.matcher(id);
        if (m.matches()) {
            return m;
        }
        throw new IllegalArgumentException("Global attribute (id=" + id + ") does not match pattern " + LC_CONDITION_ID_PATTERN);
    }

    public String getType() {
        return type;
    }

    public String getId() {
        return id;
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
