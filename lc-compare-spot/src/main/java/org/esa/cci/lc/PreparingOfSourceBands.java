package org.esa.cci.lc;

import java.io.File;
import java.util.Arrays;

public class PreparingOfSourceBands {

    public void preparedOfSourceBands(float[] sourceDataBandS1_B0,
                                      float[] sourceDataBandS1_B2,
                                      float[] sourceDataBandS1_B3,
                                      float[] sourceDataBandS1_MIR,
                                      int[] sourceDataBandS1_SM,
                                      float[] sourceDataBandP_B0,
                                      float[] sourceDataBandP_B2,
                                      float[] sourceDataBandP_B3,
                                      float[] sourceDataBandP_MIR,
                                      int[] sourceDataBandP_SM,
                                      int[] sourceDataBandP_Idepix,
                                      int sourceLength,
                                      int sourceWidth,
                                      int sourceHeight,
                                      String productName) {


        int counter_cloud_spot_s1 = 0;
        int counter_snow_spot_s1 = 0;
        int counter_water_spot_s1 = 0;
        int counter_clear_land_spot_s1 = 0;
        int counter_cloud_spot_p = 0;
        int counter_snow_spot_p = 0;
        int counter_water_spot_p = 0;
        int counter_clear_land_spot_p = 0;
        int counter_cloud_spot_p_AND_S1 = 0;
        int counter_cloud_spot_p_OR_S1 = 0;
        int counter_invalid = 0;

        float[][] clearLandDataB0;
        float[][] clearLandDataB2;
        float[][] clearLandDataB3;
        float[][] clearLandDataMIR;
        float[][] clearSnowDataB0;
        float[][] clearSnowDataB2;
        float[][] clearSnowDataB3;
        float[][] clearSnowDataMIR;

        float[] correlationClearSnow = new float[4];
        float[] correlationClearLand = new float[4];

        float[][] methodLeastSquaresClearSnow = new float[4][4];
        float[][] methodLeastSquaresClearLand = new float[4][4];

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

                if ((sourceDataBandS1_SM[i] & 2) == 2 && (sourceDataBandS1_SM[i] & 1) == 1) {
                    counter_cloud_spot_s1 += 1;
                }

                if ((sourceDataBandS1_SM[i] & 4) == 4 &&
                    !((sourceDataBandS1_SM[i] & 2) == 2 && (sourceDataBandS1_SM[i] & 1) == 1)) {
                    counter_snow_spot_s1 += 1;
                }

                if ((sourceDataBandS1_SM[i] & 8) != 8 &&
                    !((sourceDataBandS1_SM[i] & 2) == 2 && (sourceDataBandS1_SM[i] & 1) == 1)) {
                    counter_water_spot_s1 += 1;
                }

                if ((sourceDataBandS1_SM[i] & 8) == 8 &&
                    !((sourceDataBandS1_SM[i] & 2) == 2 && (sourceDataBandS1_SM[i] & 1) == 1)
                    && (sourceDataBandS1_SM[i] & 4) != 4) {
                    counter_clear_land_spot_s1 += 1;
                }

                //System.out.printf("cloud:  %d %d %d %d \n", i, (sourceDataBandP_Idepix[i] & 8), (sourceDataBandP_Idepix[i] & 4), (sourceDataBandP_Idepix[i] & 2));
                if ((sourceDataBandP_Idepix[i] & 8) == 8 || (sourceDataBandP_Idepix[i] & 4) == 4
                    || (sourceDataBandP_Idepix[i] & 2) == 2) {
                    counter_cloud_spot_p += 1;
                }

                if ((sourceDataBandP_Idepix[i] & 64) == 64 && !((sourceDataBandP_Idepix[i] & 8) == 8
                                                                || (sourceDataBandP_Idepix[i] & 4) == 4)) {
                    counter_snow_spot_p += 1;
                }

                if ((sourceDataBandP_Idepix[i] & 32) == 32 && !((sourceDataBandP_Idepix[i] & 8) == 8
                                                                || (sourceDataBandP_Idepix[i] & 4) == 4)) {
                    counter_water_spot_p += 1;
                }

                if ((sourceDataBandP_Idepix[i] & 16) == 16 && !((sourceDataBandP_Idepix[i] & 8) == 8
                                                                || (sourceDataBandP_Idepix[i] & 4) == 4)) {
                    counter_clear_land_spot_p += 1;
                }

                if (((sourceDataBandP_Idepix[i] & 8) == 8 || (sourceDataBandP_Idepix[i] & 4) == 4
                     || (sourceDataBandP_Idepix[i] & 2) == 2) &&
                    ((sourceDataBandS1_SM[i] & 2) == 2 && (sourceDataBandS1_SM[i] & 1) == 1)) {
                    counter_cloud_spot_p_AND_S1 += 1;
                }

                if (((sourceDataBandP_Idepix[i] & 8) == 8 || (sourceDataBandP_Idepix[i] & 4) == 4
                     || (sourceDataBandP_Idepix[i] & 2) == 2) ||
                    ((sourceDataBandS1_SM[i] & 2) == 2 && (sourceDataBandS1_SM[i] & 1) == 1)) {
                    counter_cloud_spot_p_OR_S1 += 1;
                }


            } else {
                counter_invalid += 1;
                sourceDataBandS1_B0[i] = Float.NaN;
                sourceDataBandS1_B2[i] = Float.NaN;
                sourceDataBandS1_B3[i] = Float.NaN;
                sourceDataBandS1_MIR[i] = Float.NaN;
                sourceDataBandP_B0[i] = Float.NaN;
                sourceDataBandP_B2[i] = Float.NaN;
                sourceDataBandP_B3[i] = Float.NaN;
                sourceDataBandP_MIR[i] = Float.NaN;
            }
        }

        for (int i = 0; i < sourceLength; i++) {
            if (Float.isNaN(sourceDataBandS1_B0[i]) ||
                Float.isNaN(sourceDataBandS1_B2[i]) ||
                Float.isNaN(sourceDataBandS1_B3[i]) ||
                Float.isNaN(sourceDataBandS1_MIR[i]) ||
                Float.isNaN(sourceDataBandP_B0[i]) ||
                Float.isNaN(sourceDataBandP_B2[i]) ||
                Float.isNaN(sourceDataBandP_B3[i]) ||
                Float.isNaN(sourceDataBandP_MIR[i])) {

                sourceDataBandS1_B0[i] = Float.NaN;
                sourceDataBandS1_B2[i] = Float.NaN;
                sourceDataBandS1_B3[i] = Float.NaN;
                sourceDataBandS1_MIR[i] = Float.NaN;
                sourceDataBandP_B0[i] = Float.NaN;
                sourceDataBandP_B2[i] = Float.NaN;
                sourceDataBandP_B3[i] = Float.NaN;
                sourceDataBandP_MIR[i] = Float.NaN;
            }
        }


        int counterClearLandData = 0;

        int clearLandDataLength = Math.max(counter_clear_land_spot_p, counter_clear_land_spot_s1);
        clearLandDataB0 = new float[2][clearLandDataLength + 1];
        clearLandDataB2 = new float[2][clearLandDataLength + 1];
        clearLandDataB3 = new float[2][clearLandDataLength + 1];
        clearLandDataMIR = new float[2][clearLandDataLength + 1];


        if (clearLandDataLength > 0) {
            for (int i = 0; i < sourceLength; i++) {
                if ((sourceDataBandP_Idepix[i] & 16) == 16 && !((sourceDataBandP_Idepix[i] & 8) == 8
                                                                || (sourceDataBandP_Idepix[i] & 4) == 4) && ((sourceDataBandS1_SM[i] & 8) == 8 &&
                                                                                                             !((sourceDataBandS1_SM[i] & 2) == 2 && (sourceDataBandS1_SM[i] & 1) == 1)
                                                                                                             && (sourceDataBandS1_SM[i] & 4) != 4) && !Float.isNaN(
                        sourceDataBandS1_B0[i])) {

                    clearLandDataB0[0][counterClearLandData] = sourceDataBandS1_B0[i];
                    clearLandDataB2[0][counterClearLandData] = sourceDataBandS1_B2[i];
                    clearLandDataB3[0][counterClearLandData] = sourceDataBandS1_B3[i];
                    clearLandDataMIR[0][counterClearLandData] = sourceDataBandS1_MIR[i];

                    clearLandDataB0[1][counterClearLandData] = sourceDataBandP_B0[i];
                    clearLandDataB2[1][counterClearLandData] = sourceDataBandP_B2[i];
                    clearLandDataB3[1][counterClearLandData] = sourceDataBandP_B3[i];
                    clearLandDataMIR[1][counterClearLandData] = sourceDataBandP_MIR[i];

                    counterClearLandData += 1;

                }
            }
            correlationClearLand[0] = PearsonCorrelation.getPearsonCorrelation(clearLandDataB0, counterClearLandData);
            correlationClearLand[1] = PearsonCorrelation.getPearsonCorrelation(clearLandDataB2, counterClearLandData);
            correlationClearLand[2] = PearsonCorrelation.getPearsonCorrelation(clearLandDataB3, counterClearLandData);
            correlationClearLand[3] = PearsonCorrelation.getPearsonCorrelation(clearLandDataMIR, counterClearLandData);

            methodLeastSquaresClearLand[0][3] = counterClearLandData;
            methodLeastSquaresClearLand[1][3] = counterClearLandData;
            methodLeastSquaresClearLand[2][3] = counterClearLandData;
            methodLeastSquaresClearLand[3][3] = counterClearLandData;

            resultLeastSquares = MethodLeastSquares.getlinearLeastSquares(clearLandDataB0, counterClearLandData);
            methodLeastSquaresClearLand[0][0] = resultLeastSquares[0];
            methodLeastSquaresClearLand[0][1] = resultLeastSquares[1];
            methodLeastSquaresClearLand[0][2] = resultLeastSquares[2];
            resultLeastSquares = MethodLeastSquares.getlinearLeastSquares(clearLandDataB2, counterClearLandData);
            methodLeastSquaresClearLand[1][0] = resultLeastSquares[0];
            methodLeastSquaresClearLand[1][1] = resultLeastSquares[1];
            methodLeastSquaresClearLand[1][2] = resultLeastSquares[2];
            resultLeastSquares = MethodLeastSquares.getlinearLeastSquares(clearLandDataB3, counterClearLandData);
            methodLeastSquaresClearLand[2][0] = resultLeastSquares[0];
            methodLeastSquaresClearLand[2][1] = resultLeastSquares[1];
            methodLeastSquaresClearLand[2][2] = resultLeastSquares[2];
            resultLeastSquares = MethodLeastSquares.getlinearLeastSquares(clearLandDataMIR, counterClearLandData);
            methodLeastSquaresClearLand[3][0] = resultLeastSquares[0];
            methodLeastSquaresClearLand[3][1] = resultLeastSquares[1];
            methodLeastSquaresClearLand[3][2] = resultLeastSquares[2];

        } else {
            for (int i = 0; i < 4; i++) {
                correlationClearLand[i] = Float.NaN;
                methodLeastSquaresClearLand[i][0] = Float.NaN;
                methodLeastSquaresClearLand[i][1] = Float.NaN;
                methodLeastSquaresClearLand[i][2] = Float.NaN;
                methodLeastSquaresClearLand[i][3] = Float.NaN;
            }
            for (int i = 0; i < clearLandDataLength + 1; i++) {
                for (int j = 0; j < 2; j++) {
                    clearLandDataB0[j][i] = Float.NaN;
                    clearLandDataB2[j][i] = Float.NaN;
                    clearLandDataB3[j][i] = Float.NaN;
                    clearLandDataMIR[j][i] = Float.NaN;
                }
            }
        }


        int counterClearSnowData = 0;
        int clearSnowDataLength = Math.max(counter_snow_spot_p, counter_snow_spot_s1);
        clearSnowDataB0 = new float[2][clearSnowDataLength + 1];
        clearSnowDataB2 = new float[2][clearSnowDataLength + 1];
        clearSnowDataB3 = new float[2][clearSnowDataLength + 1];
        clearSnowDataMIR = new float[2][clearSnowDataLength + 1];

        if (clearSnowDataLength > 0) {
            for (int i = 0; i < sourceLength; i++) {
                if (((sourceDataBandS1_SM[i] & 4) == 4 &&
                     !((sourceDataBandS1_SM[i] & 2) == 2 && (sourceDataBandS1_SM[i] & 1) == 1)) &&
                    ((sourceDataBandP_Idepix[i] & 64) == 64 && !((sourceDataBandP_Idepix[i] & 8) == 8
                                                                 || (sourceDataBandP_Idepix[i] & 4) == 4)) && !Float.isNaN(sourceDataBandS1_B0[i])) {

                    clearSnowDataB0[0][counterClearSnowData] = sourceDataBandS1_B0[i];
                    clearSnowDataB2[0][counterClearSnowData] = sourceDataBandS1_B2[i];
                    clearSnowDataB3[0][counterClearSnowData] = sourceDataBandS1_B3[i];
                    clearSnowDataMIR[0][counterClearSnowData] = sourceDataBandS1_MIR[i];

                    clearSnowDataB0[1][counterClearSnowData] = sourceDataBandP_B0[i];
                    clearSnowDataB2[1][counterClearSnowData] = sourceDataBandP_B2[i];
                    clearSnowDataB3[1][counterClearSnowData] = sourceDataBandP_B3[i];
                    clearSnowDataMIR[1][counterClearSnowData] = sourceDataBandP_MIR[i];

                    counterClearSnowData += 1;
                }
            }
            correlationClearSnow[0] = PearsonCorrelation.getPearsonCorrelation(clearSnowDataB0, counterClearSnowData);
            correlationClearSnow[1] = PearsonCorrelation.getPearsonCorrelation(clearSnowDataB2, counterClearSnowData);
            correlationClearSnow[2] = PearsonCorrelation.getPearsonCorrelation(clearSnowDataB3, counterClearSnowData);
            correlationClearSnow[3] = PearsonCorrelation.getPearsonCorrelation(clearSnowDataMIR, counterClearSnowData);

            methodLeastSquaresClearSnow[0][3] = counterClearSnowData;
            methodLeastSquaresClearSnow[1][3] = counterClearSnowData;
            methodLeastSquaresClearSnow[2][3] = counterClearSnowData;
            methodLeastSquaresClearSnow[3][3] = counterClearSnowData;

            resultLeastSquares = MethodLeastSquares.getlinearLeastSquares(clearSnowDataB0, counterClearSnowData);
            methodLeastSquaresClearSnow[0][0] = resultLeastSquares[0];
            methodLeastSquaresClearSnow[0][1] = resultLeastSquares[1];
            methodLeastSquaresClearSnow[0][2] = resultLeastSquares[2];
            resultLeastSquares = MethodLeastSquares.getlinearLeastSquares(clearSnowDataB2, counterClearSnowData);
            methodLeastSquaresClearSnow[1][0] = resultLeastSquares[0];
            methodLeastSquaresClearSnow[1][1] = resultLeastSquares[1];
            methodLeastSquaresClearSnow[1][2] = resultLeastSquares[2];
            resultLeastSquares = MethodLeastSquares.getlinearLeastSquares(clearSnowDataB3, counterClearSnowData);
            methodLeastSquaresClearSnow[2][0] = resultLeastSquares[0];
            methodLeastSquaresClearSnow[2][1] = resultLeastSquares[1];
            methodLeastSquaresClearSnow[2][2] = resultLeastSquares[2];
            resultLeastSquares = MethodLeastSquares.getlinearLeastSquares(clearSnowDataMIR, counterClearSnowData);
            methodLeastSquaresClearSnow[3][0] = resultLeastSquares[0];
            methodLeastSquaresClearSnow[3][1] = resultLeastSquares[1];
            methodLeastSquaresClearSnow[3][2] = resultLeastSquares[2];

        } else {
            for (int i = 0; i < 4; i++) {
                correlationClearSnow[i] = Float.NaN;
                methodLeastSquaresClearSnow[i][0] = Float.NaN;
                methodLeastSquaresClearSnow[i][1] = Float.NaN;
                methodLeastSquaresClearSnow[i][2] = Float.NaN;
                methodLeastSquaresClearSnow[i][3] = Float.NaN;
            }
            for (int i = 0; i < clearSnowDataLength + 1; i++) {
                for (int j = 0; j < 2; j++) {
                    clearSnowDataB0[j][i] = Float.NaN;
                    clearSnowDataB2[j][i] = Float.NaN;
                    clearSnowDataB3[j][i] = Float.NaN;
                    clearSnowDataMIR[j][i] = Float.NaN;
                }
            }
        }

        System.out.printf("counter:  %d %d \n", counterClearSnowData, counterClearLandData);


