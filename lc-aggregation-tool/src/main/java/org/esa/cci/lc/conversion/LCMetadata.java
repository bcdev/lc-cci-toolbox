package org.esa.cci.lc.conversion;

import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;

/**
 * @author Marco Peters
 */
class LcMetadata {

    private static final String GLOBAL_ATTRIBUTES_ELEMENT_NAME = "Global_Attributes";

    private String epoch;
    private String version;
    private String spatialResolution;
    private String temporalResolution;

    public LcMetadata(Product sourceProduct) {
        MetadataElement metadataRoot = sourceProduct.getMetadataRoot();
        if (metadataRoot.containsElement(GLOBAL_ATTRIBUTES_ELEMENT_NAME)) {
            MetadataElement globalAttributes = metadataRoot.getElement(GLOBAL_ATTRIBUTES_ELEMENT_NAME);
            final String id = globalAttributes.getAttributeString("id");
            int mpPos = id.indexOf("m-P");
            int yPos = id.indexOf("Y-");
            int vPos = id.indexOf("-v");
            spatialResolution = id.substring(17, mpPos);
            temporalResolution = id.substring(mpPos + 3, yPos);
            epoch = id.substring(yPos + 2, vPos);
            version = id.substring(vPos + 2);
        } else {
            epoch = metadataRoot.getAttributeString("epoch");
            version = metadataRoot.getAttributeString("version");
            spatialResolution = metadataRoot.getAttributeString("spatialResolution");
            temporalResolution = metadataRoot.getAttributeString("temporalResolution");

        }
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
