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

public class GeoToolOperator extends Operator {

    @SourceProduct
    private Product sourceProduct;

    @TargetProduct
    private Product targetProduct;

    @Parameter(rasterDataNodeType = Band.class)
    private String sourceBandMasterName;

    @Parameter(rasterDataNodeType = Band.class)
    private String sourceBandSlaveName;


    private Band sourceBandMaster;
    private Band sourceBandSlave;


    private Band targetCopySourceBandMaster;
    static String targetCopySourceBandNameMaster;
    private Band targetCopySourceBandSlave;
    static String targetCopySourceBandNameSlave;
    private Band targetMaxCorr;
    static String targetMaxCorrBandName;
    private Band targetMaxCorrDir;
    static String targetMaxCorrDirBandName;


    static int maxKernelRadius = 3;
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
        targetCopySourceBandNameMaster = sourceBandMasterName;
        targetCopySourceBandNameSlave = sourceBandSlaveName;
        targetMaxCorrBandName = "maximum_of _correlation_coefficient";
        targetMaxCorrDirBandName = "maximum_of _correlation_coefficient_drift";

        sourceBandMaster = sourceProduct.getBand(sourceBandMasterName);
        sourceBandSlave = sourceProduct.getBand(sourceBandSlaveName);

        targetCopySourceBandMaster = targetProduct.addBand(targetCopySourceBandNameMaster, ProductData.TYPE_FLOAT64);
        targetCopySourceBandMaster.setUnit(sourceBandMaster.getUnit());
        targetCopySourceBandSlave = targetProduct.addBand(targetCopySourceBandNameSlave, ProductData.TYPE_FLOAT64);
        targetCopySourceBandSlave.setUnit(sourceBandSlave.getUnit());
        targetMaxCorr = targetProduct.addBand(targetMaxCorrBandName, ProductData.TYPE_FLOAT64);
        targetMaxCorrDir = targetProduct.addBand(targetMaxCorrDirBandName, ProductData.TYPE_FLOAT64);

