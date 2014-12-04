package org.esa.cci.lc;

public class HotTransformation {


    private static final int FILL_NEIGHBOUR_VALUE = 4;


    public int calculateHOTBand(double[] sourceDataBlue,
                                double[] sourceDataRed,
                                int sourceWidth,
                                int sourceHeight,
                                double[] tachArray,
                                int[] flagArray,
                                double[] hotArray,
                                double meanValue) {

        int width = sourceWidth;
        int height = sourceHeight;
        int counterValid = 0;
        double meanBlue = 0.0;
        double meanRed = 0.0;
        double slope_numerator = 0.0;
        double slope_denominator = 0.0;
        double slope = 0.0;


        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {
                if (flagArray[j * (width) + i] == PreparingOfSourceBand.CLEAR_LAND_FLAG && tachArray[j * (width) + i] <= meanValue) {
                    counterValid = counterValid + 1;
                    meanBlue = meanBlue + sourceDataBlue[j * (width) + i];     // x = blue, y = red
                    meanRed = meanRed + sourceDataBlue[j * (width) + i];
                }
            }
        }
        meanBlue = meanBlue / counterValid;
        meanRed = meanRed / counterValid;

        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {
                if (flagArray[j * (width) + i] == PreparingOfSourceBand.CLEAR_LAND_FLAG && tachArray[j * (width) + i] <= meanValue) {
                    slope_numerator = slope_numerator
                                      + (sourceDataBlue[j * (width) + i] - meanBlue) * (sourceDataRed[j * (width) + i] - meanRed);
                    slope_denominator = slope_denominator
                                        + (sourceDataBlue[j * (width) + i] - meanBlue) * (sourceDataBlue[j * (width) + i] - meanBlue);
                }
            }
        }
        slope = Math.atan2(slope_numerator, slope_denominator);

        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {
                if (flagArray[j * (width) + i] == PreparingOfSourceBand.CLEAR_LAND_FLAG) {
                    hotArray[j * (width) + i] = (sourceDataBlue[j * (width) + i] * Math.sin(slope))
                                                - (sourceDataRed[j * (width) + i] * Math.cos(slope));
                }
            }
        }
        return counterValid;
    }

}



