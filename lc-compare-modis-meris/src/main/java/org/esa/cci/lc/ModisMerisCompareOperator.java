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
@OperatorMetadata(alias = "MODISMERISCOMPOp",
                  description = "Comparison of MODIS MOD09 and MERIS (pre-processed)",
                  authors = "",
                  version = "1.0",
                  copyright = "(C) 2010 by Brockmann Consult GmbH (beam@brockmann-consult.de)")
public class ModisMerisCompareOperator extends Operator {

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
    // todo TOC
    // private String sourceBandNameMODIS_B1 = "sur_refl_b01";
    // private String sourceBandNameMODIS_B2 = "sur_refl_b02";
    // private String sourceBandNameMODIS_B3 = "sur_refl_b03";
    // private String sourceBandNameMODIS_B4 = "sur_refl_b04";

    private String sourceBandNameMODIS_B1 = "refl_1_mod";
    private String sourceBandNameMODIS_B2 = "refl_2_mod";
    private String sourceBandNameMODIS_B3 = "refl_3_mod";
    private String sourceBandNameMODIS_B4 = "refl_4_mod";


    private String sourceBandNameMask = "mask_clear_land";


    private String sourceBandNameMERIS_B1_a = "reflec_6_M";
    private String sourceBandNameMERIS_B1_b = "reflec_7_M";
    private String sourceBandNameMERIS_B2 = "reflec_13_M";
    private String sourceBandNameMERIS_B3_a = "reflec_2_M";
    private String sourceBandNameMERIS_B3_b = "reflec_3_M";
    private String sourceBandNameMERIS_B4 = "reflec_5_M";

    //todo TOC
    //private String sourceBandNameMERIS_B1_a = "sr_6_mean";
    //private String sourceBandNameMERIS_B1_b = "sr_7_mean";
    //private String sourceBandNameMERIS_B2 = "sr_13_mean";
    //private String sourceBandNameMERIS_B3_a = "sr_2_mean";
    //private String sourceBandNameMERIS_B3_b = "sr_3_mean";
    //private String sourceBandNameMERIS_B4 = "sr_5_mean";


    // bandnames of target product
    private String targetDifferenceBandNameMODIS_MERISRR_B1 = "Difference_MODIS_1_to_MERIS_6_7";
    private String targetDifferenceBandNameMODIS_MERISRR_B2 = "Difference_MODIS_2_to_MERIS_13";
    private String targetDifferenceBandNameMODIS_MERISRR_B3 = "Difference_MODIS_3_to_MERIS_2_3";
    private String targetDifferenceBandNameMODIS_MERISRR_B4 = "Difference_MODIS_4_to_MERIS_5";


    // bands of source product
    private Band sourceBandMODIS_B1;
    private Band sourceBandMODIS_B2;
    private Band sourceBandMODIS_B3;
    private Band sourceBandMODIS_B4;

    private Band sourceBandMERIS_B1_a;
    private Band sourceBandMERIS_B1_b;
    private Band sourceBandMERIS_B2;
    private Band sourceBandMERIS_B3_a;
    private Band sourceBandMERIS_B3_b;
    private Band sourceBandMERIS_B4;

    private Band sourceBandMask;


    // band of target product
    private Band targetDifferenceBandMODIS_MERISRR_B1;
    private Band targetDifferenceBandMODIS_MERISRR_B2;
    private Band targetDifferenceBandMODIS_MERISRR_B3;
    private Band targetDifferenceBandMODIS_MERISRR_B4;


