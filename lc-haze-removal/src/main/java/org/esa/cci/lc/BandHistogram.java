package org.esa.cci.lc;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.util.math.Histogram;
import org.esa.beam.util.math.IndexValidator;

import java.util.Arrays;


public class BandHistogram {

    public Histogram computeHazeOneClass(double[] sourceData,
                                         int[] hotLevelArray,
                                         int sourceWidth,
                                         int sourceHeight,
                                         int[] flagArray,
                                         int binValue,
                                         int counterClear) {


        int histogramBins = HazeRemovalOperator.standardHistogramBins;
        int lengthHazeOneClassArray = counterClear;

        int counterHaze = 0;
        final double[] hazeOneClassDataArray = new double[lengthHazeOneClassArray];
        Arrays.fill(hazeOneClassDataArray, Double.NaN);

        for (int j = 0; j < sourceHeight; j++) {
            for (int i = 0; i < sourceWidth; i++) {
                if (flagArray[j * (sourceWidth) + i] == PreparingOfSourceBand.CLEAR_LAND_FLAG && hotLevelArray[j * (sourceWidth) + i] == binValue) {
                    hazeOneClassDataArray[counterHaze] = sourceData[j * (sourceWidth) + i];
                    counterHaze = counterHaze + 1;
                }
            }
        }

        // calculation of histogram
        Histogram histogramHazeOneClass = Histogram.computeHistogramDouble(hazeOneClassDataArray, IndexValidator.TRUE, histogramBins,
                                                                           null, null, ProgressMonitor.NULL);


        return histogramHazeOneClass;
    }

    public Histogram computeHazeAllClass(double[] sourceData,
                                         int[] hotLevelArray,
                                         int sourceWidth,
                                         int sourceHeight,
                                         int[] flagArray,
                                         int thresholdValue,
                                         int counterClear) {


        int histogramBins = HazeRemovalOperator.standardHistogramBins;
        int lengthHazeAllClassArray = counterClear;

        int counterHaze = 0;
        final double[] hazeAllClassDataArray = new double[lengthHazeAllClassArray];
        Arrays.fill(hazeAllClassDataArray, Double.NaN);

        for (int j = 0; j < sourceHeight; j++) {
            for (int i = 0; i < sourceWidth; i++) {
                if (flagArray[j * (sourceWidth) + i] == PreparingOfSourceBand.CLEAR_LAND_FLAG && hotLevelArray[j * (sourceWidth) + i] <= thresholdValue) {
                    hazeAllClassDataArray[counterHaze] = sourceData[j * (sourceWidth) + i];
                    counterHaze = counterHaze + 1;
                }
            }
        }

        // calculation of histogram
        Histogram histogramHazeAllClass = Histogram.computeHistogramDouble(hazeAllClassDataArray, IndexValidator.TRUE, histogramBins,
                                                                           null, null, ProgressMonitor.NULL);


        return histogramHazeAllClass;
    }
}