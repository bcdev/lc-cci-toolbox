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
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.util.ProductUtils;

import java.awt.Rectangle;
import java.io.IOException;
import java.util.Map;


/**
 * The sample operator implementation for an algorithm that outputs
 * all bands of the target product at once.
 */
@OperatorMetadata(alias = "WbCoOP",
                  description = "Algorithm for comparison of SWBD and SAR WB product",
                  authors = "",
                  version = "1.1",
                  copyright = "(C) 2010 by Brockmann Consult GmbH (beam@brockmann-consult.de)")
public class WaterBodyCompareOperator extends Operator {

    @SourceProduct
    private Product sourceProduct;
    @TargetProduct
    private Product targetProduct;

    //@Parameter(rasterDataNodeType = Band.class)
    private String sourceBandName1 = "SAR_Waterbody";

    //@Parameter(rasterDataNodeType = Band.class)
    private String sourceBandName2 = "land_water_fraction";

    /*
    @Parameter(valueSet = {SOBEL_OPERATOR, SCHARR_OPERATOR},
            defaultValue = SCHARR_OPERATOR, description = "Gradient Operator")
    private String operator;
    */

    private String targetBandNameSAREdge = "SAR_WB_EDGE";
    private String targetBandNameSWBDEdge = "SWBD_WB_EDGE";


    private Band sourceBand1;
    private Band sourceBand2;

    private Band targetBandSAREdge;
    private Band targetBandSWBDEdge;

    static final int maxKernelRadius = 1;
    static final int minKernelRadius = 0;


    /**********************************************************************************************/
    /************ Threshold Biomodality  (Unimpeachable Cayulas Thing = 0.7 !!!!!!!) **************/
    /**
     * ******************************************************************************************
     */

    static String productType;
    private static final String SOBEL_OPERATOR = "Sobel Operator";
    private static final String SCHARR_OPERATOR = "Scharr Operator";

    static double kernelEdgeValue;
    static double kernelCentreValue;
    static double weightingFactor;
    static int convolutionFilterKernelRadius;

    /**
     * Default constructor. The graph processing framework
     * requires that an operator has a default constructor.
     */
    public WaterBodyCompareOperator() {
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

        copyGeocodingForSameTarget();

        productType = sourceProduct.getProductType();
        System.out.printf("Product_Type:  %s  \n", productType);

        sourceBand1 = sourceProduct.getBand(sourceBandName1);
        sourceBand2 = sourceProduct.getBand(sourceBandName2);

       /*
       if (SOBEL_OPERATOR.equals(operator)) {
            kernelEdgeValue = 1.0;
            kernelCentreValue = 2.0;
            convolutionFilterKernelRadius=3;
        }
        if (SCHARR_OPERATOR.equals(operator)) {
            kernelEdgeValue = 3.0;
            kernelCentreValue = 10.0;
            convolutionFilterKernelRadius=3;
        }

        WaterBodyCompareOperator.weightingFactor = 2 * (2 * WaterBodyCompareOperator.kernelEdgeValue + WaterBodyCompareOperator.kernelCentreValue);
        System.out.printf("************    FRONTS WEIGHTING FACTOR:    %f) \n", WaterBodyCompareOperator.weightingFactor);
        */

        // TargetBands
        targetBandSAREdge = targetProduct.addBand(targetBandNameSAREdge, ProductData.TYPE_INT8);
        targetBandSWBDEdge = targetProduct.addBand(targetBandNameSWBDEdge, ProductData.TYPE_INT8);

    }


    private Product copyGeocodingForSameTarget() {
        targetProduct = new Product("SAR_WB_Compare",
                                    "org.esa.beam",
                                    sourceProduct.getSceneRasterWidth(),
                                    sourceProduct.getSceneRasterHeight());
        ProductSubsetDef def = new ProductSubsetDef();
        Product sourceSubsetProduct = null;
        def.setRegion(new Rectangle(0, 0, sourceProduct.getSceneRasterWidth(), sourceProduct.getSceneRasterHeight()));
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


        Tile sourceTileSAR = getSourceTile(sourceBand1, targetRectangle);
        Tile sourceTileSWBD = getSourceTile(sourceBand2, targetRectangle);

        Tile targetTileSAREdge = targetTiles.get(targetBandSAREdge);
        Tile targetTileSWBDEdge = targetTiles.get(targetBandSWBDEdge);


        final int[] sourceDataSAR = sourceTileSAR.getSamplesInt();
        final int[] sourceDataSWBD = sourceTileSWBD.getSamplesInt();


        int targetWidth = targetRectangle.width;
        int targetHeight = targetRectangle.height;

        PreparingOfSourceBand preparedSourceBand = new PreparingOfSourceBand();
        preparedSourceBand.preparedOfSourceBand(sourceDataSAR,
                                                sourceDataSWBD,
                                                targetWidth,
                                                targetHeight);


        EdgeOperator edge = new EdgeOperator();
        byte[] edgeSourceDataSAR = edge.computeEdge(sourceDataSAR,
                                                    targetWidth,
                                                    targetHeight);

        byte[] edgeSourceDataSWBD = edge.computeEdge(sourceDataSWBD,
                                                     targetWidth,
                                                     targetHeight);

        //System.out.printf("targetWidth:  %d  \n", targetWidth);
        //System.out.printf("targetHeight:  %d  \n", targetHeight);


        WaterBodyCompareOperator.makeFilledBand(edgeSourceDataSAR, targetWidth, targetHeight,
                                                targetTileSAREdge);

        WaterBodyCompareOperator.makeFilledBand(edgeSourceDataSWBD, targetWidth, targetHeight,
                                                targetTileSWBDEdge);


    }

    static void makeFilledBand
            (
                    byte[] inputData,
                    int inputDataWidth,
                    int inputDataHeight,
                    Tile targetTileOutputBand) {

        for (int y = targetTileOutputBand.getMinY(); y <= targetTileOutputBand.getMaxY(); y++) {
            for (int x = targetTileOutputBand.getMinX(); x <= targetTileOutputBand.getMaxX(); x++) {

                //System.out.printf("X, Y, Index %d %d %d  \n", x, y, y * (inputDataWidth) + x);
                targetTileOutputBand.setSample(x, y,
                                               inputData[((y - targetTileOutputBand.getMinY()) * inputDataWidth + (x - targetTileOutputBand.getMinX()))]);
            }
        }
        //System.out.printf("X, Y, Index %d %d  %d %d \n", inputDataWidth,targetTileOutputBand.getMaxY(), inputDataHeight,targetTileOutputBand.getMaxX());
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
            super(WaterBodyCompareOperator.class);
        }
    }
}
