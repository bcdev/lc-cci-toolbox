package org.esa.cci.lc.aggregation;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.CrsGeoCoding;
import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;

import javax.media.jai.operator.ConstantDescriptor;
import java.io.File;

abstract class TestProduct {
    protected TestProduct() {
    }

    static Product createSourceProduct() {
        final Integer width = 3600;
        final Integer height = 1800;
        final Product product = new Product("P", "T", width, height);
        product.setFileLocation(new File("/blah/ESACCI-LC-L4-LCCS-Map-300m-P5Y-2010-v2.nc"));
        final Band classesBand = product.addBand("lccs_class", ProductData.TYPE_UINT8);
        classesBand.setSourceImage(ConstantDescriptor.create(width.floatValue(), height.floatValue(),
                                                             new Byte[]{10}, null));
        final Band processedFlag = product.addBand("processed_flag", ProductData.TYPE_INT8);
        processedFlag.setSourceImage(ConstantDescriptor.create(width.floatValue(), height.floatValue(),
                                                               new Byte[]{1}, null));
        final Band currentPixelState = product.addBand("current_pixel_state", ProductData.TYPE_INT8);
        currentPixelState.setSourceImage(ConstantDescriptor.create(width.floatValue(), height.floatValue(),
                                                                   new Byte[]{1}, null));
        final Band observationBand = product.addBand("observation_count", ProductData.TYPE_INT8);
        observationBand.setSourceImage(ConstantDescriptor.create(width.floatValue(), height.floatValue(),
                                                                 new Float[]{10f}, null));
        final Band algoConfidBand = product.addBand("algorithmic_confidence_level", ProductData.TYPE_FLOAT32);
        algoConfidBand.setSourceImage(ConstantDescriptor.create(width.floatValue(), height.floatValue(),
                                                                new Float[]{10f}, null));
        final Band overallConfidBand = product.addBand("overall_confidence_level", ProductData.TYPE_INT8);
        overallConfidBand.setSourceImage(ConstantDescriptor.create(width.floatValue(), height.floatValue(),
                                                                   new Float[]{10f}, null));
        try {
            product.setGeoCoding(new CrsGeoCoding(DefaultGeographicCRS.WGS84, width, height, -179.95, 89.95, 0.1, 0.1));
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
}
