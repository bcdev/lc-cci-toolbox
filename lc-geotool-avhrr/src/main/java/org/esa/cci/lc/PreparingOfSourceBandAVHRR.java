package org.esa.cci.lc;

public class PreparingOfSourceBandAVHRR {

    static final int UNVALID_FLAG = 1000;
    static final int LAND_FLAG = 100;
    static final int CLOUD_FLAG = 10;
    static final int OCEAN_FLAG = 1;

    private static final int FILL_NEIGHBOUR_VALUE = 4;

    // todo invalid pixel


    public void cloudDetectionOfSourceBand(  double[] landWaterData,
                                       double[] cloudAlbedo1Data,
                                       double[] cloudAlbedo2Data,
                                       double[] cloudBT4Data,
                                       int[] flagData) {


        double landWaterThreshold = 50.0;

        cloudTest(cloudAlbedo1Data, cloudAlbedo2Data, cloudBT4Data, landWaterData,
                landWaterThreshold, flagData);



    }


    private void cloudTest(double[] sourceDataAlbedo1,
                           double[] sourceDataAlbedo2,
                           double[] sourceDataBT4,
                           double[] sourceDataLandWater,
                           double landWaterThreshold,
                           int[] flagArray) {

        int sourceLength = sourceDataLandWater.length;

        double testLandWater;
        double TGCTValue;
        double RRCTValue;
        double thresholdTGCTOverLand  = 244;
        double thresholdTGCTOverOcean  = 270;
        double thresholdRRCTOverLand  = 1.1;
        double thresholdRRCTOverOcean  = 0.9;

        for (int ij = 0; ij < sourceLength - 1; ij++) {

            testLandWater = sourceDataLandWater[ij];
            TGCTValue = ((1.438833 *927)/Math.log(1.+ (0.000011910659* Math.pow(927,3)/ sourceDataBT4[ij]) ));
            RRCTValue = sourceDataAlbedo1[ij]/sourceDataAlbedo2[ij];

            if (testLandWater < landWaterThreshold) {
                flagArray[ij] = LAND_FLAG;
                //land
                flagArray[ij] = LAND_FLAG;
                if (RRCTValue <= thresholdRRCTOverLand || TGCTValue <= thresholdTGCTOverLand) {
                    flagArray[ij]= flagArray[ij] + CLOUD_FLAG;
                }
            }  else {

                flagArray[ij] = OCEAN_FLAG;
                if (RRCTValue <= thresholdRRCTOverOcean || TGCTValue <= thresholdTGCTOverOcean) {
                    flagArray[ij]= flagArray[ij] + CLOUD_FLAG;
                }

            }
        }
    }

}