        targetProduct.setPreferredTileSize
                (new Dimension(targetProduct.getSceneRasterWidth(), targetProduct.getSceneRasterHeight()));
    }

    private Product copyGeocodingForSmallerTarget(int maxKernelRadius) {
        targetProduct = new Product("beam_geotool_BC",
                                    "org.esa.beam",
                                    sourceProduct.getSceneRasterWidth() - 2 * maxKernelRadius,
                                    sourceProduct.getSceneRasterHeight() - 2 * maxKernelRadius);
        ProductSubsetDef def = new ProductSubsetDef();
        Product sourceSubsetProduct = null;
        def.setRegion(new Rectangle(maxKernelRadius,
                                    maxKernelRadius,
                                    sourceProduct.getSceneRasterWidth() - maxKernelRadius,
                                    sourceProduct.getSceneRasterHeight() - maxKernelRadius));
        try {
            sourceSubsetProduct = sourceProduct.createSubset(def, "SourceSubsetProduct", "desc");
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

        Tile sourceTileMaster = getSourceTile(sourceBandMaster, sourceRectangle, new BorderExtenderConstant(new double[]{Double.NaN}));
        Tile sourceTileSlave = getSourceTile(sourceBandSlave, sourceRectangle, new BorderExtenderConstant(new double[]{Double.NaN}));

        Tile targetTileCopySourceBandMaster = targetTiles.get(targetCopySourceBandMaster);
        Tile targetTileCopySourceBandSlave = targetTiles.get(targetCopySourceBandSlave);
        Tile targetTileMaxCorr = targetTiles.get(targetMaxCorr);
        Tile targetTileMaxCorrDir = targetTiles.get(targetMaxCorrDir);


        final double[] sourceDataMaster = sourceTileMaster.getSamplesDouble();
        final double[] sourceDataSlave = sourceTileSlave.getSamplesDouble();

        int sourceWidth = sourceRectangle.width;
        int sourceHeight = sourceRectangle.height;
        int sourceLength = sourceWidth * sourceHeight;

        final double[][] sourceDataMoveMaster = new double[sourceWidth][sourceHeight];
        final double[][] sourceDataMoveSlave = new double[sourceWidth][sourceHeight];
        final double[][] correlationARRAY = new double[sourceWidth][sourceHeight];
        final double[] correlationMaxARRAY = new double[sourceWidth * sourceHeight];
        final double[] correlationDirARRAY = new double[sourceWidth * sourceHeight];

        Arrays.fill(correlationMaxARRAY, Double.MIN_VALUE);
        Arrays.fill(correlationDirARRAY, Double.MIN_VALUE);

        double[] kernelSizeArrayMaster = new double[(2 * corrKernelRadius + 1) * (2 * corrKernelRadius + 1)];
        double[] kernelSizeArraySlave = new double[(2 * corrKernelRadius + 1) * (2 * corrKernelRadius + 1)];

        double direction = 0.0;

        makeFilledBand(sourceDataMaster, sourceWidth, sourceHeight, targetTileCopySourceBandMaster, GeoToolOperator.maxKernelRadius);
        makeFilledBand(sourceDataSlave, sourceWidth, sourceHeight, targetTileCopySourceBandSlave, GeoToolOperator.maxKernelRadius);

        for (int j = 0; j < sourceHeight; j++) {
            for (int i = 0; i < sourceWidth; i++) {
                sourceDataMoveMaster[i][j] = sourceDataMaster[j * (sourceWidth) + i];
            }
        }


        for (int k = -1; k < 2; k++) {
            for (int l = -1; l < 2; l++) {

                direction += 1;

                for (int j = 0; j < sourceHeight; j++) {
                    for (int i = 0; i < sourceWidth; i++) {
                        sourceDataMoveSlave[i][j] = Double.NaN;
                        correlationARRAY[i][j] = Double.NaN;
                    }
                }

                for (int j = 1; j < sourceHeight - 1; j++) {
                    for (int i = 1; i < sourceWidth - 1; i++) {
                        sourceDataMoveSlave[i][j] = sourceDataSlave[(j - l) * (sourceWidth) + (i - k)];
                    }
                }

                for (int j = 1; j < sourceHeight - 1; j++) {
                    for (int i = 1; i < sourceWidth - 1; i++) {

                        for (int jj = -corrKernelRadius; jj < corrKernelRadius + 1; jj++) {
                            for (int ii = -corrKernelRadius; ii < corrKernelRadius + 1; ii++) {
                                // System.out.printf("1. width height 3x3matrix width height:  %d  %d  %d   \n", i + ii, j + jj, (jj + corrKernelRadius) * (2 * corrKernelRadius + 1) + (ii + corrKernelRadius));

                                kernelSizeArrayMaster[(jj + corrKernelRadius) * (2 * corrKernelRadius + 1) + (ii + corrKernelRadius)] =
                                        sourceDataMoveMaster[i + ii][j + jj];
                                kernelSizeArraySlave[(jj + corrKernelRadius) * (2 * corrKernelRadius + 1) + (ii + corrKernelRadius)] =
                                        sourceDataMoveSlave[i + ii][j + jj];
                                //System.out.printf("2. width height 3x3matrix width height:  %d  %d  %d   \n", i + ii, j + jj, (jj + corrKernelRadius) * (2 * corrKernelRadius + 1) + (ii + corrKernelRadius));
                            }
                        }
                        correlationARRAY[i][j] = GeoToolCorrelation.getPearsonCorrelation1(kernelSizeArrayMaster, kernelSizeArraySlave);
                    }
                }

                for (int j = 0; j < sourceHeight; j++) {
                    for (int i = 0; i < sourceWidth; i++) {

                        if (correlationARRAY[i][j] >= correlationMaxARRAY[j * sourceWidth + i]) {

                            correlationMaxARRAY[j * sourceWidth + i] = correlationARRAY[i][j];
                            correlationDirARRAY[j * sourceWidth + i] = direction;

                            // sourceDataMoveSPOT[i][j] = sourceDataSPOT[(j - l) * (sourceWidth) + (i - k)];
                        }
                    }
                }
            }
        }

        makeFilledBand(correlationMaxARRAY, sourceWidth, sourceHeight, targetTileMaxCorr, GeoToolOperator.maxKernelRadius);
        makeFilledBand(correlationDirARRAY, sourceWidth, sourceHeight, targetTileMaxCorrDir, GeoToolOperator.maxKernelRadius);


        //System.out.printf("source rectangle width height:  %d  %d   \n",sourceRectangle.width, sourceRectangle.height);
        //System.out.printf("collocate product width height:  %d  %d   \n",collocateProduct.getSceneRasterWidth(),collocateProduct.getSceneRasterHeight() );
    }


    static void makeFilledBand
            (
                    double[] inputData,
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
            super(GeoToolOperator.class);
        }
    }
}
