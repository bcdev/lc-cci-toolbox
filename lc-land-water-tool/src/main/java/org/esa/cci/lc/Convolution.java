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
                                      int sourceHeight) {


        double[][] output = new double[sourceWidth][sourceHeight];
        int grid = radius; //   enhnaced method 2 * radius + 1;
        // int kernelRadiusNaN = radius;  //   enhnaced method
        // int kernelSizeNaN = kernelRadiusNaN * 2 + 1; //   enhnaced method
        // double[][] kernelSizeArray = new double[kernelSizeNaN][kernelSizeNaN];  //   enhnaced method
        double meanValue;
        for (int y = grid; y < sourceHeight - grid; y++) {
            for (int x = grid; x < sourceWidth - grid; x++) {

                double sum = 0.0;

                for (int j = -radius; j < radius + 1; j++) {
                    for (int i = -radius; i < radius + 1; i++) {
                        meanValue = kernel[i + radius][j + radius] * sourceData[(y + j) * (sourceWidth) + x + i];
                        sum = sum + meanValue;
                    }
                }

                output[x][y] = sum;


                //enhanced method
                /*
                if (!Double.isNaN(d)) {
                    double sum = 0.0;
                    // It is the Convolution
                    for (int j = -radius; j < radius + 1; j++) {
                        for (int i = -radius; i < radius + 1; i++) {
                            double f = sourceData[(y + j) * (sourceWidth) + (x + i)];
                            if (Double.isNaN(f)) {
                                // DOUBLE.NaN values
                                for (int jj = -kernelRadiusNaN; jj < kernelRadiusNaN + 1; jj++) {
                                    for (int ii = -kernelRadiusNaN; ii < kernelRadiusNaN + 1; ii++) {
                                        kernelSizeArray[ii + kernelRadiusNaN][jj + kernelRadiusNaN] =
                                                sourceData[(y + j + jj) * (sourceWidth) + x + i + ii];
                                    }
                                }
                                meanValue = kernel[i + radius][j + radius] * replaceFalseValue(kernelSizeArray, kernelSizeNaN);
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
                */
            }
        }

        return output;
    }

    private double replaceFalseValue(double[][] PointsArray, int kernelSizeNaN) {
        double sum = 0;
        int sumsum = 0;
        for (int j = 0; j < kernelSizeNaN; j++) {
            for (int i = 0; i < kernelSizeNaN; i++) {
                if (!Double.isNaN(PointsArray[i][j])) {
                    sum = sum + PointsArray[i][j];
                    sumsum++;
                }
            }
        }
        return (sum / sumsum);
    }
}
  