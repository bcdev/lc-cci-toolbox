package org.esa.cci.lc;


public class GradientOperator {


    public double[][] computeGradient(double[] sourceData,
                                      int sourceWidth,
                                      int sourceHeight) {


        int sourceLength = sourceWidth * sourceHeight;

        double[][] kernelGradient3x3_Y = make3x3ConvolutionKernel(WaterBodyCompareOperator.kernelEdgeValue,
                                                                  WaterBodyCompareOperator.kernelCentreValue);
        double[][] kernelGradient3x3_X = make3x3TransposeConvolutionKernel(kernelGradient3x3_Y);

        double[][] gradientData = new double[2][sourceLength];
        int kernelRadius = WaterBodyCompareOperator.convolutionFilterKernelRadius;

        Convolution xConvolution = new Convolution(kernelGradient3x3_X, kernelRadius);
        Convolution yConvolution = new Convolution(kernelGradient3x3_Y, kernelRadius);

        double[][] xData = xConvolution.makeConvolution(sourceData, sourceWidth, sourceHeight);
        double[][] yData = yConvolution.makeConvolution(sourceData, sourceWidth, sourceHeight);

        double[] gradientMagnitudeArray = new double[sourceLength];
        double[] gradientDirectionArray = new double[sourceLength];
        double convertToDegree;

        for (int y = 0; y < sourceHeight; y++) {
            for (int x = 0; x < sourceWidth; x++) {

                gradientMagnitudeArray[y * (sourceWidth) + x] =
                        Math.sqrt((xData[x][y] * xData[x][y]) + (yData[x][y] * yData[x][y])) / (WaterBodyCompareOperator.weightingFactor);

                gradientData[0][y * (sourceWidth) + x] = gradientMagnitudeArray[y * (sourceWidth) + x];
                if (yData[x][y] == 0.0 && xData[x][y] == 0.0) {
                    gradientDirectionArray[y * (sourceWidth) + x] = 0.0;
                } else {
                    convertToDegree = (180. * Math.atan2(yData[x][y], xData[x][y])) / Math.PI;
                    if (convertToDegree <= 0) {
                        convertToDegree = convertToDegree + 360.0;
                    }
                    gradientDirectionArray[y * (sourceWidth) + x] = convertToDegree;

                }
                gradientData[1][y * (sourceWidth) + x] = gradientDirectionArray[y * (sourceWidth) + x];
            }
        }

        return gradientData;

    }


    private double[][] make3x3TransposeConvolutionKernel
            (
                    double[][] kernel3x3_Y) {
        double[][] kernel3x3_X = new double[3][3];

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                kernel3x3_X[j][i] = kernel3x3_Y[i][j];
            }
        }
        return kernel3x3_X;
    }

    private double[][] make3x3ConvolutionKernel
            (
                    double kernelEdgeValue,
                    double kernelCentreValue) {
        double[][] kernel3x3_Y = new double[3][3];


        kernel3x3_Y[0][0] = kernelEdgeValue;
        kernel3x3_Y[1][0] = kernelCentreValue;
        kernel3x3_Y[2][0] = kernelEdgeValue;
        kernel3x3_Y[0][1] = 0.0;
        kernel3x3_Y[1][1] = 0.0;
        kernel3x3_Y[2][1] = 0.0;
        kernel3x3_Y[0][2] = -kernelEdgeValue;
        kernel3x3_Y[1][2] = -kernelCentreValue;
        kernel3x3_Y[2][2] = -kernelEdgeValue;

        return kernel3x3_Y;

    }

}
