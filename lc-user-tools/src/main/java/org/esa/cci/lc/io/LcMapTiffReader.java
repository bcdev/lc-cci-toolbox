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
import org.esa.beam.dataio.geotiff.GeoTiffProductReaderPlugIn;
import org.esa.beam.framework.dataio.AbstractProductReader;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.util.ProductUtils;

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
public class LcMapTiffReader extends AbstractProductReader {
    // ESACCI-LC-L4-LCCS-Map-300m-P5Y-2000-v1.1.tif
    // or
    // ESACCI-LC-L4-LCCS-Map-300m-P1Y-1994-v2.0.1.tif
    public static final String LC_MAP_FILENAME_PATTERN = "ESACCI-LC-L4-LCCS-Map-300m-P(\\d+)Y-(....)-v(.*)\\.(tiff?)";
    public static final String LC_ALTERNATIVE_FILENAME_PATTERN = "ESACCI-LC-L4-LCCS-Map-300m-P(\\d+)Y-(....)-v(.*)_AlternativeMap.*\\.(tiff?)";
    public static final String[] LC_VARIABLE_NAMES = {
            "lccs_class",
            "processed_flag",
            "current_pixel_state",
            "observation_count",
            "algorithmic_confidence_level",
            "label_confidence_level",
            "label_source" ,
            "overall_confidence_level"
    };
    public static final String[] LC_VARIABLE_NAMES_P1Y = {
            "lccs_class",
            "processed_flag",
            "current_pixel_state",
            "observation_count",
            "change_count",
            "label_confidence_level",
            "label_source" ,
            "overall_confidence_level"
    };
    private static final String[] LC_VARIABLE_DESCRIPTIONS = new String[]{
            "Land cover class defined in LCCS",
            "LC map processed area flag",
            "LC pixel type mask",
            "number of valid observations",
            "LC map confidence level based on algorithm performance",
            "Alternative label confidence level",
            "Source of the alternative class",
            "LC map confidence level based on product validation"
    };
    private static final String[] LC_VARIABLE_DESCRIPTIONS_P1Y = new String[]{
            "Land cover class defined in LCCS",
            "LC map processed area flag",
            "LC pixel type mask",
            "number of valid observations",
            "number of class changes",
            "Alternative label confidence level",
            "Source of the alternative class",
            "LC map confidence level based on product validation"
    };
    private List<Product> bandProducts;

    public LcMapTiffReader(LcMapTiffReaderPlugin readerPlugin) {
        super(readerPlugin);
    }

    @Override
    protected Product readProductNodesImpl() throws IOException {
        final GeoTiffProductReaderPlugIn plugIn = new GeoTiffProductReaderPlugIn();
        bandProducts = new ArrayList<>();

        final File lcClassifLccsFile = getFileInput(getInput());
        if (!lcClassifLccsFile.exists()) {
            throw new IOException("Input file does not exist: " + lcClassifLccsFile.getAbsolutePath());
        }
        final File productDir = lcClassifLccsFile.getParentFile();

        final String lcClassifLccsFilename = lcClassifLccsFile.getName();
        final String mapType = mapTypeOf(lcClassifLccsFilename);
        final Matcher m = lcClassifLccsFileMatcher(lcClassifLccsFilename, mapType);
        final String temporalResolution = m.group(1);
        final String epoch = m.group(2);
        final String version = m.group(3);
        final String extension = m.group(4);

        final Product lcClassifLccsProduct = readProduct(productDir, lcClassifLccsFilename, plugIn);

        Product result = new Product("LC_Map_" + epoch + "_v" + version,
                                     "LC_Map",
                                     lcClassifLccsProduct.getSceneRasterWidth(),
                                     lcClassifLccsProduct.getSceneRasterHeight());
        result.setPreferredTileSize(new Dimension(1024, 1024));
        result.setFileLocation(lcClassifLccsFile);
        ProductUtils.copyGeoCoding(lcClassifLccsProduct, result);
        MetadataElement metadataRoot = result.getMetadataRoot();
        metadataRoot.setAttributeString("epoch", epoch);
        metadataRoot.setAttributeString("version", version);
        metadataRoot.setAttributeString("spatialResolution", "300m");
        metadataRoot.setAttributeString("temporalResolution", temporalResolution);

        bandProducts.add(lcClassifLccsProduct);
        Band band = addBand(LC_VARIABLE_NAMES[0], lcClassifLccsProduct, result);
        band.setDescription(LC_VARIABLE_DESCRIPTIONS[0]);

        if ("Map".equals(mapType) && "1".equals(temporalResolution)) {
            for (int i = 1; i < 5; ++i) {
                String lcFlagFilename;
                lcFlagFilename = "ESACCI-LC-L4-LCCS-Map" + "-300m-P" + temporalResolution + "Y-" + epoch + "-v" + version + "_qualityflag" + i + "." + extension;
                addInputToResult(productDir, lcFlagFilename, result, plugIn, LC_VARIABLE_NAMES_P1Y[i], LC_VARIABLE_DESCRIPTIONS_P1Y[i]);
            }
        } else if ("Map".equals(mapType) ) {
            for (int i = 1; i < 5; ++i) {
                String lcFlagFilename;
                lcFlagFilename = "ESACCI-LC-L4-LCCS-Map" + "-300m-P" + temporalResolution + "Y-" + epoch + "-v" + version + "_qualityflag" + i + "." + extension;
                addInputToResult(productDir, lcFlagFilename, result, plugIn, LC_VARIABLE_NAMES[i], LC_VARIABLE_DESCRIPTIONS[i]);
            }
        } else {
            for (int i = 5; i < 7; ++i) {
                String lcFlagFilename;
                if ("AlternativeMap".equals(mapType)) {
                    lcFlagFilename = "ESACCI-LC-L4-LCCS-Map" + "-300m-P" + temporalResolution + "Y-" + epoch + "-v" + version + "_AlternativeMap_QF" + (i - 4) + "." + extension;
                } else if ("AlternativeMapMaxBiomass".equals(mapType)) {
                    lcFlagFilename = "ESACCI-LC-L4-LCCS-Map" + "-300m-P" + temporalResolution + "Y-" + epoch + "-v" + version + "_AlternativeMap_MaxBiomass_QF" + (i - 4) + "." + extension;
                } else if ("AlternativeMapMinBiomass".equals(mapType)) {
                    lcFlagFilename = "ESACCI-LC-L4-LCCS-Map" + "-300m-P" + temporalResolution + "Y-" + epoch + "-v" + version + "_AlternativeMap_MinBiomass_QF" + (i - 4) + "." + extension;
                } else {
                    throw new IllegalArgumentException("unknown map type " + mapType);
                }
                addInputToResult(productDir, lcFlagFilename, result, plugIn, LC_VARIABLE_NAMES[i], LC_VARIABLE_DESCRIPTIONS[i]);
            }
        }

        return result;
    }