    /*
        * Default constructor. The graph processing framework
        * requires that an operator has a default constructor.
    */
    public ModisMerisCompareOperator() {
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

        productName = sourceProduct.getName() + "_modisComp";

        targetProduct = new Product(productName, "MODIS_COMP", sourceProduct.getSceneRasterWidth(), sourceProduct.getSceneRasterHeight());
        // Some target products may require more aid from ProductUtils methods...
        ProductUtils.copyGeoCoding(sourceProduct, targetProduct);

        sourceBandMODIS_B1 = sourceProduct.getBand(sourceBandNameMODIS_B1);
        sourceBandMODIS_B2 = sourceProduct.getBand(sourceBandNameMODIS_B2);
        sourceBandMODIS_B3 = sourceProduct.getBand(sourceBandNameMODIS_B3);
        sourceBandMODIS_B4 = sourceProduct.getBand(sourceBandNameMODIS_B4);

        sourceBandMERIS_B1_a = sourceProduct.getBand(sourceBandNameMERIS_B1_a);
        sourceBandMERIS_B1_b = sourceProduct.getBand(sourceBandNameMERIS_B1_b);
        sourceBandMERIS_B2 = sourceProduct.getBand(sourceBandNameMERIS_B2);
        sourceBandMERIS_B3_a = sourceProduct.getBand(sourceBandNameMERIS_B3_a);
        sourceBandMERIS_B3_b = sourceProduct.getBand(sourceBandNameMERIS_B3_b);
        sourceBandMERIS_B4 = sourceProduct.getBand(sourceBandNameMERIS_B4);

        sourceBandMask = sourceProduct.getBand(sourceBandNameMask);


        targetDifferenceBandMODIS_MERISRR_B1 = targetProduct.addBand(targetDifferenceBandNameMODIS_MERISRR_B1, ProductData.TYPE_FLOAT32);
        targetDifferenceBandMODIS_MERISRR_B2 = targetProduct.addBand(targetDifferenceBandNameMODIS_MERISRR_B2, ProductData.TYPE_FLOAT32);
        targetDifferenceBandMODIS_MERISRR_B3 = targetProduct.addBand(targetDifferenceBandNameMODIS_MERISRR_B3, ProductData.TYPE_FLOAT32);
        targetDifferenceBandMODIS_MERISRR_B4 = targetProduct.addBand(targetDifferenceBandNameMODIS_MERISRR_B4, ProductData.TYPE_FLOAT32);


        targetProduct.setPreferredTileSize
                (new Dimension(targetProduct.getSceneRasterWidth(), targetProduct.getSceneRasterHeight()));
    }


