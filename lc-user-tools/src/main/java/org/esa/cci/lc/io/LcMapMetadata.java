package org.esa.cci.lc.io;

import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Marco Peters
 */
public class LcMapMetadata {

    private static final String LC_MAP_ID_PATTERN = "ESACCI-LC-L4-LCCS-Map-(.*m)-P(.*)Y-[aggregated]?-?.*?-?(....)-v(.*)";
    private static final String LC_ALTERNATIVE_MAP_ID_PATTERN = "ESACCI-LC-L4-LCCS-Map-(.*m)-P(.*)Y-(....)-v(.*)_AlternativeMap.*";
    private static final String LC_ALTERNATIVE_MAP_ID_PATTERN2 = "ESACCI-LC-L4-LCCS-AlternativeMap.*-(.*m)-P(.*)Y-.*(....)-v(.*)";

    public static final String GLOBAL_ATTRIBUTES_ELEMENT_NAME = "Global_Attributes";

    private String type;
    private String id;
    private String epoch;
    private String version;
    private String spatialResolution;
    private String temporalResolution;
    private String mapType;

    public LcMapMetadata(Product sourceProduct) {
        mapType = sourceProduct.getFileLocation() != null ? mapTypeOf(sourceProduct.getFileLocation().getName()) : "unknown";
        MetadataElement metadataRoot = sourceProduct.getMetadataRoot();
        if (metadataRoot.containsElement(GLOBAL_ATTRIBUTES_ELEMENT_NAME)) {
            MetadataElement globalAttributes = metadataRoot.getElement(GLOBAL_ATTRIBUTES_ELEMENT_NAME);
            type = globalAttributes.getAttributeString("type");
            id = globalAttributes.getAttributeString("id");
            Matcher idMatcher = lcMapIdMatcher(id);

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

    static Matcher lcMapIdMatcher(String id) {
        final String regexp =
                id.contains("_AlternativeMap") ? LC_ALTERNATIVE_MAP_ID_PATTERN :
                id.contains("AlternativeMap") ? LC_ALTERNATIVE_MAP_ID_PATTERN2 : LC_MAP_ID_PATTERN;
        Pattern p = Pattern.compile(regexp);
        final Matcher m = p.matcher(id);
        if (!m.matches()) {
            throw new IllegalArgumentException("Global attribute (id=" + id + ") does not match pattern " + regexp);
        }
        return m;
    }

    public static String mapTypeOf(String filename) {
        String mapType;
        if (filename.contains("MaxBiomass")) {
            mapType = "AlternativeMapMaxBiomass";
        } else if (filename.contains("MinBiomass")) {
            mapType = "AlternativeMapMinBiomass";
        } else if (filename.contains("AlternativeMap")) {
            mapType = "AlternativeMap";
        } else {
            mapType = "Map";
        }
        return mapType;
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

    public String getMapType() { return mapType; }

}
