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
import org.esa.beam.framework.dataio.ProductSubsetDef;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.util.ProductUtils;
import org.python.indexer.ast.NEllipsis;

import javax.media.jai.BorderExtenderConstant;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;


/**
 * The sample operator implementation for an algorithm that outputs
 * all bands of the target product at once.
 */
@OperatorMetadata(alias = "GeoToolOP",
                  description = "Algorithm for geolocation test of SPOT and MERIS RRG products",
                  authors = "",
                  version = "1.0",
                  copyright = "(C) 2011 by Brockmann Consult GmbH (beam@brockmann-consult.de)")

public class Sentinel2GeoToolOperator extends Operator {

    @SourceProduct
    private Product sourceProductS2A;

    @SourceProduct
    private Product sourceProductS2B;

//    S2A__S2B_MSIL1C_20170806T180921__20170811T182429__T11SQR
//    S2A__S2B_MSIL1C_20170816T182431__20170811T182429__T11SQR
//    S2A__S2B_MSIL1C_20170820T174911__20170825T174909__T12RXT
//    S2A__S2B_MSIL1C_20170821T171901__20170816T171859__T13RGK
//    S2A__S2B_MSIL1C_20170808T170851__20170823T170849__T14QKF
//    S2A__S2B_MSIL1C_20170808T170851__20170803T170849__T14QLL
//    S2A__S2B_MSIL1C_20170816T162901__20170831T162859__T15PWS
//    S2A__S2B_MSIL1C_20170826T164851__20170831T162859__T15PWS
//    S2A__S2B_MSIL1C_20170803T161901__20170818T161859__T16QBJ
//    S2A__S2B_MSIL1C_20170823T161901__20170818T161859__T16QBJ


    @TargetProduct
    private Product targetProduct;

    @Parameter(rasterDataNodeType = Band.class)
    private String sourceBandName;



    private Band sourceBandS2A;
    private Band sourceBandS2B;
    private Band sourceBandS2ACloud;
    private Band sourceBandS2ACirrus;
    private Band sourceBandS2BCloud;
    private Band sourceBandS2BCirrus;

    static String sourceBandS2CloudName;
    static String sourceBandS2CirrusName;


    private Band targetCopySourceBandS2A;
    static String targetCopySourceBandNameS2A;
    private Band targetCopySourceBandS2B;
    static String targetCopySourceBandNameS2B;
    private Band targetMaxCorr;
    static String targetMaxCorrBandName;
    private Band targetMaxCorrDir;
    static String targetMaxCorrDirBandName;
    private Band targetCloudData;
    static String targetCloudDataBandName;

    static int maxKernelRadius = 1;
    static int minKernelRadius = 0;
    static int corrKernelRadius = 1;

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

        // copyGeocodingForSmallerTarget(maxKernelRadius);
        copyGeocodingForSmallerTarget(maxKernelRadius);
        targetCopySourceBandNameS2A = sourceBandName + "_S2A";
        targetCopySourceBandNameS2B = sourceBandName + "_S2B";;
        targetMaxCorrBandName = "maximum_of _correlation_coefficient";
        targetMaxCorrDirBandName = "maximum_of _correlation_coefficient_drift";
        sourceBandS2CloudName = "opaque_clouds";
        sourceBandS2CirrusName = "cirrus_clouds";
        targetCloudDataBandName = "cloud";

        sourceBandS2A = sourceProductS2A.getBand(sourceBandName);
        sourceBandS2B = sourceProductS2B.getBand(sourceBandName);

        sourceBandS2ACloud = sourceProductS2A.getBand(sourceBandS2CloudName);
        sourceBandS2ACirrus = sourceProductS2A.getBand(sourceBandS2CirrusName);
        sourceBandS2BCloud= sourceProductS2B.getBand(sourceBandS2CloudName);
        sourceBandS2BCirrus = sourceProductS2B.getBand(sourceBandS2CirrusName);;


        targetCopySourceBandS2A = targetProduct.addBand(targetCopySourceBandNameS2A, ProductData.TYPE_FLOAT32);
        targetCopySourceBandS2A.setUnit(sourceBandS2A.getUnit());
        targetCopySourceBandS2B = targetProduct.addBand(targetCopySourceBandNameS2B, ProductData.TYPE_FLOAT32);
        targetCopySourceBandS2B.setUnit(sourceBandS2B.getUnit());
        targetMaxCorr = targetProduct.addBand(targetMaxCorrBandName, ProductData.TYPE_FLOAT32);
        targetMaxCorrDir = targetProduct.addBand(targetMaxCorrDirBandName, ProductData.TYPE_FLOAT32);
        targetCloudData =  targetProduct.addBand(targetCloudDataBandName, ProductData.TYPE_INT16);

