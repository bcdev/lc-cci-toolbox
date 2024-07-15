package org.esa.cci.lc.util;

import org.esa.snap.core.datamodel.CrsGeoCoding;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;

import java.awt.Dimension;
import java.io.File;

public abstract class TestProduct {
    protected TestProduct() {
    }

    public static Product createMapSourceProductNetCdf(Dimension size) {
        final Product product = new Product("P", "T", size.width, size.height);
        product.setFileLocation(new File("/blah/ESACCI-LC-L4-LCCS-Map-300m-P5Y-2010-v2.nc"));
        product.addBand("lccs_class", "X", ProductData.TYPE_UINT8).setDescription("lccs");
        product.addBand("processed_flag", "Y", ProductData.TYPE_INT8).setDescription("processed");
        product.addBand("current_pixel_state", "X * Y", ProductData.TYPE_INT8).setDescription("state");
        product.addBand("observation_count", "10", ProductData.TYPE_INT8).setDescription("obs_count");
        product.addBand("algorithmic_confidence_level", "10", ProductData.TYPE_FLOAT32).setDescription("confidence");
        product.addBand("overall_confidence_level", "10", ProductData.TYPE_INT8).setDescription("overall");
        setWgs84GeoCoding(product, size);
        MetadataElement globalAttributes = new MetadataElement("Global_Attributes");
        globalAttributes.setAttributeString("id", "ESACCI-LC-L4-LCCS-Map-300m-P5Y-2010-v2");
        globalAttributes.setAttributeString("type", "ESACCI-LC-L4-LCCS-Map-300m-P5Y");
        globalAttributes.setAttributeString("time_coverage_duration", "P5Y");
        globalAttributes.setAttributeString("time_coverage_resolution", "P7D");
        globalAttributes.setAttributeString("time_coverage_start", "2000");
        globalAttributes.setAttributeString("time_coverage_end", "2012");
        globalAttributes.setAttributeString("product_version", "1.0");
        globalAttributes.setAttributeString("spatial_resolution", "300m");
        globalAttributes.setAttributeString("geospatial_lat_min", "-90");
        globalAttributes.setAttributeString("geospatial_lat_max", "90");
        globalAttributes.setAttributeString("geospatial_lon_min", "-180");
        globalAttributes.setAttributeString("geospatial_lon_max", "180");
        globalAttributes.setAttributeString("source", "MERIS FR L1B");
        globalAttributes.setAttributeString("history", "LC tool tests");
        product.getMetadataRoot().addElement(globalAttributes);
        return product;
    }

    public static Product createYearlyMapSourceProductNetCdf(Dimension size) {
        final Product product = new Product("P", "T", size.width, size.height);
        product.setFileLocation(new File("/blah/ESACCI-LC-L4-LCCS-Map-300m-P5Y-2010-v2.nc"));
        product.addBand("lccs_class", "X", ProductData.TYPE_UINT8);
        product.addBand("processed_flag", "Y", ProductData.TYPE_INT8);
        product.addBand("current_pixel_state", "X * Y", ProductData.TYPE_INT8);
        product.addBand("observation_count", "10", ProductData.TYPE_INT8);
        product.addBand("change_count", "2", ProductData.TYPE_FLOAT32);
        setWgs84GeoCoding(product, size);
        MetadataElement globalAttributes = new MetadataElement("Global_Attributes");
        globalAttributes.setAttributeString("id", "ESACCI-LC-L4-LCCS-Map-300m-P5Y-2010-v2");
        globalAttributes.setAttributeString("type", "ESACCI-LC-L4-LCCS-Map-300m-P5Y");
        globalAttributes.setAttributeString("time_coverage_duration", "P1Y");
        globalAttributes.setAttributeString("time_coverage_resolution", "P1Y");
        globalAttributes.setAttributeString("time_coverage_start", "2000");
        globalAttributes.setAttributeString("time_coverage_end", "2000");
        globalAttributes.setAttributeString("product_version", "1.0");
        globalAttributes.setAttributeString("spatial_resolution", "300m");
        globalAttributes.setAttributeString("geospatial_lat_min", "-90");
        globalAttributes.setAttributeString("geospatial_lat_max", "90");
        globalAttributes.setAttributeString("geospatial_lon_min", "-180");
        globalAttributes.setAttributeString("geospatial_lon_max", "180");
        globalAttributes.setAttributeString("source", "MERIS FR L1B");
        globalAttributes.setAttributeString("history", "LC tool tests");
        product.getMetadataRoot().addElement(globalAttributes);
        return product;
    }

