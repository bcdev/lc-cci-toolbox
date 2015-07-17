package org.esa.cci.lc;

import java.util.Arrays;

public class EdgeDetection {


    public static double[] computeEdges(double[] histogramSourceData,
                                        int[] flagArray,
                                        int sourceWidth,
                                        int sourceHeight) {

        /**************************************************************************/
        /************************** Histogram Method  *****************************/
        /**************************************************************************/


        Filter filter = new GaussFilter();
        filter.compute(histogramSourceData,
                sourceWidth,
                sourceHeight,
                flagArray,
                AvhrrGeoToolOperator.gaussFilterKernelRadius);


        int windowSize = 48;


        double[] frontsSumCayulaArray = new double[sourceWidth * sourceHeight];
        double[] frontsCayulaArray = new double[sourceWidth * sourceHeight];

        Arrays.fill(frontsSumCayulaArray, 0);

        for (int wi = 16; wi < 60; wi += 8) {
            /* SIED Operator */
            double[] frontsData = new double[sourceWidth * sourceHeight];

            Arrays.fill(frontsCayulaArray, 0);
            SplitWindow splitWindow = new SplitWindow();
            splitWindow.compute(histogramSourceData,
                        sourceWidth,
                        sourceHeight,
                        flagArray,
                        wi,
                        frontsData,
                        frontsCayulaArray );

            for (int j = 0; j < sourceHeight-1; j++) {
                for (int i = 0; i < sourceWidth-1; i++) {
                    int k = (j) * (sourceWidth) + (i);
                    frontsSumCayulaArray[k] = frontsSumCayulaArray[k] + frontsCayulaArray[k];
                    if (frontsSumCayulaArray[k] > 1) frontsSumCayulaArray[k] = 1;
                }
            }
        }


        return frontsSumCayulaArray;
    }
}
