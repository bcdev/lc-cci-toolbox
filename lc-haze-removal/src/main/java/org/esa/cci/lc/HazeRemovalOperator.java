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
import org.esa.beam.util.math.Histogram;
import org.esa.beam.util.math.Range;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.util.Arrays;
import java.util.Map;


/**
 * The sample operator implementation for an algorithm that outputs
 * all bands of the target product at once.
 */
@OperatorMetadata(alias = "HazeOp",
        description = "algorithm for haze removal",
        authors = "",
        version = "1.0",
        copyright = "(C) 2010 by Brockmann Consult GmbH (beam@brockmann-consult.de)")
public class HazeRemovalOperator extends Operator {

    @SourceProduct
    private Product sourceProduct;
    @TargetProduct
    private Product targetProduct;


    private String[] bandNames = new String[]{
            "radiance_1",
            "radiance_2",
            "radiance_3",
            "radiance_4",
            "radiance_5",
            "radiance_6",
            "radiance_7",
            "radiance_8",
            "radiance_9",
            "radiance_10",
            "radiance_11",
            "radiance_12",
            "radiance_13",
            "radiance_14",
            "radiance_15"
    };

    private String sourceBandNameRad3 = bandNames[2];
    private String sourceBandNameRad7 = bandNames[6];
    private String sourceBandNameFlag = "cloud_classif_flags";


    private String targetBandNameTach = "tasseled_cup_haze";
    private String targetBandNameHot = "hot";
    private String targetBandNameHotLevel = "hot_level";


    private Band targetBandTach;
    private Band targetBandHot;
    private Band targetBandHotLevel;

    static final int KernelRadius = 0;
    static final double tasseledCapFactorBlue = 0.846; //S2 0.846  MERIS 0.223
    static final double tasseledCapFactorRed = -0.464; //S2 -0.464 MERIS 0.120

    static final int standardHistogramBins = 255;
    static final int windowOverlap = 50;
    static final double leftHistoAreaSkipped = 0.02;
    static final double rightHistoAreaSkipped = 0.00;

    /*********************************************************************************************/
    /************           Haze Removal for MERIS N1 data !!!!!!!)                  *************/
    /**
     * *****************************************************************************************
     */


    /**
     * Default constructor. The graph processing framework
     * requires that an operator has a default constructor.
     */
    public HazeRemovalOperator() {
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

        targetProduct = new Product("HazeREmovedProducte", "NO_HAZE",
                sourceProduct.getSceneRasterWidth(), sourceProduct.getSceneRasterHeight());

        String productType = sourceProduct.getProductType();
        //System.out.printf("Product_Type:  %s  \n", productType);

        // pixel geocoding attached --> see beam.config
        ProductUtils.copyGeoCoding(sourceProduct, targetProduct);


        for (String bandName : bandNames) {
            Band sourceBand = sourceProduct.getBand(bandName);
            Band targetBand = targetProduct.addBand(bandName, ProductData.TYPE_FLOAT64);
            ProductUtils.copySpectralBandProperties(sourceBand, targetBand);
            targetBand.setUnit(sourceBand.getUnit());
        }

        ProductUtils.copyFlagBands(sourceProduct, targetProduct, true);
        ProductUtils.copyFlagCodings(sourceProduct, targetProduct);


        targetBandHot = targetProduct.addBand(targetBandNameHot, ProductData.TYPE_FLOAT64);
        targetBandTach = targetProduct.addBand(targetBandNameTach, ProductData.TYPE_FLOAT64);
        targetBandHotLevel = targetProduct.addBand(targetBandNameHotLevel, ProductData.TYPE_INT32);

        int preferredWidth = Math.min(targetProduct.getSceneRasterWidth(), 500);
        int preferredHeight = Math.min(targetProduct.getSceneRasterHeight(), 500);
        //int preferredWidth =  targetProduct.getSceneRasterWidth();
        //int preferredHeight =  targetProduct.getSceneRasterHeight();
        targetProduct.setPreferredTileSize(preferredWidth, preferredHeight);
    }


