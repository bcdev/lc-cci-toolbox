package org.esa.cci.lc.io;


import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.dataio.AbstractProductReader;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.dataio.geotiff.GeoTiffProductReaderPlugIn;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LcCdsTiffReader extends AbstractProductReader {


    public static final String LC_CONDITION_FILENAME_PATTERN = "(........)-ESACCI-L3S_FIRE-BA-MODIS-AREA_(.)-fv5.0-LC.tif";
    //20010101-ESACCI-L3S_FIRE-BA-MODIS-AREA_1-fv5.0-LC.tif
    private List<Product> bandProducts;

    public LcCdsTiffReader(LcCdsTiffReaderPlugin readerPlugin) {
        super(readerPlugin);
    }


    protected Product readProductNodesImpl() throws IOException {
        final GeoTiffProductReaderPlugIn plugIn = new GeoTiffProductReaderPlugIn();
        final File lcConditionFile = getFileInput(getInput());
        bandProducts = new ArrayList<>();
        final String lcConditionFilename = lcConditionFile.getName();
        final String jdConditionFilename = lcConditionFilename.replace("LC","JD");
        final String clConditionFilename = lcConditionFilename.replace("LC","CL");
        final File productDir = lcConditionFile.getParentFile();

         Product lcConditionProduct = readProduct(productDir, lcConditionFilename, plugIn);
         Product jdConditionProduct = readProduct(productDir, jdConditionFilename, plugIn);
         Product clConditionProduct = readProduct(productDir, clConditionFilename, plugIn);
         lcConditionProduct.setPreferredTileSize(2025,2025);
         clConditionProduct.setPreferredTileSize(2025,2025);
         jdConditionProduct.setPreferredTileSize(2025,2025);
        if (lcConditionProduct == null) {
            throw new IllegalStateException("Could not read product file: " + lcConditionFile.getAbsolutePath());
        }


        Product result = new Product("Pixel_product",
                "CF-1.6",
                lcConditionProduct.getSceneRasterWidth(),
                lcConditionProduct.getSceneRasterHeight());


        result.setFileLocation(lcConditionFile);

        final GeoCoding geoCoding = lcConditionProduct.getSceneGeoCoding();
        final GeoPos upperLeft = geoCoding.getGeoPos(new PixelPos(0, 0), null);
        final GeoPos lowerRight = geoCoding.getGeoPos(new PixelPos(lcConditionProduct.getSceneRasterWidth(), lcConditionProduct.getSceneRasterHeight()), null);
        final String latMax = String.valueOf(upperLeft.getLat());
        final String latMin = String.valueOf(lowerRight.getLat());
        //String lonMin = String.valueOf((upperLeft.getLon()+360d)%360);  ///change longitude here
        String lonMin = String.valueOf((upperLeft.getLon()));  ///change longitude here
        //String lonMax = String.valueOf((lowerRight.getLon()+360d)%360);
        String lonMax = String.valueOf((lowerRight.getLon()));

        ////
        result.getMetadataRoot().addElement(new MetadataElement("global_attributes"));
        result.getMetadataRoot().getElement("global_attributes").setAttributeString("title","Fire_cci Pixel MODIS Burned Area product");
        result.getMetadataRoot().getElement("global_attributes").setAttributeString("institution","University of Alcala");
        result.getMetadataRoot().getElement("global_attributes").setAttributeString("source","MODIS MOD09GQ Collection 6, MODIS MOD09GA Collection 6, MODIS MCD14ML Collection 6, ESA CCI Land Cover dataset v1.6.1");
        result.getMetadataRoot().getElement("global_attributes").setAttributeString("history","created on "+new Date());
        result.getMetadataRoot().getElement("global_attributes").setAttributeString("references","See www.esa-fire-cci.org");
        result.getMetadataRoot().getElement("global_attributes").setAttributeString("tracking_id", UUID.randomUUID().toString());
        result.getMetadataRoot().getElement("global_attributes").setAttributeString("conventions", "CF-1.6");
        result.getMetadataRoot().getElement("global_attributes").setAttributeString("product_version", "v5.0");
        result.getMetadataRoot().getElement("global_attributes").setAttributeString("summary", "The grid product is the result of summing up burned area pixels and their attributes, as extracted from their original sinusoidal projection, within each cell of 0.25 degrees in a regular grid covering the whole Earth in biweekly composites. The attributes stored are sum of burned area, standard error, fraction of burnable area, fraction of observedd area, number of patches and the burned area for 18 land cover classes of Land Cover CCI." );
        result.getMetadataRoot().getElement("global_attributes").setAttributeString("keywords", "Burned Area, Fire Disturbance, Climate Change, ESA, GCOS");
        result.getMetadataRoot().getElement("global_attributes").setAttributeString("naming_authority", "org.esa-fire-cci");
        result.getMetadataRoot().getElement("global_attributes").setAttributeString("keywords_vocabulary", "none");
        result.getMetadataRoot().getElement("global_attributes").setAttributeString("cdm_data_type", "Grid");
        result.getMetadataRoot().getElement("global_attributes").setAttributeString("type", "pixel_product");
        result.getMetadataRoot().getElement("global_attributes").setAttributeString("comment", "These data were produced as part of the ESA Fire_cci programme");
        result.getMetadataRoot().getElement("global_attributes").setAttributeString("creation_date", LcWriterUtils.COMPACT_ISO_FORMAT.format(new Date()));
        result.getMetadataRoot().getElement("global_attributes").setAttributeString("creator_name", "University of Alcala");
        result.getMetadataRoot().getElement("global_attributes").setAttributeString("creator_url", "www.esa-fire-cci.org");
        result.getMetadataRoot().getElement("global_attributes").setAttributeString("creator_email", "emilio.chuvieco@uah.es");
        result.getMetadataRoot().getElement("global_attributes").setAttributeString("contact", "http://copernicus-support.ecmwf.int");
        result.getMetadataRoot().getElement("global_attributes").setAttributeString("project", "Climate Change Initiative - European Space Agency");
        result.getMetadataRoot().getElement("global_attributes").setAttributeDouble("geospatial_lat_min",Double.parseDouble(latMin));
        result.getMetadataRoot().getElement("global_attributes").setAttributeDouble("geospatial_lat_max",Double.parseDouble(latMax));
        result.getMetadataRoot().getElement("global_attributes").setAttributeDouble("geospatial_lon_min",Double.parseDouble(lonMin));
        result.getMetadataRoot().getElement("global_attributes").setAttributeDouble("geospatial_lon_max",Double.parseDouble(lonMax));
        result.getMetadataRoot().getElement("global_attributes").setAttributeString("geospatial_vertical_min","0");
        result.getMetadataRoot().getElement("global_attributes").setAttributeString("geospatial_vertical_max","0");
        result.getMetadataRoot().getElement("global_attributes").setAttributeString("time_coverage_start","unk");
        result.getMetadataRoot().getElement("global_attributes").setAttributeString("time_coverage_end","unk");
        result.getMetadataRoot().getElement("global_attributes").setAttributeString("time_coverage_duration","P31D");
        result.getMetadataRoot().getElement("global_attributes").setAttributeString("time_coverage_resolution","P31D");
        result.getMetadataRoot().getElement("global_attributes").setAttributeString("standard_name_vocabulary","NetCDF Climate and Forecast (CF) Metadata Convention");
        result.getMetadataRoot().getElement("global_attributes").setAttributeString("license","ESA CCI Data Policy: free and open access");
        result.getMetadataRoot().getElement("global_attributes").setAttributeString("platform","Terra");
        result.getMetadataRoot().getElement("global_attributes").setAttributeString("sensor","MODIS");
        result.getMetadataRoot().getElement("global_attributes").setAttributeString("spatial_resolution","250m");
        result.getMetadataRoot().getElement("global_attributes").setAttributeString("geospatial_lon_units","degrees_east");
        result.getMetadataRoot().getElement("global_attributes").setAttributeString("geospatial_lat_units","degrees_north");
        result.getMetadataRoot().getElement("global_attributes").setAttributeString("geospatial_lon_resolution","0.0022457331");
        result.getMetadataRoot().getElement("global_attributes").setAttributeString("geospatial_lat_resolution","0.0022457331");

        /// time part
        String timeYear = lcConditionProduct.getFileLocation().getName().substring(0,4);
        String timeMonth = lcConditionProduct.getFileLocation().getName().substring(4,6);
        Calendar calendar = Calendar.getInstance();
        calendar.set(Integer.parseInt(timeYear),Integer.parseInt(timeMonth)-1,1);
        String lastDay  = Integer.toString(calendar.getActualMaximum( Calendar.DAY_OF_MONTH));
        String startObservation = timeYear+timeMonth+"01T000000Z";
        String endObservation = timeYear+timeMonth+lastDay+"T235959Z";
        result.getMetadataRoot().getElement("global_attributes").setAttributeString("time_coverage_start",startObservation);
        result.getMetadataRoot().getElement("global_attributes").setAttributeString("time_coverage_end",endObservation);
        ///

        bandProducts.add(lcConditionProduct);
        //Band band = addBand(condition.toLowerCase() + "_" + mainVariable, lcConditionProduct, result);
        Band bandJD = addBand("JD", jdConditionProduct, result);
        Band bandCL = addBand("CL", clConditionProduct, result);
        Band bandLC = addBand("LC", lcConditionProduct, result);

        bandLC.setDescription("LC");
        bandJD.setDescription("JD");
        bandCL.setDescription("CL");

        return result;

    }


    @Override
    protected void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight, int sourceStepX, int sourceStepY,
                                          Band destBand, int destOffsetX, int destOffsetY, int destWidth, int destHeight, ProductData destBuffer,
                                          ProgressMonitor pm) throws IOException {
        // all bands use source images as source for its data
        throw new IllegalStateException();
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

    private static File getFileInput(Object input) {
        if (input instanceof String) {
            return new File((String) input);
        } else if (input instanceof File) {
            return (File) input;
        } else {
            throw new IllegalArgumentException("unexpected class " + input.getClass() + " of the input");
        }
    }


    private static Matcher lcConditionFileMatcher(String lcConditionFilename) {
        Pattern p = Pattern.compile(LC_CONDITION_FILENAME_PATTERN);
        final Matcher m = p.matcher(lcConditionFilename);
        if (!m.matches()) {
            throw new IllegalArgumentException("input file name " + lcConditionFilename + " does not match pattern " + LC_CONDITION_FILENAME_PATTERN);
        }
        return m;
    }


    private static Band addBand(String bandName, Product lcFlagProduct, Product result) {
        final Band srcBand = lcFlagProduct.getBandAt(0);
        final Band band = result.addBand(bandName, srcBand.getDataType());
        band.setNoDataValueUsed(false);
        band.setSourceImage(srcBand.getSourceImage());
        return band;
    }

    @Override
    public void close() throws IOException {
        for (Product bandProduct : bandProducts) {
            bandProduct.closeIO();
        }
        bandProducts.clear();
        super.close();
    }

    private void addVariableToConditionResult(String conditionName, String variableName, Product variableProduct, Product result) {
        Band band;
        if (variableProduct != null) {
            if (result.getSceneRasterWidth() != variableProduct.getSceneRasterWidth() ||
                    result.getSceneRasterHeight() != variableProduct.getSceneRasterHeight()) {
                throw new IllegalArgumentException("dimensions of " + variableName + " band does not match dimensions of 'mainVariable' band");
            }
            bandProducts.add(variableProduct);
            band = addBand(conditionName.toLowerCase() + "_" + variableName, variableProduct, result);
            band.setDescription(conditionName + "_" + variableName);
        }
    }


}
