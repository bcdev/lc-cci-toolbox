package org.esa.cci.lc;

import java.io.File;
import java.util.Arrays;

public class PreparingOfSourceBands {

    public void preparedOfSourceBands(float[] sourceDataBandMeris_B1v1,
                                      float[] sourceDataBandMeris_B2v1,
                                      float[] sourceDataBandMeris_B3v1,
                                      float[] sourceDataBandMeris_B4v1,
                                      float[] sourceDataBand_Maskv1,
                                      float[] sourceDataBand_Maskv2,
                                      float[] sourceDataBandMeris_B1,
                                      float[] sourceDataBandMeris_B2,
                                      float[] sourceDataBandMeris_B3,
                                      float[] sourceDataBandMeris_B4,
                                      int sourceLength,
                                      int sourceWidth,
                                      int sourceHeight,
                                      String productName) {


        int counter_clear_land = 0;
        int counter_invalid = 0;

        float[][] clearLandDataMerisMerisB1;
        float[][] clearLandDataMerisMerisB2;
        float[][] clearLandDataMerisMerisB3;
        float[][] clearLandDataMerisMerisB4;

        float[] correlationClearLand = new float[4];

        float[][] methodLeastSquaresClearLand = new float[4][5];

        float[] resultLeastSquares;

        ///System.out.printf("sourceLength:  %d\n", sourceLength);
        for (int i = 0; i < sourceLength; i++) {

            if (sourceDataBand_Maskv1[i] > 0.99 && sourceDataBand_Maskv1[i] < 1.01 &&
                    sourceDataBand_Maskv2[i] > 0.99 && sourceDataBand_Maskv2[i] < 1.01 ) {
                counter_clear_land += 1;
            } else {
                counter_invalid += 1;
                sourceDataBandMeris_B1v1[i] = Float.NaN;
                sourceDataBandMeris_B2v1[i] = Float.NaN;
                sourceDataBandMeris_B3v1[i] = Float.NaN;
                sourceDataBandMeris_B4v1[i] = Float.NaN;
                sourceDataBandMeris_B1[i] = Float.NaN;
                sourceDataBandMeris_B2[i] = Float.NaN;
                sourceDataBandMeris_B3[i] = Float.NaN;
                sourceDataBandMeris_B4[i] = Float.NaN;
            }
        }

        int counterClearLandData = 0;

        int clearLandDataLength = counter_clear_land;
        clearLandDataMerisMerisB1 = new float[2][clearLandDataLength + 1];
        clearLandDataMerisMerisB2 = new float[2][clearLandDataLength + 1];
        clearLandDataMerisMerisB3 = new float[2][clearLandDataLength + 1];
        clearLandDataMerisMerisB4 = new float[2][clearLandDataLength + 1];

        if (clearLandDataLength > 0) {
            for (int i = 0; i < sourceLength; i++) {
                if (sourceDataBand_Maskv1[i] > 0.99 && sourceDataBand_Maskv1[i] < 1.01 &&
                        sourceDataBand_Maskv2[i] > 0.99 && sourceDataBand_Maskv2[i] < 1.01 ) {

                    // y
                    clearLandDataMerisMerisB1[0][counterClearLandData] = sourceDataBandMeris_B1v1[i];
                    clearLandDataMerisMerisB2[0][counterClearLandData] = sourceDataBandMeris_B2v1[i];
                    clearLandDataMerisMerisB3[0][counterClearLandData] = sourceDataBandMeris_B3v1[i];
                    clearLandDataMerisMerisB4[0][counterClearLandData] = sourceDataBandMeris_B4v1[i];
                    // x
                    clearLandDataMerisMerisB1[1][counterClearLandData] = sourceDataBandMeris_B1[i];
                    clearLandDataMerisMerisB2[1][counterClearLandData] = sourceDataBandMeris_B2[i];
                    clearLandDataMerisMerisB3[1][counterClearLandData] = sourceDataBandMeris_B3[i];
                    clearLandDataMerisMerisB4[1][counterClearLandData] = sourceDataBandMeris_B4[i];

                    counterClearLandData += 1;
                }
            }

            correlationClearLand[0] = PearsonCorrelation.getPearsonCorrelation(clearLandDataMerisMerisB1, counterClearLandData);
            correlationClearLand[1] = PearsonCorrelation.getPearsonCorrelation(clearLandDataMerisMerisB2, counterClearLandData);
            correlationClearLand[2] = PearsonCorrelation.getPearsonCorrelation(clearLandDataMerisMerisB3, counterClearLandData);
            correlationClearLand[3] = PearsonCorrelation.getPearsonCorrelation(clearLandDataMerisMerisB4, counterClearLandData);


            methodLeastSquaresClearLand[0][4] = counterClearLandData;
            methodLeastSquaresClearLand[1][4] = counterClearLandData;
            methodLeastSquaresClearLand[2][4] = counterClearLandData;
            methodLeastSquaresClearLand[3][4] = counterClearLandData;


            resultLeastSquares = MethodLeastSquares.getlinearLeastSquares(clearLandDataMerisMerisB1, counterClearLandData);
            methodLeastSquaresClearLand[0][0] = resultLeastSquares[0];
            methodLeastSquaresClearLand[0][1] = resultLeastSquares[1];
            methodLeastSquaresClearLand[0][2] = resultLeastSquares[2];
            methodLeastSquaresClearLand[0][3] = resultLeastSquares[3];
            resultLeastSquares = MethodLeastSquares.getlinearLeastSquares(clearLandDataMerisMerisB2, counterClearLandData);
            methodLeastSquaresClearLand[1][0] = resultLeastSquares[0];
            methodLeastSquaresClearLand[1][1] = resultLeastSquares[1];
            methodLeastSquaresClearLand[1][2] = resultLeastSquares[2];
            methodLeastSquaresClearLand[1][3] = resultLeastSquares[3];
            resultLeastSquares = MethodLeastSquares.getlinearLeastSquares(clearLandDataMerisMerisB3, counterClearLandData);
            methodLeastSquaresClearLand[2][0] = resultLeastSquares[0];
            methodLeastSquaresClearLand[2][1] = resultLeastSquares[1];
            methodLeastSquaresClearLand[2][2] = resultLeastSquares[2];
            methodLeastSquaresClearLand[2][3] = resultLeastSquares[3];
            resultLeastSquares = MethodLeastSquares.getlinearLeastSquares(clearLandDataMerisMerisB4, counterClearLandData);
            methodLeastSquaresClearLand[3][0] = resultLeastSquares[0];
            methodLeastSquaresClearLand[3][1] = resultLeastSquares[1];
            methodLeastSquaresClearLand[3][2] = resultLeastSquares[2];
            methodLeastSquaresClearLand[3][3] = resultLeastSquares[3];


        } else {
            for (int i = 0; i < 5; i++) {
                correlationClearLand[i] = Float.NaN;
                methodLeastSquaresClearLand[i][0] = Float.NaN;
                methodLeastSquaresClearLand[i][1] = Float.NaN;
                methodLeastSquaresClearLand[i][2] = Float.NaN;
                methodLeastSquaresClearLand[i][3] = Float.NaN;
                methodLeastSquaresClearLand[i][4] = Float.NaN;
            }
            for (int i = 0; i < clearLandDataLength + 1; i++) {
                for (int j = 0; j < 2; j++) {
                    clearLandDataMerisMerisB1[j][i] = Float.NaN;
                    clearLandDataMerisMerisB2[j][i] = Float.NaN;
                    clearLandDataMerisMerisB3[j][i] = Float.NaN;
                    clearLandDataMerisMerisB4[j][i] = Float.NaN;
                }
            }
        }


        System.out.printf("counter:  %d \n", counterClearLandData);


        // writing log files for batch processing
        WriteFiles logfiles = new WriteFiles();
        logfiles.writingFiles(productName, sourceWidth, sourceHeight,
                              counter_invalid,
                              counter_clear_land,
                              correlationClearLand,
                              methodLeastSquaresClearLand,
                              counterClearLandData);


// Scatterplot

        String surfaceType;

        // File outputDirectory = new File("E:/CCI_LC_Daten/PhaseII/MODIS/MOD09/MODIS_IMAGE_COMPLETT_VERGLEICH/");
        File outputDirectory = new File("E:/CCI_LC_Daten/PhaseII/PVIR/01_MERIS_phaseI_phaseII/");
        if (clearLandDataLength > 0)

        {
            surfaceType = "clear land";
            //clear Land B1 MERISv2/ MERISv1
            ScatterPlotExporter exporterB0 = new ScatterPlotExporter(450, 450);
            String scatterplotTitle = "Comparison of surface reflectance values of MERIS FR v2.0 and MERIS FR v1.0";
            String xAxisLabel = "Reflectance (Band 3 MERIS FR v2.0)";
            String yAxisLabel = "Reflectance (Band 3 MERIS FR v1.0)";
            String fileName = productName + "_clearLand_MERIS_MERIS_v1_v2_B3.png";


            float[][] clearLandDataB1NoNaNs = prepareDataForChart(clearLandDataMerisMerisB1, counterClearLandData);
            float[] Line = methodLeastSquaresClearLand[0];
            try {
                exporterB0.export(clearLandDataB1NoNaNs, Line, scatterplotTitle, xAxisLabel, yAxisLabel, new File(outputDirectory, fileName),
                                  surfaceType);
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
            }


            //clear Land B2 MERIS/MODIS
            ScatterPlotExporter exporterB2 = new ScatterPlotExporter(450, 450);
            xAxisLabel = "Reflectance (Band 5 MERIS FR v2.0)";
            yAxisLabel = "Reflectance (Band 5 MERIS FR v1.0)";
            fileName = productName + "_clearLand_MERIS_MERIS_v1_v2_B5.png";;

            float[][] clearLandDataB2NoNaNs = prepareDataForChart(clearLandDataMerisMerisB2, counterClearLandData);
            Line = methodLeastSquaresClearLand[1];
            try {
                exporterB2.export(clearLandDataB2NoNaNs, Line, scatterplotTitle, xAxisLabel, yAxisLabel, new File(outputDirectory, fileName),
                                  surfaceType);
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
            }


            //clear Land B3   MERIS/MODIS
            ScatterPlotExporter exporterB3 = new ScatterPlotExporter(450, 450);
            xAxisLabel = "Reflectance (Band 7 MERIS FR v2.0)";
            yAxisLabel = "Reflectance (Band 7 MERIS FR v1.0)";
            fileName = productName + "_clearLand_MERIS_MERIS_v1_v2_B7.png";;

            float[][] clearLandDataB3NoNaNs = prepareDataForChart(clearLandDataMerisMerisB3, counterClearLandData);
            Line = methodLeastSquaresClearLand[2];
            try {
                exporterB3.export(clearLandDataB3NoNaNs, Line, scatterplotTitle, xAxisLabel, yAxisLabel, new File(outputDirectory, fileName),
                                  surfaceType);
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
            }


            //clear Land B4 MERIS/MODIS
            ScatterPlotExporter exporterB4 = new ScatterPlotExporter(450, 450);
            xAxisLabel = "Reflectance (Band 14 MERIS FR v2.0)";
            yAxisLabel = "Reflectance (Band 14 MERIS FR v1.0)";
            fileName = productName + "_clearLand_MERIS_MERIS_v1_v2_B14.png";;


            float[][] clearLandDataB4NoNaNs = prepareDataForChart(clearLandDataMerisMerisB4, counterClearLandData);
            Line = methodLeastSquaresClearLand[3];
            try {
                exporterB4.export(clearLandDataB4NoNaNs, Line, scatterplotTitle, xAxisLabel, yAxisLabel, new File(outputDirectory, fileName),
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




