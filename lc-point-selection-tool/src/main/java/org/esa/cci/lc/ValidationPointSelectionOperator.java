/*
 * Copyright (C) 2002-2007 by ?
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.cci.lc;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.glevel.MultiLevelImage;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.Mask;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.ProductNodeGroup;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.util.ProductUtils;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.image.Raster;
import java.util.Arrays;
import java.util.Map;


/**
 * The sample operator implementation for an algorithm that outputs
 * all bands of the target product at once.
 */
@OperatorMetadata(alias = "VaPoSeOp",
                  description = "Algorithm for selection of Validation Points LC-MAP",
                  authors = "",
                  version = "1.1",
                  copyright = "(C) 2010 by Brockmann Consult GmbH (beam@brockmann-consult.de)")
public class ValidationPointSelectionOperator extends Operator {

    @SourceProduct
    private Product sourceProduct;
    @TargetProduct
    private Product targetProduct;

    @Parameter(valueSet = {FR, RR}, defaultValue = FR)
    private String ResolutionParameter;


    //@Parameter(rasterDataNodeType = Band.class)
    private String sourceBandNameMap2005 = "map_2005";
    private String sourceBandNameMap2010 = "map_2010";

    private String targetBandNameMap = "selected_point";

    private Band sourceBandMap2005;
    private Band sourceBandMap2010;

    private Band targetBandMap;

    private int step = 500;

    private static final String RR = "RR";
    private static final String FR = "FR";

    /**
     * Default constructor. The graph processing framework
     * requires that an operator has a default constructor.
     */
    public ValidationPointSelectionOperator() {
    }

    /**
     * Initializes this operator and sets the one and only target product.
     * <p>The target product can be either defined by a field of type {@link org.esa.beam.framework.datamodel.Product}
     * annotated with the {@link org.esa.beam.framework.gpf.annotations.TargetProduct TargetProduct} annotation or
     * by calling {@link #setTargetProduct} method.</p>
     * <p>The framework calls this method after it has created this operator.
     * Any client code that must be performed before computation of tile data
     * should be placed here.</p>
     *
     * @throws org.esa.beam.framework.gpf.OperatorException If an error occurs during operator initialisation.
     * @see #getTargetProduct()
     */
    @Override
    public void initialize() throws OperatorException {
        targetProduct = new Product("VALIDATION_INDEX",
                                    "org.esa.beam",
                                    sourceProduct.getSceneRasterWidth(),
                                    sourceProduct.getSceneRasterHeight());
        ProductUtils.copyGeoCoding(sourceProduct, targetProduct);
        ProductUtils.copyMasks(sourceProduct, targetProduct);

        // SourceBands
        sourceBandMap2005 = sourceProduct.getBand(sourceBandNameMap2005);
        sourceBandMap2010 = sourceProduct.getBand(sourceBandNameMap2010);


        // TargetBands
        targetBandMap = targetProduct.addBand(targetBandNameMap, ProductData.TYPE_INT32);

        //targetProduct.setPreferredTileSize(new Dimension(targetProduct.getSceneRasterWidth(), targetProduct.getSceneRasterHeight()));
        targetProduct.setPreferredTileSize(new Dimension(step, step));
    }


    @Override
    public void computeTileStack(Map<Band, Tile> targetTiles, Rectangle targetRectangle, ProgressMonitor pm) throws OperatorException {

        Tile sourceTileMap2005 = getSourceTile(sourceBandMap2005, targetRectangle);
        Tile sourceTileMap2010 = getSourceTile(sourceBandMap2010, targetRectangle);

        Tile targetTileBandMap = targetTiles.get(targetBandMap);

        int targetWidth = targetRectangle.width;
        int targetHeight = targetRectangle.height;

        final long[] SelectedPointMap = new long[targetWidth * targetHeight];
        Arrays.fill(SelectedPointMap, 0);


        ProductNodeGroup<Mask> maskGroup = sourceProduct.getMaskGroup();
        int maskNumber = maskGroup.getNodeCount();
        Raster[] maskData = new Raster[maskGroup.getNodeCount()];
        String[] maskNames = maskGroup.getNodeNames();
        for (int i = 0; i < maskGroup.getNodeCount(); i++) {
            Mask mask = maskGroup.get(i);
            MultiLevelImage sourceImage = mask.getSourceImage();
            maskData[i] = sourceImage.getData(targetRectangle);
        }

        final GeoCoding geoCoding = sourceProduct.getGeoCoding();


        if (FR.equals(ResolutionParameter)) {
            SelectionData selectedData = new SelectionData();
            selectedData.selectedFRData(sourceTileMap2005,
                                        sourceTileMap2010,
                                        SelectedPointMap,
                                        geoCoding,
                                        maskNumber,
                                        maskNames,
                                        maskData,
                                        step,
                                        targetRectangle);
        }


        if (RR.equals(ResolutionParameter)) {
            SelectionData selectedData = new SelectionData();
            selectedData.selectedRRData(sourceTileMap2005,
                                        sourceTileMap2010,
                                        SelectedPointMap,
                                        geoCoding,
                                        maskNumber,
                                        maskNames,
                                        maskData,
                                        step,
                                        targetRectangle);
        }


        ValidationPointSelectionOperator.makeFilledBand(SelectedPointMap, targetWidth, targetTileBandMap);

    }


    static void makeFilledBand
            (
                    long[] inputData,
                    int inputDataWidth,
                    Tile targetTileOutputBand) {

        for (int y = targetTileOutputBand.getMinY(); y <= targetTileOutputBand.getMaxY(); y++) {
            for (int x = targetTileOutputBand.getMinX(); x <= targetTileOutputBand.getMaxX(); x++) {

                targetTileOutputBand.setSample(x, y,
                                               inputData[((y - targetTileOutputBand.getMinY()) * inputDataWidth + (x - targetTileOutputBand.getMinX()))]);
            }
        }
    }

    static void makeFilledBand
            (
                    int[] inputData,
                    int inputDataWidth,
                    Tile targetTileOutputBand) {

        for (int y = targetTileOutputBand.getMinY(); y <= targetTileOutputBand.getMaxY(); y++) {
            for (int x = targetTileOutputBand.getMinX(); x <= targetTileOutputBand.getMaxX(); x++) {

                targetTileOutputBand.setSample(x, y,
                                               inputData[((y - targetTileOutputBand.getMinY()) * inputDataWidth + (x - targetTileOutputBand.getMinX()))]);
            }
        }
    }

    /**
     * The SPI is used to register this operator in the graph processing framework
     * via the SPI configuration file
     * {@code META-INF/services/org.esa.beam.framework.gpf.OperatorSpi}.
     * This class may also serve as a factory for new operator instances.
     *
     * @see OperatorSpi#createOperator()
     * @see OperatorSpi#createOperator(java.util.Map, java.util.Map)
     */
    public static class Spi extends OperatorSpi {

        public Spi() {
            super(ValidationPointSelectionOperator.class);
        }
    }
}
