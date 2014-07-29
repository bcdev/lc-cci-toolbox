package org.esa.cci.lc;

import java.io.BufferedWriter;
import java.io.FileWriter;


public class WriteFiles {


    public void writingFiles(String productName,
                             int sourceWidth,
                             int sourceHeight,
                             int counter_invalid,
                             int counter_cloud_spot_s1,
                             int counter_snow_spot_s1,
                             int counter_water_spot_s1,
                             int counter_clear_land_spot_s1,
                             int counter_cloud_spot_p,
                             int counter_snow_spot_p,
                             int counter_water_spot_p,
                             int counter_clear_land_spot_p,
                             int counter_cloud_spot_p_AND_S1,
                             int counter_cloud_spot_p_OR_S1,
                             float[] correlationClearLand,
                             float[] correlationClearSnow,
                             float[][] methodLeastSquaresClearLand,
                             float[][] methodLeastSquaresClearSnow,
                             int counterClearLandData,
                             float[][] clearLandDataB0,
                             float[][] clearLandDataB2,
                             float[][] clearLandDataB3,
                             float[][] clearLandDataMIR,
                             int counterClearSnowData,
                             float[][] clearSnowDataB0,
                             float[][] clearSnowDataB2,
                             float[][] clearSnowDataB3,
                             float[][] clearSnowDataMIR) {

        try {
            FileWriter fstream = new FileWriter("E:/CCI_LC_Daten/SPOT_VGT_P_and_VGT_S1/" + productName + "_result.txt", true);
            BufferedWriter out = new BufferedWriter(fstream);
            out.write(String.format("Scene_Width______________________:  %+10d %n", sourceWidth));
            out.write(String.format("Scene_Height_____________________:  %+10d %n", sourceHeight));
            out.write(String.format("number_of_invalid___SPOT_VGT_S1_P:  %+10d %n", counter_invalid));
            out.write(String.format("number_of_cloud_______SPOT_VGT_S1:  %+10d %n", counter_cloud_spot_s1));
            out.write(String.format("number_of_snow________SPOT_VGT_S1:  %+10d %n", counter_snow_spot_s1));
            out.write(String.format("number_of_water_______SPOT_VGT_S1:  %+10d %n", counter_water_spot_s1));
            out.write(String.format("number_of_clear_land__SPOT_VGT_S1:  %+10d %n", counter_clear_land_spot_s1));
            out.write(String.format("number_of_cloud________SPOT_VGT_P:  %+10d %n", counter_cloud_spot_p));
            out.write(String.format("number_of_snow_________SPOT_VGT_P:  %+10d %n", counter_snow_spot_p));
            out.write(String.format("number_of_water________SPOT_VGT_P:  %+10d %n", counter_water_spot_p));
            out.write(String.format("number_of_clear_land___SPOT_VGT_P:  %+10d %n", counter_clear_land_spot_p));
            out.write(String.format("number_of_cloud_SPOT_VGT_S1_AND_P:  %+10d %n", counter_cloud_spot_p_AND_S1));
            out.write(String.format("number_of_cloud_ SPOT_VGT_S1_OR_P:  %+10d %n", counter_cloud_spot_p_OR_S1));
            out.write("\n");
            out.write("\n");
            out.write("clear_land \n");
            out.write(String.format("pearson_correlation_coefficient_SPOT_VGT_S1_and_P_of____B0:  %+10.4f %n", correlationClearLand[0]));
            out.write(String.format("ALPHA0___LeastSquares_coefficient_SPOT_VGT_S1_and_P_of__B0:  %+10.4f %n", methodLeastSquaresClearLand[0][0]));
            out.write(String.format("ALPHA1___LeastSquares_coefficient_SPOT_VGT_S1_and_P_of__B0:  %+10.4f %n", methodLeastSquaresClearLand[0][1]));
            out.write(String.format("RESIDUUM_LeastSquares_coefficient_SPOT_VGT_S1_and_P_of__B0:  %+10.4f %n", methodLeastSquaresClearLand[0][2]));
            out.write("\n");
            out.write(String.format("pearson_correlation_coefficient_SPOT_VGT_S1_and_P_of____B2:  %+10.4f %n", correlationClearLand[1]));
            out.write(String.format("ALPHA0___LeastSquares_coefficient_SPOT_VGT_S1_and_P_of__B2:  %+10.4f %n", methodLeastSquaresClearLand[1][0]));
            out.write(String.format("ALPHA1___LeastSquares_coefficient_SPOT_VGT_S1_and_P_of__B2:  %+10.4f %n", methodLeastSquaresClearLand[1][1]));
            out.write(String.format("RESIDUUM_LeastSquares_coefficient_SPOT_VGT_S1_and_P_of__B2:  %+10.4f %n", methodLeastSquaresClearLand[1][2]));
            out.write("\n");
            out.write(String.format("pearson_correlation_coefficient_SPOT_VGT_S1_and_P_of____B3:  %+10.4f %n", correlationClearLand[2]));
            out.write(String.format("ALPHA0___LeastSquares_coefficient_SPOT_VGT_S1_and_P_of__B3:  %+10.4f %n", methodLeastSquaresClearLand[2][0]));
            out.write(String.format("ALPHA1___LeastSquares_coefficient_SPOT_VGT_S1_and_P_of__B3:  %+10.4f %n", methodLeastSquaresClearLand[2][1]));
            out.write(String.format("RESIDUUM_LeastSquares_coefficient_SPOT_VGT_S1_and_P_of__B3:  %+10.4f %n", methodLeastSquaresClearLand[2][2]));
            out.write("\n");
            out.write(String.format("pearson_correlation_coefficient_SPOT_VGT_S1_and_P_of___MIR:  %+10.4f %n", correlationClearLand[3]));
            out.write(String.format("ALPHA0___LeastSquares_coefficient_SPOT_VGT_S1_and_P_of_MIR:  %+10.4f %n", methodLeastSquaresClearLand[3][0]));
            out.write(String.format("ALPHA1___LeastSquares_coefficient_SPOT_VGT_S1_and_P_of_MIR:  %+10.4f %n", methodLeastSquaresClearLand[3][1]));
            out.write(String.format("RESIDUUM_LeastSquares_coefficient_SPOT_VGT_S1_and_P_of_MIR:  %+10.4f %n", methodLeastSquaresClearLand[3][2]));
            out.write("\n");
            out.write("\n");
            out.write("clear_snow \n");
            out.write(String.format("pearson_correlation_coefficient_SPOT_VGT_S1_and_P_of____B0:  %+10.4f %n", correlationClearSnow[0]));
            out.write(String.format("ALPHA0___LeastSquares_coefficient_SPOT_VGT_S1_and_P_of__B0:  %+10.4f %n", methodLeastSquaresClearSnow[0][0]));
            out.write(String.format("ALPHA1___LeastSquares_coefficient_SPOT_VGT_S1_and_P_of__B0:  %+10.4f %n", methodLeastSquaresClearSnow[0][1]));
            out.write(String.format("RESIDUUM_LeastSquares_coefficient_SPOT_VGT_S1_and_P_of__B0:  %+10.4f %n", methodLeastSquaresClearSnow[0][2]));
            out.write("\n");
            out.write(String.format("pearson_correlation_coefficient_SPOT_VGT_S1_and_P_of____B2:  %+10.4f %n", correlationClearSnow[1]));
            out.write(String.format("ALPHA0___LeastSquares_coefficient_SPOT_VGT_S1_and_P_of__B2:  %+10.4f %n", methodLeastSquaresClearSnow[1][0]));
            out.write(String.format("ALPHA1___LeastSquares_coefficient_SPOT_VGT_S1_and_P_of__B2:  %+10.4f %n", methodLeastSquaresClearSnow[1][1]));
            out.write(String.format("RESIDUUM_LeastSquares_coefficient_SPOT_VGT_S1_and_P_of__B2:  %+10.4f %n", methodLeastSquaresClearSnow[1][2]));
            out.write("\n");
            out.write(String.format("pearson_correlation_coefficient_SPOT_VGT_S1_and_P_of____B3:  %+10.4f %n", correlationClearSnow[2]));
            out.write(String.format("ALPHA0___LeastSquares_coefficient_SPOT_VGT_S1_and_P_of__B3:  %+10.4f %n", methodLeastSquaresClearSnow[2][0]));
            out.write(String.format("ALPHA1___LeastSquares_coefficient_SPOT_VGT_S1_and_P_of__B3:  %+10.4f %n", methodLeastSquaresClearSnow[2][1]));
            out.write(String.format("RESIDUUM_LeastSquares_coefficient_SPOT_VGT_S1_and_P_of__B3:  %+10.4f %n", methodLeastSquaresClearSnow[2][2]));
            out.write("\n");
            out.write(String.format("pearson_correlation_coefficient_SPOT_VGT_S1_and_P_of___MIR:  %+10.4f %n", correlationClearSnow[3]));
            out.write(String.format("ALPHA0___LeastSquares_coefficient_SPOT_VGT_S1_and_P_of_MIR:  %+10.4f %n", methodLeastSquaresClearSnow[3][0]));
            out.write(String.format("ALPHA1___LeastSquares_coefficient_SPOT_VGT_S1_and_P_of_MIR:  %+10.4f %n", methodLeastSquaresClearSnow[3][1]));
            out.write(String.format("RESIDUUM_LeastSquares_coefficient_SPOT_VGT_S1_and_P_of_MIR:  %+10.4f %n", methodLeastSquaresClearSnow[3][2]));
            out.write("\n");
            out.write("\n");
            out.write(String.format(
                    "clear_land_data: B0_SPOT_S1 B0_SPOT_P B2_SPOT_S1 B2_SPOT_P  B3_SPOT_S1 B3_SPOT_P  MIR_SPOT_S1 MIR_SPOT_P: %+10d %n",
                    counterClearLandData));
//            for (int i = 0; i < counterClearLandData; i++) {
//                out.write(String.format("%+10.3f %+10.3f %+10.3f %+10.3f %+10.3f %+10.3f %+10.3f %+10.3f%n",
//                        clearLandDataB0[0][i], clearLandDataB0[1][i],
//                        clearLandDataB2[0][i], clearLandDataB2[1][i],
//                        clearLandDataB3[0][i], clearLandDataB3[1][i],
//                        clearLandDataMIR[0][i], clearLandDataMIR[1][i]));
//            }
            out.write(String.format(
                    "clear_snow_data: B0_SPOT_S1 B0_SPOT_P B2_SPOT_S1 B2_SPOT_P  B3_SPOT_S1 B3_SPOT_P  MIR_SPOT_S1 MIR_SPOT_P: %+10d %n",
                    counterClearSnowData));
//            for (int i = 0; i < counterClearSnowData; i++) {
//                out.write(String.format("%+10.3f %+10.3f %+10.3f %+10.3f %+10.3f %+10.3f %+10.3f %+10.3f%n",
//                        clearSnowDataB0[0][i], clearSnowDataB0[1][i],
//                        clearSnowDataB2[0][i], clearSnowDataB2[1][i],
//                        clearSnowDataB3[0][i], clearSnowDataB3[1][i],
//                        clearSnowDataMIR[0][i], clearSnowDataMIR[1][i]));
//            }
            out.close();
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
        //System.out.printf("==> Empty Product:  %s\n", productName);
    }
}
