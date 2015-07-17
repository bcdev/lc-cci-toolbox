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
@OperatorMetadata(alias = "AvhrrGeoToolOP",
                  description = "Algorithm for image to image registration of AVHRR products",
                  authors = "",
                  version = "1.0",
                  copyright = "(C) 2011 by Brockmann Consult GmbH (beam@brockmann-consult.de)")



public class AvhrrGeoToolOperator extends Operator {

    @Parameter(valueSet = {ALBEDO_BASED, EDGE_DETECTION},
            defaultValue = ALBEDO_BASED, description = "chip matching algorithm")
    private String operator;

    @SourceProduct
    private Product sourceProductReference;
    private Product sourceProduct2Register;

    @Parameter(rasterDataNodeType = Band.class)
    private String sourceBandReferenceName;

    @Parameter(rasterDataNodeType = Band.class)
    private String sourceBand2RegisterName;


    @Parameter(rasterDataNodeType = Band.class)
    private String landWaterBandReferenceName;

    @Parameter(rasterDataNodeType = Band.class)
    private String landWaterBand2RegisterName;


    @Parameter(rasterDataNodeType = Band.class)
    private String cloudAlbedo1BandReferenceName;

    @Parameter(rasterDataNodeType = Band.class)
    private String cloudAlbedo2BandReferenceName;

    @Parameter(rasterDataNodeType = Band.class)
    private String cloudBT4BandReferenceName;


    @Parameter(rasterDataNodeType = Band.class)
    private String cloudAlbedo1Band2RegisterName;

    @Parameter(rasterDataNodeType = Band.class)
    private String cloudAlbedo2Band2RegisterName;

    @Parameter(rasterDataNodeType = Band.class)
    private String cloudBT4Band2RegisterName;


    @Parameter(rasterDataNodeType = Band.class)
    private String panoramaEffectBandReferenceName;

    @Parameter(rasterDataNodeType = Band.class)
    private String panoramaEffectBand2RegisterName;


    
    @TargetProduct
    private Product targetProduct;

    private Band sourceBandReference;
    private Band sourceBand2Register;
    private Band landWaterBandReference;
    private Band landWaterBand2Register;
    private Band panoramaEffectBandReference;
    private Band panoramaEffectBand2Register;
    private Band cloudAlbedo1BandReference;
    private Band cloudAlbedo1Band2Register;
    private Band cloudAlbedo2BandReference;
    private Band cloudAlbedo2Band2Register;
    private Band cloudBT4BandReference;
    private Band cloudBT4Band2Register;

    private Band targetCopySourceBandReference;
    static String targetCopySourceBandNameReference;
    private Band targetCopySourceBand2Register;
    static String targetCopySourceBandName2Register;
    private Band targetMaxCorr;
    static String targetMaxCorrBandName;
    private Band targetMaxCorrDir;
    static String targetMaxCorrDirBandName;
    private Band targetFlagBandReference;
    static String targetFlagBandNameReference;
    private Band targetFlagBand2Register;
    static String targetFlagBandName2Register;



    private static final String ALBEDO_BASED = "ALBEDO_BASED";
    private static final String EDGE_DETECTION = "EDGE_DETECTION";

    static int corrKernelRadius = 1;
    static int maxKernelRadius = 10;
    static  int gaussFilterKernelRadius = 2;
    static final int frontValue = 1;
    static final int windowOverlap = 50;
    static final int standardHistogramBins = 32;
    static final double thresholdSegmentationGoodness = 0.7;

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
        //copyGeocodingForSmallerTarget(maxKernelRadius);
        targetCopySourceBandNameReference = sourceBandReferenceName + "_Reference";
        targetCopySourceBandName2Register = sourceBand2RegisterName + "_2Register";;
        targetMaxCorrBandName = "maximum_of _correlation_coefficient";
        targetMaxCorrDirBandName = "maximum_of _correlation_coefficient_drift";
        targetFlagBandNameReference = "Flag_Reference";
        targetFlagBandName2Register = "Flag_2Register";;


        sourceBandReference = sourceProductReference.getBand(sourceBandReferenceName);
        sourceBand2Register = sourceProduct2Register.getBand(sourceBand2RegisterName);

        landWaterBandReference = sourceProductReference.getBand(landWaterBandReferenceName);
        landWaterBand2Register = sourceProduct2Register.getBand(landWaterBand2RegisterName);

        panoramaEffectBandReference = sourceProductReference.getBand(panoramaEffectBandReferenceName);
        panoramaEffectBand2Register = sourceProduct2Register.getBand(panoramaEffectBand2RegisterName);

        cloudAlbedo1BandReference = sourceProductReference.getBand(cloudAlbedo1BandReferenceName);
        cloudAlbedo1Band2Register = sourceProduct2Register.getBand(cloudAlbedo1Band2RegisterName);

        cloudAlbedo2BandReference = sourceProductReference.getBand(cloudAlbedo2BandReferenceName);
        cloudAlbedo2Band2Register = sourceProduct2Register.getBand(cloudAlbedo2Band2RegisterName);

