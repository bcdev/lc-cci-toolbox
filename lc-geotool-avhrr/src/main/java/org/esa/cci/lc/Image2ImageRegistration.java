package org.esa.cci.lc;

import java.util.Arrays;

import org.esa.beam.framework.gpf.Tile;

public class Image2ImageRegistration {


    public static void findingBestMatch(double[] sourceDataReference,
                                        double[] sourceData2Register,
                                        int[] flagDataReference,
                                        int[] flagData2Registered,
                                        int sourceDataRefWidth,
                                        int sourceDataRefHeight,
                                        int sourceData2RegWidth,
                                        int sourceData2RegHeight,
                                        Tile targetTileCopySourceBandReference,
                                        Tile targetTileCopySourceBandRegistered) {


        System.out.printf("source data reference length:  %d    \n", sourceDataReference.length);
        System.out.printf("source data 2register length:  %d    \n", sourceData2Register.length);


        final double[][] sourceDataMoveReference = new double[sourceDataRefWidth][sourceDataRefHeight];
        final double[][] sourceDataMove2Register = new double[sourceDataRefWidth][sourceDataRefHeight];


        double correlationValue = Double.MIN_VALUE;
        double correlationMaxValue = Double.MIN_VALUE;


        double[] ArrayReference = new double[sourceDataRefWidth * sourceDataRefHeight];
        double[] Array2Register = new double[sourceDataRefWidth * sourceDataRefHeight];

        int directionX = -1;
        int directionY = -1;
        int shiftX = 0;
        int shiftY = 0;

        for (int j = 0; j < sourceDataRefHeight; j++) {
            for (int i = 0; i < sourceDataRefWidth; i++) {
                sourceDataMoveReference[i][j] = sourceDataReference[j * (sourceDataRefWidth) + i];
            }
        }


        for (int k = 0; k < sourceData2RegWidth - sourceDataRefWidth + 1; k++) {
            directionX += 1;
            directionY = -1;
            for (int m = 0; m < sourceData2RegHeight - sourceDataRefHeight + 1; m++) {
                directionY += 1;

                //System.out.printf("shiftX:  %d    \n",directionX);
                //System.out.printf("shiftY:  %d    \n",directionY);

                for (int j = 0; j < sourceDataRefHeight; j++) {
                    for (int i = 0; i < sourceDataRefWidth; i++) {
                        sourceDataMove2Register[i][j] = Double.NaN;
                        correlationValue = Double.NaN;
                    }
                }

                for (int j = 0; j < sourceDataRefHeight; j++) {
                    for (int i = 0; i < sourceDataRefWidth; i++) {
                        sourceDataMove2Register[i][j] = sourceData2Register[(j + directionY) * (sourceData2RegWidth) + (i + directionX)];
                    }
                }


                for (int j = 0; j < sourceDataRefHeight; j++) {
                    for (int i = 0; i < sourceDataRefWidth; i++) {
                        ArrayReference[j * (sourceDataRefWidth) + i] = sourceDataMoveReference[i][j];
                        Array2Register[j * (sourceDataRefWidth) + i] = sourceDataMove2Register[i][j];
                    }
                }

                correlationValue = AvhrrGeoToolCorrelation.getPearsonCorrelation1(
                        ArrayReference,
                        Array2Register);

                System.out.printf("directionX directionY:  %d %d %f  \n", directionX, directionY, correlationValue);


                if (correlationValue >= correlationMaxValue) {
                    correlationMaxValue = correlationValue;
                    shiftX = directionX;
                    shiftY = directionY;
                }
            }
        }

        System.out.printf("shiftX:  %d    \n", shiftX);
        System.out.printf("shiftY:  %d    \n", shiftY);

        for (int j = 0; j < sourceDataRefHeight; j++) {
            for (int i = 0; i < sourceDataRefWidth; i++) {
                sourceDataMove2Register[i][j] = sourceData2Register[(j + shiftY) * (sourceData2RegWidth) + (i + shiftX)];
            }
        }


        AvhrrGeoToolOperator.makeFilledBand(sourceDataMove2Register,
                sourceDataRefWidth,
                sourceDataRefHeight,
                targetTileCopySourceBandRegistered,
                0);
        AvhrrGeoToolOperator.makeFilledBand(sourceDataReference,
                sourceDataRefWidth,
                sourceDataRefHeight,
                targetTileCopySourceBandReference,
                0);

    }

