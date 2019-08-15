package org.esa.cci.lc.io;

import org.esa.snap.core.datamodel.MetadataAttribute;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Marco Peters, Martin Böttcher
 */
public class LcWbMetadata {

    private static final String LC_WB_ID_PATTERN = "ESACCI-LC-L4-WB-Map-(.*m)-P(.*)Y-[aggregated]?-?.*?-?(....)-v(.*)";
    private static final String ALTERNATIVE_LC_WB_ID_PATTERN = "ESACCI-LC-L4-WB-Ocean-Map-(.*m)-P(.*)Y-[aggregated]?-?.*?-?(....)-v(.*)";

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
            if (metadataRoot.containsAttribute("epoch")) {
                epoch = metadataRoot.getAttributeString("epoch");
            }
            if (metadataRoot.containsAttribute("version")) {
                version = metadataRoot.getAttributeString("version");
            }
            if (metadataRoot.containsAttribute("spatialResolution")) {
                spatialResolution = metadataRoot.getAttributeString("spatialResolution");
            }
            if (metadataRoot.containsAttribute("temporalResolution")) {
                temporalResolution = metadataRoot.getAttributeString("temporalResolution");
            }
        }

        if (sourceProduct.getName().contains("150m")){
            MetadataElement globalAttributes = metadata150mResolution();
            sourceProduct.getMetadataRoot().addElement(globalAttributes);
        }
    }

    private MetadataElement metadata150mResolution(){
        this.type               = "ESACCI-LC-L4-WB-Map-150m-P6Y";
        this.id                 = "ESACCI-LC-L4-WB-Ocean-Map-150m-P13Y-2000-v4.0";
        this.epoch              = "2000";
        this.spatialResolution  = "150m";
        this.temporalResolution = "1";
        this.version            = "4.0";

        MetadataElement globalAttributes = new MetadataElement("Global_Attributes");
        //MetadataAttribute typeAttribute = new MetadataAttribute("type",2);
        globalAttributes.setAttributeString("type",this.type);
        globalAttributes.setAttributeString("id",this.id);
        globalAttributes.setAttributeString("epoch",this.epoch);
        globalAttributes.setAttributeString("spatialResolution",this.spatialResolution);
        globalAttributes.setAttributeString("temporalResolution",this.temporalResolution);
        globalAttributes.setAttributeString("version",this.version);
        
        return globalAttributes;
    }

    static Matcher lcMapTypeMatcher(String id) {
        Pattern p = Pattern.compile(LC_WB_ID_PATTERN);
        final Matcher m = p.matcher(id);
        if (!m.matches()) {
            return lcMapAlternativeMatcher(id);
            //throw new IllegalArgumentException("Global attribute (id=" + id + ") does not match pattern " + LC_WB_ID_PATTERN);
        }
        return m;
    }

    static Matcher lcMapAlternativeMatcher(String id) {
        Pattern p = Pattern.compile(ALTERNATIVE_LC_WB_ID_PATTERN);
        final Matcher m = p.matcher(id);
        if (!m.matches()) {
            throw new IllegalArgumentException("Global attribute (id=" + id + ") does not match pattern " + LC_WB_ID_PATTERN+" or "+ALTERNATIVE_LC_WB_ID_PATTERN);
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
