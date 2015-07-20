package org.esa.cci.lc;

public class PreparationOfSourceBands {

    static final int UNVALID_FLAG = 1000;
    static final int CLOUD_FLAG = 100;
    static final int LAND_FLAG = 10;
    static final int OCEAN_FLAG = 1;

    private static final int FILL_NEIGHBOUR_VALUE = 4;

    // todo invalid pixel


    public void cloudDetectionOfAvhrrSourceBand(double[] landWaterData,
                                                double[] cloudAlbedo1Data,
                                                double[] cloudAlbedo2Data,
                                                double[] cloudBT4Data,
                                                int[] flagData) {


        double landWaterThreshold = 50.0;

        cloudTest(cloudAlbedo1Data, cloudAlbedo2Data, cloudBT4Data, landWaterData,
                landWaterThreshold, flagData);



    }

    public void preparationOfAvhrrSourceBand(double[] sourceData,
                                             int[] flagData) {


        int sourceLength = sourceData.length;

        for (int ij = 0; ij < sourceLength - 1; ij++) {

            if (flagData[ij] > LAND_FLAG + OCEAN_FLAG){
                sourceData[ij] = Double.NaN;
                flagData[ij] = UNVALID_FLAG;

            }
        }
    }


    public void preparationOfMerisSourceBand(double[] sourceData,
                                             int[] flagData) {


        int sourceLength = sourceData.length;

        for (int ij = 0; ij < sourceLength - 1; ij++) {

            if (Double.isNaN(sourceData[ij]) ==true ){
                sourceData[ij] = Double.NaN;
                flagData[ij] = UNVALID_FLAG;

            }  else flagData[ij] = OCEAN_FLAG + LAND_FLAG;
        }
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
        double thresholdTGCTOverLand  = 244.;
        double thresholdTGCTOverOcean  = 270.;
        double thresholdRRCTOverLand  = 1.1;
        double thresholdRRCTOverOcean  = 0.9;

        for (int ij = 0; ij < sourceLength; ij++) {

            testLandWater = sourceDataLandWater[ij];
            TGCTValue = ((1.438833 *927)/Math.log(1.+ (0.000011910659* Math.pow(927,3)/ sourceDataBT4[ij]) ));
            RRCTValue = sourceDataAlbedo1[ij]/sourceDataAlbedo2[ij];

            if (testLandWater < landWaterThreshold) {
                //land
                flagArray[ij] = LAND_FLAG;
                if (RRCTValue <= thresholdRRCTOverLand || TGCTValue <= thresholdTGCTOverLand) {
                //    flagArray[ij]= CLOUD_FLAG;
                }
            }  else {
                flagArray[ij] = OCEAN_FLAG;
                if (RRCTValue >= thresholdRRCTOverOcean || TGCTValue <= thresholdTGCTOverOcean) {
                //    flagArray[ij]= CLOUD_FLAG;
                }

            }
        }
    }

}

