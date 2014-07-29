package org.esa.cci.lc;

import java.io.BufferedWriter;
import java.io.FileWriter;


public class WriteFiles {


    public void writingFiles(String productName,
                             int sourceWidth,
                             int sourceHeight,
                             int counter_invalid,
                             int counter_snow_spot_s1,
                             int counter_clear_land_spot_s1,
                             int counter_snow_spot_p,
                             int counter_clear_land_spot_p,
                             int counter_snow_meris,
                             int counter_clear_land_meris,
                             float[] correlationClearLand,
                             float[] correlationClearSnow,
                             float[][] methodLeastSquaresClearLand,
                             float[][] methodLeastSquaresClearSnow,
                             int counterClearLandData,
                             int counterClearSnowData,
                             float[][] clearLandDataSPOTS1MERISRRB0,
                             float[][] clearLandDataSPOTS1MERISRRB2,
                             float[][] clearLandDataSPOTS1MERISRRB3,
                             float[][] clearLandDataSPOTPMERISRRB0,
                             float[][] clearLandDataSPOTPMERISRRB2,
                             float[][] clearLandDataSPOTPMERISRRB3) {


        try {
            FileWriter fstream = new FileWriter("E:/CCI_LC_Daten/SPOT_VGT_P_and_VGT_S1/SPOT_to_MERIS/" + productName + "_result.txt", true);
            BufferedWriter out = new BufferedWriter(fstream);
            out.write(String.format("Scene_Width__________________________:  %+10d %n", sourceWidth));
            out.write(String.format("Scene_Height_________________________:  %+10d %n", sourceHeight));
            out.write(String.format("number_of_invalid_SPOT_VGT_S1_P_MERIS:  %+10d %n", counter_invalid));
            out.write(String.format("number_of_snow____________SPOT_VGT_S1:  %+10d %n", counter_snow_spot_s1));
            out.write(String.format("number_of_clear_land______SPOT_VGT_S1:  %+10d %n", counter_clear_land_spot_s1));
            out.write(String.format("number_of_snow_____________SPOT_VGT_P:  %+10d %n", counter_snow_spot_p));
            out.write(String.format("number_of_clear_land_______SPOT_VGT_P:  %+10d %n", counter_clear_land_spot_p));
            out.write(String.format("number_of_snow_______________MERIS_RR:  %+10d %n", counter_snow_meris));
            out.write(String.format("number_of_clear_land_________MERIS_RR:  %+10d %n", counter_clear_land_meris));
            out.write("\n");
            out.write("\n");
            out.write("clear_land \n");
            out.write(String.format("number of clear land pixel:  %+10d %n", counterClearLandData));
            out.write(String.format("pearson_correlation_coefficient_SPOT_VGT_S1_and_MERIS_of____B0:  %+10.4f %n", correlationClearLand[0]));
            out.write(
                    String.format("ALPHA0___LeastSquares_coefficient_SPOT_VGT_S1_and_MERIS_of__B0:  %+10.4f %n", methodLeastSquaresClearLand[0][0]));
            out.write(
                    String.format("ALPHA1___LeastSquares_coefficient_SPOT_VGT_S1_and_MERIS_of__B0:  %+10.4f %n", methodLeastSquaresClearLand[0][1]));
            out.write(
                    String.format("RESIDUUM_LeastSquares_coefficient_SPOT_VGT_S1_and_MERIS_of__B0:  %+10.4f %n", methodLeastSquaresClearLand[0][2]));
            out.write("\n");
            out.write(String.format("pearson_correlation_coefficient_SPOT_VGT_S1_and_MERIS_of____B2:  %+10.4f %n", correlationClearLand[1]));
            out.write(
                    String.format("ALPHA0___LeastSquares_coefficient_SPOT_VGT_S1_and_MERIS_of__B2:  %+10.4f %n", methodLeastSquaresClearLand[1][0]));
            out.write(
                    String.format("ALPHA1___LeastSquares_coefficient_SPOT_VGT_S1_and_MERIS_of__B2:  %+10.4f %n", methodLeastSquaresClearLand[1][1]));
            out.write(
                    String.format("RESIDUUM_LeastSquares_coefficient_SPOT_VGT_S1_and_MERIS_of__B2:  %+10.4f %n", methodLeastSquaresClearLand[1][2]));
            out.write("\n");
            out.write(String.format("pearson_correlation_coefficient_SPOT_VGT_S1_and_MERIS_of____B3:  %+10.4f %n", correlationClearLand[2]));
            out.write(
                    String.format("ALPHA0___LeastSquares_coefficient_SPOT_VGT_S1_and_MERIS_of__B3:  %+10.4f %n", methodLeastSquaresClearLand[2][0]));
            out.write(
                    String.format("ALPHA1___LeastSquares_coefficient_SPOT_VGT_S1_and_MERIS_of__B3:  %+10.4f %n", methodLeastSquaresClearLand[2][1]));
            out.write(
                    String.format("RESIDUUM_LeastSquares_coefficient_SPOT_VGT_S1_and_MERIS_of__B3:  %+10.4f %n", methodLeastSquaresClearLand[2][2]));
            out.write("\n");
            out.write(String.format("pearson_correlation_coefficient_SPOT_VGT_P_and_MERIS_of_____B0:  %+10.4f %n", correlationClearLand[3]));
            out.write(
                    String.format("ALPHA0___LeastSquares_coefficient_SPOT_VGT_P_and_MERIS_of___B0:  %+10.4f %n", methodLeastSquaresClearLand[3][0]));
            out.write(
                    String.format("ALPHA1___LeastSquares_coefficient_SPOT_VGT_P_and_MERIS_of___B0:  %+10.4f %n", methodLeastSquaresClearLand[3][1]));
            out.write(
                    String.format("RESIDUUM_LeastSquares_coefficient_SPOT_VGT_P_and_MERIS_of___B0:  %+10.4f %n", methodLeastSquaresClearLand[3][2]));
            out.write("\n");
            out.write(String.format("pearson_correlation_coefficient_SPOT_VGT_P_and_MERIS_of_____B2:  %+10.4f %n", correlationClearLand[4]));
            out.write(
                    String.format("ALPHA0___LeastSquares_coefficient_SPOT_VGT_P_and_MERIS_of___B2:  %+10.4f %n", methodLeastSquaresClearLand[4][0]));
            out.write(
                    String.format("ALPHA1___LeastSquares_coefficient_SPOT_VGT_P_and_MERIS_of___B2:  %+10.4f %n", methodLeastSquaresClearLand[4][1]));
            out.write(
                    String.format("RESIDUUM_LeastSquares_coefficient_SPOT_VGT_P_and_MERIS_of___B2:  %+10.4f %n", methodLeastSquaresClearLand[4][2]));
            out.write("\n");
            out.write(String.format("pearson_correlation_coefficient_SPOT_VGT_P_and_MERIS_of_____B3:  %+10.4f %n", correlationClearLand[5]));
            out.write(
                    String.format("ALPHA0___LeastSquares_coefficient_SPOT_VGT_P_and_MERIS_of___B3:  %+10.4f %n", methodLeastSquaresClearLand[5][0]));
            out.write(
                    String.format("ALPHA1___LeastSquares_coefficient_SPOT_VGT_P_and_MERIS_of___B3:  %+10.4f %n", methodLeastSquaresClearLand[5][1]));
            out.write(
                    String.format("RESIDUUM_LeastSquares_coefficient_SPOT_VGT_P_and_MERIS_of___B3:  %+10.4f %n", methodLeastSquaresClearLand[5][2]));
            out.write("\n");
            out.write("\n");
            out.write("clear_snow \n");
            out.write(String.format("number of snow covered pixel:  %+10d %n", counterClearSnowData));
            out.write(String.format("pearson_correlation_coefficient_SPOT_VGT_S1_and_MERIS_of____B0:  %+10.4f %n", correlationClearSnow[0]));
            out.write(
                    String.format("ALPHA0___LeastSquares_coefficient_SPOT_VGT_S1_and_MERIS_of__B0:  %+10.4f %n", methodLeastSquaresClearSnow[0][0]));
            out.write(
                    String.format("ALPHA1___LeastSquares_coefficient_SPOT_VGT_S1_and_MERIS_of__B0:  %+10.4f %n", methodLeastSquaresClearSnow[0][1]));
            out.write(
                    String.format("RESIDUUM_LeastSquares_coefficient_SPOT_VGT_S1_and_MERIS_of__B0:  %+10.4f %n", methodLeastSquaresClearSnow[0][2]));
            out.write("\n");
            out.write(String.format("pearson_correlation_coefficient_SPOT_VGT_S1_and_MERIS_of____B2:  %+10.4f %n", correlationClearSnow[1]));
            out.write(
                    String.format("ALPHA0___LeastSquares_coefficient_SPOT_VGT_S1_and_MERIS_of__B2:  %+10.4f %n", methodLeastSquaresClearSnow[1][0]));
            out.write(
                    String.format("ALPHA1___LeastSquares_coefficient_SPOT_VGT_S1_and_MERIS_of__B2:  %+10.4f %n", methodLeastSquaresClearSnow[1][1]));
            out.write(
                    String.format("RESIDUUM_LeastSquares_coefficient_SPOT_VGT_S1_and_MERIS_of__B2:  %+10.4f %n", methodLeastSquaresClearSnow[1][2]));
            out.write("\n");
            out.write(String.format("pearson_correlation_coefficient_SPOT_VGT_S1_and_MERIS_of____B3:  %+10.4f %n", correlationClearSnow[2]));
            out.write(
                    String.format("ALPHA0___LeastSquares_coefficient_SPOT_VGT_S1_and_MERIS_of__B3:  %+10.4f %n", methodLeastSquaresClearSnow[2][0]));
            out.write(
                    String.format("ALPHA1___LeastSquares_coefficient_SPOT_VGT_S1_and_MERIS_of__B3:  %+10.4f %n", methodLeastSquaresClearSnow[2][1]));
            out.write(
                    String.format("RESIDUUM_LeastSquares_coefficient_SPOT_VGT_S1_and_MERIS_of__B3:  %+10.4f %n", methodLeastSquaresClearSnow[2][2]));
            out.write("\n");
            out.write(String.format("pearson_correlation_coefficient_SPOT_VGT_P_and_MERIS_of_____B0:  %+10.4f %n", correlationClearSnow[3]));
            out.write(
                    String.format("ALPHA0___LeastSquares_coefficient_SPOT_VGT_P_and_MERIS_of___B0:  %+10.4f %n", methodLeastSquaresClearSnow[3][0]));
            out.write(
                    String.format("ALPHA1___LeastSquares_coefficient_SPOT_VGT_P_and_MERIS_of___B0:  %+10.4f %n", methodLeastSquaresClearSnow[3][1]));
            out.write(
                    String.format("RESIDUUM_LeastSquares_coefficient_SPOT_VGT_P_and_MERIS_of___B0:  %+10.4f %n", methodLeastSquaresClearSnow[3][2]));
            out.write("\n");
            out.write(String.format("pearson_correlation_coefficient_SPOT_VGT_P_and_MERIS_of_____B2:  %+10.4f %n", correlationClearSnow[4]));
            out.write(
                    String.format("ALPHA0___LeastSquares_coefficient_SPOT_VGT_P_and_MERIS_of___B2:  %+10.4f %n", methodLeastSquaresClearSnow[4][0]));
            out.write(
                    String.format("ALPHA1___LeastSquares_coefficient_SPOT_VGT_P_and_MERIS_of___B2:  %+10.4f %n", methodLeastSquaresClearSnow[4][1]));
            out.write(
                    String.format("RESIDUUM_LeastSquares_coefficient_SPOT_VGT_P_and_MERIS_of___B2:  %+10.4f %n", methodLeastSquaresClearSnow[4][2]));
            out.write("\n");
            out.write(String.format("pearson_correlation_coefficient_SPOT_VGT_P_and_MERIS_of_____B3:  %+10.4f %n", correlationClearSnow[5]));
            out.write(
                    String.format("ALPHA0___LeastSquares_coefficient_SPOT_VGT_P_and_MERIS_of___B3:  %+10.4f %n", methodLeastSquaresClearSnow[5][0]));
            out.write(
                    String.format("ALPHA1___LeastSquares_coefficient_SPOT_VGT_P_and_MERIS_of___B3:  %+10.4f %n", methodLeastSquaresClearSnow[5][1]));
            out.write(
                    String.format("RESIDUUM_LeastSquares_coefficient_SPOT_VGT_P_and_MERIS_of___B3:  %+10.4f %n", methodLeastSquaresClearSnow[5][2]));
            out.write("\n");
            out.write("\n");
//            out.write(String.format("clear_land_data: B0_SPOT_S1 B0_SPOT_P B2_SPOT_S1 B2_SPOT_P  B3_SPOT_S1 B3_SPOT_P  MIR_SPOT_S1 MIR_SPOT_P: %+10d %n", counterClearLandData));
//            for (int i = 0; i < counterClearLandData; i++) {
//                out.write(String.format("%+10.3f %+10.3f %+10.3f %+10.3f %+10.3f %+10.3f %+10.3f %+10.3f %+10.3f %+10.3f %+10.3f %+10.3f%n",
//                        clearLandDataSPOTS1MERISRRB0[0][i], clearLandDataSPOTS1MERISRRB0[1][i],
//                        clearLandDataSPOTS1MERISRRB2[0][i], clearLandDataSPOTS1MERISRRB2[1][i],
//                        clearLandDataSPOTS1MERISRRB3[0][i], clearLandDataSPOTS1MERISRRB3[1][i],
//                        clearLandDataSPOTPMERISRRB0[0][i], clearLandDataSPOTPMERISRRB0[1][i],
//                        clearLandDataSPOTPMERISRRB2[0][i], clearLandDataSPOTPMERISRRB2[1][i],
//                        clearLandDataSPOTPMERISRRB3[0][i], clearLandDataSPOTPMERISRRB3[1][i]));
//            }


//            out.write(String.format("clear_snow_data: B0_SPOT_S1 B0_SPOT_P B2_SPOT_S1 B2_SPOT_P  B3_SPOT_S1 B3_SPOT_P  MIR_SPOT_S1 MIR_SPOT_P: %+10d %n", counterClearSnowData));
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
