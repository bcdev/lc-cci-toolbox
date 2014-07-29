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
@OperatorMetadata(alias = "SPOTVGTMERISCOMPOp",
                  description = "Comparison of SPOT VGT S1 and SPOT VGT P (pre-processed) and MERIS (pre-processed)",
                  authors = "",
                  version = "1.0",
                  copyright = "(C) 2010 by Brockmann Consult GmbH (beam@brockmann-consult.de)")
public class SpotVgtMerisCompareOperator extends Operator {

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
    private String sourceBandNameS1_SM = "SM_S1";

    private String sourceBandNameP_B0 = "sdr_B0_P";
    private String sourceBandNameP_B2 = "sdr_B2_P";
    private String sourceBandNameP_B3 = "sdr_B3_P";
    private String sourceBandNameP_SM = "SM_P";

    private String sourceBandNameMERIS_B0 = "sdr_2";
    private String sourceBandNameMERIS_B2_a = "sdr_6";
    private String sourceBandNameMERIS_B2_b = "sdr_7";
    private String sourceBandNameMERIS_B3_a = "sdr_12";
    private String sourceBandNameMERIS_B3_b = "sdr_13";
    private String sourceBandNameMERIS_SM = "status";

    private String sourceBandNameP_Idepix = "cloud_classif_flags_P";
    private String sourceBandNameMERIS_Idepix = "cloud_classif_flags";

    // bandnames of target product
    private String targetDifferenceBandNameSPOTS1MERISRRB0 = "Difference_SPOT_VGT_S1_to_MERIS_2";
    private String targetDifferenceBandNameSPOTS1MERISRRB2 = "Difference_SPOT_VGT_S1_to_MERIS_6_7";
    private String targetDifferenceBandNameSPOTS1MERISRRB3 = "Difference_SPOT_VGT_S1_to_MERIS_12_13";

    private String targetDifferenceBandNameSPOTPMERISRRB0 = "Difference_SPOT_VGT_P_to_MERIS_2";
    private String targetDifferenceBandNameSPOTPMERISRRB2 = "Difference_SPOT_VGT_P_to_MERIS_6_7";
    private String targetDifferenceBandNameSPOTPMERISRRB3 = "Difference_SPOT_VGT_P_to_MERIS_12_13";


    // bands of source product
    private Band sourceBandS1_B0;
    private Band sourceBandS1_B2;
    private Band sourceBandS1_B3;
    private Band sourceBandS1_SM;

    private Band sourceBandP_B0;
    private Band sourceBandP_B2;
    private Band sourceBandP_B3;
    private Band sourceBandP_SM;

    private Band sourceBandMERIS_B0;
    private Band sourceBandMERIS_B2_a;
    private Band sourceBandMERIS_B2_b;
    private Band sourceBandMERIS_B3_a;
    private Band sourceBandMERIS_B3_b;
    private Band sourceBandMERIS_SM;


    private Band sourceBandMERIS_Idepix;
    private Band sourceBandP_Idepix;

    // band of target product
    private Band targetDifferenceBandSPOTS1MERISRRB0;
    private Band targetDifferenceBandSPOTS1MERISRRB2;
    private Band targetDifferenceBandSPOTS1MERISRRB3;

    private Band targetDifferenceBandSPOTPMERISRRB0;
    private Band targetDifferenceBandSPOTPMERISRRB2;
    private Band targetDifferenceBandSPOTPMERISRRB3;


