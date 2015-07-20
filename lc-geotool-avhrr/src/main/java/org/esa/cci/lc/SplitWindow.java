package org.esa.cci.lc;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.util.math.Histogram;
import org.esa.beam.util.math.IndexValidator;

import java.util.Arrays;


public class SplitWindow {

    public void compute(double[] sourceData,
                        int sourceWidth,
                        int sourceHeight,
                        int[] flagArray,
                        int windowSize,
                        double[] frontsData) {

        double[] windowData = new double[windowSize * windowSize];
        double[] frontsArray = new double[windowSize * windowSize];

        Arrays.fill(frontsArray, 0);
        int  histogramBins = AvhrrGeoToolOperator.standardHistogramBins;
        int step = (int) (windowSize * AvhrrGeoToolOperator.windowOverlap / 100.);
        step = windowSize - step;
        int height_count = (int) Math.floor((double) (sourceHeight - windowSize) / (double) step);
        int width_count = (int) Math.floor((double) (sourceWidth - windowSize) / (double) step);

        width_count = width_count * step;
        height_count = height_count * step;

        //    image segmentation into overlapping windows
        for (int k = 0; k <= height_count; k = k + step) {
            for (int l = 0; l <= width_count; l = l + step) {
                double winMax = -Double.MAX_VALUE;
                double winMin = Double.MAX_VALUE;
                // Edge detection at the window level for the each window
                // filling with source data
                int counterNoNaN = 0;
                for (int j = 0; j < windowSize; j++) {
                    for (int i = 0; i < windowSize; i++) {
                        windowData[j * (windowSize) + i] = Double.NaN;

                        if (Double.isNaN(sourceData[(j + k) * (sourceWidth) + (i + l)]) == false) {
                            counterNoNaN++;
                            windowData[j * (windowSize) + i] = sourceData[(j + k) * (sourceWidth) + (i + l)];
                            if (windowData[j * (windowSize) + i] > winMax) {
                                winMax = windowData[j * (windowSize) + i];
                            }
                            if (windowData[j * (windowSize) + i] < winMin) {
                                winMin = windowData[j * (windowSize) + i];
                            }
                        }
                    }
                }
                if (counterNoNaN > 2) {
                    histogramBins = (int) ((winMax - winMin) / 0.01);  //0.05
                } //else histogramBins = 32;
                if (histogramBins == 0) {
                    histogramBins = AvhrrGeoToolOperator.standardHistogramBins;
                }


                // calculation of histogram
                Histogram histogram = Histogram.computeHistogramDouble(windowData, IndexValidator.TRUE, histogramBins,
                        null, null, ProgressMonitor.NULL);

                // bimodality test
                BiomodalityTest bimodality = new BiomodalityTest();
                double[] splitValue = bimodality.computeBiomodalityTest(windowData,
                        windowSize,
                        histogramBins,
                        histogram);

                if (splitValue[1] > 0.0) {
                    // cohesion algorithm
                    double[][] maskArray = new double[windowSize][windowSize];
                    CohesionTest cohesion = new CohesionTest();
                    double[] cohesionValue = cohesion.computeCohesionTest(windowData,
                            windowSize,
                            splitValue,
                            maskArray);
                    if (cohesionValue[0] > 0.90 && cohesionValue[1] > 0.90 && cohesionValue[2] > 0.92) {

                        CayulaFrontsTest cayulaFronts = new CayulaFrontsTest();
                        frontsArray = cayulaFronts.computeCayulaFrontsTest(windowData,
                                windowSize,
                                splitValue,
                                maskArray);
                    } else Arrays.fill(frontsArray, 0.0);
                } else Arrays.fill(frontsArray, 0.0);

                for (int j = 0; j < windowSize; j++) {
                    for (int i = 0; i < windowSize; i++) {
                        int brr = (j + k) * (sourceWidth) + (i + l);
                        frontsData[brr] = frontsData[brr] + frontsArray[j * (windowSize) + i];
                        if (frontsData[brr] > 1) frontsData[brr] = 1;
                      //System.out.printf("Start point %d %d  %d %d\n", (j + k), (i + l), k, l);
                    }
                }
            }
        }

    }
}