package org.esa.cci.lc;


public class TasseledCapTransformation {


    public double calculateTasseledCapTransformationHaze(double[] sourceDataBlue,
                                                         double[] sourceDataRed,
                                                         int sourceWidth,
                                                         int sourceHeight,
                                                         double[] tachArray,
                                                         int[] flagArray) {

        int kk;
        int counter = 0;
        double mean = 0.0;

        for (int j = 0; j < sourceHeight; j++) {
            for (int i = 0; i < sourceWidth; i++) {
                kk = j * (sourceWidth) + i;
                if (flagArray[kk] != PreparingOfSourceBand.INVALID_FLAG && flagArray[kk] == PreparingOfSourceBand.CLEAR_LAND_FLAG) {
                    tachArray[kk] = HazeRemovalOperator.tasseledCapFactorBlue * sourceDataBlue[kk]
                                    + HazeRemovalOperator.tasseledCapFactorRed * sourceDataRed[kk];
                    mean += tachArray[kk];
                    counter += 1;

                } else {
                    tachArray[kk] = Double.NaN;
                    flagArray[kk] = PreparingOfSourceBand.INVALID_FLAG;
                }
            }
        }
        HazeRemovalOperator.counterValid = counter;
        System.out.printf("counterValid:  %d  \n", HazeRemovalOperator.counterValid);

        return mean / HazeRemovalOperator.counterValid;
    }

}


