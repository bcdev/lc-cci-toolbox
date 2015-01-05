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
public class LcWbTiffReader extends AbstractProductReader {
    // ESACCI-LC-L4-WB-Map-300m-P6Y-2005-2010-v3.0.tif
    // ESACCI-LC-L4-WB-NObsImsWS-300m-P6Y-2005-2010-v2.0.tif
    // ESACCI-LC-L4-WB-NObsImsGM-300m-P6Y-2005-2010-v2.0.tif
    public static final String LC_WB_FILENAME_PATTERN =
            "ESACCI-LC-L4-WB-Map-(.*m)-P(.*)Y-(....)-(....)-v(.*)\\.(tiff?)";
    public static final String[] FLAG_NAMES = new String[]{"NObsImsWS", "NObsImsGM"};
    public static final String[] LC_VARIABLE_NAMES = new String[] {
            "wb_class",
            "ws_observation_count",
            "gm_observation_count"
    };
    private static final String[] LC_VARIABLE_DESCRIPTIONS = new String[] {
            "terrestrial or water pixel classification",
            "number of valid observations from WS mode",
            "number of valid observations from general mode"
    };
    private List<Product> bandProducts;

    public LcWbTiffReader(LcWbTiffReaderPlugin readerPlugin) {
        super(readerPlugin);
    }

    @Override
    protected Product readProductNodesImpl() throws IOException {
        final GeoTiffProductReaderPlugIn plugIn = new GeoTiffProductReaderPlugIn();
        bandProducts = new ArrayList<Product>();

        final File lcWbFile = getFileInput(getInput());
        if (!lcWbFile.exists()) {
            throw new IOException("Input file does not exist: " + lcWbFile.getAbsolutePath());
        }
        final File productDir = lcWbFile.getParentFile();

        final String lcWbFilename = lcWbFile.getName();
        final Matcher m = lcWbFileMatcher(lcWbFilename);
        final String spatialResolution = m.group(1);
        final String temporalResolution = m.group(2);
        final String epoch = m.group(3);
        final String version = m.group(5);
        final String extension = m.group(6);

        final Product lcWbProduct = readProduct(productDir, lcWbFilename, plugIn);

        Product result = new Product("LC_WB_" + epoch + "_v" + version,
                                     "LC_WB_Map",
                                     lcWbProduct.getSceneRasterWidth(),
                                     lcWbProduct.getSceneRasterHeight());
        result.setPreferredTileSize(new Dimension(1024, 1024));
        result.setFileLocation(lcWbFile);
        ProductUtils.copyGeoCoding(lcWbProduct, result);
        MetadataElement metadataRoot = result.getMetadataRoot();
        metadataRoot.setAttributeString("epoch", epoch);
        metadataRoot.setAttributeString("version", version);
        metadataRoot.setAttributeString("spatialResolution", spatialResolution);
        metadataRoot.setAttributeString("temporalResolution", temporalResolution);

        bandProducts.add(lcWbProduct);
        Band band = addBand(0, lcWbProduct, result);
        band.setDescription(LC_VARIABLE_DESCRIPTIONS[0]);


        for (int i=0; i<FLAG_NAMES.length; ++i) {
            String lcFlagFilename = lcWbFilename.replace("Map", FLAG_NAMES[i]);
            final Product lcFlagProduct = readProduct(productDir, lcFlagFilename, plugIn);
            if (lcFlagProduct == null) {
                continue;
            }
            if (result.getSceneRasterWidth() != lcFlagProduct.getSceneRasterWidth() ||
                result.getSceneRasterHeight() != lcFlagProduct.getSceneRasterHeight()) {
                throw new IllegalArgumentException("dimensions of flag band " + FLAG_NAMES[i] + " does not match WB map");
            }
            bandProducts.add(lcFlagProduct);
            band = addBand(i+1, lcFlagProduct, result);
            band.setDescription(LC_VARIABLE_DESCRIPTIONS[i+1]);
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

    private static Matcher lcWbFileMatcher(String lcWbFilename) {
        Pattern p = Pattern.compile(LC_WB_FILENAME_PATTERN);
        final Matcher m = p.matcher(lcWbFilename);
        if (!m.matches()) {
            throw new IllegalArgumentException("input file name " + lcWbFilename + " does not match pattern " + LC_WB_FILENAME_PATTERN);
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
