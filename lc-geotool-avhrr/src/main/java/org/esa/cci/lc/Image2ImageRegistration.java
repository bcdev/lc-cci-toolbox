package org.esa.cci.lc;

import java.util.Arrays;

public class Image2ImageRegistration {


    public static void findingBestMatch(double[] sourceDataReference,
                                            double[] sourceData2Register,
                                            int[] flagDataReference,
                                            int[] flagData2Register,
                                            int sourceDataRefWidth,
                                            int sourceDataRefHeight,
                                            int sourceData2RegWidth,
                                            int sourceData2RegHeight) {

        /**************************************************************************/
        /************************** Histogram Method  *****************************/
        /**************************************************************************/


        Filter filter = new GaussFilter();

    /*
        final double[][] sourceDataMoveReference = new double[sourceRefWidth][sourceRefHeight];
        final double[][] sourceDataMove2Register = new double[sourceRefWidth][sourceRefHeight];
        final double[][] correlationARRAY = new double[sourceRefWidth][sourceRefHeight];
        final double[] correlationMaxARRAY = new double[sourceRefWidth * sourceRefHeight];
        final double[] correlationDirARRAY = new double[sourceRefWidth * sourceRefHeight];


        //Image2ImageRegistration(flagArray, sourceRefWidth, sourceRefHeight, flagTile, FrontsOperator.maxKernelRadius);
        //Image2ImageRegistration(sourceData, sourceRefWidth, sourceRefHeight, targetTileCopySourceBand, maxKernelRadius);


        // Number of e.g. SST data
        totalParameterNumber = 0;
        totalParameterNumber = Image2ImageRegistration(sourceRefWidth, sourceRefHeight, sourceData);

        // copy source data for histogram method
        double[] histogramSourceData = new double[sourceData.length];
        System.arraycopy(sourceData, 0, histogramSourceData, 0, sourceData.length);


        Arrays.fill(correlationMaxARRAY, Double.MIN_VALUE);
        Arrays.fill(correlationDirARRAY, Double.MIN_VALUE);

        double[] kernelSizeArrayReference = new double[(2 * corrKernelRadius + 1) * (2 * corrKernelRadius + 1)];
        double[] kernelSizeArray2Register = new double[(2 * corrKernelRadius + 1) * (2 * corrKernelRadius + 1)];

        double direction = 0.0;

        Image2ImageRegistration(sourceDataReference, sourceRefWidth, sourceRefHeight, targetTileCopySourceBandReference, AvhrrGeoToolOperator.maxKernelRadius);
        Image2ImageRegistration(sourceData2Register, sourceRefWidth, sourceRefHeight, targetTileCopySourceBand2Register, AvhrrGeoToolOperator.maxKernelRadius);

        for (int j = 0; j < sourceRefHeight; j++) {
            for (int i = 0; i < sourceRefWidth; i++) {
                sourceDataMoveReference[i][j] = sourceDataReference[j * (sourceRefWidth) + i];
            }
        }


        for (int k = -1; k < 2; k++) {
            for (int l = -1; l < 2; l++) {

                direction += 1;

                for (int j = 0; j < sourceRefHeight; j++) {
                    for (int i = 0; i < sourceRefWidth; i++) {
                        sourceDataMove2Register[i][j] = Double.NaN;
                        correlationARRAY[i][j] = Double.NaN;
                    }
                }

                for (int j = 1; j < sourceRefHeight - 1; j++) {
                    for (int i = 1; i < sourceRefWidth - 1; i++) {
                        sourceDataMove2Register[i][j] = sourceData2Register[(j - l) * (sourceRefWidth) + (i - k)];
                    }
                }

                for (int j = 1; j < sourceRefHeight - 1; j++) {
                    for (int i = 1; i < sourceRefWidth - 1; i++) {

                        for (int jj = -corrKernelRadius; jj < corrKernelRadius + 1; jj++) {
                            for (int ii = -corrKernelRadius; ii < corrKernelRadius + 1; ii++) {
                                // System.out.printf("1. width height 3x3matrix width height:  %d  %d  %d   \n", i + ii, j + jj, (jj + corrKernelRadius) * (2 * corrKernelRadius + 1) + (ii + corrKernelRadius));

                                kernelSizeArrayReference[(jj + corrKernelRadius) * (2 * corrKernelRadius + 1) + (ii + corrKernelRadius)] =
                                        sourceDataMoveReference[i + ii][j + jj];
                                kernelSizeArray2Register[(jj + corrKernelRadius) * (2 * corrKernelRadius + 1) + (ii + corrKernelRadius)] =
                                        sourceDataMove2Register[i + ii][j + jj];
                                //System.out.printf("2. width height 3x3matrix width height:  %d  %d  %d   \n", i + ii, j + jj, (jj + corrKernelRadius) * (2 * corrKernelRadius + 1) + (ii + corrKernelRadius));
                            }
                        }
                        correlationARRAY[i][j] = AvhrrGeoToolCorrelation.getPearsonCorrelation1(kernelSizeArrayReference, kernelSizeArray2Register);
                    }
                }

                for (int j = 0; j < sourceRefHeight; j++) {
                    for (int i = 0; i < sourceRefWidth; i++) {

                        if (correlationARRAY[i][j] >= correlationMaxARRAY[j * sourceRefWidth + i]) {

                            correlationMaxARRAY[j * sourceRefWidth + i] = correlationARRAY[i][j];
                            correlationDirARRAY[j * sourceRefWidth + i] = direction;

                            // sourceDataMoveSPOT[i][j] = sourceDataSPOT[(j - l) * (sourceRefWidth) + (i - k)];
                        }
                    }
                }
            }
        }

        Image2ImageRegistration(correlationMaxARRAY, sourceRefWidth, sourceRefHeight, targetTileMaxCorr, AvhrrGeoToolOperator.maxKernelRadius);
        Image2ImageRegistration(correlationDirARRAY, sourceRefWidth, sourceRefHeight, targetTileMaxCorrDir, AvhrrGeoToolOperator.maxKernelRadius);*/

    }
}