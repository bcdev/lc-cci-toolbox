package org.esa.cci.lc.io;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.dataio.AbstractProductReader;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.dataio.geotiff.GeoTiffProductReaderPlugIn;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class LcPftTiffReader extends AbstractProductReader {

    private List<Product> bandProducts;
    public static final String LC_PFT_CONDITION_FILENAME_PATTERN = "ESACCI-LC-L4-INLAND-WATER-PFT-Map-300m-P1Y-....-v0.1.tif";
    public static final String LC_PFT_ALTERNATIVE_CONDITION_FILENAME_PATTERN = "PFT_WATER_300m_...._GLOBAL_v2.tif";
    private String[] listVariables = {"BARE","BUILT","GRASS-MAN","GRASS-NAT","SHRUBS-BD","SHRUBS-BE","SHRUBS-ND","SHRUBS-NE","INLAND-WATER",
            "SNOWICE","TREES-BD","TREES-BE","TREES-ND","TREES-NE"};

    public LcPftTiffReader(LcPftTiffReaderPlugin readerPlugin) {
        super(readerPlugin);
    }


    protected Product readProductNodesImpl() throws IOException {
        final GeoTiffProductReaderPlugIn plugIn = new GeoTiffProductReaderPlugIn();
        final File lcWaterFile = getFileInput(getInput());
        final File productDir = lcWaterFile.getParentFile();

        final String lcWaterFilename = lcWaterFile.getName();
        Product lcWaterProduct = readProduct(productDir, lcWaterFilename, plugIn);

        Product result = new Product("Pixel_product",
                "CF-1.6",
                lcWaterProduct.getSceneRasterWidth(),
                lcWaterProduct.getSceneRasterHeight());
        result.setFileLocation(lcWaterFile);

        final GeoCoding geoCoding = lcWaterProduct.getSceneGeoCoding();
        final GeoPos upperLeft = geoCoding.getGeoPos(new PixelPos(0, 0), null);
        final GeoPos lowerRight = geoCoding.getGeoPos(new PixelPos(lcWaterProduct.getSceneRasterWidth(), lcWaterProduct.getSceneRasterHeight()), null);
        final String latMax = String.valueOf(upperLeft.getLat());
        final String latMin = String.valueOf(lowerRight.getLat());
        String lonMin = String.valueOf((upperLeft.getLon()));  ///change longitude here
        String lonMax = String.valueOf((lowerRight.getLon()));

        result.getMetadataRoot().addElement(new MetadataElement("global_attributes"));
        result.getMetadataRoot().getElement("global_attributes").setAttributeString("product_version", "v5.0");
        result.getMetadataRoot().getElement("global_attributes").setAttributeString("type", "PFT_product");
        result.getMetadataRoot().getElement("global_attributes").setAttributeDouble("geospatial_lat_min",Double.parseDouble(latMin));
        result.getMetadataRoot().getElement("global_attributes").setAttributeDouble("geospatial_lat_max",Double.parseDouble(latMax));
        result.getMetadataRoot().getElement("global_attributes").setAttributeDouble("geospatial_lon_min",Double.parseDouble(lonMin));
        result.getMetadataRoot().getElement("global_attributes").setAttributeDouble("geospatial_lon_max",Double.parseDouble(lonMax));
        result.getMetadataRoot().getElement("global_attributes").setAttributeString("time_coverage_duration","P1Y");
        result.getMetadataRoot().getElement("global_attributes").setAttributeString("time_coverage_resolution","P1Y");
        result.getMetadataRoot().getElement("global_attributes").setAttributeString("sensor","MODIS");
        result.getMetadataRoot().getElement("global_attributes").setAttributeString("spatial_resolution","300m");
        //result.getMetadataRoot().getElement("global_attributes").setAttributeString("geospatial_lon_resolution","0.0022457331");
        //result.getMetadataRoot().getElement("global_attributes").setAttributeString("geospatial_lat_resolution","0.0022457331");

        /// time part
        String timeYear = lcWaterProduct.getFileLocation().getName().substring(15,19);

        String startObservation = timeYear+"0101T000000Z";
        String endObservation = timeYear+"1231T235959Z";
        result.getMetadataRoot().getElement("global_attributes").setAttributeString("time_coverage_start",startObservation);
        result.getMetadataRoot().getElement("global_attributes").setAttributeString("time_coverage_end",endObservation);
        ///

        //bandProducts = new ArrayList<>();
        //bandProducts.add(lcWaterProduct);
        Band waterBand = addBand("WATER",lcWaterProduct,result);
        waterBand.setDescription("WATER");

        for (String bandName : listVariables) {
            String lcFilename = lcWaterFilename.replace("WATER",bandName);
            Product lcProduct = readProduct(productDir,lcFilename,plugIn);
            Band bandBare = addBand(bandName, lcProduct, result);
            bandBare.setDescription(bandName);
        }

        return result;
    }

    @Override
    protected void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight, int sourceStepX, int sourceStepY,
                                          Band destBand, int destOffsetX, int destOffsetY, int destWidth, int destHeight, ProductData destBuffer,
                                          ProgressMonitor pm) throws IOException {
        // all bands use source images as source for its data
        throw new IllegalStateException();
    }

    private static File getFileInput(Object input) {
        if (input instanceof String) {
            return new File((String) input);
        } else if (input instanceof File) {
            return (File) input;
        } else {
            throw new IllegalArgumentException("unexpected class " + input.getClass() + " of the input");
        }
    }

    private static Product readProduct(File productDir, String lcFlagFilename, ProductReaderPlugIn plugIn)
            throws IOException {
        File bandFile = new File(productDir, lcFlagFilename);
        if (!bandFile.canRead()) {
            return null;
        }
        final ProductReader productReader1 = plugIn.createReaderInstance();
        return productReader1.readProductNodes(bandFile, null);
    }

    private static Band addBand(String bandName, Product lcFlagProduct, Product result) {
        final Band srcBand = lcFlagProduct.getBandAt(0);
        final Band band = result.addBand(bandName, srcBand.getDataType());
        band.setNoDataValueUsed(false);
        band.setSourceImage(srcBand.getSourceImage());
        return band;
    }

}