        cloudBT4BandReference = sourceProductReference.getBand(cloudBT4BandReferenceName);
        cloudBT4Band2Register = sourceProduct2Register.getBand(cloudBT4Band2RegisterName);



        targetCopySourceBandReference = targetProduct.addBand(targetCopySourceBandNameReference, ProductData.TYPE_FLOAT64);
        targetCopySourceBandReference.setUnit(sourceBandReference.getUnit());
        targetCopySourceBand2Register = targetProduct.addBand(targetCopySourceBandName2Register, ProductData.TYPE_FLOAT64);
        targetCopySourceBand2Register.setUnit(sourceBand2Register.getUnit());
        targetMaxCorr = targetProduct.addBand(targetMaxCorrBandName, ProductData.TYPE_FLOAT64);
        targetMaxCorrDir = targetProduct.addBand(targetMaxCorrDirBandName, ProductData.TYPE_FLOAT64);
        targetFlagBandReference = targetProduct.addBand(targetFlagBandNameReference, ProductData.TYPE_FLOAT64);
        targetFlagBand2Register = targetProduct.addBand(targetFlagBandName2Register, ProductData.TYPE_FLOAT64);

        targetProduct = new Product("beam_geotool_avhrr", "org.esa.beam", sourceProductReference.getSceneRasterWidth(),
                sourceProductReference.getSceneRasterHeight());

