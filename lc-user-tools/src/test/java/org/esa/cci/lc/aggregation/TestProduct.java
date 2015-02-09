package org.esa.cci.lc.aggregation;

import org.esa.beam.framework.datamodel.CrsGeoCoding;
import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;

import java.awt.*;
import java.io.File;

abstract class TestProduct {
    protected TestProduct() {
    }

    static Product createMapSourceProduct(Dimension size) {
        final Product product = new Product("P", "T", size.width, size.height);
        product.setFileLocation(new File("/blah/ESACCI-LC-L4-LCCS-Map-300m-P5Y-2010-v2.nc"));
        product.addBand("lccs_class", "X", ProductData.TYPE_UINT8);
        product.addBand("processed_flag", "Y", ProductData.TYPE_INT8);
        product.addBand("current_pixel_state", "X * Y", ProductData.TYPE_INT8);
        product.addBand("observation_count", "10", ProductData.TYPE_INT8);
        product.addBand("algorithmic_confidence_level", "10", ProductData.TYPE_FLOAT32);
        product.addBand("overall_confidence_level", "10", ProductData.TYPE_INT8);
        try {
            product.setGeoCoding(new CrsGeoCoding(DefaultGeographicCRS.WGS84, size.width, size.height, -179.95, 89.95, 0.1, 0.1));
        } catch (FactoryException | TransformException e) {
            // should not come here, creation og GC should work
            e.printStackTrace();
        }
        MetadataElement globalAttributes = new MetadataElement("Global_Attributes");
        globalAttributes.addAttribute(new MetadataAttribute("id", ProductData.createInstance("ESACCI-LC-L4-LCCS-Map-300m-P5Y-2010-v2"), true));
        globalAttributes.addAttribute(new MetadataAttribute("time_coverage_duration", ProductData.createInstance("P5Y"), true));
        globalAttributes.addAttribute(new MetadataAttribute("time_coverage_resolution", ProductData.createInstance("P7D"), true));
        globalAttributes.addAttribute(new MetadataAttribute("time_coverage_start", ProductData.createInstance("2000"), true));
        globalAttributes.addAttribute(new MetadataAttribute("time_coverage_end", ProductData.createInstance("2012"), true));
        globalAttributes.addAttribute(new MetadataAttribute("product_version", ProductData.createInstance("1.0"), true));
        globalAttributes.addAttribute(new MetadataAttribute("spatial_resolution", ProductData.createInstance("300m"), true));
        globalAttributes.addAttribute(new MetadataAttribute("geospatial_lat_min", ProductData.createInstance("-90"), true));
        globalAttributes.addAttribute(new MetadataAttribute("geospatial_lat_max", ProductData.createInstance("90"), true));
        globalAttributes.addAttribute(new MetadataAttribute("geospatial_lon_min", ProductData.createInstance("-180"), true));
        globalAttributes.addAttribute(new MetadataAttribute("geospatial_lon_max", ProductData.createInstance("180"), true));
        globalAttributes.addAttribute(new MetadataAttribute("source", ProductData.createInstance("MERIS FR L1B"), true));
        globalAttributes.addAttribute(new MetadataAttribute("history", ProductData.createInstance("LC tool tests"), true));
        product.getMetadataRoot().addElement(globalAttributes);
        return product;
    }

    static Product createConditionSourceProduct(Dimension size) {
        final Product product = new Product("P", "T", size.width, size.height);
        product.setFileLocation(new File("/blah/ESACCI-LC-L4-Snow-Cond-500m-P13Y7D-20000108-v2.0.nc"));
        product.addBand("snow_occ", "X", ProductData.TYPE_UINT8);
        product.addBand("snow_nYearObs", "Y", ProductData.TYPE_INT8);
        try {
            product.setGeoCoding(new CrsGeoCoding(DefaultGeographicCRS.WGS84, size.width, size.height, -179.95, 89.95, 0.1, 0.1));
        } catch (FactoryException | TransformException e) {
            // should not come here, creation og GC should work
            e.printStackTrace();
        }
        MetadataElement globalAttributes = new MetadataElement("Global_Attributes");
        globalAttributes.addAttribute(new MetadataAttribute("id", ProductData.createInstance("ESACCI-LC-L4-Snow-Cond-500m-P13Y7D-20000108-v2.0"), true));
        globalAttributes.addAttribute(new MetadataAttribute("type", ProductData.createInstance("ESACCI-LC-L4-Snow-Cond-500m-P13Y7D"), true));
        globalAttributes.addAttribute(new MetadataAttribute("time_coverage_duration", ProductData.createInstance("P13Y"), true));
        globalAttributes.addAttribute(new MetadataAttribute("time_coverage_resolution", ProductData.createInstance("P7D"), true));
        globalAttributes.addAttribute(new MetadataAttribute("time_coverage_start", ProductData.createInstance("20000108"), true));
        globalAttributes.addAttribute(new MetadataAttribute("time_coverage_end", ProductData.createInstance("20120108"), true));
        globalAttributes.addAttribute(new MetadataAttribute("product_version", ProductData.createInstance("2.0"), true));
        globalAttributes.addAttribute(new MetadataAttribute("spatial_resolution", ProductData.createInstance("500m"), true));
        globalAttributes.addAttribute(new MetadataAttribute("geospatial_lat_min", ProductData.createInstance("-90"), true));
        globalAttributes.addAttribute(new MetadataAttribute("geospatial_lat_max", ProductData.createInstance("90"), true));
        globalAttributes.addAttribute(new MetadataAttribute("geospatial_lon_min", ProductData.createInstance("-180"), true));
        globalAttributes.addAttribute(new MetadataAttribute("geospatial_lon_max", ProductData.createInstance("180"), true));
        globalAttributes.addAttribute(new MetadataAttribute("source", ProductData.createInstance("ERIS FR L1B version 5.05, MERIS RR L1B version 8.0, SPOT VGT P"), true));
        globalAttributes.addAttribute(new MetadataAttribute("history", ProductData.createInstance("amorgos-4,0, lc-sdr-1.0, lc-sr-1.0, lc-classification-1.0, lc-user-tools-4.11.1"), true));
        product.getMetadataRoot().addElement(globalAttributes);
        return product;
    }
}