    public static void findingBestMatchEdges(double[] sourceEdgeDataReference,
                                             double[] sourceEdgeData2Register,
                                             double[] sourceDataReference,
                                             double[] sourceData2Register,
                                             int[] flagDataReference,
                                             int[] flagData2Registered,
                                             int sourceDataRefWidth,
                                             int sourceDataRefHeight,
                                             int sourceData2RegWidth,
                                             int sourceData2RegHeight,
                                             Tile targetTileCopySourceBandReference,
                                             Tile targetTileCopySourceBandRegistered,
                                             Tile targetTileEdgeSourceBandReference,
                                             Tile targetTileEdgeSourceBandRegistered) {


        System.out.printf("source data reference length:  %d    \n", sourceEdgeDataReference.length);
        System.out.printf("source data 2register length:  %d    \n", sourceEdgeData2Register.length);


        final double[][] sourceDataEdgeMoveReference = new double[sourceDataRefWidth][sourceDataRefHeight];
        final double[][] sourceDataEdgeMove2Register = new double[sourceDataRefWidth][sourceDataRefHeight];
        final double[][] sourceDataMove2Register = new double[sourceDataRefWidth][sourceDataRefHeight];


        double correlationValue = Double.MIN_VALUE;
        double correlationMaxValue = Double.MIN_VALUE;


        double[] ArrayReference = new double[sourceDataRefWidth * sourceDataRefHeight];
        double[] Array2Register = new double[sourceDataRefWidth * sourceDataRefHeight];

        int directionX = -1;
        int directionY = -1;
        int shiftX = 0;
        int shiftY = 0;

        for (int j = 0; j < sourceDataRefHeight; j++) {
            for (int i = 0; i < sourceDataRefWidth; i++) {
                sourceDataEdgeMoveReference[i][j] = sourceEdgeDataReference[j * (sourceDataRefWidth) + i];
            }
        }


        for (int k = 0; k < sourceData2RegWidth - sourceDataRefWidth + 1; k++) {
            directionX += 1;
            directionY = -1;
            for (int m = 0; m < sourceData2RegHeight - sourceDataRefHeight + 1; m++) {
                directionY += 1;

                //System.out.printf("shiftX:  %d    \n",directionX);
                //System.out.printf("shiftY:  %d    \n",directionY);

                for (int j = 0; j < sourceDataRefHeight; j++) {
                    for (int i = 0; i < sourceDataRefWidth; i++) {
                        sourceDataEdgeMove2Register[i][j] = Double.NaN;
                        correlationValue = Double.NaN;
                    }
                }

                for (int j = 0; j < sourceDataRefHeight; j++) {
                    for (int i = 0; i < sourceDataRefWidth; i++) {
                        sourceDataEdgeMove2Register[i][j] = sourceEdgeData2Register[(j + directionY) * (sourceData2RegWidth) + (i + directionX)];
                    }
                }


                for (int j = 0; j < sourceDataRefHeight; j++) {
                    for (int i = 0; i < sourceDataRefWidth; i++) {
                        ArrayReference[j * (sourceDataRefWidth) + i] = sourceDataEdgeMoveReference[i][j];
                        Array2Register[j * (sourceDataRefWidth) + i] = sourceDataEdgeMove2Register[i][j];
                    }
                }

                correlationValue = AvhrrGeoToolCorrelation.getPearsonCorrelation1(
                        ArrayReference,
                        Array2Register);

                System.out.printf("directionX directionY:  %d %d %f  \n", directionX, directionY, correlationValue);


                if (correlationValue >= correlationMaxValue) {
                    correlationMaxValue = correlationValue;
                    shiftX = directionX;
                    shiftY = directionY;
                }
            }
        }

        System.out.printf("shiftX:  %d    \n", shiftX);
        System.out.printf("shiftY:  %d    \n", shiftY);

        for (int j = 0; j < sourceDataRefHeight; j++) {
            for (int i = 0; i < sourceDataRefWidth; i++) {
                sourceDataEdgeMove2Register[i][j] = sourceEdgeData2Register[(j + shiftY) * (sourceData2RegWidth) + (i + shiftX)];
                sourceDataMove2Register[i][j] = sourceData2Register[(j + shiftY) * (sourceData2RegWidth) + (i + shiftX)];
            }
        }


        AvhrrGeoToolOperator.makeFilledBand(sourceDataEdgeMove2Register,
                sourceDataRefWidth,
                sourceDataRefHeight,
                targetTileEdgeSourceBandRegistered,
                0);
        AvhrrGeoToolOperator.makeFilledBand(sourceEdgeDataReference,
                sourceDataRefWidth,
                sourceDataRefHeight,
                targetTileEdgeSourceBandReference,
                0);
        AvhrrGeoToolOperator.makeFilledBand(sourceDataMove2Register,
                sourceDataRefWidth,
                sourceDataRefHeight,
                targetTileCopySourceBandRegistered,
                0);
        AvhrrGeoToolOperator.makeFilledBand(sourceDataReference,
                sourceDataRefWidth,
                sourceDataRefHeight,
                targetTileCopySourceBandReference,
                0);

    }

