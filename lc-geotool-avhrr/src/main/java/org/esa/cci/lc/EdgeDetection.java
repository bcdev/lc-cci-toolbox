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


        int windowSize = (sourceWidth-sourceWidth%2)/2;

        //System.out.printf("source width height windowsize:  %d  %d %d  \n",sourceWidth, sourceHeight, windowSize);

        double[] frontsCayulaArray = new double[sourceWidth * sourceHeight];
        Arrays.fill(frontsCayulaArray, 0.0);
        double[] frontsData = new double[sourceWidth * sourceHeight];

        for (int wi = windowSize; wi < sourceWidth; wi += windowSize) {
            /* SIED Operator */
            Arrays.fill(frontsData, 0.0);
            //Arrays.fill(frontsCayulaArray, 0);
            SplitWindow splitWindow = new SplitWindow();
            splitWindow.compute(histogramSourceData,
                        sourceWidth,
                        sourceHeight,
                        flagArray,
                        wi,
                        frontsData);

            for (int j = 0; j < sourceHeight; j++) {
                for (int i = 0; i < sourceWidth; i++) {
                    int k = (j) * (sourceWidth) + (i);
                    frontsCayulaArray[k] = frontsCayulaArray[k] + frontsData[k];
                    if (frontsCayulaArray[k] > 1) frontsCayulaArray[k] = 1;
                }
            }
        }


        return frontsCayulaArray;
    }
}
