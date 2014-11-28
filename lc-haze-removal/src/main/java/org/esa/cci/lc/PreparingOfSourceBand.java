package org.esa.cci.lc;

import java.awt.Rectangle;


public class PreparingOfSourceBand {



    static final int CLEAR_LAND_FLAG = 1;
    static final int LAND_FLAG = 10;
    static final int INVALID_FLAG = 100;

    private static final int FILL_NEIGHBOUR_VALUE = 4;


    public void preparedOfSourceBand(FlagDetector FlagDetector,
                                     int[] flagArray,
                                     Rectangle this_rectangle) {

        int width = this_rectangle.width;
        int height = this_rectangle.height;

        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {


                if (FlagDetector.isInvalid(i, j) == true) {
                    flagArray[j * (width) + i] = INVALID_FLAG;
                }

                if (FlagDetector.isClearLand(i, j) == true) {
                        flagArray[j * (width) + i] = CLEAR_LAND_FLAG;
                }
            }
        }
    }
}



