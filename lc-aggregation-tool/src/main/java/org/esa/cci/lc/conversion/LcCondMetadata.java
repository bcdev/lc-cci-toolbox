package org.esa.cci.lc.conversion;

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
        condition = product.getMetadataRoot().getAttributeString("condition");
        spatialResolution = product.getMetadataRoot().getAttributeString("spatialResolution");
        temporalResolution = product.getMetadataRoot().getAttributeString("temporalResolution");
        startYear = product.getMetadataRoot().getAttributeString("startYear");
        endYear = product.getMetadataRoot().getAttributeString("endYear");
        weekNumber = product.getMetadataRoot().getAttributeString("weekNumber");
        version = product.getMetadataRoot().getAttributeString("version");
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