    @Override
    public void computeTileStack(Map<Band, Tile> targetTiles, Rectangle targetRectangle, ProgressMonitor pm) throws OperatorException {

        Rectangle sourceRectangle = new Rectangle(targetRectangle);

        Tile sourceTileBandModis_B1 = getSourceTile(sourceBandMODIS_B1, sourceRectangle);
        Tile sourceTileBandModis_B2 = getSourceTile(sourceBandMODIS_B2, sourceRectangle);
        Tile sourceTileBandModis_B3 = getSourceTile(sourceBandMODIS_B3, sourceRectangle);
        Tile sourceTileBandModis_B4 = getSourceTile(sourceBandMODIS_B4, sourceRectangle);


        Tile sourceTileBandMask = getSourceTile(sourceBandMask, sourceRectangle);


        Tile sourceTileBandMeris_B1_a = getSourceTile(sourceBandMERIS_B1_a, sourceRectangle);
        Tile sourceTileBandMeris_B1_b = getSourceTile(sourceBandMERIS_B1_b, sourceRectangle);
        Tile sourceTileBandMeris_B2 = getSourceTile(sourceBandMERIS_B2, sourceRectangle);
        Tile sourceTileBandMeris_B3_a = getSourceTile(sourceBandMERIS_B3_a, sourceRectangle);
        Tile sourceTileBandMeris_B3_b = getSourceTile(sourceBandMERIS_B3_b, sourceRectangle);
        Tile sourceTileBandMeris_B4 = getSourceTile(sourceBandMERIS_B4, sourceRectangle);


        // output difference difference MODIS - MERIS_ bands//


        Tile targetTileDifferenceBandMODIS_MERISRR_B1 = targetTiles.get(targetDifferenceBandMODIS_MERISRR_B1);
        Tile targetTileDifferenceBandMODIS_MERISRR_B2 = targetTiles.get(targetDifferenceBandMODIS_MERISRR_B2);
        Tile targetTileDifferenceBandMODIS_MERISRR_B3 = targetTiles.get(targetDifferenceBandMODIS_MERISRR_B3);
        Tile targetTileDifferenceBandMODIS_MERISRR_B4 = targetTiles.get(targetDifferenceBandMODIS_MERISRR_B4);


        final float[] sourceDataBandModis_B1 = sourceTileBandModis_B1.getSamplesFloat();
        final float[] sourceDataBandModis_B2 = sourceTileBandModis_B2.getSamplesFloat();
        final float[] sourceDataBandModis_B3 = sourceTileBandModis_B3.getSamplesFloat();
        final float[] sourceDataBandModis_B4 = sourceTileBandModis_B4.getSamplesFloat();

        final float[] sourceDataBand_Mask = sourceTileBandMask.getSamplesFloat();


        final float[] sourceDataBandMeris_B1_a = sourceTileBandMeris_B1_a.getSamplesFloat();
        final float[] sourceDataBandMeris_B1_b = sourceTileBandMeris_B1_b.getSamplesFloat();
        final float[] sourceDataBandMeris_B2 = sourceTileBandMeris_B2.getSamplesFloat();
        final float[] sourceDataBandMeris_B3_a = sourceTileBandMeris_B3_a.getSamplesFloat();
        final float[] sourceDataBandMeris_B3_b = sourceTileBandMeris_B3_b.getSamplesFloat();
        final float[] sourceDataBandMeris_B4 = sourceTileBandMeris_B4.getSamplesFloat();


        int sourceWidth = sourceRectangle.width;
        int sourceHeight = sourceRectangle.height;
        int sourceLength = sourceHeight * sourceWidth;

        final float[] targetDataDifferenceBandMODIS_MERIS_B1 = new float[sourceLength];
        final float[] targetDataDifferenceBandMODIS_MERIS_B2 = new float[sourceLength];
        final float[] targetDataDifferenceBandMODIS_MERIS_B3 = new float[sourceLength];
        final float[] targetDataDifferenceBandMODIS_MERIS_B4 = new float[sourceLength];

        final float[] sourceDataBandMeris_B1 = new float[sourceLength];
        final float[] sourceDataBandMeris_B3 = new float[sourceLength];


        for (int z = 0; z < sourceLength; z++) {

            sourceDataBandMeris_B1[z] = (sourceDataBandMeris_B1_a[z] + sourceDataBandMeris_B1_b[z]) / 2.f;
            sourceDataBandMeris_B3[z] = (sourceDataBandMeris_B3_a[z] + sourceDataBandMeris_B3_b[z]) / 2.f;
        }


        // Pepraring Source Band - Land/Cloud detection

        PreparingOfSourceBands preparedSourceBands = new PreparingOfSourceBands();
        preparedSourceBands.preparedOfSourceBands(sourceDataBandModis_B1,
                                                  sourceDataBandModis_B2,
                                                  sourceDataBandModis_B3,
                                                  sourceDataBandModis_B4,
                                                  sourceDataBand_Mask,
                                                  sourceDataBandMeris_B1,
                                                  sourceDataBandMeris_B2,
                                                  sourceDataBandMeris_B3,
                                                  sourceDataBandMeris_B4,
                                                  sourceLength,
                                                  sourceWidth,
                                                  sourceHeight,
                                                  productName);


        for (int i = 1; i < sourceLength; i++) {
            targetDataDifferenceBandMODIS_MERIS_B1[i] = Math.abs(sourceDataBandModis_B1[i] - sourceDataBandMeris_B1[i]);
            targetDataDifferenceBandMODIS_MERIS_B2[i] = Math.abs(sourceDataBandModis_B2[i] - sourceDataBandMeris_B2[i]);
            targetDataDifferenceBandMODIS_MERIS_B3[i] = Math.abs(sourceDataBandModis_B3[i] - sourceDataBandMeris_B3[i]);
            targetDataDifferenceBandMODIS_MERIS_B4[i] = Math.abs(sourceDataBandModis_B4[i] - sourceDataBandMeris_B4[i]);
        }

        makeFilledBand(targetDataDifferenceBandMODIS_MERIS_B1, sourceWidth, sourceHeight, targetTileDifferenceBandMODIS_MERISRR_B1);

        makeFilledBand(targetDataDifferenceBandMODIS_MERIS_B2, sourceWidth, sourceHeight, targetTileDifferenceBandMODIS_MERISRR_B2);

        makeFilledBand(targetDataDifferenceBandMODIS_MERIS_B3, sourceWidth, sourceHeight, targetTileDifferenceBandMODIS_MERISRR_B3);

        makeFilledBand(targetDataDifferenceBandMODIS_MERIS_B4, sourceWidth, sourceHeight, targetTileDifferenceBandMODIS_MERISRR_B4);

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
            super(ModisMerisCompareOperator.class);
        }
    }
}
