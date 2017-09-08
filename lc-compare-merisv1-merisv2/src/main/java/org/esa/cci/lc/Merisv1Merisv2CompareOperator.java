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
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.util.ProductUtils;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.util.Map;


/**
 * The sample operator implementation for an algorithm that outputs
 * all bands of the target product at once.
 */
@OperatorMetadata(alias = "MERISv1MERISv2COMPOp",
                  description = "Comparison of MERISv1 and MERISv2 (pre-processed)",
                  authors = "",
                  version = "1.0",
                  copyright = "(C) 2010 by Brockmann Consult GmbH (beam@brockmann-consult.de)")
public class Merisv1Merisv2CompareOperator extends Operator {

    @SourceProduct
    private Product sourceProduct;
    @TargetProduct
    private Product targetProduct;


    /*
    @Parameter(valueSet = {SIED_ALGORITHM, ENTROPY_ALGORITHM},
            defaultValue = SIED_ALGORITHM, description = "Histogram Algorithm")
    private String algorithm;

    @Parameter(interval = "[0.01,10]", defaultValue = "0.025", description = "interval for rounding value")
    private float roundingInputData;
    */
    private String productName;
    // bandnames of source product

    private String sourceBandNameMERIS_B1v1 = "sr_3_mean_S";
    private String sourceBandNameMERIS_B2v1 = "sr_5_mean_S";
    private String sourceBandNameMERIS_B3v1 = "sr_7_mean_S";
    private String sourceBandNameMERIS_B4v1 = "sr_14_mean_S";


    private String sourceBandNameMaskv1 = "status_S";
    private String sourceBandNameMaskv2 = "current_pixel_state_M";

    private String sourceBandNameMERIS_B1 = "sr_3_mean_M";
    private String sourceBandNameMERIS_B2 = "sr_5_mean_M";
    private String sourceBandNameMERIS_B3 = "sr_7_mean_M";
    private String sourceBandNameMERIS_B4 = "sr_14_mean_M";

    //todo TOC
    //private String sourceBandNameMERIS_B1_a = "sr_6_mean";
    //private String sourceBandNameMERIS_B1_b = "sr_7_mean";
    //private String sourceBandNameMERIS_B2 = "sr_13_mean";
    //private String sourceBandNameMERIS_B3_a = "sr_2_mean";
    //private String sourceBandNameMERIS_B3_b = "sr_3_mean";
    //private String sourceBandNameMERIS_B4 = "sr_5_mean";


    // bandnames of target product
    private String targetDifferenceBandNameMERISv1_MERISv2_B1 = "Difference_MERIS_3_to_MERIS_3";
    private String targetDifferenceBandNameMERISv1_MERISv2_B2 = "Difference_MERIS_5_to_MERIS_5";
    private String targetDifferenceBandNameMERISv1_MERISv2_B3 = "Difference_MERIS_7_to_MERIS_7";
    private String targetDifferenceBandNameMERISv1_MERISv2_B4 = "Difference_MERIS_14_to_MERIS_14";


    // bands of source product
    private Band sourceBandMERIS_B1v1;
    private Band sourceBandMERIS_B2v1;
    private Band sourceBandMERIS_B3v1;
    private Band sourceBandMERIS_B4v1;


    private Band sourceBandMERIS_B1;
    private Band sourceBandMERIS_B2;
    private Band sourceBandMERIS_B3;
    private Band sourceBandMERIS_B4;

    private Band sourceBandMaskv1;
    private Band sourceBandMaskv2;

    // band of target product
    private Band targetDifferenceBandMERISv1_MERISv2_B1;
    private Band targetDifferenceBandMERISv1_MERISv2_B2;
    private Band targetDifferenceBandMERISv1_MERISv2_B3;
    private Band targetDifferenceBandMERISv1_MERISv2_B4;


    /*
        * Default constructor. The graph processing framework
        * requires that an operator has a default constructor.
    */
    public Merisv1Merisv2CompareOperator() {
    }