    /*
        * Default constructor. The graph processing framework
        * requires that an operator has a default constructor.
    */
    public SpotVgtMerisCompareOperator() {
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
        sourceBandS1_SM = sourceProduct.getBand(sourceBandNameS1_SM);

        sourceBandP_B0 = sourceProduct.getBand(sourceBandNameP_B0);
        sourceBandP_B2 = sourceProduct.getBand(sourceBandNameP_B2);
        sourceBandP_B3 = sourceProduct.getBand(sourceBandNameP_B3);
        sourceBandP_SM = sourceProduct.getBand(sourceBandNameP_SM);

        sourceBandMERIS_B0 = sourceProduct.getBand(sourceBandNameMERIS_B0);
        sourceBandMERIS_B2_a = sourceProduct.getBand(sourceBandNameMERIS_B2_a);
        sourceBandMERIS_B2_b = sourceProduct.getBand(sourceBandNameMERIS_B2_b);
        sourceBandMERIS_B3_a = sourceProduct.getBand(sourceBandNameMERIS_B3_a);
        sourceBandMERIS_B3_b = sourceProduct.getBand(sourceBandNameMERIS_B3_b);
        sourceBandMERIS_SM = sourceProduct.getBand(sourceBandNameMERIS_SM);

        sourceBandP_Idepix = sourceProduct.getBand(sourceBandNameP_Idepix);
        sourceBandMERIS_Idepix = sourceProduct.getBand(sourceBandNameMERIS_Idepix);

        targetDifferenceBandSPOTS1MERISRRB0 = targetProduct.addBand(targetDifferenceBandNameSPOTS1MERISRRB0, ProductData.TYPE_FLOAT32);
        targetDifferenceBandSPOTS1MERISRRB2 = targetProduct.addBand(targetDifferenceBandNameSPOTS1MERISRRB2, ProductData.TYPE_FLOAT32);
        targetDifferenceBandSPOTS1MERISRRB3 = targetProduct.addBand(targetDifferenceBandNameSPOTS1MERISRRB3, ProductData.TYPE_FLOAT32);

        targetDifferenceBandSPOTPMERISRRB0 = targetProduct.addBand(targetDifferenceBandNameSPOTPMERISRRB0, ProductData.TYPE_FLOAT32);
        targetDifferenceBandSPOTPMERISRRB2 = targetProduct.addBand(targetDifferenceBandNameSPOTPMERISRRB2, ProductData.TYPE_FLOAT32);
        targetDifferenceBandSPOTPMERISRRB3 = targetProduct.addBand(targetDifferenceBandNameSPOTPMERISRRB3, ProductData.TYPE_FLOAT32);


        targetProduct.setPreferredTileSize
                (new Dimension(targetProduct.getSceneRasterWidth(), targetProduct.getSceneRasterHeight()));
    }


