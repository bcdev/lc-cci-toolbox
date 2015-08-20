package org.esa.cci.lc;

import java.util.Arrays;

public class CayulaFrontsTest {

    public double[] computeCayulaFrontsTest(double[] windowData,
                                            int windowSize,
                                            double[] splitValue,
                                            double[][] maskArray) {


        int windowSquare = windowSize * windowSize;
        double[] frontsArray = new double[windowSquare];
        Arrays.fill(frontsArray, 0.0);

        for (int j = 0; j < windowSize; j++) {
            for (int i = 0; i < windowSize; i++) {

                if (!Double.isNaN(maskArray[i][j])) {
                    if (maskArray[i][j] > 0.5 && j < windowSize - 1 && i < windowSize - 1) {
                        //System.out.printf("Neighbour %f %f %f  \n", maskArray[i][j], maskArray[i][j + 1], maskArray[i + 1][j]);
                        if (maskArray[i][j + 1] < 0.5 && !Double.isNaN(maskArray[i][j + 1])) {
                            frontsArray[j * windowSize + i] = AvhrrGeoToolOperator.frontValue;
                        }
                        if (maskArray[i + 1][j] < 0.5 && !Double.isNaN(maskArray[i + 1][j])) {
                            frontsArray[j * windowSize + i] = AvhrrGeoToolOperator.frontValue;
                        }
                    }
                    if (maskArray[i][j] < 0.5 && j < windowSize - 1 && i < windowSize - 1) {
                        if (maskArray[i][j + 1] > 0.5 && !Double.isNaN(maskArray[i][j + 1])) {
                            frontsArray[j * windowSize + i] = AvhrrGeoToolOperator.frontValue;
                        }
                        if (maskArray[i + 1][j] > 0.5 && !Double.isNaN(maskArray[i + 1][j])) {
                            frontsArray[j * windowSize + i] = AvhrrGeoToolOperator.frontValue;
                        }

                    }
                }
            }
        }

 //       System.out.printf("Cohesion values %f %f %f  \n", cohesionValue[0], cohesionValue[1], cohesionValue[2]);

        return frontsArray;
    }

}