    /**
     * Initializes this operator and sets the one and only target product.
     * <p>The target product can be either defined by a field of type {@link Product}
     * annotated with the {@link TargetProduct TargetProduct} annotation or
     * by calling {@link #setTargetProduct} method.</p>
     * <p>The framework calls this method after it has created this operator.
     * Any client code that must be performed before computation of tile data
     * should be placed here.</p>
     *
     * @throws OperatorException If an error occurs during operator initialisation.
     * @see #getTargetProduct()
     */
    @Override
    public void initialize() throws OperatorException {

        productName = sourceProduct.getName() + "_merisComp";

        targetProduct = new Product(productName, "MERIS_COMP", sourceProduct.getSceneRasterWidth(), sourceProduct.getSceneRasterHeight());
        // Some target products may require more aid from ProductUtils methods...
        ProductUtils.copyGeoCoding(sourceProduct, targetProduct);

        sourceBandMERIS_B1v1 = sourceProduct.getBand(sourceBandNameMERIS_B1v1);
        sourceBandMERIS_B2v1 = sourceProduct.getBand(sourceBandNameMERIS_B2v1);
        sourceBandMERIS_B3v1 = sourceProduct.getBand(sourceBandNameMERIS_B3v1);
        sourceBandMERIS_B4v1 = sourceProduct.getBand(sourceBandNameMERIS_B4v1);

        sourceBandMERIS_B1 = sourceProduct.getBand(sourceBandNameMERIS_B1);
        sourceBandMERIS_B2 = sourceProduct.getBand(sourceBandNameMERIS_B2);
        sourceBandMERIS_B3 = sourceProduct.getBand(sourceBandNameMERIS_B3);
        sourceBandMERIS_B4 = sourceProduct.getBand(sourceBandNameMERIS_B4);

        sourceBandMaskv1 = sourceProduct.getBand(sourceBandNameMaskv1);
        sourceBandMaskv2 = sourceProduct.getBand(sourceBandNameMaskv2);

        targetDifferenceBandMERISv1_MERISv2_B1 = targetProduct.addBand(targetDifferenceBandNameMERISv1_MERISv2_B1, ProductData.TYPE_FLOAT32);
        targetDifferenceBandMERISv1_MERISv2_B2 = targetProduct.addBand(targetDifferenceBandNameMERISv1_MERISv2_B2, ProductData.TYPE_FLOAT32);
        targetDifferenceBandMERISv1_MERISv2_B3 = targetProduct.addBand(targetDifferenceBandNameMERISv1_MERISv2_B3, ProductData.TYPE_FLOAT32);
        targetDifferenceBandMERISv1_MERISv2_B4 = targetProduct.addBand(targetDifferenceBandNameMERISv1_MERISv2_B4, ProductData.TYPE_FLOAT32);


        targetProduct.setPreferredTileSize
                (new Dimension(targetProduct.getSceneRasterWidth(), targetProduct.getSceneRasterHeight()));
    }


