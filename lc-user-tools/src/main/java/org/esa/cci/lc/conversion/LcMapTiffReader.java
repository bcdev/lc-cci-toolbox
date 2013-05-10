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

package org.esa.cci.lc.conversion;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.dataio.geotiff.GeoTiffProductReaderPlugIn;
import org.esa.beam.framework.dataio.AbstractProductReader;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.framework.datamodel.Band;
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

    public static final String LC_CLASSIF_FILENAME_PATTERN = "lc_classif_lccs_(....)_v(.*)\\.(tiff?)";
    public static final String[] LC_VARIABLE_NAMES = {
            "lccs_class",
            "processed_flag",
            "current_pixel_state",
            "observation_count",
            "algorithmic_confidence_level",
            "overall_confidence_level"
    };
    private static final String[] LC_VARIABLE_DESCRIPTIONS = new String[]{
            "Land cover class defined in LCCS",
            "LC map processed area flag",
            "LC pixel type mask",
            "number of valid observations",
            "LC map confidence level based on algorithm performance",
            "LC map confidence level based on product validation"
    };
    private List<Product> bandProducts;
    public LcMapTiffReader(LcMapTiffReaderPlugin readerPlugin) {
        super(readerPlugin);
    }

    @Override
    protected Product readProductNodesImpl() throws IOException {
        final GeoTiffProductReaderPlugIn plugIn = new GeoTiffProductReaderPlugIn();
        bandProducts = new ArrayList<Product>();

        final File lcClassifLccsFile = getFileInput(getInput());
        if (!lcClassifLccsFile.exists()) {
            throw new IOException("Input file does not exist: " + lcClassifLccsFile.getAbsolutePath());
        }
        final File productDir = lcClassifLccsFile.getParentFile();

        final String lcClassifLccsFilename = lcClassifLccsFile.getName();
        final Matcher m = lcClassifLccsFileMatcher(lcClassifLccsFilename);
        final String epoch = m.group(1);
        final String version = m.group(2);
        final String extension = m.group(3);

        final Product lcClassifLccsProduct = readProduct(productDir, lcClassifLccsFilename, plugIn);

        Product result = new Product("LC_Map_" + epoch + "_v" + version,
                                     "LC_Map",
                                     lcClassifLccsProduct.getSceneRasterWidth(),
                                     lcClassifLccsProduct.getSceneRasterHeight());
        result.setPreferredTileSize(new Dimension(1024, 1024));
        result.setFileLocation(lcClassifLccsFile);
        ProductUtils.copyGeoCoding(lcClassifLccsProduct, result);
        result.getMetadataRoot().setAttributeString("epoch", epoch);
        result.getMetadataRoot().setAttributeString("version", version);
        result.getMetadataRoot().setAttributeString("spatialResolution", "300");
        result.getMetadataRoot().setAttributeString("temporalResolution", "5");

        bandProducts.add(lcClassifLccsProduct);
        Band band = addBand(0, lcClassifLccsProduct, result);
        band.setDescription(LC_VARIABLE_DESCRIPTIONS[0]);

        for (int i = 1; i <= 5; ++i) {
            String lcFlagFilename = "lc_flag" + i + "_" + epoch + "_v" + version + "." + extension;
            final Product lcFlagProduct = readProduct(productDir, lcFlagFilename, plugIn);
            if (lcFlagProduct == null) {
                continue;
            }
            if (result.getSceneRasterWidth() != lcFlagProduct.getSceneRasterWidth() ||
                result.getSceneRasterHeight() != lcFlagProduct.getSceneRasterHeight()) {
                throw new IllegalArgumentException("dimensions of flag band " + i + " does not match map");
            }
            bandProducts.add(lcFlagProduct);
            band = addBand(i, lcFlagProduct, result);
            band.setDescription(LC_VARIABLE_DESCRIPTIONS[i]);
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

    private static Matcher lcClassifLccsFileMatcher(String lcClassifLccsFilename) {
        Pattern p = Pattern.compile(LC_CLASSIF_FILENAME_PATTERN);
        final Matcher m = p.matcher(lcClassifLccsFilename);
        if (!m.matches()) {
            throw new IllegalArgumentException("input file name " + lcClassifLccsFilename + " does not match pattern " + LC_CLASSIF_FILENAME_PATTERN);
        }
        return m;
    }

    private static Product readProduct(File productDir, String lcFlagFilename, ProductReaderPlugIn plugIn)
            throws IOException {
        File lcFlagFile = new File(productDir, lcFlagFilename);
        if (!lcFlagFile.canRead()) {
            return null;
        }
        final ProductReader productReader1 = plugIn.createReaderInstance();
        return productReader1.readProductNodes(lcFlagFile, null);
    }

    private static Band addBand(int i, Product lcFlagProduct, Product result) {
        final Band srcBand = lcFlagProduct.getBandAt(0);
        final String bandName = LC_VARIABLE_NAMES[i];
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