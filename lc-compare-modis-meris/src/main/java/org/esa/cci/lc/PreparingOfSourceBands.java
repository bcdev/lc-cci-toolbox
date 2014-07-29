package org.esa.cci.lc;

import java.io.File;
import java.util.Arrays;

public class PreparingOfSourceBands {

    public void preparedOfSourceBands(float[] sourceDataBandModis_B1,
                                      float[] sourceDataBandModis_B2,
                                      float[] sourceDataBandModis_B3,
                                      float[] sourceDataBandModis_B4,
                                      float[] sourceDataBand_Mask,
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

        float[][] clearLandDataModisMerisB1;
        float[][] clearLandDataModisMerisB2;
        float[][] clearLandDataModisMerisB3;
        float[][] clearLandDataModisMerisB4;

        float[] correlationClearLand = new float[4];

        float[][] methodLeastSquaresClearLand = new float[4][4];

        float[] resultLeastSquares = new float[3];

        ///System.out.printf("sourceLength:  %d\n", sourceLength);
        for (int i = 0; i < sourceLength; i++) {

            if (sourceDataBand_Mask[i] > 0.99 && sourceDataBand_Mask[i] < 1.01) {
                counter_clear_land += 1;
            } else {
                counter_invalid += 1;
                sourceDataBandModis_B1[i] = Float.NaN;
                sourceDataBandModis_B2[i] = Float.NaN;
                sourceDataBandModis_B3[i] = Float.NaN;
                sourceDataBandModis_B4[i] = Float.NaN;
                sourceDataBandMeris_B1[i] = Float.NaN;
                sourceDataBandMeris_B2[i] = Float.NaN;
                sourceDataBandMeris_B3[i] = Float.NaN;
                sourceDataBandMeris_B4[i] = Float.NaN;
            }
        }

        int counterClearLandData = 0;

        int clearLandDataLength = counter_clear_land;
        clearLandDataModisMerisB1 = new float[2][clearLandDataLength + 1];
        clearLandDataModisMerisB2 = new float[2][clearLandDataLength + 1];
        clearLandDataModisMerisB3 = new float[2][clearLandDataLength + 1];
        clearLandDataModisMerisB4 = new float[2][clearLandDataLength + 1];

        if (clearLandDataLength > 0) {
            for (int i = 0; i < sourceLength; i++) {
                if (sourceDataBand_Mask[i] > 0.99 && sourceDataBand_Mask[i] < 1.01) {

                    clearLandDataModisMerisB1[0][counterClearLandData] = sourceDataBandModis_B1[i];
                    clearLandDataModisMerisB2[0][counterClearLandData] = sourceDataBandModis_B2[i];
                    clearLandDataModisMerisB3[0][counterClearLandData] = sourceDataBandModis_B3[i];
                    clearLandDataModisMerisB4[0][counterClearLandData] = sourceDataBandModis_B4[i];

                    clearLandDataModisMerisB1[1][counterClearLandData] = sourceDataBandMeris_B1[i];
                    clearLandDataModisMerisB2[1][counterClearLandData] = sourceDataBandMeris_B2[i];
                    clearLandDataModisMerisB3[1][counterClearLandData] = sourceDataBandMeris_B3[i];
                    clearLandDataModisMerisB4[1][counterClearLandData] = sourceDataBandMeris_B4[i];

                    counterClearLandData += 1;
                }
            }

            correlationClearLand[0] = PearsonCorrelation.getPearsonCorrelation(clearLandDataModisMerisB1, counterClearLandData);
            correlationClearLand[1] = PearsonCorrelation.getPearsonCorrelation(clearLandDataModisMerisB2, counterClearLandData);
            correlationClearLand[2] = PearsonCorrelation.getPearsonCorrelation(clearLandDataModisMerisB3, counterClearLandData);
            correlationClearLand[3] = PearsonCorrelation.getPearsonCorrelation(clearLandDataModisMerisB4, counterClearLandData);


            methodLeastSquaresClearLand[0][3] = counterClearLandData;
            methodLeastSquaresClearLand[1][3] = counterClearLandData;
            methodLeastSquaresClearLand[2][3] = counterClearLandData;
            methodLeastSquaresClearLand[3][3] = counterClearLandData;


            resultLeastSquares = MethodLeastSquares.getlinearLeastSquares(clearLandDataModisMerisB1, counterClearLandData);
            methodLeastSquaresClearLand[0][0] = resultLeastSquares[0];
            methodLeastSquaresClearLand[0][1] = resultLeastSquares[1];
            methodLeastSquaresClearLand[0][2] = resultLeastSquares[2];
            resultLeastSquares = MethodLeastSquares.getlinearLeastSquares(clearLandDataModisMerisB2, counterClearLandData);
            methodLeastSquaresClearLand[1][0] = resultLeastSquares[0];
            methodLeastSquaresClearLand[1][1] = resultLeastSquares[1];
            methodLeastSquaresClearLand[1][2] = resultLeastSquares[2];
            resultLeastSquares = MethodLeastSquares.getlinearLeastSquares(clearLandDataModisMerisB3, counterClearLandData);
            methodLeastSquaresClearLand[2][0] = resultLeastSquares[0];
            methodLeastSquaresClearLand[2][1] = resultLeastSquares[1];
            methodLeastSquaresClearLand[2][2] = resultLeastSquares[2];
            resultLeastSquares = MethodLeastSquares.getlinearLeastSquares(clearLandDataModisMerisB4, counterClearLandData);
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
                    clearLandDataModisMerisB1[j][i] = Float.NaN;
                    clearLandDataModisMerisB2[j][i] = Float.NaN;
                    clearLandDataModisMerisB3[j][i] = Float.NaN;
                    clearLandDataModisMerisB4[j][i] = Float.NaN;
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
                              counterClearLandData,
                              clearLandDataModisMerisB1,
                              clearLandDataModisMerisB2,
                              clearLandDataModisMerisB3,
                              clearLandDataModisMerisB4);


// Scatterplot

        String surfaceType;

        // todo Toc
        // File outputDirectory = new File("E:/CCI_LC_Daten/PhaseII/MODIS/MOD09/MODIS_IMAGE_COMPLETT_VERGLEICH/");
        File outputDirectory = new File("E:/CCI_LC_Daten/PhaseII/MODIS/Modis_L1b/MODIS_IMAGE_COMPLETT_VERGLEICH/");
        if (clearLandDataLength > 0)

        {
            surfaceType = "clear land";
            //clear Land B1 MERIS/ MODIS
            ScatterPlotExporter exporterB0 = new ScatterPlotExporter(450, 450);
            // todo  TOC
            // String scatterplotTitle = "Comparison of surface reflectance values of MERIS RR and MODIS MOD09";
            String scatterplotTitle = "Comparison of apparent reflectance values of MERIS RR and MODIS MOD01";
            String xAxisLabel = "Reflectance (Band 6/7 MERIS_RR )";
            String yAxisLabel = "Reflectance (Band 1 MODIS)";
            String fileName = productName + "_clearLand_MERIS_MODIS_B1.png";


            float[][] clearLandDataB1NoNaNs = prepareDataForChart(clearLandDataModisMerisB1, counterClearLandData);
            float[] Line = methodLeastSquaresClearLand[0];
            try {
                exporterB0.export(clearLandDataB1NoNaNs, Line, scatterplotTitle, xAxisLabel, yAxisLabel, new File(outputDirectory, fileName),
                                  surfaceType);
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
            }


            //clear Land B2 MERIS/MODIS
            ScatterPlotExporter exporterB2 = new ScatterPlotExporter(450, 450);
            xAxisLabel = "Reflectance (Band 13 MERIS_RR )";
            yAxisLabel = "Reflectance (Band 2 MODIS)";
            fileName = productName + "_clearLand_MERIS_MODIS_B2.png";

            float[][] clearLandDataB2NoNaNs = prepareDataForChart(clearLandDataModisMerisB2, counterClearLandData);
            Line = methodLeastSquaresClearLand[1];
            try {
                exporterB2.export(clearLandDataB2NoNaNs, Line, scatterplotTitle, xAxisLabel, yAxisLabel, new File(outputDirectory, fileName),
                                  surfaceType);
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
            }


            //clear Land B3   MERIS/MODIS
            ScatterPlotExporter exporterB3 = new ScatterPlotExporter(450, 450);
            xAxisLabel = "Reflectance (Band 2/3 MERIS_RR )";
            yAxisLabel = "Reflectance (Band 3 MODIS)";
            fileName = productName + "_clearLand_MERIS_MODIS_B3.png";

            float[][] clearLandDataB3NoNaNs = prepareDataForChart(clearLandDataModisMerisB3, counterClearLandData);
            Line = methodLeastSquaresClearLand[2];
            try {
                exporterB3.export(clearLandDataB3NoNaNs, Line, scatterplotTitle, xAxisLabel, yAxisLabel, new File(outputDirectory, fileName),
                                  surfaceType);
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
            }


            //clear Land B4 MERIS/MODIS
            ScatterPlotExporter exporterB4 = new ScatterPlotExporter(450, 450);
            xAxisLabel = "Reflectance (Band 5 MERIS_RR )";
            yAxisLabel = "Reflectance (Band 4 MODIS)";
            fileName = productName + "_clearLand_MERIS_MODIS_B4.png";


            float[][] clearLandDataB4NoNaNs = prepareDataForChart(clearLandDataModisMerisB4, counterClearLandData);
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




