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
@OperatorMetadata(alias = "SPOTVGTCOMPOp",
                  description = "Comparison of SPOT VGT S1 and SPOT VGT P (pre-processed)",
                  authors = "",
                  version = "1.0",
                  copyright = "(C) 2010 by Brockmann Consult GmbH (beam@brockmann-consult.de)")
public class SpotVgtCompareOperator extends Operator {

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
    private String sourceBandNameS1_B0 = "B0_S1";
    private String sourceBandNameS1_B2 = "B2_S1";
    private String sourceBandNameS1_B3 = "B3_S1";
    private String sourceBandNameS1_MIR = "MIR_S1";
    private String sourceBandNameS1_SM = "SM_S1";

    private String sourceBandNameP_B0 = "sdr_B0_P";
    private String sourceBandNameP_B2 = "sdr_B2_P";
    private String sourceBandNameP_B3 = "sdr_B3_P";
    private String sourceBandNameP_MIR = "sdr_MIR_P";
    private String sourceBandNameP_SM = "SM_P";

    private String sourceBandNameP_Idepix = "cloud_classif_flags_P";

    // bandnames of target product
    private String targetDifferenceBandNameB0 = "Difference_SPOT_VGT_S1_to_P_B0";
    private String targetDifferenceBandNameB2 = "Difference_SPOT_VGT_S1_to_P_B2";
    private String targetDifferenceBandNameB3 = "Difference_SPOT_VGT_S1_to_P_B3";
    private String targetDifferenceBandNameMIR = "Difference_SPOT_VGT_S1_to_P_MIR";


    // bands of source product
    private Band sourceBandS1_B0;
    private Band sourceBandS1_B2;
    private Band sourceBandS1_B3;
    private Band sourceBandS1_MIR;
    private Band sourceBandS1_SM;

    private Band sourceBandP_B0;
    private Band sourceBandP_B2;
    private Band sourceBandP_B3;
    private Band sourceBandP_MIR;
    private Band sourceBandP_SM;

    private Band sourceBandP_Idepix;

    // band of target product
    private Band targetDifferenceBandB0;
    private Band targetDifferenceBandB2;
    private Band targetDifferenceBandB3;
    private Band targetDifferenceBandMIR;


    /*
        * Default constructor. The graph processing framework
        * requires that an operator has a default constructor.
    */
    public SpotVgtCompareOperator() {
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

        productName = sourceProduct.getName() + "_vgtComp";

        targetProduct = new Product(productName, "VGT_COMP", sourceProduct.getSceneRasterWidth(), sourceProduct.getSceneRasterHeight());
        // Some target products may require more aid from ProductUtils methods...
        ProductUtils.copyGeoCoding(sourceProduct, targetProduct);

        sourceBandS1_B0 = sourceProduct.getBand(sourceBandNameS1_B0);
        sourceBandS1_B2 = sourceProduct.getBand(sourceBandNameS1_B2);
        sourceBandS1_B3 = sourceProduct.getBand(sourceBandNameS1_B3);
        sourceBandS1_MIR = sourceProduct.getBand(sourceBandNameS1_MIR);
        sourceBandS1_SM = sourceProduct.getBand(sourceBandNameS1_SM);

        sourceBandP_B0 = sourceProduct.getBand(sourceBandNameP_B0);
        sourceBandP_B2 = sourceProduct.getBand(sourceBandNameP_B2);
        sourceBandP_B3 = sourceProduct.getBand(sourceBandNameP_B3);
        sourceBandP_MIR = sourceProduct.getBand(sourceBandNameP_MIR);
        sourceBandP_SM = sourceProduct.getBand(sourceBandNameP_SM);

        sourceBandP_Idepix = sourceProduct.getBand(sourceBandNameP_Idepix);

        targetDifferenceBandB0 = targetProduct.addBand(targetDifferenceBandNameB0, ProductData.TYPE_FLOAT32);
        targetDifferenceBandB2 = targetProduct.addBand(targetDifferenceBandNameB2, ProductData.TYPE_FLOAT32);
        targetDifferenceBandB3 = targetProduct.addBand(targetDifferenceBandNameB3, ProductData.TYPE_FLOAT32);
        targetDifferenceBandMIR = targetProduct.addBand(targetDifferenceBandNameMIR, ProductData.TYPE_FLOAT32);


        targetProduct.setPreferredTileSize
                (new Dimension(targetProduct.getSceneRasterWidth(), targetProduct.getSceneRasterHeight()));
    }