        targetProduct.setPreferredTileSize
                (new Dimension(sourceBandReference.getSceneRasterWidth(), sourceBandReference.getSceneRasterHeight()));
    }

    private Product copyGeocodingForSmallerTarget(int maxKernelRadius) {
        targetProduct = new Product("beam_geotool_BC",
                                    "org.esa.beam",
                sourceProductReference.getSceneRasterWidth() - 2 * maxKernelRadius,
                sourceProductReference.getSceneRasterHeight() - 2 * maxKernelRadius);
        ProductSubsetDef def = new ProductSubsetDef();
        Product sourceSubsetProduct = null;
        def.setRegion(new Rectangle(maxKernelRadius,
                                    maxKernelRadius,
                sourceProductReference.getSceneRasterWidth() - maxKernelRadius,
                sourceProductReference.getSceneRasterHeight() - maxKernelRadius));
        try {
            sourceSubsetProduct = sourceProductReference.createSubset(def, "SourceSubsetProduct", "desc");
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        // Some target products may require more aid from ProductUtils methods...
        ProductUtils.copyGeoCoding(sourceSubsetProduct, targetProduct);


        return targetProduct;
    }


    @Override
    public void computeTileStack(Map<Band, Tile> targetTiles, Rectangle targetRectangle, ProgressMonitor pm) throws OperatorException {

        Rectangle sourceRectangleRef = new Rectangle(targetRectangle);
        Rectangle sourceRectangle2Reg = new Rectangle(targetRectangle);
        sourceRectangle2Reg.grow(maxKernelRadius, maxKernelRadius);
        sourceRectangle2Reg.translate(maxKernelRadius, maxKernelRadius);

        Tile sourceTileReference = getSourceTile(sourceBandReference, sourceRectangleRef, new BorderExtenderConstant(new double[]{Double.NaN}));
        Tile sourceTile2Register = getSourceTile(sourceBand2Register, sourceRectangle2Reg, new BorderExtenderConstant(new double[]{Double.NaN}));

        Tile landWaterTileReference = getSourceTile(landWaterBandReference, sourceRectangleRef, new BorderExtenderConstant(new double[]{Double.NaN}));
        Tile landWaterTile2Register = getSourceTile(landWaterBand2Register, sourceRectangle2Reg, new BorderExtenderConstant(new double[]{Double.NaN}));

        Tile panoramaEffectTileReference = getSourceTile(panoramaEffectBandReference, sourceRectangleRef, new BorderExtenderConstant(new double[]{Double.NaN}));
        Tile panoramaEffectTile2Register = getSourceTile(panoramaEffectBand2Register, sourceRectangle2Reg, new BorderExtenderConstant(new double[]{Double.NaN}));

        Tile cloudAlbedo1TileReference = getSourceTile(cloudAlbedo1BandReference, sourceRectangleRef, new BorderExtenderConstant(new double[]{Double.NaN}));
        Tile cloudAlbedo1Tile2Register = getSourceTile(cloudAlbedo1Band2Register, sourceRectangle2Reg, new BorderExtenderConstant(new double[]{Double.NaN}));

        Tile cloudAlbedo2TileReference = getSourceTile(cloudAlbedo2BandReference, sourceRectangleRef, new BorderExtenderConstant(new double[]{Double.NaN}));
        Tile cloudAlbedo2Tile2Register = getSourceTile(cloudAlbedo2Band2Register, sourceRectangle2Reg, new BorderExtenderConstant(new double[]{Double.NaN}));

        Tile cloudBT4TileReference = getSourceTile(cloudBT4BandReference, sourceRectangleRef, new BorderExtenderConstant(new double[]{Double.NaN}));
        Tile cloudBT4Tile2Register = getSourceTile(cloudBT4Band2Register, sourceRectangle2Reg, new BorderExtenderConstant(new double[]{Double.NaN}));


        Tile targetTileCopySourceBandReference = targetTiles.get(targetCopySourceBandReference);
        Tile targetTileCopySourceBand2Register = targetTiles.get(targetCopySourceBand2Register);
        Tile targetTileMaxCorr = targetTiles.get(targetMaxCorr);
        Tile targetTileMaxCorrDir = targetTiles.get(targetMaxCorrDir);

        Tile targetTileFlagBandReference = targetTiles.get(targetFlagBandReference);
        Tile targetTileFlagBand2Register = targetTiles.get(targetFlagBand2Register);


        final double[] sourceDataReference = sourceTileReference.getSamplesDouble();
        final double[] sourceData2Register = sourceTile2Register.getSamplesDouble();

        final double[] landWaterDataReference = landWaterTileReference.getSamplesDouble();
        final double[] landWaterData2Register = landWaterTile2Register.getSamplesDouble();

        final double[] panoramaEffectDataReference = panoramaEffectTileReference.getSamplesDouble();
        final double[] panoramaEffectData2Register = panoramaEffectTile2Register.getSamplesDouble();

        final double[] cloudAlbedo1DataReference = cloudAlbedo1TileReference.getSamplesDouble();
        final double[] cloudAlbedo1Data2Register = cloudAlbedo1Tile2Register.getSamplesDouble();

        final double[] cloudAlbedo2DataReference = cloudAlbedo2TileReference.getSamplesDouble();
        final double[] cloudAlbedo2Data2Register = cloudAlbedo2Tile2Register.getSamplesDouble();

        final double[] cloudBT4DataReference = cloudBT4TileReference.getSamplesDouble();
        final double[] cloudBT4Data2Register = cloudBT4Tile2Register.getSamplesDouble();


        int sourceDataRefLength = sourceDataReference.length;
        int sourceData2RegLength = sourceData2Register.length;

        final int[] flagDataReference = new int[sourceDataRefLength];
        final int[] flagData2Register = new int[sourceData2RegLength];

        int sourceDataRefWidth = sourceRectangleRef.width;
        int sourceDataRefHeight = sourceRectangleRef.height;

        int sourceData2RegWidth = sourceRectangle2Reg.width;
        int sourceData2RegHeight = sourceRectangle2Reg.height;


        PreparingOfSourceBandAVHRR preparedSourceBand2Reg = new PreparingOfSourceBandAVHRR();
        preparedSourceBand2Reg.cloudDetectionOfSourceBand(
                landWaterData2Register,
                cloudAlbedo1Data2Register,
                cloudAlbedo2Data2Register,
                cloudBT4Data2Register,
                flagData2Register);

        // copy source data for histogram method
        double[] histogramSourceData2Register = new double[sourceData2RegLength];
        System.arraycopy(sourceData2Register, 0, histogramSourceData2Register, 0, sourceData2RegLength);


        EdgeDetection detectionEdges2Register = new EdgeDetection();
        double[] edgesArray2Register = detectionEdges2Register.computeEdges(
                histogramSourceData2Register,
                flagData2Register,
                sourceData2RegWidth,
                sourceData2RegHeight);


        if (ALBEDO_BASED.equals(operator)) {
            PreparingOfSourceBandAVHRR preparedSourceBandRef = new PreparingOfSourceBandAVHRR();
            preparedSourceBandRef.cloudDetectionOfSourceBand(
                    landWaterDataReference,
                    cloudAlbedo1DataReference,
                    cloudAlbedo2DataReference,
                    cloudBT4DataReference,
                    flagDataReference);

            // copy source data for histogram method
            double[] histogramSourceDataReference = new double[sourceDataRefLength];
            System.arraycopy(sourceDataReference, 0, histogramSourceDataReference, 0, sourceDataRefLength);


            EdgeDetection detectionEdgesReference = new EdgeDetection();
            double[] edgesArrayReference = detectionEdgesReference.computeEdges(
                    histogramSourceData2Register,
                    flagData2Register,
                    sourceDataRefWidth,
                    sourceDataRefHeight);


            Image2ImageRegistration image2image = new Image2ImageRegistration();
            image2image.findingBestMatch(
                    sourceDataReference,
                    sourceData2Register,
                    flagDataReference,
                    flagData2Register,
                    sourceDataRefWidth,
                    sourceDataRefHeight,
                    sourceData2RegWidth,
                    sourceData2RegHeight);

        } else {

            Image2ImageRegistration image2image = new Image2ImageRegistration();
            image2image.findingBestMatch(
                    sourceDataReference,
                    edgesArray2Register,
                    flagDataReference,
                    flagData2Register,
                    sourceDataRefWidth,
                    sourceDataRefHeight,
                    sourceData2RegWidth,
                    sourceData2RegHeight);

        }


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
            super(AvhrrGeoToolOperator.class);
        }
    }
}