    @Override
    public void computeTileStack(Map<Band, Tile> targetTiles, Rectangle targetRectangle, ProgressMonitor pm) throws OperatorException {

        Rectangle sourceRectangle = new Rectangle(targetRectangle);

        Tile sourceTileBandS1_B0 = getSourceTile(sourceBandS1_B0, sourceRectangle);
        Tile sourceTileBandS1_B2 = getSourceTile(sourceBandS1_B2, sourceRectangle);
        Tile sourceTileBandS1_B3 = getSourceTile(sourceBandS1_B3, sourceRectangle);

        Tile sourceTileBandS1_SM = getSourceTile(sourceBandS1_SM, sourceRectangle);


        Tile sourceTileBandP_B0 = getSourceTile(sourceBandP_B0, sourceRectangle);
        Tile sourceTileBandP_B2 = getSourceTile(sourceBandP_B2, sourceRectangle);
        Tile sourceTileBandP_B3 = getSourceTile(sourceBandP_B3, sourceRectangle);

        Tile sourceTileBandP_SM = getSourceTile(sourceBandP_SM, sourceRectangle);

        Tile sourceTileBandP_Idepix = getSourceTile(sourceBandP_Idepix, sourceRectangle);


        Tile sourceTileBandMeris_B0 = getSourceTile(sourceBandMERIS_B0, sourceRectangle);
        Tile sourceTileBandMeris_B2_a = getSourceTile(sourceBandMERIS_B2_a, sourceRectangle);
        Tile sourceTileBandMeris_B2_b = getSourceTile(sourceBandMERIS_B2_b, sourceRectangle);
        Tile sourceTileBandMeris_B3_a = getSourceTile(sourceBandMERIS_B3_a, sourceRectangle);
        Tile sourceTileBandMeris_B3_b = getSourceTile(sourceBandMERIS_B3_b, sourceRectangle);

        Tile sourceTileBandMeris_SM = getSourceTile(sourceBandMERIS_SM, sourceRectangle);

        Tile sourceTileBandMeris_Idepix = getSourceTile(sourceBandMERIS_Idepix, sourceRectangle);


        // output difference difference_SPOT_VGT_S1_to_P_ bands//


        Tile targetTileDifferenceBandSPOTS1MERISRRB0 = targetTiles.get(targetDifferenceBandSPOTS1MERISRRB0);
        Tile targetTileDifferenceBandSPOTS1MERISRRB2 = targetTiles.get(targetDifferenceBandSPOTS1MERISRRB2);
        Tile targetTileDifferenceBandSPOTS1MERISRRB3 = targetTiles.get(targetDifferenceBandSPOTS1MERISRRB3);
        Tile targetTileDifferenceBandSPOTPMERISRRB0 = targetTiles.get(targetDifferenceBandSPOTPMERISRRB0);
        Tile targetTileDifferenceBandSPOTPMERISRRB2 = targetTiles.get(targetDifferenceBandSPOTPMERISRRB2);
        Tile targetTileDifferenceBandSPOTPMERISRRB3 = targetTiles.get(targetDifferenceBandSPOTPMERISRRB3);


        final float[] sourceDataBandS1_B0 = sourceTileBandS1_B0.getSamplesFloat();
        final float[] sourceDataBandS1_B2 = sourceTileBandS1_B2.getSamplesFloat();
        final float[] sourceDataBandS1_B3 = sourceTileBandS1_B3.getSamplesFloat();


        final float[] sourceDataBandP_B0 = sourceTileBandP_B0.getSamplesFloat();
        final float[] sourceDataBandP_B2 = sourceTileBandP_B2.getSamplesFloat();
        final float[] sourceDataBandP_B3 = sourceTileBandP_B3.getSamplesFloat();


        final int[] sourceDataBandP_Idepix = sourceTileBandP_Idepix.getSamplesInt();
        final int[] sourceDataBandP_SM = sourceTileBandP_SM.getSamplesInt();
        final int[] sourceDataBandS1_SM = sourceTileBandS1_SM.getSamplesInt();

        final float[] sourceDataBandMeris_B0 = sourceTileBandMeris_B0.getSamplesFloat();
        final float[] sourceDataBandMeris_B2_a = sourceTileBandMeris_B2_a.getSamplesFloat();
        final float[] sourceDataBandMeris_B2_b = sourceTileBandMeris_B2_b.getSamplesFloat();
        final float[] sourceDataBandMeris_B3_a = sourceTileBandMeris_B3_a.getSamplesFloat();
        final float[] sourceDataBandMeris_B3_b = sourceTileBandMeris_B3_b.getSamplesFloat();

        final int[] sourceDataBandMeris_Idepix = sourceTileBandMeris_Idepix.getSamplesInt();
        final int[] sourceDataBandMeris_SM = sourceTileBandMeris_SM.getSamplesInt();


        int sourceWidth = sourceRectangle.width;
        int sourceHeight = sourceRectangle.height;
        int sourceLength = sourceHeight * sourceWidth;

        final float[] targetDataDifferenceBandSPOTS1MERISRRB0 = new float[sourceLength];
        final float[] targetDataDifferenceBandSPOTS1MERISRRB2 = new float[sourceLength];
        final float[] targetDataDifferenceBandSPOTS1MERISRRB3 = new float[sourceLength];

        final float[] targetDataDifferenceBandSPOTPMERISRRB0 = new float[sourceLength];
        final float[] targetDataDifferenceBandSPOTPMERISRRB2 = new float[sourceLength];
        final float[] targetDataDifferenceBandSPOTPMERISRRB3 = new float[sourceLength];

        final float[] sourceDataBandMeris_B2 = new float[sourceLength];
        final float[] sourceDataBandMeris_B3 = new float[sourceLength];


        // todo North_America_Sample
        // final float[] sourceDataBandP_SM_float= sourceTileBandP_SM.getSamplesFloat();

        for (int z = 0; z < sourceLength; z++) {
            //System.out.printf("SPOTVGT_S1_SM:  %f\n", (sourceDataBandS1_SM_float[z]));

            // todo North_America_Sample
            //sourceDataBandP_SM[z] = ((int) (sourceDataBandP_SM_float[z]));

            sourceDataBandMeris_B2[z] = (sourceDataBandMeris_B2_a[z] + sourceDataBandMeris_B2_b[z]) / 2.f;
            sourceDataBandMeris_B3[z] = (sourceDataBandMeris_B3_a[z] + sourceDataBandMeris_B3_b[z]) / 2.f;
        }


        // Pepraring Source Band - Land/Cloud detection

        PreparingOfSourceBands preparedSourceBands = new PreparingOfSourceBands();
        preparedSourceBands.preparedOfSourceBands(sourceDataBandS1_B0,
                                                  sourceDataBandS1_B2,
                                                  sourceDataBandS1_B3,
                                                  sourceDataBandS1_SM,
                                                  sourceDataBandP_B0,
                                                  sourceDataBandP_B2,
                                                  sourceDataBandP_B3,
                                                  sourceDataBandP_SM,
                                                  sourceDataBandP_Idepix,
                                                  sourceDataBandMeris_B0,
                                                  sourceDataBandMeris_B2,
                                                  sourceDataBandMeris_B3,
                                                  sourceDataBandMeris_SM,
                                                  sourceDataBandMeris_Idepix,
                                                  sourceLength,
                                                  sourceWidth,
                                                  sourceHeight,
                                                  productName);


        for (int i = 1; i < sourceLength; i++) {
            targetDataDifferenceBandSPOTS1MERISRRB0[i] = sourceDataBandS1_B0[i] + sourceDataBandMeris_B0[i];
            targetDataDifferenceBandSPOTS1MERISRRB2[i] = sourceDataBandS1_B2[i] + sourceDataBandMeris_B2[i];
            targetDataDifferenceBandSPOTS1MERISRRB3[i] = sourceDataBandS1_B3[i] + sourceDataBandMeris_B3[i];
            targetDataDifferenceBandSPOTPMERISRRB0[i] = sourceDataBandP_B0[i] + sourceDataBandMeris_B0[i];
            targetDataDifferenceBandSPOTPMERISRRB2[i] = sourceDataBandP_B2[i] + sourceDataBandMeris_B2[i];
            targetDataDifferenceBandSPOTPMERISRRB3[i] = sourceDataBandP_B3[i] + sourceDataBandMeris_B3[i];

        }

        makeFilledBand(targetDataDifferenceBandSPOTS1MERISRRB0, sourceWidth, sourceHeight, targetTileDifferenceBandSPOTS1MERISRRB0);
        makeFilledBand(targetDataDifferenceBandSPOTS1MERISRRB2, sourceWidth, sourceHeight, targetTileDifferenceBandSPOTS1MERISRRB2);
        makeFilledBand(targetDataDifferenceBandSPOTS1MERISRRB3, sourceWidth, sourceHeight, targetTileDifferenceBandSPOTS1MERISRRB3);
        makeFilledBand(targetDataDifferenceBandSPOTPMERISRRB0, sourceWidth, sourceHeight, targetTileDifferenceBandSPOTPMERISRRB0);
        makeFilledBand(targetDataDifferenceBandSPOTPMERISRRB2, sourceWidth, sourceHeight, targetTileDifferenceBandSPOTPMERISRRB2);
        makeFilledBand(targetDataDifferenceBandSPOTPMERISRRB3, sourceWidth, sourceHeight, targetTileDifferenceBandSPOTPMERISRRB3);

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
            super(SpotVgtMerisCompareOperator.class);
        }
    }
}
