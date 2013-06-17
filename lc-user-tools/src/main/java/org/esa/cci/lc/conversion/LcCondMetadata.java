package org.esa.cci.lc.conversion;

import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;

/**
 * @author Marco Peters
 */
class LcCondMetadata {

    private String condition;
    private String spatialResolution;
    private String temporalResolution;
    private String startYear;
    private String endYear;
    private String weekNumber;
    private String version;

    public LcCondMetadata(Product product) {
        MetadataElement metadataRoot = product.getMetadataRoot();
        condition = metadataRoot.getAttributeString("condition");
        spatialResolution = metadataRoot.getAttributeString("spatialResolution");
        temporalResolution = metadataRoot.getAttributeString("temporalResolution");
        startYear = metadataRoot.getAttributeString("startYear");
        endYear = metadataRoot.getAttributeString("endYear");
        weekNumber = metadataRoot.getAttributeString("weekNumber");
        version = metadataRoot.getAttributeString("version");
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

    public String getWeekNumber() {
        return weekNumber;
    }

    public String getVersion() {
        return version;
    }

}
