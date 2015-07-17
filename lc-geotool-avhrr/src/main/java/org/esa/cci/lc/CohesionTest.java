package org.esa.cci.lc;

public class CohesionTest {
    public double[] computeCohesionTest(double[] windowData,
                                        int windowSize,
                                        double[] splitValue,
                                        double[][] maskArray) {

        double[] cohesionValue = new double[3];
        int windowSquare = windowSize * windowSize;


        //double[] populationTwoArray = new double[windowSquare];
        int R1Counter = 0;
        int R2Counter = 0;
        int T1Counter = 0;
        int T2Counter = 0;

        // Arrays.fill(populationOneArray, Double.NaN);
        //Arrays.fill(populationOneArray, Double.NaN);
        for (int j = 0; j < windowSize; j++) {
            for (int i = 0; i < windowSize; i++) {
                if (Double.isNaN(windowData[j * windowSize + i])) {
                    maskArray[i][j] = Double.NaN;
                } else {
                    //System.out.printf("maskArray  %f %f  \n", windowData[j * windowSize + i],splitValue[0]);
                    if (windowData[j * windowSize + i] <= splitValue[0]) {
                        maskArray[i][j] = 0.0;
                    } else maskArray[i][j] = 1.0;
                }
            }
        }


        for (int j = 0; j < windowSize; j++) {
            for (int i = 0; i < windowSize; i++) {
                if (!Double.isNaN(maskArray[i][j])) {
                    if (maskArray[i][j] > 0.5 && j < windowSize - 1 && i < windowSize - 1) {
             //System.out.printf("Neighbour %f %f %f  \n", maskArray[i][j], maskArray[i][j + 1], maskArray[i + 1][j]);
                        if (maskArray[i][j + 1] > 0.5 && !Double.isNaN(maskArray[i][j + 1])) {
                            R1Counter++;
                            T1Counter++;
                        }
                        if (maskArray[i + 1][j] > 0.5 && !Double.isNaN(maskArray[i + 1][j])) {
                            R1Counter++;
                            T1Counter++;
                        }
                        if (maskArray[i][j + 1] < 0.5 && !Double.isNaN(maskArray[i][j + 1])) {
                            T1Counter++;
                        }
                        if (maskArray[i + 1][j] < 0.5 && !Double.isNaN(maskArray[i + 1][j])) {
                            T1Counter++;
                        }
                    }
                    if (maskArray[i][j] < 0.5 && j < windowSize - 1 && i < windowSize - 1) {
                        if (maskArray[i][j + 1] < 0.5 && !Double.isNaN(maskArray[i][j + 1])) {
                            R2Counter++;
                            T2Counter++;
                        }
                        if (maskArray[i + 1][j] < 0.5 && !Double.isNaN(maskArray[i + 1][j])) {
                            R2Counter++;
                            T2Counter++;
                        }
                        if (maskArray[i][j + 1] > 0.5 && !Double.isNaN(maskArray[i][j + 1])) {
                            T2Counter++;
                        }
                        if (maskArray[i + 1][j] > 0.5 && !Double.isNaN(maskArray[i + 1][j])) {
                            T2Counter++;
                        }

                    }
                }
            }
        }
        if ((double)T1Counter > 0.0) cohesionValue[0] = (double)R1Counter / (double)T1Counter;
        if ((double)T2Counter > 0.0) cohesionValue[1] = (double)R2Counter / (double)T2Counter;
        if ((double)(T1Counter + T2Counter) > 0.0) cohesionValue[2] = (double)(R1Counter + R2Counter) / (double)(T1Counter + T2Counter);
        // System.out.printf("Cohesion values %f %f %f  \n", cohesionValue[0], cohesionValue[1], cohesionValue[2]);
        // System.out.printf("\n");
        // System.out.printf("\n");
        return cohesionValue;
    }
}