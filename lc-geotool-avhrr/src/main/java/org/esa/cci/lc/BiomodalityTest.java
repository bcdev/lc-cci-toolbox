package org.esa.cci.lc;

import org.esa.beam.util.math.Histogram;

import java.util.Arrays;

public class BiomodalityTest {

    public double[] computeBiomodalityTest(double[] windowData,
                                           int windowSize,
                                           int histogramBins,
                                           Histogram histogram) {

        int[] binCount = histogram.getBinCounts();
        int windowSquare = windowSize * windowSize;
        double[] leftFromSplitArray = new double[windowSquare];
        double[] rightFromSplitArray = new double[windowSquare];
        double[] binData = new double[histogramBins];
        int leftCounter = 0;
        int rightCounter = 0;
        int nanCounter = 0;
        double sumLeft = 0.;
        double sumRight = 0.;
        double meanLeft;
        double meanRight;
        double varianceLeft;
        double varianceRight;
        double[] withinClusterVariance = new double[histogramBins];
        double[] betweenClusterVariance = new double[histogramBins];
        double[] goodnessSegmentationRatio = new double[histogramBins];
        double maxBetweenClusterVariance = Double.MIN_VALUE;
        int thetaOptimum = 0;
        double[] splitValue = new double[2];
        double allRoundCounter;
        double[] SNR = new double[histogramBins];

        for (int i = 0; i < histogramBins; i++) {
            binData[i] = (histogram.getRange(i).getMax() + histogram.getRange(i).getMin()) / 2.;
            Arrays.fill(leftFromSplitArray, 0);
            Arrays.fill(rightFromSplitArray, 0);
            leftCounter = 0;
            rightCounter = 0;
            nanCounter = 0;
            sumLeft = 0.;
            sumRight = 0.;

            for (int j = 0; j < windowSquare; j++) {
                if (!Double.isNaN(windowData[j])) {
                    if (windowData[j] < binData[i]) {
                        leftFromSplitArray[leftCounter] = windowData[j];
                        sumLeft = sumLeft + leftFromSplitArray[leftCounter];
                        leftCounter++;
                    } else {
                        rightFromSplitArray[rightCounter] = windowData[j];
                        sumRight = sumRight + rightFromSplitArray[rightCounter];
                        rightCounter++;
                    }
                } else nanCounter++;
            }
            //System.out.printf("SquareCounter %d %d %d %d %d  \n", leftCounter, rightCounter, nanCounter, leftCounter + rightCounter + nanCounter, windowSquare);
            meanLeft = sumLeft / leftCounter;
            meanRight = sumRight / rightCounter;
            //System.out.printf("%f %f  \n", meanLeft, meanRight);
            varianceLeft = 0.;
            for (int l = 0; l < leftCounter; l++) {
                varianceLeft = varianceLeft + Math.pow((leftFromSplitArray[l] - meanLeft), 2.);
            }
            varianceRight = 0.;
            for (int r = 0; r < rightCounter; r++) {
                varianceRight = varianceRight + Math.pow((rightFromSplitArray[r] - meanRight), 2.);
            }
            varianceLeft = varianceLeft / leftCounter;
            varianceRight = varianceRight / rightCounter;

            allRoundCounter = rightCounter + leftCounter;
            withinClusterVariance[i] = ((leftCounter) / (allRoundCounter)) * varianceLeft
                    + ((rightCounter) / (allRoundCounter)) * varianceRight;
            betweenClusterVariance[i] = ((leftCounter * rightCounter) / (Math.pow(allRoundCounter, 2.)))
                    * Math.pow((meanLeft - meanRight), 2.);

            if (betweenClusterVariance[i] > maxBetweenClusterVariance) {
                maxBetweenClusterVariance = betweenClusterVariance[i];
                thetaOptimum = i;
                splitValue[0] = binData[i];
            }
            goodnessSegmentationRatio[i]
                    = betweenClusterVariance[i] / (betweenClusterVariance[i] + withinClusterVariance[i]);
            //compute SNR - signal to noise ratio

            SNR[i] = Math.sqrt(Math.abs(meanLeft-meanRight)/withinClusterVariance[i]);
            //System.out.printf("SNR  %f  %d  %f %f %f\n", SNR[i], i, meanLeft, meanRight, withinClusterVariance[i]);
        } /* threshold loop*/
        
       if (goodnessSegmentationRatio[thetaOptimum]>AvhrrGeoToolOperator.thresholdSegmentationGoodness){
            splitValue[1] = 1.0;
        }else{
            splitValue[1] = -1.0;
        }
        // System.out.printf("SNR  %f  %d %f %f\n", SNR[thetaOptimum], thetaOptimum, splitValue[0],splitValue[1]);
        //System.out.printf("maxBetweenClusterVariance  %f  %d %f %f\n", goodnessSegmentationRatio[thetaOptimum], thetaOptimum, splitValue[0],splitValue[1]);

        /*
        //System.out.printf("%d %d  %d \n", k, l, counterNoNaN);
        for (int i = 0; i < histogramBins; i++) {
            System.out.printf("%d   ", binCount[i]);
        }
        System.out.printf("\n");
        for (int i = 0; i < histogramBins; i++) {
            System.out.printf("%f   ", binData[i]);
        }
        System.out.printf("\n");
        */

        return splitValue;
    }

}