    @Override
    public void computeTileStack(Map<Band, Tile> targetTiles, Rectangle targetRectangle, ProgressMonitor pm) throws OperatorException {

        Rectangle sourceRectangle = new Rectangle(targetRectangle);

        Tile sourceTileBandMeris_B1v1 = getSourceTile(sourceBandMERIS_B1v1, sourceRectangle);
        Tile sourceTileBandMeris_B2v1 = getSourceTile(sourceBandMERIS_B2v1, sourceRectangle);
        Tile sourceTileBandMeris_B3v1 = getSourceTile(sourceBandMERIS_B3v1, sourceRectangle);
        Tile sourceTileBandMeris_B4v1 = getSourceTile(sourceBandMERIS_B4v1, sourceRectangle);


        Tile sourceTileBandMaskv1 = getSourceTile(sourceBandMaskv1, sourceRectangle);
        Tile sourceTileBandMaskv2 = getSourceTile(sourceBandMaskv2, sourceRectangle);

        Tile sourceTileBandMeris_B1 = getSourceTile(sourceBandMERIS_B1, sourceRectangle);
        Tile sourceTileBandMeris_B2 = getSourceTile(sourceBandMERIS_B2, sourceRectangle);
        Tile sourceTileBandMeris_B3 = getSourceTile(sourceBandMERIS_B3, sourceRectangle);
        Tile sourceTileBandMeris_B4 = getSourceTile(sourceBandMERIS_B4, sourceRectangle);


        // output difference difference MODIS - MERIS_ bands//


        Tile targetTileDifferenceBandMODIS_MERISRR_B1 = targetTiles.get(targetDifferenceBandMERISv1_MERISv2_B1);
        Tile targetTileDifferenceBandMODIS_MERISRR_B2 = targetTiles.get(targetDifferenceBandMERISv1_MERISv2_B2);
        Tile targetTileDifferenceBandMODIS_MERISRR_B3 = targetTiles.get(targetDifferenceBandMERISv1_MERISv2_B3);
        Tile targetTileDifferenceBandMODIS_MERISRR_B4 = targetTiles.get(targetDifferenceBandMERISv1_MERISv2_B4);


        final float[] sourceDataBandMeris_B1v1 = sourceTileBandMeris_B1v1.getSamplesFloat();
        final float[] sourceDataBandMeris_B2v1 = sourceTileBandMeris_B2v1.getSamplesFloat();
        final float[] sourceDataBandMeris_B3v1 = sourceTileBandMeris_B3v1.getSamplesFloat();
        final float[] sourceDataBandMeris_B4v1 =  sourceTileBandMeris_B4v1.getSamplesFloat();

        final float[] sourceDataBand_Maskv1 = sourceTileBandMaskv1.getSamplesFloat();
        final float[] sourceDataBand_Maskv2 = sourceTileBandMaskv2.getSamplesFloat();


        final float[] sourceDataBandMeris_B1 = sourceTileBandMeris_B1.getSamplesFloat();
        final float[] sourceDataBandMeris_B2 = sourceTileBandMeris_B2.getSamplesFloat();
        final float[] sourceDataBandMeris_B3 = sourceTileBandMeris_B3.getSamplesFloat();
        final float[] sourceDataBandMeris_B4 = sourceTileBandMeris_B4.getSamplesFloat();


        int sourceWidth = sourceRectangle.width;
        int sourceHeight = sourceRectangle.height;
        int sourceLength = sourceHeight * sourceWidth;

        final float[] targetDataDifferenceBandMERISv1_MERISv2_B1 = new float[sourceLength];
        final float[] targetDataDifferenceBandMERISv1_MERISv2_B2 = new float[sourceLength];
        final float[] targetDataDifferenceBandMERISv1_MERISv2_B3 = new float[sourceLength];
        final float[] targetDataDifferenceBandMERISv1_MERISv2_B4 = new float[sourceLength];




        // Pepraring Source Band - Land/Cloud detection

        PreparingOfSourceBands preparedSourceBands = new PreparingOfSourceBands();
        preparedSourceBands.preparedOfSourceBands(sourceDataBandMeris_B1v1,
                                                  sourceDataBandMeris_B2v1,
                                                  sourceDataBandMeris_B3v1,
                                                  sourceDataBandMeris_B4v1,
                                                  sourceDataBand_Maskv1,
                                                  sourceDataBand_Maskv2,
                                                  sourceDataBandMeris_B1,
                                                  sourceDataBandMeris_B2,
                                                  sourceDataBandMeris_B3,
                                                  sourceDataBandMeris_B4,
                                                  sourceLength,
                                                  sourceWidth,
                                                  sourceHeight,
                                                  productName);


        for (int i = 1; i < sourceLength; i++) {
            targetDataDifferenceBandMERISv1_MERISv2_B1[i] = Math.abs(sourceDataBandMeris_B1v1[i] - sourceDataBandMeris_B1[i]);
            targetDataDifferenceBandMERISv1_MERISv2_B2[i] = Math.abs(sourceDataBandMeris_B2v1[i] - sourceDataBandMeris_B2[i]);
            targetDataDifferenceBandMERISv1_MERISv2_B3[i] = Math.abs(sourceDataBandMeris_B3v1[i] - sourceDataBandMeris_B3[i]);
            targetDataDifferenceBandMERISv1_MERISv2_B4[i] = Math.abs(sourceDataBandMeris_B4v1[i] - sourceDataBandMeris_B4[i]);
        }

        makeFilledBand(targetDataDifferenceBandMERISv1_MERISv2_B1, sourceWidth, sourceHeight, targetTileDifferenceBandMODIS_MERISRR_B1);

        makeFilledBand(targetDataDifferenceBandMERISv1_MERISv2_B2, sourceWidth, sourceHeight, targetTileDifferenceBandMODIS_MERISRR_B2);

        makeFilledBand(targetDataDifferenceBandMERISv1_MERISv2_B3, sourceWidth, sourceHeight, targetTileDifferenceBandMODIS_MERISRR_B3);

        makeFilledBand(targetDataDifferenceBandMERISv1_MERISv2_B4, sourceWidth, sourceHeight, targetTileDifferenceBandMODIS_MERISRR_B4);

    }

