package org.esa.cci.lc.io;

import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Marco Peters, Martin Böttcher
 */
public class LcWbMetadata {

    private static final String LC_WB_ID_PATTERN = "ESACCI-LC-L4-WB-(?:Ocean-Land-)?Map-(.*m)-P(.*)Y-[aggregated]?-?.*?-?(....)-v(.*)";

    public static final String GLOBAL_ATTRIBUTES_ELEMENT_NAME = "Global_Attributes";

    private String type;
    private String id;
    private String epoch;
    private String version;
    private String spatialResolution;
    private String temporalResolution;

    public LcWbMetadata(Product sourceProduct) {
        MetadataElement metadataRoot = sourceProduct.getMetadataRoot();
        if (metadataRoot.containsElement(GLOBAL_ATTRIBUTES_ELEMENT_NAME)) {
            MetadataElement globalAttributes = metadataRoot.getElement(GLOBAL_ATTRIBUTES_ELEMENT_NAME);
            type = globalAttributes.getAttributeString("type");
            id = globalAttributes.getAttributeString("id");
            Matcher idMatcher = lcMapTypeMatcher(id);

            spatialResolution = idMatcher.group(1);
            temporalResolution = idMatcher.group(2);
            epoch = idMatcher.group(3);
            version = idMatcher.group(4);
        } else {
            if (metadataRoot.containsAttribute("type")) {
                type = metadataRoot.getAttributeString("type");
            }
            if (metadataRoot.containsAttribute("id")) {
                id = metadataRoot.getAttributeString("id");
            }
            epoch = metadataRoot.getAttributeString("epoch");
            version = metadataRoot.getAttributeString("version");
            spatialResolution = metadataRoot.getAttributeString("spatialResolution");
            temporalResolution = metadataRoot.getAttributeString("temporalResolution");
        }
    }

    static Matcher lcMapTypeMatcher(String id) {
        Pattern p = Pattern.compile(LC_WB_ID_PATTERN);
        final Matcher m = p.matcher(id);
        if (!m.matches()) {
            throw new IllegalArgumentException("Global attribute (id=" + id + ") does not match pattern " + LC_WB_ID_PATTERN);
        }
        return m;
    }

    public String getType() {
        return type;
    }

    public String getId() {
        return id;
    }

    public String getEpoch() {
        return epoch;
    }

    public String getVersion() {
        return version;
    }

    public String getSpatialResolution() {
        return spatialResolution;
    }

    public String getTemporalResolution() {
        return temporalResolution;
    }

}