    public static void findingBestMatchCoast(double[] sourceDataReference,
                                             double[] sourceEdgeData2Register,
                                             double[] sourceData2Register,
                                             int[] flagDataReference,
                                             int[] flagData2Registered,
                                             int sourceDataRefWidth,
                                             int sourceDataRefHeight,
                                             int sourceData2RegWidth,
                                             int sourceData2RegHeight,
                                             Tile targetTileCopySourceBandReference,
                                             Tile targetTileCopySourceBandRegistered,
                                             Tile targetTileEdgeSourceBandRegistered) {


        System.out.printf("source data reference length:  %d    \n", sourceDataReference.length);
        System.out.printf("source data 2register length:  %d    \n", sourceEdgeData2Register.length);


        final double[][] sourceDataEdgeMoveReference = new double[sourceDataRefWidth][sourceDataRefHeight];
        final double[][] sourceDataEdgeMove2Register = new double[sourceDataRefWidth][sourceDataRefHeight];
        final double[][] sourceDataMove2Register = new double[sourceDataRefWidth][sourceDataRefHeight];


        double correlationValue = Double.MIN_VALUE;
        double correlationMaxValue = Double.MIN_VALUE;


        double[] ArrayReference = new double[sourceDataRefWidth * sourceDataRefHeight];
        double[] Array2Register = new double[sourceDataRefWidth * sourceDataRefHeight];

        int directionX = -1;
        int directionY = -1;
        int shiftX = 0;
        int shiftY = 0;

        for (int j = 0; j < sourceDataRefHeight; j++) {
            for (int i = 0; i < sourceDataRefWidth; i++) {
                sourceDataEdgeMoveReference[i][j] = sourceDataReference[j * (sourceDataRefWidth) + i];
            }
        }


        for (int k = 0; k < sourceData2RegWidth - sourceDataRefWidth + 1; k++) {
            directionX += 1;
            directionY = -1;
            for (int m = 0; m < sourceData2RegHeight - sourceDataRefHeight + 1; m++) {
                directionY += 1;

                //System.out.printf("shiftX:  %d    \n",directionX);
                //System.out.printf("shiftY:  %d    \n",directionY);

                for (int j = 0; j < sourceDataRefHeight; j++) {
                    for (int i = 0; i < sourceDataRefWidth; i++) {
                        sourceDataEdgeMove2Register[i][j] = Double.NaN;
                        correlationValue = Double.NaN;
                    }
                }

                for (int j = 0; j < sourceDataRefHeight; j++) {
                    for (int i = 0; i < sourceDataRefWidth; i++) {
                        sourceDataEdgeMove2Register[i][j] = sourceEdgeData2Register[(j + directionY) * (sourceData2RegWidth) + (i + directionX)];
                    }
                }


                for (int j = 0; j < sourceDataRefHeight; j++) {
                    for (int i = 0; i < sourceDataRefWidth; i++) {
                        ArrayReference[j * (sourceDataRefWidth) + i] = sourceDataEdgeMoveReference[i][j];
                        Array2Register[j * (sourceDataRefWidth) + i] = sourceDataEdgeMove2Register[i][j];
                    }
                }

                correlationValue = AvhrrGeoToolCorrelation.getPearsonCorrelation1(
                        ArrayReference,
                        Array2Register);

                System.out.printf("directionX directionY:  %d %d %f  \n", directionX, directionY, correlationValue);


                if (correlationValue >= correlationMaxValue) {
                    correlationMaxValue = correlationValue;
                    shiftX = directionX;
                    shiftY = directionY;
                }
            }
        }

        System.out.printf("shiftX:  %d    \n", shiftX);
        System.out.printf("shiftY:  %d    \n", shiftY);

        for (int j = 0; j < sourceDataRefHeight; j++) {
            for (int i = 0; i < sourceDataRefWidth; i++) {
                sourceDataEdgeMove2Register[i][j] = sourceEdgeData2Register[(j + shiftY) * (sourceData2RegWidth) + (i + shiftX)];
                sourceDataMove2Register[i][j] = sourceData2Register[(j + shiftY) * (sourceData2RegWidth) + (i + shiftX)];
            }
        }


        AvhrrGeoToolOperator.makeFilledBand(sourceDataEdgeMove2Register,
                sourceDataRefWidth,
                sourceDataRefHeight,
                targetTileEdgeSourceBandRegistered,
                0);
        AvhrrGeoToolOperator.makeFilledBand(sourceDataMove2Register,
                sourceDataRefWidth,
                sourceDataRefHeight,
                targetTileCopySourceBandRegistered,
                0);
        AvhrrGeoToolOperator.makeFilledBand(sourceDataReference,
                sourceDataRefWidth,
                sourceDataRefHeight,
                targetTileCopySourceBandReference,
                0);

    }

}