    static void makeFilledBand
            (
                    float[] inputData,
                    int inputDataWidth,
                    int inputDataHeight,
                    Tile targetTileOutputBand) {

        for (int y = 0; y < inputDataHeight; y++) {
            for (int x = 0; x < inputDataWidth; x++) {
                targetTileOutputBand.setSample(x, y, inputData[y * (inputDataWidth) + x]);
            }
        }
    }


    static void makeFilledBand
            (
                    float[] inputData,
                    int inputDataWidth,
                    int inputDataHeight, Tile
                    targetTileOutputBand, int mkr) {

        for (int y = mkr; y < inputDataHeight - mkr; y++) {
            for (int x = mkr; x < inputDataWidth - mkr; x++) {
                targetTileOutputBand.setSample(x - mkr, y - mkr, inputData[y * (inputDataWidth) + x]);
            }
        }
    }

    static void makeFilledBand
            (
                    float[][] inputData,
                    int inputDataWidth,
                    int inputDataHeight,
                    Tile targetTileOutputBand1,
                    Tile targetTileOutputBand2,
                    int mkr) {

        for (int y = mkr; y < inputDataHeight - mkr; y++) {
            for (int x = mkr; x < inputDataWidth - mkr; x++) {
                targetTileOutputBand1.setSample(x - mkr, y - mkr, inputData[0][y * (inputDataWidth) + x]);
                targetTileOutputBand2.setSample(x - mkr, y - mkr, inputData[1][y * (inputDataWidth) + x]);
            }
        }
    }

    static void makeFilledBand
            (
                    float[][] inputData,
                    int inputDataWidth,
                    int inputDataHeight,
                    Tile targetTileOutputBand,
                    int index,
                    int mkr) {

        for (int y = mkr; y < inputDataHeight - mkr; y++) {
            for (int x = mkr; x < inputDataWidth - mkr; x++) {
                targetTileOutputBand.setSample(x - mkr, y - mkr, inputData[index][y * (inputDataWidth) + x]);
            }
        }
    }

    static void makeFilledBand
            (
                    int[] inputData,
                    int inputDataWidth,
                    int inputDataHeight,
                    Tile targetTileOutputBand,
                    int mkr) {

        for (int y = mkr; y < inputDataHeight - mkr; y++) {
            for (int x = mkr; x < inputDataWidth - mkr; x++) {
                targetTileOutputBand.setSample(x - mkr, y - mkr, inputData[y * (inputDataWidth) + x]);
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
            super(Merisv1Merisv2CompareOperator.class);
        }
    }
}
