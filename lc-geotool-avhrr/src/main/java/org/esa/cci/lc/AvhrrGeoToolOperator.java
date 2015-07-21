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
import java.awt.*;
import java.io.IOException;
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

    static final int frontValue = 1;
    static final int windowOverlap = 50;
    static final int standardHistogramBins = 32;
    static final double thresholdSegmentationGoodness = 0.7;


    static final int gaussFilterKernelRadius = 2;

    private static final String ALBEDO_BASED = "ALBEDO_BASED";
    private static final String EDGE_DETECTION = "EDGE_DETECTION";

    private static final String AVHRR = "AVHRR";
    private static final String MERIS = "MERIS";
    private static final String COAST = "COAST";

    static String targetCopySourceBandNameReference;
    static String targetCopySourceBandNameRegistered;
    static String targetEdgeSourceBandNameReference;
    static String targetEdgSourceBandNameRegistered;

    private String sourceBandReferenceName;
    private String landWaterBandReferenceName;

    private String cloudAlbedo1BandReferenceName;
    private String cloudAlbedo2BandReferenceName;
    private String cloudBT4BandReferenceName;
    private String panoramaEffectBandReferenceName;

    private String sourceBand2RegisterName;
    private String landWaterBand2RegisterName;
    private String cloudAlbedo1Band2RegisterName;
    private String cloudAlbedo2Band2RegisterName;
    private String cloudBT4Band2RegisterName;
    private String panoramaEffectBand2RegisterName;

    private String productName;


    @Parameter(valueSet = {ALBEDO_BASED, EDGE_DETECTION},
            defaultValue = EDGE_DETECTION, description = "chip matching algorithm")
    private String operator;

    @Parameter(valueSet = {MERIS, AVHRR, COAST},
            defaultValue = AVHRR, description = "sensor type")
    private String sensor;

    @SourceProduct
    private Product sourceProductReference;

    @SourceProduct
    private Product sourceProduct2Register;

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
    private Band targetCopySourceBandRegistered;

    private Band targetEdgeSourceBandReference;
    private Band targetEdgeSourceBandRegistered;


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

        productName = sourceProductReference.getName() + "_img2Img";

        targetProduct = new Product(productName, "beam_geotool_BC",
                sourceProductReference.getSceneRasterWidth(), sourceProductReference.getSceneRasterHeight());

        ProductUtils.copyGeoCoding(sourceProductReference, targetProduct);

        sourceBand2RegisterName = "albedo_2";
        landWaterBand2RegisterName = "land_water_fraction";
        cloudAlbedo1Band2RegisterName = "albedo_1";
        cloudAlbedo2Band2RegisterName = "albedo_2";
        cloudBT4Band2RegisterName = "radiance_4";
        panoramaEffectBand2RegisterName = "X_Band";

        landWaterBand2Register = sourceProduct2Register.getBand(landWaterBand2RegisterName);
        sourceBand2Register = sourceProduct2Register.getBand(sourceBand2RegisterName);
        panoramaEffectBand2Register = sourceProduct2Register.getBand(panoramaEffectBand2RegisterName);
        cloudAlbedo1Band2Register = sourceProduct2Register.getBand(cloudAlbedo1Band2RegisterName);
        cloudAlbedo2Band2Register = sourceProduct2Register.getBand(cloudAlbedo2Band2RegisterName);
        cloudBT4Band2Register = sourceProduct2Register.getBand(cloudBT4Band2RegisterName);

        if (AVHRR.equals(sensor)) {
            sourceBandReferenceName = "albedo_2";
            landWaterBandReferenceName = "land_water_fraction";
            cloudAlbedo1BandReferenceName = "albedo_1";
            cloudAlbedo2BandReferenceName = "albedo_2";
            cloudBT4BandReferenceName = "radiance_4";
            panoramaEffectBandReferenceName = "X_Band";

            sourceBandReference = sourceProductReference.getBand(sourceBandReferenceName);
            landWaterBandReference = sourceProductReference.getBand(landWaterBandReferenceName);
            panoramaEffectBandReference = sourceProductReference.getBand(panoramaEffectBandReferenceName);
            cloudAlbedo1BandReference = sourceProductReference.getBand(cloudAlbedo1BandReferenceName);
            cloudAlbedo2BandReference = sourceProductReference.getBand(cloudAlbedo2BandReferenceName);
            cloudBT4BandReference = sourceProductReference.getBand(cloudBT4BandReferenceName);
        }
        if (MERIS.equals(sensor)) {
            sourceBandReferenceName = "sr_10_mean";
            sourceBandReference = sourceProductReference.getBand(sourceBandReferenceName);
        }
        if (COAST.equals(sensor)) {
            sourceBandReferenceName = "band_1_new";
            sourceBandReference = sourceProductReference.getBand(sourceBandReferenceName);
        }

        targetCopySourceBandNameReference = sourceBandReferenceName + "_Reference";
        targetCopySourceBandNameRegistered = sourceBand2RegisterName + "_Registered";


        targetCopySourceBandReference = targetProduct.addBand(targetCopySourceBandNameReference, ProductData.TYPE_FLOAT64);
        if (AVHRR.equals(sensor)) {
            targetCopySourceBandReference.setUnit(sourceBandReference.getUnit());
        }

        targetCopySourceBandRegistered = targetProduct.addBand(targetCopySourceBandNameRegistered, ProductData.TYPE_FLOAT64);
        targetCopySourceBandRegistered.setUnit(sourceBand2Register.getUnit());

        if (EDGE_DETECTION.equals(operator)) {
            if (AVHRR.equals(sensor) || MERIS.equals(sensor)) {

                targetEdgeSourceBandNameReference = sourceBandReferenceName + "_Edge_Reference";
                targetEdgSourceBandNameRegistered = sourceBand2RegisterName + "_Edge_Registered";

                targetEdgeSourceBandReference = targetProduct.addBand(targetEdgeSourceBandNameReference, ProductData.TYPE_FLOAT64);
                targetEdgeSourceBandRegistered = targetProduct.addBand(targetEdgSourceBandNameRegistered, ProductData.TYPE_FLOAT64);
            }
            if (COAST.equals(sensor)) {
                targetEdgSourceBandNameRegistered = sourceBand2RegisterName + "Edge_Registered";
                targetEdgeSourceBandRegistered = targetProduct.addBand(targetEdgSourceBandNameRegistered, ProductData.TYPE_FLOAT64);
            }
        }

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
        Rectangle sourceRectangle2Reg = new Rectangle(0, 0, sourceBand2Register.getSceneRasterWidth(), sourceBand2Register.getSceneRasterHeight());

        System.out.printf("source rectangle reference width height:  %d  %d   \n", sourceRectangleRef.width, sourceRectangleRef.height);
        System.out.printf("source rectangle 2register width height:  %d  %d   \n", sourceRectangle2Reg.width, sourceRectangle2Reg.height);

        Tile sourceTileReference = null;
        Tile landWaterTileReference = null;
        Tile panoramaEffectTileReference = null;
        Tile cloudAlbedo1TileReference = null;
        Tile cloudAlbedo2TileReference = null;
        Tile cloudBT4TileReference = null;
        Tile targetTileEdgeSourceBandReference = null;
        Tile targetTileEdgeSourceBandRegistered = null;

        double[] sourceDataReference = new double[0];
        double[] landWaterDataReference = new double[0];
        double[] panoramaEffectDataReference = new double[0];
        double[] cloudAlbedo1DataReference = new double[0];
        double[] cloudAlbedo2DataReference = new double[0];
        double[] cloudBT4DataReference = new double[0];


        Tile sourceTile2Register = getSourceTile(sourceBand2Register, sourceRectangle2Reg,
                new BorderExtenderConstant(new double[]{Double.NaN}));
        Tile landWaterTile2Register = getSourceTile(landWaterBand2Register, sourceRectangle2Reg,
                new BorderExtenderConstant(new double[]{Double.NaN}));
        Tile panoramaEffectTile2Register = getSourceTile(panoramaEffectBand2Register, sourceRectangle2Reg,
                new BorderExtenderConstant(new double[]{Double.NaN}));
        Tile cloudAlbedo1Tile2Register = getSourceTile(cloudAlbedo1Band2Register, sourceRectangle2Reg,
                new BorderExtenderConstant(new double[]{Double.NaN}));
        Tile cloudAlbedo2Tile2Register = getSourceTile(cloudAlbedo2Band2Register, sourceRectangle2Reg,
                new BorderExtenderConstant(new double[]{Double.NaN}));
        Tile cloudBT4Tile2Register = getSourceTile(cloudBT4Band2Register, sourceRectangle2Reg,
                new BorderExtenderConstant(new double[]{Double.NaN}));


        double[] sourceData2Register = sourceTile2Register.getSamplesDouble();
        double[] landWaterData2Register = landWaterTile2Register.getSamplesDouble();
        double[] panoramaEffectData2Register = panoramaEffectTile2Register.getSamplesDouble();
        double[] cloudAlbedo1Data2Register = cloudAlbedo1Tile2Register.getSamplesDouble();
        double[] cloudAlbedo2Data2Register = cloudAlbedo2Tile2Register.getSamplesDouble();
        double[] cloudBT4Data2Register = cloudBT4Tile2Register.getSamplesDouble();

        if (MERIS.equals(sensor) || COAST.equals(sensor)) {
            sourceTileReference = getSourceTile(sourceBandReference, sourceRectangleRef,
                    new BorderExtenderConstant(new double[]{Double.NaN}));
            sourceDataReference = sourceTileReference.getSamplesDouble();

        } else {
            if (AVHRR.equals(sensor)) {
                sourceTileReference = getSourceTile(sourceBandReference, sourceRectangleRef,
                        new BorderExtenderConstant(new double[]{Double.NaN}));
                sourceDataReference = sourceTileReference.getSamplesDouble();

                landWaterTileReference = getSourceTile(landWaterBandReference, sourceRectangleRef,
                        new BorderExtenderConstant(new double[]{Double.NaN}));
                landWaterDataReference = landWaterTileReference.getSamplesDouble();

                panoramaEffectTileReference = getSourceTile(panoramaEffectBandReference, sourceRectangleRef,
                        new BorderExtenderConstant(new double[]{Double.NaN}));
                panoramaEffectDataReference = panoramaEffectTileReference.getSamplesDouble();

                cloudAlbedo1TileReference = getSourceTile(cloudAlbedo1BandReference, sourceRectangleRef,
                        new BorderExtenderConstant(new double[]{Double.NaN}));
                cloudAlbedo1DataReference = cloudAlbedo1TileReference.getSamplesDouble();

                cloudAlbedo2TileReference = getSourceTile(cloudAlbedo2BandReference, sourceRectangleRef,
                        new BorderExtenderConstant(new double[]{Double.NaN}));
                cloudAlbedo2DataReference = cloudAlbedo2TileReference.getSamplesDouble();

                cloudBT4TileReference = getSourceTile(cloudBT4BandReference, sourceRectangleRef,
                        new BorderExtenderConstant(new double[]{Double.NaN}));
                cloudBT4DataReference = cloudBT4TileReference.getSamplesDouble();
            }
        }

        Tile targetTileCopySourceBandReference = targetTiles.get(targetCopySourceBandReference);
        Tile targetTileCopySourceBandRegistered = targetTiles.get(targetCopySourceBandRegistered);

        if (EDGE_DETECTION.equals(operator)) {
            targetTileEdgeSourceBandReference = targetTiles.get(targetEdgeSourceBandReference);
            targetTileEdgeSourceBandRegistered = targetTiles.get(targetEdgeSourceBandRegistered);
        }
        if (COAST.equals(sensor)) {
            targetTileEdgeSourceBandRegistered = targetTiles.get(targetEdgeSourceBandRegistered);
        }


        int sourceDataRefLength = sourceDataReference.length;
        int sourceData2RegLength = sourceData2Register.length;

        final int[] flagDataReference = new int[sourceDataRefLength];
        final int[] flagData2Register = new int[sourceData2RegLength];


        int sourceDataRefWidth = sourceRectangleRef.width;
        int sourceDataRefHeight = sourceRectangleRef.height;

        int sourceData2RegWidth = sourceRectangle2Reg.width;
        int sourceData2RegHeight = sourceRectangle2Reg.height;


        PreparationOfSourceBands preparedSourceBand2Reg = new PreparationOfSourceBands();
        preparedSourceBand2Reg.cloudDetectionOfAvhrrSourceBand(landWaterData2Register,
                cloudAlbedo1Data2Register,
                cloudAlbedo2Data2Register,
                cloudBT4Data2Register,
                flagData2Register);

        preparedSourceBand2Reg.preparationOfAvhrrSourceBand(sourceData2Register,
                flagData2Register);

        // copy source data for histogram method
        double[] histogramSourceData2Register = new double[sourceData2RegLength];
        System.arraycopy(sourceData2Register, 0, histogramSourceData2Register, 0, sourceData2RegLength);


        EdgeDetection detectionEdges2Register = new EdgeDetection();
        double[] edgesArray2Register = detectionEdges2Register.computeEdges(histogramSourceData2Register,
                flagData2Register,
                sourceData2RegWidth,
                sourceData2RegWidth);


        Image2ImageRegistration image2image = new Image2ImageRegistration();

        if (ALBEDO_BASED.equals(operator)) {
            if (AVHRR.equals(sensor)) {

                PreparationOfSourceBands preparedAvhrrSourceBandRef = new PreparationOfSourceBands();
                preparedAvhrrSourceBandRef.cloudDetectionOfAvhrrSourceBand(landWaterDataReference,
                        cloudAlbedo1DataReference,
                        cloudAlbedo2DataReference,
                        cloudBT4DataReference,
                        flagDataReference);

                preparedAvhrrSourceBandRef.preparationOfAvhrrSourceBand(sourceDataReference,
                        flagDataReference);

            } else {
                if (MERIS.equals(sensor)) {
                    PreparationOfSourceBands preparedMerisSourceBandRef = new PreparationOfSourceBands();
                    preparedMerisSourceBandRef.preparationOfMerisSourceBand(sourceDataReference,
                            flagDataReference);
                }
            }

            image2image.findingBestMatch(sourceDataReference,
                    sourceData2Register,
                    flagDataReference,
                    flagData2Register,
                    sourceDataRefWidth,
                    sourceDataRefHeight,
                    sourceData2RegWidth,
                    sourceData2RegHeight,
                    targetTileCopySourceBandReference,
                    targetTileCopySourceBandRegistered);
        } else {
            if (MERIS.equals(sensor) || AVHRR.equals(sensor)) {
                targetTileEdgeSourceBandRegistered = targetTiles.get(targetEdgeSourceBandRegistered);


                // copy source data for histogram method
                double[] histogramSourceDataReference = new double[sourceDataRefLength];
                System.arraycopy(sourceDataReference, 0, histogramSourceDataReference, 0, sourceDataRefLength);


                EdgeDetection detectionEdgesReference = new EdgeDetection();
                double[] edgesArrayReference = detectionEdgesReference.computeEdges(histogramSourceDataReference,
                        flagDataReference,
                        sourceDataRefWidth,
                        sourceDataRefHeight);

                image2image.findingBestMatchEdges(edgesArrayReference,
                        edgesArray2Register,
                        sourceDataReference,
                        sourceData2Register,
                        flagDataReference,
                        flagData2Register,
                        sourceDataRefWidth,
                        sourceDataRefHeight,
                        sourceData2RegWidth,
                        sourceData2RegHeight,
                        targetTileCopySourceBandReference,
                        targetTileCopySourceBandRegistered,
                        targetTileEdgeSourceBandReference,
                        targetTileEdgeSourceBandRegistered);


            } else {
                if (COAST.equals(sensor)) {

                    image2image.findingBestMatchCoast(sourceDataReference,
                            edgesArray2Register,
                            sourceData2Register,
                            flagDataReference,
                            flagData2Register,
                            sourceDataRefWidth,
                            sourceDataRefHeight,
                            sourceData2RegWidth,
                            sourceData2RegHeight,
                            targetTileCopySourceBandReference,
                            targetTileCopySourceBandRegistered,
                            targetTileEdgeSourceBandRegistered);
                }
            }
        }
        //System.out.printf("source rectangle width height:  %d  %d   \n",sourceRectangle.width, sourceRectangle.height);
    }


    static void makeFilledBand
            (
                    double[][] inputData,
                    int inputDataWidth,
                    int inputDataHeight,
                    Tile targetTileOutputBand,
                    int mkr) {

        for (int y = mkr; y < inputDataHeight - mkr; y++) {
            for (int x = mkr; x < inputDataWidth - mkr; x++) {
                targetTileOutputBand.setSample(x - mkr, y - mkr, inputData[x][y]);

            }
        }
    }

    static void makeFilledBand
            (
                    int[][] inputData,
                    int inputDataWidth,
                    int inputDataHeight,
                    Tile targetTileOutputBand,
                    int mkr) {

        for (int y = mkr; y < inputDataHeight - mkr; y++) {
            for (int x = mkr; x < inputDataWidth - mkr; x++) {
                targetTileOutputBand.setSample(x - mkr, y - mkr, inputData[x][y]);

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