    public static Product createMapSourceProductGeoTiff(Dimension size) {
        final Product product = new Product("P", "T", size.width, size.height);
        product.setFileLocation(new File("/blah/ESACCI-LC-L4-LCCS-Map-300m-P5Y-2010-v2.tif"));
        product.addBand("lccs_class", "X", ProductData.TYPE_UINT8);
        product.addBand("processed_flag", "Y", ProductData.TYPE_INT8);
        product.addBand("current_pixel_state", "X * Y", ProductData.TYPE_INT8);
        product.addBand("observation_count", "10", ProductData.TYPE_INT8);
        product.addBand("algorithmic_confidence_level", "10", ProductData.TYPE_FLOAT32);
        product.addBand("overall_confidence_level", "10", ProductData.TYPE_INT8);
        setWgs84GeoCoding(product, size);
        final MetadataElement metadataRoot = product.getMetadataRoot();
        metadataRoot.setAttributeString("type", "ESACCI-LC-L4-LCCS-Map-300m-P5Y");
        metadataRoot.setAttributeString("id", "ESACCI-LC-L4-LCCS-Map-300m-P5Y-2010-v2");
        metadataRoot.setAttributeString("epoch", "2010");
        metadataRoot.setAttributeString("version", "2");
        metadataRoot.setAttributeString("spatialResolution", "300m");
        metadataRoot.setAttributeString("temporalResolution", "5");
        return product;
    }

    public static Product createConditionSourceProduct(Dimension size) {
        final Product product = new Product("P", "T", size.width, size.height);
        product.setFileLocation(new File("/blah/ESACCI-LC-L4-Snow-Cond-500m-P13Y7D-20000108-v2.0.nc"));
        product.addBand("snow_occ", "X", ProductData.TYPE_UINT8);
        product.addBand("snow_nYearObs", "Y", ProductData.TYPE_INT8);
        setWgs84GeoCoding(product, size);
        MetadataElement globalAttributes = new MetadataElement("Global_Attributes");
        globalAttributes.setAttributeString("id", "ESACCI-LC-L4-Snow-Cond-500m-P13Y7D-20000108-v2.0");
        globalAttributes.setAttributeString("type", "ESACCI-LC-L4-Snow-Cond-500m-P13Y7D");
        globalAttributes.setAttributeString("time_coverage_duration", "P13Y");
        globalAttributes.setAttributeString("time_coverage_resolution", "P7D");
        globalAttributes.setAttributeString("time_coverage_start", "20000108");
        globalAttributes.setAttributeString("time_coverage_end", "20120108");
        globalAttributes.setAttributeString("product_version", "2.0");
        globalAttributes.setAttributeString("spatial_resolution", "500m");
        globalAttributes.setAttributeString("geospatial_lat_min", "-90");
        globalAttributes.setAttributeString("geospatial_lat_max", "90");
        globalAttributes.setAttributeString("geospatial_lon_min", "-180");
        globalAttributes.setAttributeString("geospatial_lon_max", "180");
        globalAttributes.setAttributeString("source", "ERIS FR L1B version 5.05, MERIS RR L1B version 8.0, SPOT VGT P");
        globalAttributes.setAttributeString("history", "amorgos-4,0, lc-sdr-1.0, lc-sr-1.0, lc-classification-1.0, lc-user-tools-4.11.1");
        product.getMetadataRoot().addElement(globalAttributes);
        return product;
    }


    public static Product createAdditionalUserMapProduct(Dimension size) {
        final Product product = new Product("user map", "um", size.width, size.height);
        product.setFileLocation(new File("/over/the/rainbow/koeppen-geiger.tif"));
        product.addBand("band_1", "X", ProductData.TYPE_UINT8);
        setWgs84GeoCoding(product, size);
        return product;
    }

    private static void setWgs84GeoCoding(Product product, Dimension size) {
        try {
            final double pixelSizeX = 360.0 / size.getWidth();
            final double pixelSizeY = 180.0 / size.getHeight();
            product.setSceneGeoCoding(new CrsGeoCoding(DefaultGeographicCRS.WGS84, size.width, size.height,
                                                  -180.0 + pixelSizeX / 2, 90.0 - pixelSizeY / 2,
                                                  pixelSizeX, pixelSizeY));
        } catch (FactoryException | TransformException e) {
            // should not come here, creation og GC should work
            e.printStackTrace();
        }
    }

}