// writing log files for batch processing
        WriteFiles logfiles = new WriteFiles();
        logfiles.writingFiles(productName, sourceWidth, sourceHeight,
                              counter_invalid,
                              counter_cloud_spot_s1,
                              counter_snow_spot_s1,
                              counter_water_spot_s1,
                              counter_clear_land_spot_s1,
                              counter_cloud_spot_p,
                              counter_snow_spot_p,
                              counter_water_spot_p,
                              counter_clear_land_spot_p,
                              counter_cloud_spot_p_AND_S1,
                              counter_cloud_spot_p_OR_S1,
                              correlationClearLand,
                              correlationClearSnow,
                              methodLeastSquaresClearLand,
                              methodLeastSquaresClearSnow,
                              counterClearLandData,
                              clearLandDataB0,
                              clearLandDataB2,
                              clearLandDataB3,
                              clearLandDataMIR,
                              counterClearSnowData,
                              clearSnowDataB0,
                              clearSnowDataB2,
                              clearSnowDataB3,
                              clearSnowDataMIR);


// Scatterplot

        String surfaceType;

        File outputDirectory = new File("E:/CCI_LC_Daten/SPOT_VGT_P_and_VGT_S1/");
        if (clearLandDataLength > 0) {
            surfaceType = "clear land";
            //clear Land B0
            ScatterPlotExporter exporterB0 = new ScatterPlotExporter(450, 450);

            String xAxisLabel = "Reflectance (Band B0_SPOT_VGT_P)";
            String yAxisLabel = "Reflectance (Band B0_SPOT_VGT_S1)";
            String fileName = productName + "_clearLand_B0.png";


            float[][] clearLandDataB0NoNaNs = prepareDataForChart(clearLandDataB0, counterClearLandData);
            float[] Line = methodLeastSquaresClearLand[0];
            try {
                exporterB0.export(clearLandDataB0NoNaNs, Line, xAxisLabel, yAxisLabel, new File(outputDirectory, fileName), surfaceType);
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
            }


            //clear Land B2
            ScatterPlotExporter exporterB2 = new ScatterPlotExporter(450, 450);
            xAxisLabel = "Reflectance (Band B2_SPOT_VGT_P)";
            yAxisLabel = "Reflectance (Band B2_SPOT_VGT_S1)";
            fileName = productName + "_clearLand_B2.png";

            float[][] clearLandDataB2NoNaNs = prepareDataForChart(clearLandDataB2, counterClearLandData);
            Line = methodLeastSquaresClearLand[1];
            try {
                exporterB2.export(clearLandDataB2NoNaNs, Line, xAxisLabel, yAxisLabel, new File(outputDirectory, fileName), surfaceType);
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
            }


            //clear Land B3
            ScatterPlotExporter exporterB3 = new ScatterPlotExporter(450, 450);
            xAxisLabel = "Reflectance (Band B3_SPOT_VGT_P)";
            yAxisLabel = "Reflectance (Band B3_SPOT_VGT_S1)";
            fileName = productName + "_clearLand_B3.png";

            float[][] clearLandDataB3NoNaNs = prepareDataForChart(clearLandDataB3, counterClearLandData);
            Line = methodLeastSquaresClearLand[2];
            try {
                exporterB3.export(clearLandDataB3NoNaNs, Line, xAxisLabel, yAxisLabel, new File(outputDirectory, fileName), surfaceType);
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
            }

            //clear Land MIR
            ScatterPlotExporter exporterMIR = new ScatterPlotExporter(450, 450);
            xAxisLabel = "Reflectance (Band MIR_SPOT_VGT_P)";
            yAxisLabel = "Reflectance (Band MIR_SPOT_VGT_S1)";
            fileName = productName + "_clearLand_MIR.png";

            float[][] clearLandDataMIRNoNaNs = prepareDataForChart(clearLandDataMIR, counterClearLandData);
            Line = methodLeastSquaresClearLand[3];
            try {
                exporterMIR.export(clearLandDataMIRNoNaNs, Line, xAxisLabel, yAxisLabel, new File(outputDirectory, fileName), surfaceType);
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
            }
        }

        if (clearSnowDataLength > 0) {

            surfaceType = "clear smow";

            //clear Snow B0
            ScatterPlotExporter exporterB0S = new ScatterPlotExporter(450, 450);
            String xAxisLabel = "Reflectance (Band B0_SPOT_VGT_P)";
            String yAxisLabel = "Reflectance (Band B0_SPOT_VGT_S1)";
            String fileName = productName + "_clearSnow_B0.png";

            float[][] clearSnowDataB0NoNaNs = prepareDataForChart(clearSnowDataB0, counterClearSnowData);
            float[] Line = methodLeastSquaresClearSnow[0];
            try {
                exporterB0S.export(clearSnowDataB0NoNaNs, Line, xAxisLabel, yAxisLabel, new File(outputDirectory, fileName), surfaceType);
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
            }


            //clear Snow B2
            ScatterPlotExporter exporterB2S = new ScatterPlotExporter(450, 450);

            xAxisLabel = "Reflectance (Band B2_SPOT_VGT_P)";
            yAxisLabel = "Reflectance (Band B2_SPOT_VGT_S1)";
            fileName = productName + "_clearSnow_B2.png";

            float[][] clearSnowDataB2NoNaNs = prepareDataForChart(clearSnowDataB2, counterClearSnowData);
            Line = methodLeastSquaresClearSnow[1];
            try {
                exporterB2S.export(clearSnowDataB2NoNaNs, Line, xAxisLabel, yAxisLabel, new File(outputDirectory, fileName), surfaceType);
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
            }


            //clear Snow B3
            ScatterPlotExporter exporterB3S = new ScatterPlotExporter(450, 450);

            xAxisLabel = "Reflectance (Band B3_SPOT_VGT_P)";
            yAxisLabel = "Reflectance (Band B3_SPOT_VGT_S1)";
            fileName = productName + "_clearSnow_B3.png";

            float[][] clearSnowDataB3NoNaNs = prepareDataForChart(clearSnowDataB3, counterClearSnowData);
            Line = methodLeastSquaresClearSnow[2];
            try {
                exporterB3S.export(clearSnowDataB3NoNaNs, Line, xAxisLabel, yAxisLabel, new File(outputDirectory, fileName), surfaceType);
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
            }

            //clear Snow MIR
            ScatterPlotExporter exporterMIRS = new ScatterPlotExporter(450, 450);

            xAxisLabel = "Reflectance (Band MIR_SPOT_VGT_P)";
            yAxisLabel = "Reflectance (Band MIR_SPOT_VGT_S1)";
            fileName = productName + "_clearSnow_MIR.png";

            float[][] clearSnowDataMIRNoNaNs = prepareDataForChart(clearSnowDataMIR, counterClearSnowData);
            Line = methodLeastSquaresClearSnow[3];
            try {
                exporterMIRS.export(clearSnowDataMIRNoNaNs, Line, xAxisLabel, yAxisLabel, new File(outputDirectory, fileName), surfaceType);
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




