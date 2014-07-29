package org.esa.cci.lc;

public class PreparingOfSourceBand {

    static int waterValue = 5;
    static int landValue = 10;

    public void preparedOfSourceBand(int[] sourceData1,
                                     int[] sourceData2,
                                     int sourceWidth,
                                     int sourceHeight) {

        // sourceBandFlagProperties = createSourceBandFlagProperties(preparedSourceTile.getWidth(), preparedSourceTile.getHeight());

        int k;


        for (int j = 0; j < sourceHeight; j++) {
            for (int i = 0; i < sourceWidth; i++) {
                k = j * (sourceWidth) + i;


                if (sourceData1[k] > 200 && sourceData1[k] < 300) { // SAR_WB_PRODUCT - Land
                    sourceData1[k] = landValue;
                } else {
                    sourceData1[k] = waterValue;
                }
                if (sourceData2[k] < 40) { // SWBD_WB_PRODUCT - LAND
                    sourceData2[k] = landValue;
                } else {
                    sourceData2[k] = waterValue;
                }
            }

        }
    }

}