    @Override
    public void computeTileStack(Map<Band, Tile> targetTiles, Rectangle targetRectangle, ProgressMonitor pm) throws OperatorException {

        Rectangle sourceRectangle = new Rectangle(targetRectangle);

        Tile sourceTileBandS1_B0 = getSourceTile(sourceBandS1_B0, sourceRectangle);
        Tile sourceTileBandS1_B2 = getSourceTile(sourceBandS1_B2, sourceRectangle);
        Tile sourceTileBandS1_B3 = getSourceTile(sourceBandS1_B3, sourceRectangle);
        Tile sourceTileBandS1_MIR = getSourceTile(sourceBandS1_MIR, sourceRectangle);
        Tile sourceTileBandS1_SM = getSourceTile(sourceBandS1_SM, sourceRectangle);

        Tile sourceTileBandP_B0 = getSourceTile(sourceBandP_B0, sourceRectangle);
        Tile sourceTileBandP_B2 = getSourceTile(sourceBandP_B2, sourceRectangle);
        Tile sourceTileBandP_B3 = getSourceTile(sourceBandP_B3, sourceRectangle);
        Tile sourceTileBandP_MIR = getSourceTile(sourceBandP_MIR, sourceRectangle);
        Tile sourceTileBandP_SM = getSourceTile(sourceBandP_SM, sourceRectangle);

        Tile sourceTileBandP_Idepix = getSourceTile(sourceBandP_Idepix, sourceRectangle);

        // output difference difference_SPOT_VGT_S1_to_P_ bands//

        Tile targetTileDifferenceBandB0 = targetTiles.get(targetDifferenceBandB0);
        Tile targetTileDifferenceBandB2 = targetTiles.get(targetDifferenceBandB2);
        Tile targetTileDifferenceBandB3 = targetTiles.get(targetDifferenceBandB3);
        Tile targetTileDifferenceBandMIR = targetTiles.get(targetDifferenceBandMIR);


        final float[] sourceDataBandS1_B0 = sourceTileBandS1_B0.getSamplesFloat();
        final float[] sourceDataBandS1_B2 = sourceTileBandS1_B2.getSamplesFloat();
        final float[] sourceDataBandS1_B3 = sourceTileBandS1_B3.getSamplesFloat();
        final float[] sourceDataBandS1_MIR = sourceTileBandS1_MIR.getSamplesFloat();


        final float[] sourceDataBandP_B0 = sourceTileBandP_B0.getSamplesFloat();
        final float[] sourceDataBandP_B2 = sourceTileBandP_B2.getSamplesFloat();
        final float[] sourceDataBandP_B3 = sourceTileBandP_B3.getSamplesFloat();
        final float[] sourceDataBandP_MIR = sourceTileBandP_MIR.getSamplesFloat();


        // todo check
        final int[] sourceDataBandP_Idepix = sourceTileBandP_Idepix.getSamplesInt();
        final int[] sourceDataBandP_SM = sourceTileBandP_SM.getSamplesInt();
        final int[] sourceDataBandS1_SM = sourceTileBandS1_SM.getSamplesInt();

        int sourceWidth = sourceRectangle.width;
        int sourceHeight = sourceRectangle.height;
        int sourceLength = sourceHeight * sourceWidth;

        final float[] targetDataDifferenceBandB0 = new float[sourceLength];
        final float[] targetDataDifferenceBandB2 = new float[sourceLength];
        final float[] targetDataDifferenceBandB3 = new float[sourceLength];
        final float[] targetDataDifferenceBandMIR = new float[sourceLength];


        // todo North_America_Sample  todo North_America_Sample

        /*
        final float[] sourceDataBandP_SM_float= sourceTileBandP_SM.getSamplesFloat();
        for (int z = 0; z < sourceLength; z++) {
            sourceDataBandP_SM[z] = ((int) (sourceDataBandP_SM_float[z]));
        }

        */

        // Pepraring Source Band - Land/Cloud detection

        PreparingOfSourceBands preparedSourceBands = new PreparingOfSourceBands();
        preparedSourceBands.preparedOfSourceBands(sourceDataBandS1_B0,
                                                  sourceDataBandS1_B2,
                                                  sourceDataBandS1_B3,
                                                  sourceDataBandS1_MIR,
                                                  sourceDataBandS1_SM,
                                                  sourceDataBandP_B0,
                                                  sourceDataBandP_B2,
                                                  sourceDataBandP_B3,
                                                  sourceDataBandP_MIR,
                                                  sourceDataBandP_SM,
                                                  sourceDataBandP_Idepix,
                                                  sourceLength,
                                                  sourceWidth,
                                                  sourceHeight,
                                                  productName);


        for (int i = 1; i < sourceLength - 1; i++) {
            targetDataDifferenceBandB0[i] = sourceDataBandS1_B0[i] + sourceDataBandP_B0[i];
            targetDataDifferenceBandB2[i] = sourceDataBandS1_B2[i] + sourceDataBandP_B2[i];
            targetDataDifferenceBandB3[i] = sourceDataBandS1_B3[i] + sourceDataBandP_B3[i];
            targetDataDifferenceBandMIR[i] = sourceDataBandS1_MIR[i] + sourceDataBandP_MIR[i];
        }

        makeFilledBand(targetDataDifferenceBandB0, sourceWidth, sourceHeight, targetTileDifferenceBandB0);
        makeFilledBand(targetDataDifferenceBandB2, sourceWidth, sourceHeight, targetTileDifferenceBandB2);
        makeFilledBand(targetDataDifferenceBandB3, sourceWidth, sourceHeight, targetTileDifferenceBandB3);
        makeFilledBand(targetDataDifferenceBandMIR, sourceWidth, sourceHeight, targetTileDifferenceBandMIR);


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
            super(SpotVgtCompareOperator.class);
        }
    }
}