    @Override
    public void computeTileStack(Map<Band, Tile> targetTileMap, Rectangle targetRectangle, ProgressMonitor pm) throws OperatorException {

        Rectangle sourceRectangle = new Rectangle(targetRectangle);

        int [] counterValue = new int[2];
        counterValue[0] = 0;     // valid pixel
        counterValue[1] = 0;     // clear pixel


        Tile[] sourceTiles = new Tile[bandNames.length];
        Tile[] targetTiles = new Tile[bandNames.length];
        for (int i = 0; i < bandNames.length; i++) {
            String bandName = bandNames[i];
            sourceTiles[i] = getSourceTile(sourceProduct.getBand(bandName), sourceRectangle);
            targetTiles[i] = targetTileMap.get(targetProduct.getBand(bandName));
        }
        Tile sourceTileRad3 = sourceTiles[2];
        Tile sourceTileRad7 = sourceTiles[6];
        Tile sourceTileFlag = getSourceTile(sourceProduct.getBand(sourceBandNameFlag), sourceRectangle);

        Tile targetTileTach = targetTileMap.get(targetBandTach);
        Tile targetTileHot = targetTileMap.get(targetBandHot);
        Tile targetTileHotLevel = targetTileMap.get(targetBandHotLevel);


        FlagDetector flagDetector = new MerisFlagDetector(sourceTileFlag, sourceRectangle);

        final double[] sourceDataBlue = sourceTileRad3.getSamplesDouble();
        final double[] sourceDataRed = sourceTileRad7.getSamplesDouble();
        double[] sourceData;

        int sourceWidth = sourceRectangle.width;
        int sourceHeight = sourceRectangle.height;
        int sourceLength = sourceHeight * sourceWidth;

        final double[] tachArray = new double[sourceLength];
        Arrays.fill(tachArray, Double.NaN);

        final double[] hotArray = new double[sourceLength];
        Arrays.fill(hotArray, Double.NaN);

        final int[] flagArray = new int[sourceLength];
        Arrays.fill(flagArray, 0);

        PreparingOfSourceBand preparedSourceBand = new PreparingOfSourceBand();
        preparedSourceBand.preparedOfSourceBand(flagDetector, flagArray, targetRectangle);


        double correlationCefficient = BandCorrelationTest.getPearsonCorrelation(sourceDataBlue, sourceDataRed, flagArray);
        //System.out.printf("CorelationCoefficient:  %f  \n", correlationCefficient);

        if (correlationCefficient > 0.8) {

            TasseledCapTransformation tasseledCapBand = new TasseledCapTransformation();
            double meanValue = tasseledCapBand.calculateTasseledCapTransformationHaze(sourceDataBlue,
                    sourceDataRed,
                    sourceWidth,
                    sourceHeight,
                    tachArray,
                    flagArray,
                    counterValue);

            makeFilledBand(tachArray, sourceWidth, sourceHeight, targetTileTach, sourceRectangle, HazeRemovalOperator.KernelRadius);

            HotTransformation HotBand = new HotTransformation();
            HotBand.calculateHOTBand(sourceDataBlue,
                    sourceDataRed,
                    sourceWidth,
                    sourceHeight,
                    tachArray,
                    flagArray,
                    hotArray,
                    meanValue,
                    counterValue);

            makeFilledBand(hotArray, sourceWidth, sourceHeight, targetTileHot, sourceRectangle, HazeRemovalOperator.KernelRadius);

            HotHistogram hotHistogram = new HotHistogram();
            Histogram histogramHotAll = hotHistogram.compute(tachArray,
                    hotArray,
                    sourceWidth,
                    sourceHeight,
                    flagArray,
                    meanValue,
                    counterValue);


            System.out.printf("counterValid   counterClear:  %d %d \n", counterValue[0], counterValue[1]);
            Range range = histogramHotAll.findRange(leftHistoAreaSkipped, rightHistoAreaSkipped);
            int minBinIndex = histogramHotAll.getBinIndex(range.getMin());
            int maxBinIndex = histogramHotAll.getBinIndex(range.getMax());
            int[] binIndexCounts = histogramHotAll.getBinCounts();

            HotLevelArray hotLevel = new HotLevelArray();
            final int[] hotLevelArray = hotLevel.compute(hotArray,
                    sourceWidth,
                    sourceHeight,
                    flagArray,
                    histogramHotAll,
                    binIndexCounts);

            makeFilledBand(hotLevelArray, sourceWidth, sourceHeight, targetTileHotLevel, sourceRectangle, HazeRemovalOperator.KernelRadius);

            double[] deltaBandHazeRemoval = new double[maxBinIndex + 1];

            for (int i = 0; i < bandNames.length; i++) {

                sourceData = sourceTiles[i].getSamplesDouble();

                BandHistogram bandHazeAllClassHistogram = new BandHistogram();
                Histogram histogramBandHazeAllClass = bandHazeAllClassHistogram.computeHazeAllClass(sourceData,
                        hotLevelArray,
                        sourceWidth,
                        sourceHeight,
                        flagArray,
                        minBinIndex,
                        counterValue);

                Range rangeHazeAllClass = histogramBandHazeAllClass.findRange(leftHistoAreaSkipped, rightHistoAreaSkipped);


                Arrays.fill(deltaBandHazeRemoval, 0.0);

                for (int j = minBinIndex; j < maxBinIndex + 1; j++) {
                    BandHistogram bandHazeOneClassHistogram = new BandHistogram();
                    Histogram histogramBandHazeOneClass = bandHazeOneClassHistogram.computeHazeOneClass(sourceData,
                            hotLevelArray,
                            sourceWidth,
                            sourceHeight,
                            flagArray,
                            j,
                            counterValue);

                    Range rangeHazeOneClass = histogramBandHazeOneClass.findRange(leftHistoAreaSkipped, rightHistoAreaSkipped);
                    deltaBandHazeRemoval[j] = rangeHazeOneClass.getMin() - rangeHazeAllClass.getMin();
                }

                for (int k = 0; k < sourceHeight; k++) {
                    for (int m = 0; m < sourceWidth; m++) {
                        for (int n = 0; n < maxBinIndex + 1; n++) {
                            int kk = k * (sourceWidth) + m;
                            if (hotLevelArray[kk] == n) {
                                sourceData[kk] = sourceData[kk] - deltaBandHazeRemoval[n];
                            }
                        }
                    }
                }

                makeFilledBand(sourceData, sourceWidth, sourceHeight, targetTiles[i], sourceRectangle, HazeRemovalOperator.KernelRadius);
            }

        } else {

            for (int i = 0; i < bandNames.length; i++) {
                sourceData = sourceTiles[i].getSamplesDouble();
                for (int k = 0; k < sourceHeight; k++) {
                    for (int m = 0; m < sourceWidth; m++) {
                        int kk = k * (sourceWidth) + m;
                        sourceData[kk] = sourceData[kk];
                    }
                }
                makeFilledBand(sourceData, sourceWidth, sourceHeight, targetTiles[i], sourceRectangle, HazeRemovalOperator.KernelRadius);
                makeFilledBand(tachArray, sourceWidth, sourceHeight, targetTileTach, sourceRectangle, HazeRemovalOperator.KernelRadius);
                makeFilledBand(hotArray, sourceWidth, sourceHeight, targetTileHot, sourceRectangle, HazeRemovalOperator.KernelRadius);
                int[] hotLevelArrayZeros = new int[sourceLength];
                Arrays.fill(hotLevelArrayZeros, -1);
                makeFilledBand(hotLevelArrayZeros, sourceWidth, sourceHeight, targetTileHotLevel, sourceRectangle, HazeRemovalOperator.KernelRadius);

            }
        }
    }

    static void makeFilledBand
            (
                    double[] inputData,
                    int inputDataWidth,
                    int inputDataHeight,
                    Tile targetTileOutputBand,
                    Rectangle targetRectangle,
                    int mkr) {

        int xLocation = targetRectangle.x;
        int yLocation = targetRectangle.y;

        for (int y = 0; y < inputDataHeight; y++) {
            for (int x = 0; x < inputDataWidth; x++) {
                targetTileOutputBand.setSample(x + xLocation - mkr, y + yLocation - mkr, inputData[y * (inputDataWidth) + x]);

            }
        }
    }


    static void makeFilledBand
            (
                    int[] inputData,
                    int inputDataWidth,
                    int inputDataHeight,
                    Tile targetTileOutputBand,
                    Rectangle targetRectangle,
                    int mkr) {

        int xLocation = targetRectangle.x;
        int yLocation = targetRectangle.y;

        for (int y = 0; y < inputDataHeight; y++) {
            for (int x = 0; x < inputDataWidth; x++) {
                targetTileOutputBand.setSample(x + xLocation - mkr, y + yLocation - mkr, inputData[y * (inputDataWidth) + x]);

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
            super(HazeRemovalOperator.class);
        }
    }
}
