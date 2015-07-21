package org.esa.cci.lc;

public class Convolution {
    private double[][] kernel;
    private int radius;

    public Convolution(double[][] kernel, int radius) {
        this.kernel = kernel;
        this.radius = radius;
    }

    public double[][] makeConvolution(double[] sourceData,
                                      int sourceWidth,
                                      int sourceHeight,
                                      int[] flagArray) {


        double[][] output = new double[sourceWidth][sourceHeight];
        int grid = 2*radius + 1; //
        int kernelRadiusNaN = radius;
        int kernelSizeNaN = kernelRadiusNaN * 2 + 1;
        double[][] kernelSizeArray = new double[kernelSizeNaN][kernelSizeNaN];
        double[][] kernelSizeFlagArray = new double[kernelSizeNaN][kernelSizeNaN];
        double meanValue;

        for (int y = 0; y < sourceHeight; y++) {
            for (int x = 0; x < sourceWidth; x++) {
                output[x][y] = Double.NaN;
            }
        }
        for (int y = grid; y < sourceHeight - grid; y++) {
            for (int x = grid; x < sourceWidth - grid; x++) {
                int d = flagArray[y * (sourceWidth) + x];
                if (d < PreparationOfSourceBands.LAND_FLAG + PreparationOfSourceBands.OCEAN_FLAG) {
                    //ocean + land clean
                    double sum = 0.0;
                    // It is the Convolution
                    for (int j = -radius; j < radius + 1; j++) {
                        for (int i = -radius; i < radius + 1; i++) {
                            int f = flagArray[(y + j) * (sourceWidth) + (x + i)];
                            if (f > PreparationOfSourceBands.LAND_FLAG + PreparationOfSourceBands.OCEAN_FLAG) {
                                // ocean + land cloudly
                                for (int jj = -kernelRadiusNaN; jj < kernelRadiusNaN + 1; jj++) {
                                    for (int ii = -kernelRadiusNaN; ii < kernelRadiusNaN + 1; ii++) {
                                        kernelSizeArray[ii + kernelRadiusNaN][jj + kernelRadiusNaN] =
                                                sourceData[(y + j + jj) * (sourceWidth) + x + i + ii];
                                        kernelSizeFlagArray[ii + kernelRadiusNaN][jj + kernelRadiusNaN] =
                                                flagArray[(y + j + jj) * (sourceWidth) + x + i + ii];
                                    }
                                }
                                meanValue = kernel[i + radius][j + radius] *
                                        replaceFalseValue(kernelSizeArray, kernelSizeFlagArray, kernelSizeNaN);
                            } else {
                                meanValue = kernel[i + radius][j + radius] * sourceData[(y + j) * (sourceWidth) + x + i];
                            }

                            sum = sum + meanValue;

                        }
                    }
                    output[x][y] = sum;
                } else {
                    output[x][y] = Double.NaN;

                }
            }
        }

        return output;
    }

    private double replaceFalseValue(double[][] PointsArray, double[][] FlagArray, int kernelSizeNaN) {
        double sum = 0;
        int sumsum = 0;
        for (int j = 0; j < kernelSizeNaN; j++) {
            for (int i = 0; i < kernelSizeNaN; i++) {
                if ((FlagArray[i][j] == PreparationOfSourceBands.OCEAN_FLAG ||
                        FlagArray[i][j] == PreparationOfSourceBands.LAND_FLAG ||
                        FlagArray[i][j] == PreparationOfSourceBands.LAND_FLAG + PreparationOfSourceBands.OCEAN_FLAG )) {
                    sum = sum + PointsArray[i][j];
                    sumsum++;
                }
            }
        }
        return (sum / sumsum);
    }
}
  