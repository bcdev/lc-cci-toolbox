/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.cci.lc.io;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.dataio.geotiff.GeoTiffProductReaderPlugIn;
import org.esa.snap.core.dataio.AbstractProductReader;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.util.ProductUtils;

import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This reader is capable of reading a collection of Land Cover CCI TIF products
 * located in one directory into a single product.
 * Each of the single TIFs will be represented by one band in the target product.
 *
 * @author Martin Böttcher
 */
public class LcConditionTiffReader extends AbstractProductReader {

    //ESACCI-LC-L4-Cond-NDVI-AggMean-1000m-P13Y7D-19990101-v1.0.tif
    //ESACCI-LC-L4-Cond-NDVI-Std-1000m-P13Y7D-19990101-v1.0.tif
    //ESACCI-LC-L4-Cond-NDVI-Status-1000m-P13Y7D-19990101-v1.0.tif
    //ESACCI-LC-L4-Cond-NDVI-NYearObs-1000m-P13Y7D-19990101-v1.0.tif
    //ESACCI-LC-L4-Snow-Cond-AggOcc-500m-P13Y7D-20000402-v2.0.tif
    //ESACCI-LC-L4-BA-Cond-AggOcc-500m-P13Y7D-20001022-v2.0.tif
    public static final String LC_CONDITION_FILENAME_PATTERN = "ESACCI-LC-L4-(.*)-Cond-Agg(Mean|Occ)-(.*m)-P(.*)Y(.*)D-(........)-v(.*)\\.(tiff?)";
    private List<Product> bandProducts;

    public LcConditionTiffReader(LcConditionTiffReaderPlugin readerPlugin) {
        super(readerPlugin);
    }

    @Override
    protected Product readProductNodesImpl() throws IOException {
        final GeoTiffProductReaderPlugIn plugIn = new GeoTiffProductReaderPlugIn();
        bandProducts = new ArrayList<>();

        final File lcConditionFile = getFileInput(getInput());
        final File productDir = lcConditionFile.getParentFile();

        final String lcConditionFilename = lcConditionFile.getName();
        final Matcher m = lcConditionFileMatcher(lcConditionFilename);
        final String condition = m.group(1);
        final String mainVariable = m.group(2).toLowerCase();
        final String spatialResolution = m.group(3);
        final String temporalCoverageYears = m.group(4);
        final String temporalResolution = m.group(5);
        final String startDate = m.group(6);
        final String startYear = startDate.substring(0, 4);
        final String endYear = String.valueOf(Integer.parseInt(startYear) + Integer.parseInt(temporalCoverageYears) - 1);
        final String version = m.group(7);
        final String extension = m.group(8);

        final Product lcConditionProduct = readProduct(productDir, lcConditionFilename, plugIn);

        if (lcConditionProduct == null) {
            throw new IllegalStateException("Could not read product file: " + lcConditionFile.getAbsolutePath());
        }

        Product result = new Product("LC_Cond_" + condition + "_" + startYear + "_" + endYear + "_" + startDate + "_v" + version,
                                     "LC_Cond",
                                     lcConditionProduct.getSceneRasterWidth(),
                                     lcConditionProduct.getSceneRasterHeight());
        result.setPreferredTileSize(new Dimension(1024, 1024));
        result.setFileLocation(lcConditionFile);
        ProductUtils.copyGeoCoding(lcConditionProduct, result);
        result.getMetadataRoot().setAttributeString("condition", condition);
        result.getMetadataRoot().setAttributeString("spatialResolution", spatialResolution);
        result.getMetadataRoot().setAttributeString("temporalResolution", temporalResolution);
        result.getMetadataRoot().setAttributeString("startYear", startYear);
        result.getMetadataRoot().setAttributeString("endYear", endYear);
        result.getMetadataRoot().setAttributeString("startDate", startDate);
        result.getMetadataRoot().setAttributeString("version", version);

        //adding all attributes to the global attributes
        MetadataElement globalAttributes = new MetadataElement("global_attributes");
        MetadataElement metadataRoot = result.getMetadataRoot();
        metadataRoot.addElement(globalAttributes);
        globalAttributes = metadataRoot.getElement("global_attributes");
        globalAttributes.setAttributeString ("condition",condition);
        globalAttributes.setAttributeString ("spatialResolution",spatialResolution);
        globalAttributes.setAttributeString ("temporalResolution",temporalResolution);
        globalAttributes.setAttributeString ("startYear",startYear);
        globalAttributes.setAttributeString ("endYear",endYear);
        globalAttributes.setAttributeString ("startDate",startDate);
        globalAttributes.setAttributeString ("version",version);


        bandProducts.add(lcConditionProduct);
        Band band = addBand(condition.toLowerCase() + "_" + mainVariable, lcConditionProduct, result);
        band.setDescription(condition + " " + mainVariable);
        //ESACCI-LC-L4-Cond-NDVI-AggMean-1000m-P13Y7D-19990101-v1.0.tif
        //ESACCI-LC-L4-Cond-NDVI-Std-1000m-P13Y7D-19990101-v1.0.tif
        //ESACCI-LC-L4-Cond-NDVI-Status-1000m-P13Y7D-19990101-v1.0.tif
        //ESACCI-LC-L4-Cond-NDVI-NYearObs-1000m-P13Y7D-19990101-v1.0.tif
        final String stdFilename = createFileName("Std", condition, spatialResolution, temporalCoverageYears, temporalResolution,
                                                  startDate, version, extension);
        final Product stdProduct = readProduct(productDir, stdFilename, plugIn);
        addVariableToConditionResult(condition, "std", stdProduct, result);

        final String statusFilename = createFileName("Status", condition, spatialResolution, temporalCoverageYears, temporalResolution,
                                                     startDate, version, extension);
        final Product statusProduct = readProduct(productDir, statusFilename, plugIn);
        addVariableToConditionResult(condition, "status", statusProduct, result);

        final String nYearObsFilename = createFileName("NYearObs", condition, spatialResolution, temporalCoverageYears, temporalResolution,
                                                       startDate, version, extension);
        final Product nYearObsProduct = readProduct(productDir, nYearObsFilename, plugIn);
        addVariableToConditionResult(condition, "nYearObs", nYearObsProduct, result);

        return result;
    }

    @Override
    protected void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight, int sourceStepX, int sourceStepY,
                                          Band destBand, int destOffsetX, int destOffsetY, int destWidth, int destHeight, ProductData destBuffer,
                                          ProgressMonitor pm) throws IOException {
        // all bands use source images as source for its data
        throw new IllegalStateException();
    }


    private String createFileName(String variable, String condition, String spatialResolution, String temporalCoverageYears,
                                  String temporalResolution,
                                  String startDate, String version, String extension) {
        return "ESACCI-LC-L4-" + condition + "-Cond-" + variable + "-" + spatialResolution + "-P" + temporalCoverageYears + "Y" + temporalResolution + "D-" + startDate + "-v" + version + "." + extension;
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

    @Override
    public void close() throws IOException {
        for (Product bandProduct : bandProducts) {
            bandProduct.closeIO();
        }
        bandProducts.clear();
        super.close();
    }
}