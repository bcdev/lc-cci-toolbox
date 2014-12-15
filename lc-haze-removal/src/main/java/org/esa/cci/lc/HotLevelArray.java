package org.esa.cci.lc;

import org.esa.beam.util.math.Histogram;

import java.util.Arrays;


public class HotLevelArray {

    public int[] compute(double[] hotArray,
                         int sourceWidth,
                         int sourceHeight,
                         int[] flagArray,
                         Histogram histogramHotAll,
                         int[] binIndex) {


        double minValueHot = histogramHotAll.getMin();
        double maxValueHot = histogramHotAll.getMax();
        int minBinIndex = histogramHotAll.getBinIndex(minValueHot);
        int maxBinIndex = histogramHotAll.getBinIndex(maxValueHot);
        double[] lowerBinValue = new double[binIndex.length];
        double[] upperBinValue = new double[binIndex.length];

        double step = ((maxValueHot - minValueHot) / (maxBinIndex - minBinIndex)) / 2.;

        lowerBinValue[0] = minValueHot - step;
        upperBinValue[0] = minValueHot + step;

        for (int k = 1; k < binIndex.length; k++) {
            lowerBinValue[k] = 2 * step + lowerBinValue[k - 1];
            upperBinValue[k] = 2 * step + upperBinValue[k - 1];

            //System.out.printf("Bin_Value:  %d  %d   %f  %f  \n", binIndex[k], k, lowerBinValue[k], upperBinValue[k]);
        }


        int[] hotLevelArray = new int[sourceWidth * sourceHeight];

        Arrays.fill(hotLevelArray, HazeRemovalOperator.standardHistogramBins + 1);

        for (int j = 0; j < sourceHeight; j++) {
            for (int i = 0; i < sourceWidth; i++) {
                for (int k = 0; k < binIndex.length; k++) {
                    if (lowerBinValue[k] < hotArray[j * (sourceWidth) + i] && hotArray[j * (sourceWidth) + i] <= upperBinValue[k]) {
                        hotLevelArray[j * (sourceWidth) + i] = k;
                    }
                }
            }
        }
        return hotLevelArray;
    }
}