    private void addInputToResult(File productDir, String lcFlagFilename, Product result, GeoTiffProductReaderPlugIn plugIn, String variableName, String variableDescription) throws IOException {
        Band band;
        final Product lcFlagProduct = readProduct(productDir, lcFlagFilename, plugIn);
        if (lcFlagProduct == null) {
            return;
        }
        if (result.getSceneRasterWidth() != lcFlagProduct.getSceneRasterWidth() ||
            result.getSceneRasterHeight() != lcFlagProduct.getSceneRasterHeight()) {
            throw new IllegalArgumentException("dimensions of " + lcFlagFilename + " does not match map");
        }
        bandProducts.add(lcFlagProduct);
        band = addBand(variableName, lcFlagProduct, result);
        band.setDescription(variableDescription);
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

    private static Matcher lcClassifLccsFileMatcher(String lcClassifLccsFilename, String mapType) {
        final String regexp = mapType.startsWith("AlternativeMap") ? LC_ALTERNATIVE_FILENAME_PATTERN : LC_MAP_FILENAME_PATTERN;
        Pattern p = Pattern.compile(regexp);
        final Matcher m = p.matcher(lcClassifLccsFilename);
        if (!m.matches()) {
            throw new IllegalArgumentException("input file name " + lcClassifLccsFilename + " does not match pattern " + regexp);
        }
        return m;
    }

    private static String mapTypeOf(String filename) {
        String mapType;
        if (filename.contains("AlternativeMap_MaxBiomass")) {
            mapType = "AlternativeMapMaxBiomass";
        } else if (filename.contains("AlternativeMap_MinBiomass")) {
            mapType = "AlternativeMapMinBiomass";
        } else if (filename.contains("AlternativeMap")) {
            mapType = "AlternativeMap";
        } else {
            mapType = "Map";
        }
        return mapType;
    }

    private static Product readProduct(File productDir, String lcClassifLccsFilename, ProductReaderPlugIn plugIn)
            throws IOException {
        File lcClassifLccsFile = new File(productDir, lcClassifLccsFilename);
        if (!lcClassifLccsFile.canRead()) {
            return null;
        }
        final ProductReader productReader = plugIn.createReaderInstance();
        Product product = productReader.readProductNodes(lcClassifLccsFile, null);
        if (product == null) {
            throw new IllegalStateException("Could not read product: " + lcClassifLccsFile);
        }
        return product;
    }

    private static Band addBand(String variableName, Product lcProduct, Product result) {
        final Band srcBand = lcProduct.getBandAt(0);
        final Band band = result.addBand(variableName, srcBand.getDataType());
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
