package org.esa.cci.lc;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.util.math.Histogram;
import org.esa.beam.util.math.IndexValidator;

import java.util.Arrays;


public class HotHistogram {

    public Histogram compute(double[] tachArray,
                             double[] hotArray,
                             int sourceWidth,
                             int sourceHeight,
                             int[] flagArray,
                             double meanValue,
                             int [] counterValue) {


        int histogramBins = HazeRemovalOperator.standardHistogramBins;
        int lengthHazeArray = counterValue[0] - counterValue[1] +2;
        System.out.printf("Valid pixel:  %s  \n", counterValue[0]);
        System.out.printf("Clear pixel:  %s  \n", counterValue[1]);
        System.out.printf("Haze pixel:  %s  \n", lengthHazeArray);

        int counterHaze = 0;
        double[] hotDataArray;
        hotDataArray = new double[lengthHazeArray];
        Arrays.fill(hotDataArray, Double.NaN);

        for (int j = 0; j < sourceHeight; j++) {
            for (int i = 0; i < sourceWidth; i++) {
                if (flagArray[j * (sourceWidth) + i] == PreparingOfSourceBand.CLEAR_LAND_FLAG && tachArray[j * (sourceWidth) + i] > meanValue) {
                    hotDataArray[counterHaze] = hotArray[j * (sourceWidth) + i];
                    counterHaze = counterHaze + 1;
                }
            }
        }


        // calculation of histogram
        Histogram histogram = Histogram.computeHistogramDouble(hotDataArray, IndexValidator.TRUE, histogramBins,
                                                               null, null, ProgressMonitor.NULL);


        return histogram;
    }
}