        targetProduct.setPreferredTileSize(122, 122); //(366,366) (915, 915); //1500
        // targetProduct.setPreferredTileSize(new Dimension(targetProduct.getSceneRasterWidth(), targetProduct.getSceneRasterHeight()));
    }

    private Product copyGeocodingForSmallerTarget(int maxKernelRadius) {
        targetProduct = new Product("beam_geotool_BC",
                                    "org.esa.beam",
                                    sourceProductS2A.getSceneRasterWidth() - 2 * maxKernelRadius,
                                    sourceProductS2A.getSceneRasterHeight() - 2 * maxKernelRadius);
        ProductSubsetDef def = new ProductSubsetDef();
        Product sourceSubsetProduct = null;
        def.setRegion(new Rectangle(maxKernelRadius,
                                    maxKernelRadius,
                                    sourceProductS2A.getSceneRasterWidth() - maxKernelRadius,
                                    sourceProductS2A.getSceneRasterHeight() - maxKernelRadius));
        try {
            sourceSubsetProduct = sourceProductS2A.createSubset(def, "SourceSubsetProduct", "desc");
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        // Some target products may require more aid from ProductUtils methods...
        ProductUtils.copyGeoCoding(sourceSubsetProduct, targetProduct);


        return targetProduct;
    }


    @Override
    public void computeTileStack(Map<Band, Tile> targetTiles, Rectangle targetRectangle, ProgressMonitor pm) throws OperatorException {

        Rectangle sourceRectangle = new Rectangle(targetRectangle);
        sourceRectangle.grow(maxKernelRadius, maxKernelRadius);
        // todo check translate requested?
        //sourceRectangle.translate(maxKernelRadius, maxKernelRadius);


        Tile sourceTileS2A = getSourceTile(sourceBandS2A, sourceRectangle, new BorderExtenderConstant(new double[]{Double.NaN}));
        Tile sourceTileS2B = getSourceTile(sourceBandS2B, sourceRectangle, new BorderExtenderConstant(new double[]{Double.NaN}));

        Tile sourceTileS2ACloud = getSourceTile(sourceBandS2ACloud, sourceRectangle, new BorderExtenderConstant(new double[]{Double.NaN}));
        Tile sourceTileS2ACirrus = getSourceTile(sourceBandS2ACirrus, sourceRectangle, new BorderExtenderConstant(new double[]{Double.NaN}));
        Tile sourceTileS2BCloud = getSourceTile(sourceBandS2BCloud, sourceRectangle, new BorderExtenderConstant(new double[]{Double.NaN}));
        Tile sourceTileS2BCirrus = getSourceTile(sourceBandS2BCirrus, sourceRectangle, new BorderExtenderConstant(new double[]{Double.NaN}));


        Tile targetTileCopySourceBandS2A = targetTiles.get(targetCopySourceBandS2A);
        Tile targetTileCopySourceBandS2B = targetTiles.get(targetCopySourceBandS2B);
        Tile targetTileMaxCorr = targetTiles.get(targetMaxCorr);
        Tile targetTileMaxCorrDir = targetTiles.get(targetMaxCorrDir);
        Tile targetTileCloudData =  targetTiles.get(targetCloudData);


        final float[] sourceDataS2A = sourceTileS2A.getSamplesFloat();
        final float[] sourceDataS2B = sourceTileS2B.getSamplesFloat();

        final float[] sourceDataS2ACloud = sourceTileS2ACloud.getSamplesFloat();
        final float[] sourceDataS2ACirrus = sourceTileS2ACirrus.getSamplesFloat();
        final float[] sourceDataS2BCloud = sourceTileS2BCloud.getSamplesFloat();
        final float[] sourceDataS2BCirrus = sourceTileS2BCirrus.getSamplesFloat();

        int sourceWidth = sourceRectangle.width;
        int sourceHeight = sourceRectangle.height;
        int sourceLength = sourceWidth * sourceHeight;
        System.out.printf("sourceWidth: %d sourceHeight: %d\n", sourceWidth, sourceHeight);

        final float[][] sourceDataMoveS2A = new float[sourceWidth][sourceHeight];
        final float[][] sourceDataMoveS2B = new float[sourceWidth][sourceHeight];
        final float[][] correlationARRAY = new float[sourceWidth][sourceHeight];
        final float[] correlationMaxARRAY = new float[sourceLength];
        final float[] correlationDirARRAY = new float[sourceLength];
        final int[] cloudData = new int[sourceLength];

        Arrays.fill(correlationMaxARRAY, Float.MIN_VALUE);
        Arrays.fill(correlationDirARRAY, Float.MIN_VALUE);

        float[] kernelSizeArrayS2A = new float[(2 * corrKernelRadius + 1) * (2 * corrKernelRadius + 1)];
        float[] kernelSizeArrayS2B = new float[(2 * corrKernelRadius + 1) * (2 * corrKernelRadius + 1)];

        float direction = 0.0f;

        makeFilledBand(sourceDataS2A, targetRectangle, targetTileCopySourceBandS2A, Sentinel2GeoToolOperator.maxKernelRadius);
        makeFilledBand(sourceDataS2B, targetRectangle, targetTileCopySourceBandS2B, Sentinel2GeoToolOperator.maxKernelRadius);
        // makeFilledBand(sourceDataS2B, sourceWidth, sourceHeight, targetTileCopySourceBandS2B, Sentinel2GeoToolOperator.maxKernelRadius);

        int indexS2A;
        for (int j = 0; j < sourceHeight; j++) {
            for (int i = 0; i < sourceWidth; i++) {
                indexS2A = j * (sourceWidth) + i;
                if(sourceDataS2ACloud[indexS2A] == 0 &&  sourceDataS2ACirrus[indexS2A]== 0
                        && sourceDataS2BCloud[indexS2A] == 0 &&  sourceDataS2BCirrus[indexS2A]== 0 ){
                    sourceDataMoveS2A[i][j] = sourceDataS2A[indexS2A];
                    cloudData[indexS2A] = 0;
                }  else {
                    sourceDataMoveS2A[i][j] = Float.NaN;
                    cloudData[indexS2A] = 1;
                }
            }
        }

        // direction
        //  NW N NE    1 4 7
        //   W - E     2 5 8
        //  SW S SE    3 6 9
        //  0 noDataValue


        int indexS2B;
        for (int k = -1; k < 2; k++) {
            for (int l = -1; l < 2; l++) {

                direction += 1;

                for (int j = 0; j < sourceHeight; j++) {
                    for (int i = 0; i < sourceWidth; i++) {
                        sourceDataMoveS2B[i][j] = Float.NaN;
                        correlationARRAY[i][j] = Float.NaN;
                    }
                }

                for (int j = 1; j < sourceHeight - 1; j++) {
                    for (int i = 1; i < sourceWidth - 1; i++) {
                        indexS2B = (j - l) * (sourceWidth) + (i - k);
                        if(sourceDataS2ACloud[indexS2B] == 0 &&  sourceDataS2ACirrus[indexS2B]== 0 &&
                                sourceDataS2BCloud[indexS2B] == 0 &&  sourceDataS2BCirrus[indexS2B]== 0 ){
                            sourceDataMoveS2B[i][j] = sourceDataS2B[indexS2B];
                        }


                    }
                }

                int indexValid;
                for (int j = 1; j < sourceHeight - 1; j++) {
                    for (int i = 1; i < sourceWidth - 1; i++) {

                        indexValid =0;
                        for (int jj = -corrKernelRadius; jj < corrKernelRadius + 1; jj++) {
                            for (int ii = -corrKernelRadius; ii < corrKernelRadius + 1; ii++) {
                                // System.out.printf("1. width height 3x3matrix width height:  %d  %d  %d   \n", i + ii, j + jj, (jj + corrKernelRadius) * (2 * corrKernelRadius + 1) + (ii + corrKernelRadius));
                                //kernelSizeArrayS2A[(jj + corrKernelRadius) * (2 * corrKernelRadius + 1) + (ii + corrKernelRadius)] = sourceDataMoveS2A[i + ii][j + jj];
                                //kernelSizeArrayS2B[(jj + corrKernelRadius) * (2 * corrKernelRadius + 1) + (ii + corrKernelRadius)] = sourceDataMoveS2B[i + ii][j + jj];
                                //System.out.printf("2. width height 3x3matrix width height:  %d  %d  %d   \n", i + ii, j + jj, (jj + corrKernelRadius) * (2 * corrKernelRadius + 1) + (ii + corrKernelRadius));
                                if (!Float.isNaN(sourceDataMoveS2A[i + ii][j + jj]) && !Float.isNaN(sourceDataMoveS2B[i + ii][j + jj])) {
                                    kernelSizeArrayS2A[indexValid] = sourceDataMoveS2A[i + ii][j + jj];
                                    kernelSizeArrayS2B[indexValid] = sourceDataMoveS2B[i + ii][j + jj];
                                    indexValid++;
                                }
                            }
                        }
                        if (indexValid > 1) {
                            correlationARRAY[i][j] = Sentinel2GeoToolCorrelation.getPearsonCorrelation1(kernelSizeArrayS2A, kernelSizeArrayS2B, indexValid);
                        }else {
                            correlationARRAY[i][j] = -0.1f;
                        }
                    }
                }

                for (int j = 0; j < sourceHeight; j++) {
                    for (int i = 0; i < sourceWidth; i++) {

                        if (correlationARRAY[i][j] >= correlationMaxARRAY[j * sourceWidth + i] &&
                                correlationARRAY[i][j] > -0.05f) {

                            correlationMaxARRAY[j * sourceWidth + i] = correlationARRAY[i][j];
                            correlationDirARRAY[j * sourceWidth + i] = direction;
                        }
                    }
                }
            }
        }

        makeFilledBand(correlationMaxARRAY, targetRectangle, targetTileMaxCorr, Sentinel2GeoToolOperator.maxKernelRadius);
        makeFilledBand(correlationDirARRAY, targetRectangle, targetTileMaxCorrDir, Sentinel2GeoToolOperator.maxKernelRadius);
        makeFilledBand(cloudData, targetRectangle, targetTileCloudData, Sentinel2GeoToolOperator.maxKernelRadius);
        //makeFilledBand(correlationDirARRAY, sourceWidth, sourceHeight, targetTileMaxCorrDir, Sentinel2GeoToolOperator.maxKernelRadius);


        System.out.printf("source rectangle width height:  %d  %d   \n",sourceRectangle.width, sourceRectangle.height);
        System.out.printf("source rectangle xPos yPos:  %d  %d   \n",sourceRectangle.x, sourceRectangle.y);
        System.out.printf("target product width height:  %d  %d   \n",targetRectangle.width, targetRectangle.height );
        System.out.printf("target product xPos yPos:  %d  %d   \n",targetRectangle.x, targetRectangle.y );

    }


    private static void makeFilledBand
            (
                    float[] inputData,
                    Rectangle targetRectangle,
                    Tile targetTileOutputBand,
                    int mkr) {

        int xLocation = targetRectangle.x;
        int yLocation = targetRectangle.y;
        int inputDataWidth = targetRectangle.width + 2 * mkr;
        int inputDataHeight = targetRectangle.height + 2 * mkr;

        //System.out.printf("rectangle:  %d  %d _______________ rectangle_target_input_data:  %d  %d  \n", targetRectangle.width, targetRectangle.height,inputDataWidth, inputDataHeight);

        for (int y = mkr; y < inputDataHeight - mkr; y++) {
            for (int x = mkr; x < inputDataWidth - mkr; x++) {
                targetTileOutputBand.setSample(x - mkr + xLocation, y - mkr + yLocation, inputData[y * (inputDataWidth) + x]);
            }
        }
    }

    private static void makeFilledBand
            (
                    int[] inputData,
                    Rectangle targetRectangle,
                    Tile targetTileOutputBand,
                    int mkr) {

        int xLocation = targetRectangle.x;
        int yLocation = targetRectangle.y;
        int inputDataWidth = targetRectangle.width + 2 * mkr;
        int inputDataHeight = targetRectangle.height + 2 * mkr;

        //System.out.printf("rectangle:  %d  %d _______________ rectangle_target_input_data:  %d  %d  \n", targetRectangle.width, targetRectangle.height,inputDataWidth, inputDataHeight);

        for (int y = mkr; y < inputDataHeight - mkr; y++) {
            for (int x = mkr; x < inputDataWidth - mkr; x++) {
                targetTileOutputBand.setSample(x - mkr + xLocation, y - mkr + yLocation, inputData[y * (inputDataWidth) + x]);
            }
        }
    }

    static void makeFilledBand
            (
                    double[] inputData,
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

    static void makeFilledBand
            (
                    double[][] inputData,
                    int inputDataWidth,
                    int inputDataHeight,
                    Tile targetTileOutputBand1, Tile
                    targetTileOutputBand2, int mkr) {

        for (int y = mkr; y < inputDataHeight - mkr; y++) {
            for (int x = mkr; x < inputDataWidth - mkr; x++) {
                targetTileOutputBand1.setSample(x - mkr, y - mkr, inputData[0][y * (inputDataWidth) + x]);
                targetTileOutputBand2.setSample(x - mkr, y - mkr, inputData[1][y * (inputDataWidth) + x]);
            }
        }
    }

    static void makeFilledBand
            (
                    double[][] inputData,
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
                    int inputDataHeight
                    , Tile targetTileOutputBand,
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
            super(Sentinel2GeoToolOperator.class);
        }
    }
}
