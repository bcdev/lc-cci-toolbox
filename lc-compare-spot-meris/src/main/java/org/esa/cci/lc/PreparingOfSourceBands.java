package org.esa.cci.lc;

import java.io.File;
import java.util.Arrays;

public class PreparingOfSourceBands {

    public void preparedOfSourceBands(float[] sourceDataBandS1_B0,
                                      float[] sourceDataBandS1_B2,
                                      float[] sourceDataBandS1_B3,
                                      int[] sourceDataBandS1_SM,
                                      float[] sourceDataBandP_B0,
                                      float[] sourceDataBandP_B2,
                                      float[] sourceDataBandP_B3,
                                      int[] sourceDataBandP_SM,
                                      int[] sourceDataBandP_Idepix,
                                      float[] sourceDataBandMeris_B0,
                                      float[] sourceDataBandMeris_B2,
                                      float[] sourceDataBandMeris_B3,
                                      int[] sourceDataBandMeris_SM,
                                      int[] sourceDataBandMeris_Idepix,
                                      int sourceLength,
                                      int sourceWidth,
                                      int sourceHeight,
                                      String productName) {


        int counter_snow_spot_s1 = 0;
        int counter_clear_land_spot_s1 = 0;

        int counter_snow_spot_p = 0;
        int counter_clear_land_spot_p = 0;

        int counter_snow_meris = 0;
        int counter_clear_land_meris = 0;

        int counter_invalid = 0;

        float[][] clearLandDataSPOTS1MERISRRB0;
        float[][] clearLandDataSPOTS1MERISRRB2;
        float[][] clearLandDataSPOTS1MERISRRB3;
        float[][] clearSnowDataSPOTS1MERISRRB0;
        float[][] clearSnowDataSPOTS1MERISRRB2;
        float[][] clearSnowDataSPOTS1MERISRRB3;
        float[][] clearLandDataSPOTPMERISRRB0;
        float[][] clearLandDataSPOTPMERISRRB2;
        float[][] clearLandDataSPOTPMERISRRB3;
        float[][] clearSnowDataSPOTPMERISRRB0;
        float[][] clearSnowDataSPOTPMERISRRB2;
        float[][] clearSnowDataSPOTPMERISRRB3;


        float[] correlationClearSnow = new float[6];
        float[] correlationClearLand = new float[6];

        float[][] methodLeastSquaresClearSnow = new float[6][4];
        float[][] methodLeastSquaresClearLand = new float[6][4];

        float[] resultLeastSquares = new float[3];

        ///System.out.printf("sourceLength:  %d\n", sourceLength);
        for (int i = 0; i < sourceLength; i++) {

            if ((sourceDataBandS1_SM[i] & 128) == 128 &&
                (sourceDataBandS1_SM[i] & 64) == 64 &&
                (sourceDataBandS1_SM[i] & 32) == 32 &&
                (sourceDataBandS1_SM[i] & 16) == 16 &&
                (sourceDataBandP_SM[i] & 128) == 128 &&
                (sourceDataBandP_SM[i] & 64) == 64 &&
                (sourceDataBandP_SM[i] & 32) == 32 &&
                (sourceDataBandP_SM[i] & 16) == 16) {  // no influenced by suspected pixel
                if ((sourceDataBandS1_SM[i] & 4) == 4 &&
                    !((sourceDataBandS1_SM[i] & 2) == 2 && (sourceDataBandS1_SM[i] & 1) == 1)) {
                    counter_snow_spot_s1 += 1;
                }
                if ((sourceDataBandS1_SM[i] & 8) == 8 &&
                    !((sourceDataBandS1_SM[i] & 2) == 2 && (sourceDataBandS1_SM[i] & 1) == 1)
                    && (sourceDataBandS1_SM[i] & 4) != 4) {
                    counter_clear_land_spot_s1 += 1;
                }

                //System.out.printf("cloud:  %d %d %d %d \n", i, (sourceDataBandP_Idepix[i] & 8), (sourceDataBandP_Idepix[i] & 4), (sourceDataBandP_Idepix[i] & 2));
                if ((sourceDataBandP_Idepix[i] & 64) == 64 && !((sourceDataBandP_Idepix[i] & 8) == 8
                                                                || (sourceDataBandP_Idepix[i] & 4) == 4)) {
                    counter_snow_spot_p += 1;
                }
                if ((sourceDataBandP_Idepix[i] & 16) == 16 && !((sourceDataBandP_Idepix[i] & 8) == 8
                                                                || (sourceDataBandP_Idepix[i] & 4) == 4)) {
                    counter_clear_land_spot_p += 1;
                }
            } else {
                sourceDataBandS1_B0[i] = Float.NaN;
                sourceDataBandS1_B2[i] = Float.NaN;
                sourceDataBandS1_B3[i] = Float.NaN;

                sourceDataBandP_B0[i] = Float.NaN;
                sourceDataBandP_B2[i] = Float.NaN;
                sourceDataBandP_B3[i] = Float.NaN;
            }


            if (!Float.isNaN(sourceDataBandMeris_B0[i]) ||
                !Float.isNaN(sourceDataBandMeris_B2[i]) ||
                !Float.isNaN(sourceDataBandMeris_B3[i])) {

                if ((sourceDataBandMeris_Idepix[i] & 64) == 64 && !((sourceDataBandMeris_Idepix[i] & 8) == 8
                                                                    || (sourceDataBandMeris_Idepix[i] & 4) == 4)) {
                    counter_snow_meris += 1;
                }
                if ((sourceDataBandMeris_Idepix[i] & 16) == 16 && !((sourceDataBandMeris_Idepix[i] & 8) == 8
                                                                    || (sourceDataBandMeris_Idepix[i] & 4) == 4)) {
                    counter_clear_land_meris += 1;
                }
            }

            if (Float.isNaN(sourceDataBandS1_B0[i]) ||
                Float.isNaN(sourceDataBandS1_B2[i]) ||
                Float.isNaN(sourceDataBandS1_B3[i]) ||
                Float.isNaN(sourceDataBandP_B0[i]) ||
                Float.isNaN(sourceDataBandP_B2[i]) ||
                Float.isNaN(sourceDataBandP_B3[i]) ||
                Float.isNaN(sourceDataBandMeris_B0[i]) ||
                Float.isNaN(sourceDataBandMeris_B2[i]) ||
                Float.isNaN(sourceDataBandMeris_B3[i])) {

                counter_invalid += 1;
                sourceDataBandS1_B0[i] = Float.NaN;
                sourceDataBandS1_B2[i] = Float.NaN;
                sourceDataBandS1_B3[i] = Float.NaN;
                sourceDataBandP_B0[i] = Float.NaN;
                sourceDataBandP_B2[i] = Float.NaN;
                sourceDataBandP_B3[i] = Float.NaN;
                sourceDataBandMeris_B0[i] = Float.NaN;
                sourceDataBandMeris_B2[i] = Float.NaN;
                sourceDataBandMeris_B3[i] = Float.NaN;
            }
        }


        int counterClearLandData = 0;

        int clearLandDataLength = Math.max(counter_clear_land_spot_p, Math.max(counter_clear_land_spot_s1, counter_clear_land_meris));
        clearLandDataSPOTS1MERISRRB0 = new float[2][clearLandDataLength + 1];
        clearLandDataSPOTS1MERISRRB2 = new float[2][clearLandDataLength + 1];
        clearLandDataSPOTS1MERISRRB3 = new float[2][clearLandDataLength + 1];

        clearLandDataSPOTPMERISRRB0 = new float[2][clearLandDataLength + 1];
        clearLandDataSPOTPMERISRRB2 = new float[2][clearLandDataLength + 1];
        clearLandDataSPOTPMERISRRB3 = new float[2][clearLandDataLength + 1];


        if (clearLandDataLength > 0) {
            for (int i = 0; i < sourceLength; i++) {
                if ((sourceDataBandP_Idepix[i] & 16) == 16 && !((sourceDataBandP_Idepix[i] & 8) == 8
                                                                || (sourceDataBandP_Idepix[i] & 4) == 4) && ((sourceDataBandS1_SM[i] & 8) == 8 &&
                                                                                                             !((sourceDataBandS1_SM[i] & 2) == 2 && (sourceDataBandS1_SM[i] & 1) == 1)
                                                                                                             && (sourceDataBandS1_SM[i] & 4) != 4) && !Float.isNaN(
                        sourceDataBandS1_B0[i]) &&
                    (sourceDataBandMeris_Idepix[i] & 16) == 16 && !((sourceDataBandMeris_Idepix[i] & 8) == 8
                                                                    || (sourceDataBandMeris_Idepix[i] & 4) == 4)) {

                    clearLandDataSPOTS1MERISRRB0[0][counterClearLandData] = sourceDataBandS1_B0[i];
                    clearLandDataSPOTS1MERISRRB2[0][counterClearLandData] = sourceDataBandS1_B2[i];
                    clearLandDataSPOTS1MERISRRB3[0][counterClearLandData] = sourceDataBandS1_B3[i];
                    clearLandDataSPOTS1MERISRRB0[1][counterClearLandData] = sourceDataBandMeris_B0[i];
                    clearLandDataSPOTS1MERISRRB2[1][counterClearLandData] = sourceDataBandMeris_B2[i];
                    clearLandDataSPOTS1MERISRRB3[1][counterClearLandData] = sourceDataBandMeris_B3[i];

                    clearLandDataSPOTPMERISRRB0[0][counterClearLandData] = sourceDataBandP_B0[i];
                    clearLandDataSPOTPMERISRRB2[0][counterClearLandData] = sourceDataBandP_B2[i];
                    clearLandDataSPOTPMERISRRB3[0][counterClearLandData] = sourceDataBandP_B3[i];
                    clearLandDataSPOTPMERISRRB0[1][counterClearLandData] = sourceDataBandMeris_B0[i];
                    clearLandDataSPOTPMERISRRB2[1][counterClearLandData] = sourceDataBandMeris_B2[i];
                    clearLandDataSPOTPMERISRRB3[1][counterClearLandData] = sourceDataBandMeris_B3[i];

                    counterClearLandData += 1;

                }
            }
            correlationClearLand[0] = PearsonCorrelation.getPearsonCorrelation(clearLandDataSPOTS1MERISRRB0, counterClearLandData);
            correlationClearLand[1] = PearsonCorrelation.getPearsonCorrelation(clearLandDataSPOTS1MERISRRB2, counterClearLandData);
            correlationClearLand[2] = PearsonCorrelation.getPearsonCorrelation(clearLandDataSPOTS1MERISRRB3, counterClearLandData);
            correlationClearLand[3] = PearsonCorrelation.getPearsonCorrelation(clearLandDataSPOTPMERISRRB0, counterClearLandData);
            correlationClearLand[4] = PearsonCorrelation.getPearsonCorrelation(clearLandDataSPOTPMERISRRB2, counterClearLandData);
            correlationClearLand[5] = PearsonCorrelation.getPearsonCorrelation(clearLandDataSPOTPMERISRRB3, counterClearLandData);


            methodLeastSquaresClearLand[0][3] = counterClearLandData;
            methodLeastSquaresClearLand[1][3] = counterClearLandData;
            methodLeastSquaresClearLand[2][3] = counterClearLandData;
            methodLeastSquaresClearLand[3][3] = counterClearLandData;
            methodLeastSquaresClearLand[4][3] = counterClearLandData;
            methodLeastSquaresClearLand[5][3] = counterClearLandData;


            resultLeastSquares = MethodLeastSquares.getlinearLeastSquares(clearLandDataSPOTS1MERISRRB0, counterClearLandData);
            methodLeastSquaresClearLand[0][0] = resultLeastSquares[0];
            methodLeastSquaresClearLand[0][1] = resultLeastSquares[1];
            methodLeastSquaresClearLand[0][2] = resultLeastSquares[2];
            resultLeastSquares = MethodLeastSquares.getlinearLeastSquares(clearLandDataSPOTS1MERISRRB2, counterClearLandData);
            methodLeastSquaresClearLand[1][0] = resultLeastSquares[0];
            methodLeastSquaresClearLand[1][1] = resultLeastSquares[1];
            methodLeastSquaresClearLand[1][2] = resultLeastSquares[2];
            resultLeastSquares = MethodLeastSquares.getlinearLeastSquares(clearLandDataSPOTS1MERISRRB3, counterClearLandData);
            methodLeastSquaresClearLand[2][0] = resultLeastSquares[0];
            methodLeastSquaresClearLand[2][1] = resultLeastSquares[1];
            methodLeastSquaresClearLand[2][2] = resultLeastSquares[2];
            resultLeastSquares = MethodLeastSquares.getlinearLeastSquares(clearLandDataSPOTPMERISRRB0, counterClearLandData);
            methodLeastSquaresClearLand[3][0] = resultLeastSquares[0];
            methodLeastSquaresClearLand[3][1] = resultLeastSquares[1];
            methodLeastSquaresClearLand[3][2] = resultLeastSquares[2];
            resultLeastSquares = MethodLeastSquares.getlinearLeastSquares(clearLandDataSPOTPMERISRRB2, counterClearLandData);
            methodLeastSquaresClearLand[4][0] = resultLeastSquares[0];
            methodLeastSquaresClearLand[4][1] = resultLeastSquares[1];
            methodLeastSquaresClearLand[4][2] = resultLeastSquares[2];
            resultLeastSquares = MethodLeastSquares.getlinearLeastSquares(clearLandDataSPOTPMERISRRB3, counterClearLandData);
            methodLeastSquaresClearLand[5][0] = resultLeastSquares[0];
            methodLeastSquaresClearLand[5][1] = resultLeastSquares[1];
            methodLeastSquaresClearLand[5][2] = resultLeastSquares[2];

        } else {
            for (int i = 0; i < 6; i++) {
                correlationClearLand[i] = Float.NaN;
                methodLeastSquaresClearLand[i][0] = Float.NaN;
                methodLeastSquaresClearLand[i][1] = Float.NaN;
                methodLeastSquaresClearLand[i][2] = Float.NaN;
                methodLeastSquaresClearLand[i][3] = Float.NaN;
            }
            for (int i = 0; i < clearLandDataLength + 1; i++) {
                for (int j = 0; j < 6; j++) {
                    clearLandDataSPOTS1MERISRRB0[j][i] = Float.NaN;
                    clearLandDataSPOTS1MERISRRB2[j][i] = Float.NaN;
                    clearLandDataSPOTS1MERISRRB3[j][i] = Float.NaN;
                    clearLandDataSPOTPMERISRRB0[j][i] = Float.NaN;
                    clearLandDataSPOTPMERISRRB2[j][i] = Float.NaN;
                    clearLandDataSPOTPMERISRRB3[j][i] = Float.NaN;
                }
            }
        }


        int counterClearSnowData = 0;
        int clearSnowDataLength = Math.max(counter_snow_spot_p, Math.max(counter_snow_spot_s1, counter_snow_meris));
        clearSnowDataSPOTS1MERISRRB0 = new float[2][clearSnowDataLength + 1];
        clearSnowDataSPOTS1MERISRRB2 = new float[2][clearSnowDataLength + 1];
        clearSnowDataSPOTS1MERISRRB3 = new float[2][clearSnowDataLength + 1];

        clearSnowDataSPOTPMERISRRB0 = new float[2][clearSnowDataLength + 1];
        clearSnowDataSPOTPMERISRRB2 = new float[2][clearSnowDataLength + 1];
        clearSnowDataSPOTPMERISRRB3 = new float[2][clearSnowDataLength + 1];

        if (clearSnowDataLength > 0) {
            for (int i = 0; i < sourceLength; i++) {
                if (((sourceDataBandS1_SM[i] & 4) == 4 &&
                     !((sourceDataBandS1_SM[i] & 2) == 2 && (sourceDataBandS1_SM[i] & 1) == 1)) &&
                    ((sourceDataBandP_Idepix[i] & 64) == 64 && !((sourceDataBandP_Idepix[i] & 8) == 8
                                                                 || (sourceDataBandP_Idepix[i] & 4) == 4)) && !Float.isNaN(sourceDataBandS1_B0[i]) &&
                    ((sourceDataBandMeris_Idepix[i] & 64) == 64 && !((sourceDataBandMeris_Idepix[i] & 8) == 8
                                                                     || (sourceDataBandMeris_Idepix[i] & 4) == 4))) {

                    clearSnowDataSPOTS1MERISRRB0[0][counterClearSnowData] = sourceDataBandS1_B0[i];
                    clearSnowDataSPOTS1MERISRRB2[0][counterClearSnowData] = sourceDataBandS1_B2[i];
                    clearSnowDataSPOTS1MERISRRB3[0][counterClearSnowData] = sourceDataBandS1_B3[i];
                    clearSnowDataSPOTS1MERISRRB0[1][counterClearSnowData] = sourceDataBandMeris_B0[i];
                    clearSnowDataSPOTS1MERISRRB2[1][counterClearSnowData] = sourceDataBandMeris_B2[i];
                    clearSnowDataSPOTS1MERISRRB3[1][counterClearSnowData] = sourceDataBandMeris_B3[i];

                    clearSnowDataSPOTPMERISRRB0[0][counterClearSnowData] = sourceDataBandP_B0[i];
                    clearSnowDataSPOTPMERISRRB2[0][counterClearSnowData] = sourceDataBandP_B2[i];
                    clearSnowDataSPOTPMERISRRB3[0][counterClearSnowData] = sourceDataBandP_B3[i];
                    clearSnowDataSPOTPMERISRRB0[1][counterClearSnowData] = sourceDataBandMeris_B0[i];
                    clearSnowDataSPOTPMERISRRB2[1][counterClearSnowData] = sourceDataBandMeris_B2[i];
                    clearSnowDataSPOTPMERISRRB3[1][counterClearSnowData] = sourceDataBandMeris_B3[i];

                    counterClearSnowData += 1;

                }
            }
            correlationClearSnow[0] = PearsonCorrelation.getPearsonCorrelation(clearSnowDataSPOTS1MERISRRB0, counterClearSnowData);
            correlationClearSnow[1] = PearsonCorrelation.getPearsonCorrelation(clearSnowDataSPOTS1MERISRRB2, counterClearSnowData);
            correlationClearSnow[2] = PearsonCorrelation.getPearsonCorrelation(clearSnowDataSPOTS1MERISRRB3, counterClearSnowData);
            correlationClearSnow[3] = PearsonCorrelation.getPearsonCorrelation(clearSnowDataSPOTPMERISRRB0, counterClearSnowData);
            correlationClearSnow[4] = PearsonCorrelation.getPearsonCorrelation(clearSnowDataSPOTPMERISRRB2, counterClearSnowData);
            correlationClearSnow[5] = PearsonCorrelation.getPearsonCorrelation(clearSnowDataSPOTPMERISRRB3, counterClearSnowData);


            methodLeastSquaresClearSnow[0][3] = counterClearSnowData;
            methodLeastSquaresClearSnow[1][3] = counterClearSnowData;
            methodLeastSquaresClearSnow[2][3] = counterClearSnowData;
            methodLeastSquaresClearSnow[3][3] = counterClearSnowData;
            methodLeastSquaresClearSnow[4][3] = counterClearSnowData;
            methodLeastSquaresClearSnow[5][3] = counterClearSnowData;


            resultLeastSquares = MethodLeastSquares.getlinearLeastSquares(clearSnowDataSPOTS1MERISRRB0, counterClearSnowData);
            methodLeastSquaresClearSnow[0][0] = resultLeastSquares[0];
            methodLeastSquaresClearSnow[0][1] = resultLeastSquares[1];
            methodLeastSquaresClearSnow[0][2] = resultLeastSquares[2];
            resultLeastSquares = MethodLeastSquares.getlinearLeastSquares(clearSnowDataSPOTS1MERISRRB2, counterClearSnowData);
            methodLeastSquaresClearSnow[1][0] = resultLeastSquares[0];
            methodLeastSquaresClearSnow[1][1] = resultLeastSquares[1];
            methodLeastSquaresClearSnow[1][2] = resultLeastSquares[2];
            resultLeastSquares = MethodLeastSquares.getlinearLeastSquares(clearSnowDataSPOTS1MERISRRB3, counterClearSnowData);
            methodLeastSquaresClearSnow[2][0] = resultLeastSquares[0];
            methodLeastSquaresClearSnow[2][1] = resultLeastSquares[1];
            methodLeastSquaresClearSnow[2][2] = resultLeastSquares[2];
            resultLeastSquares = MethodLeastSquares.getlinearLeastSquares(clearSnowDataSPOTPMERISRRB0, counterClearSnowData);
            methodLeastSquaresClearSnow[3][0] = resultLeastSquares[0];
            methodLeastSquaresClearSnow[3][1] = resultLeastSquares[1];
            methodLeastSquaresClearSnow[3][2] = resultLeastSquares[2];
            resultLeastSquares = MethodLeastSquares.getlinearLeastSquares(clearSnowDataSPOTPMERISRRB2, counterClearSnowData);
            methodLeastSquaresClearSnow[4][0] = resultLeastSquares[0];
            methodLeastSquaresClearSnow[4][1] = resultLeastSquares[1];
            methodLeastSquaresClearSnow[4][2] = resultLeastSquares[2];
            resultLeastSquares = MethodLeastSquares.getlinearLeastSquares(clearSnowDataSPOTPMERISRRB3, counterClearSnowData);
            methodLeastSquaresClearSnow[5][0] = resultLeastSquares[0];
            methodLeastSquaresClearSnow[5][1] = resultLeastSquares[1];
            methodLeastSquaresClearSnow[5][2] = resultLeastSquares[2];

        } else {
            for (int i = 0; i < 6; i++) {
                correlationClearSnow[i] = Float.NaN;
                methodLeastSquaresClearSnow[i][0] = Float.NaN;
                methodLeastSquaresClearSnow[i][1] = Float.NaN;
                methodLeastSquaresClearSnow[i][2] = Float.NaN;
                methodLeastSquaresClearSnow[i][3] = Float.NaN;
            }
            for (int i = 0; i < clearSnowDataLength; i++) {
                for (int j = 0; j < 6; j++) {
                    clearSnowDataSPOTS1MERISRRB0[j][i] = Float.NaN;
                    clearSnowDataSPOTS1MERISRRB2[j][i] = Float.NaN;
                    clearSnowDataSPOTS1MERISRRB3[j][i] = Float.NaN;
                    clearSnowDataSPOTPMERISRRB0[j][i] = Float.NaN;
                    clearSnowDataSPOTPMERISRRB2[j][i] = Float.NaN;
                    clearSnowDataSPOTPMERISRRB3[j][i] = Float.NaN;
                }
            }
        }

        System.out.printf("counter:  %d %d \n", counterClearSnowData, counterClearLandData);


// writing log files for batch processing
        WriteFiles logfiles = new WriteFiles();
        logfiles.writingFiles(productName, sourceWidth, sourceHeight,
                              counter_invalid,
                              counter_snow_spot_s1,
                              counter_clear_land_spot_s1,
                              counter_snow_spot_p,
                              counter_clear_land_spot_p,
                              counter_snow_meris,
                              counter_clear_land_meris,
                              correlationClearLand,
                              correlationClearSnow,
                              methodLeastSquaresClearLand,
                              methodLeastSquaresClearSnow,
                              counterClearLandData,
                              counterClearSnowData,
                              clearLandDataSPOTS1MERISRRB0,
                              clearLandDataSPOTS1MERISRRB2,
                              clearLandDataSPOTS1MERISRRB3,
                              clearLandDataSPOTPMERISRRB0,
                              clearLandDataSPOTPMERISRRB2,
                              clearLandDataSPOTPMERISRRB3);


// Scatterplot

        String surfaceType;

        File outputDirectory = new File("E:/CCI_LC_Daten/SPOT_VGT_P_and_VGT_S1/");
        if (clearLandDataLength > 0) {
            surfaceType = "clear land";
            //clear Land B0 MERIS/SPOT VGT S1
            ScatterPlotExporter exporterB0 = new ScatterPlotExporter(450, 450);
            String scatterplotTitle = "Comparison of reflectance values of MERIS RR and SPOT_VGT_S1";
            String xAxisLabel = "Reflectance (Band 2 MERIS_RR )";
            String yAxisLabel = "Reflectance (Band B0 SPOT_VGT_S1)";
            String fileName = productName + "_clearLand_MERIS_SPOT_VGT_S1_B0.png";


            float[][] clearLandDataB0NoNaNs = prepareDataForChart(clearLandDataSPOTS1MERISRRB0, counterClearLandData);
            float[] Line = methodLeastSquaresClearLand[0];
            try {
                exporterB0.export(clearLandDataB0NoNaNs, Line, scatterplotTitle, xAxisLabel, yAxisLabel, new File(outputDirectory, fileName),
                                  surfaceType);
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
            }


            //clear Land B2 MERIS/SPOT VGT S1
            ScatterPlotExporter exporterB2 = new ScatterPlotExporter(450, 450);
            xAxisLabel = "Reflectance (Band 6/7 MERIS_RR )";
            yAxisLabel = "Reflectance (Band B2 SPOT_VGT_S1)";
            fileName = productName + "_clearLand_MERIS_SPOT_VGT_S1_B2.png";

            float[][] clearLandDataB2NoNaNs = prepareDataForChart(clearLandDataSPOTS1MERISRRB2, counterClearLandData);
            Line = methodLeastSquaresClearLand[1];
            try {
                exporterB2.export(clearLandDataB2NoNaNs, Line, scatterplotTitle, xAxisLabel, yAxisLabel, new File(outputDirectory, fileName),
                                  surfaceType);
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
            }


            //clear Land B3   MERIS/SPOT VGT S1
            ScatterPlotExporter exporterB3 = new ScatterPlotExporter(450, 450);
            xAxisLabel = "Reflectance (Band 12/13 MERIS_RR )";
            yAxisLabel = "Reflectance (Band B3 SPOT_VGT_S1)";
            fileName = productName + "_clearLand_MERIS_SPOT_VGT_S1_B3.png";

            float[][] clearLandDataB3NoNaNs = prepareDataForChart(clearLandDataSPOTS1MERISRRB3, counterClearLandData);
            Line = methodLeastSquaresClearLand[2];
            try {
                exporterB3.export(clearLandDataB3NoNaNs, Line, scatterplotTitle, xAxisLabel, yAxisLabel, new File(outputDirectory, fileName),
                                  surfaceType);
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
            }


            //clear Land B0 MERIS/SPOT VGT P
            ScatterPlotExporter exporterB4 = new ScatterPlotExporter(450, 450);
            scatterplotTitle = "Comparison of reflectance values of MERIS RR and SPOT_VGT_P";
            xAxisLabel = "Reflectance (Band 2 MERIS_RR )";
            yAxisLabel = "Reflectance (Band B0 SPOT_VGT_P)";
            fileName = productName + "_clearLand_MERIS_SPOT_VGT_P_B0.png";


            float[][] clearLandDataB4NoNaNs = prepareDataForChart(clearLandDataSPOTPMERISRRB0, counterClearLandData);
            Line = methodLeastSquaresClearLand[3];
            try {
                exporterB4.export(clearLandDataB4NoNaNs, Line, scatterplotTitle, xAxisLabel, yAxisLabel, new File(outputDirectory, fileName),
                                  surfaceType);
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
            }


            //clear Land B2 MERIS/SPOT VGT P
            ScatterPlotExporter exporterB5 = new ScatterPlotExporter(450, 450);
            xAxisLabel = "Reflectance (Band 6/7 MERIS_RR )";
            yAxisLabel = "Reflectance (Band B2 SPOT_VGT_P)";
            fileName = productName + "_clearLand_MERIS_SPOT_VGT_P_B2.png";

            float[][] clearLandDataB5NoNaNs = prepareDataForChart(clearLandDataSPOTPMERISRRB2, counterClearLandData);
            Line = methodLeastSquaresClearLand[4];
            try {
                exporterB5.export(clearLandDataB5NoNaNs, Line, scatterplotTitle, xAxisLabel, yAxisLabel, new File(outputDirectory, fileName),
                                  surfaceType);
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
            }


            //clear Land B3   MERIS/SPOT VGT P
            ScatterPlotExporter exporterB6 = new ScatterPlotExporter(450, 450);
            xAxisLabel = "Reflectance (Band 12/13 MERIS_RR )";
            yAxisLabel = "Reflectance (Band B3 SPOT_VGT_P)";
            fileName = productName + "_clearLand_MERIS_SPOT_VGT_P_B3.png";

            float[][] clearLandDataB6NoNaNs = prepareDataForChart(clearLandDataSPOTPMERISRRB3, counterClearLandData);
            Line = methodLeastSquaresClearLand[5];
            try {
                exporterB6.export(clearLandDataB6NoNaNs, Line, scatterplotTitle, xAxisLabel, yAxisLabel, new File(outputDirectory, fileName),
                                  surfaceType);
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
            }


        }

        if (clearSnowDataLength > 0) {

            surfaceType = "clear smow";

            //clear Land B0 MERIS/SPOT VGT S1
            ScatterPlotExporter exporterB0 = new ScatterPlotExporter(450, 450);
            String scatterplotTitle = "Comparison of reflectance values of MERIS RR and SPOT_VGT_S1";
            String xAxisLabel = "Reflectance (Band 2 MERIS_RR )";
            String yAxisLabel = "Reflectance (Band B0 SPOT_VGT_S1)";
            String fileName = productName + "_clearSnow_MERIS_SPOT_VGT_S1_B0.png";


            float[][] clearSnowDataB0NoNaNs = prepareDataForChart(clearSnowDataSPOTS1MERISRRB0, counterClearSnowData);
            float[] Line = methodLeastSquaresClearSnow[0];
            try {
                exporterB0.export(clearSnowDataB0NoNaNs, Line, scatterplotTitle, xAxisLabel, yAxisLabel, new File(outputDirectory, fileName),
                                  surfaceType);
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
            }


            //clear Land B2 MERIS/SPOT VGT S1
            ScatterPlotExporter exporterB2 = new ScatterPlotExporter(450, 450);
            xAxisLabel = "Reflectance (Band 6/7 MERIS_RR )";
            yAxisLabel = "Reflectance (Band B2 SPOT_VGT_S1)";
            fileName = productName + "_clearSnow_MERIS_SPOT_VGT_S1_B2.png";

            float[][] clearSnowDataB2NoNaNs = prepareDataForChart(clearSnowDataSPOTS1MERISRRB2, counterClearSnowData);
            Line = methodLeastSquaresClearSnow[1];
            try {
                exporterB2.export(clearSnowDataB2NoNaNs, Line, scatterplotTitle, xAxisLabel, yAxisLabel, new File(outputDirectory, fileName),
                                  surfaceType);
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
            }


            //clear Land B3   MERIS/SPOT VGT S1
            ScatterPlotExporter exporterB3 = new ScatterPlotExporter(450, 450);
            xAxisLabel = "Reflectance (Band 12/13 MERIS_RR )";
            yAxisLabel = "Reflectance (Band B3 SPOT_VGT_S1)";
            fileName = productName + "_clearSnow_MERIS_SPOT_VGT_S1_B3.png";

            float[][] clearSnowDataB3NoNaNs = prepareDataForChart(clearSnowDataSPOTS1MERISRRB3, counterClearSnowData);
            Line = methodLeastSquaresClearSnow[2];
            try {
                exporterB3.export(clearSnowDataB3NoNaNs, Line, scatterplotTitle, xAxisLabel, yAxisLabel, new File(outputDirectory, fileName),
                                  surfaceType);
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
            }


            //clear Land B0 MERIS/SPOT VGT P
            ScatterPlotExporter exporterB4 = new ScatterPlotExporter(450, 450);
            scatterplotTitle = "Comparison of reflectance values of MERIS RR and SPOT_VGT_P";
            xAxisLabel = "Reflectance (Band 2 MERIS_RR )";
            yAxisLabel = "Reflectance (Band B0 SPOT_VGT_P)";
            fileName = productName + "_clearSnow_MERIS_SPOT_VGT_P_B0.png";


            float[][] clearSnowDataB4NoNaNs = prepareDataForChart(clearSnowDataSPOTPMERISRRB0, counterClearSnowData);
            Line = methodLeastSquaresClearSnow[3];
            try {
                exporterB4.export(clearSnowDataB4NoNaNs, Line, scatterplotTitle, xAxisLabel, yAxisLabel, new File(outputDirectory, fileName),
                                  surfaceType);
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
            }


            //clear Land B2 MERIS/SPOT VGT P
            ScatterPlotExporter exporterB5 = new ScatterPlotExporter(450, 450);
            xAxisLabel = "Reflectance (Band 6/7 MERIS_RR )";
            yAxisLabel = "Reflectance (Band B2 SPOT_VGT_P)";
            fileName = productName + "_clearSnow_MERIS_SPOT_VGT_P_B2.png";

            float[][] clearSnowDataB5NoNaNs = prepareDataForChart(clearSnowDataSPOTPMERISRRB2, counterClearSnowData);
            Line = methodLeastSquaresClearSnow[4];
            try {
                exporterB5.export(clearSnowDataB5NoNaNs, Line, scatterplotTitle, xAxisLabel, yAxisLabel, new File(outputDirectory, fileName),
                                  surfaceType);
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
            }


            //clear Land B3   MERIS/SPOT VGT P
            ScatterPlotExporter exporterB6 = new ScatterPlotExporter(450, 450);
            xAxisLabel = "Reflectance (Band 12/13 MERIS_RR )";
            yAxisLabel = "Reflectance (Band B3 SPOT_VGT_P)";
            fileName = productName + "_clearSnow_MERIS_SPOT_VGT_P_B3.png";

            float[][] clearSnowDataB6NoNaNs = prepareDataForChart(clearSnowDataSPOTPMERISRRB3, counterClearSnowData);
            Line = methodLeastSquaresClearSnow[5];
            try {
                exporterB6.export(clearSnowDataB6NoNaNs, Line, scatterplotTitle, xAxisLabel, yAxisLabel, new File(outputDirectory, fileName),
                                  surfaceType);
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
            }
        }
    }

    private float[][] prepareDataForChart(float[][] data, int maxValidSecondIndex) {
        float[][] preparedData = new float[data.length][maxValidSecondIndex];
        for (int i = 0; i < preparedData.length; i++) {
            preparedData[i] = Arrays.copyOfRange(data[i], 0, maxValidSecondIndex);
        }

        return preparedData;
    }
}




