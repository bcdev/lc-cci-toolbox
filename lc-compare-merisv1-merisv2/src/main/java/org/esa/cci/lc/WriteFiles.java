package org.esa.cci.lc;

import java.io.BufferedWriter;
import java.io.FileWriter;


public class WriteFiles {

    public void writingFiles(String productName,
                             int sourceWidth,
                             int sourceHeight,
                             int counter_invalid,
                             int counter_clear_land,
                             float[] correlationClearLand,
                             float[][] methodLeastSquaresClearLand,
                             int counterClearLandData) {

        try {
            // FileWriter fstream = new FileWriter("E:/CCI_LC_Daten/PhaseII/MODIS/MOD09/MODIS_IMAGE_COMPLETT_VERGLEICH/" + productName + "_result.txt", true);
            FileWriter fstream = new FileWriter(
                    "E:/CCI_LC_Daten/PhaseII/PVIR/01_MERIS_phaseI_phaseII/" + productName + "_result.txt", true);
            BufferedWriter out = new BufferedWriter(fstream);
            out.write(String.format("Scene_Width__________________________:  %+10d %n", sourceWidth));
            out.write(String.format("Scene_Height_________________________:  %+10d %n", sourceHeight));
            out.write(String.format("number_of_invalid_MERISv1_MERISv2________:  %+10d %n", counter_invalid));
            out.write(String.format("number_of_clear_land_MERISv1_MERISv2_____:  %+10d %n", counter_clear_land));
            out.write("\n");
            out.write("\n");
            out.write("clear_land \n");
            out.write(String.format("number of clear land pixel:  %+10d %n", counterClearLandData));
            out.write(String.format("pearson_correlation_coefficient_MERISv1_MERISv2_of_________B1:  %+10.4f %n", correlationClearLand[0]));
            out.write(String.format("ALPHA0________LeastSquares_coefficient_MERISv1_MERISv2_of__B1:  %+10.4f %n", methodLeastSquaresClearLand[0][0]));
            out.write(String.format("ALPHA1________LeastSquares_coefficient_MERISv1_MERISv2_of__B1:  %+10.4f %n", methodLeastSquaresClearLand[0][1]));
            out.write(String.format("RESIDUUM______LeastSquares_coefficient_MERISv1_MERISv2_of__B1:  %+10.4f %n", methodLeastSquaresClearLand[0][2]));
            out.write(String.format("RESIDUUM_1to1_LeastSquares_coefficient_MERISv1_MERISv2_of__B1:  %+10.4f %n", methodLeastSquaresClearLand[0][3]));
            out.write("\n");
            out.write(String.format("pearson_correlation_coefficient_MERISv1_MERISv2_of____B2:  %+10.4f %n", correlationClearLand[1]));
            out.write(String.format("ALPHA0________LeastSquares_coefficient_MERISv1_MERISv2_of__B2:  %+10.4f %n", methodLeastSquaresClearLand[1][0]));
            out.write(String.format("ALPHA1________LeastSquares_coefficient_MERISv1_MERISv2_of__B2:  %+10.4f %n", methodLeastSquaresClearLand[1][1]));
            out.write(String.format("RESIDUUM______LeastSquares_coefficient_MERISv1_MERISv2_of__B2:  %+10.4f %n", methodLeastSquaresClearLand[1][2]));
            out.write(String.format("RESIDUUM_1to1_LeastSquares_coefficient_MERISv1_MERISv2_of__B2:  %+10.4f %n", methodLeastSquaresClearLand[1][3]));
            out.write("\n");
            out.write(String.format("pearson_correlation_coefficient_MERISv1_MERISv2_of_________B3:  %+10.4f %n", correlationClearLand[2]));
            out.write(String.format("ALPHA0________LeastSquares_coefficient_MERISv1_MERISv2_of__B3:  %+10.4f %n", methodLeastSquaresClearLand[2][0]));
            out.write(String.format("ALPHA1________LeastSquares_coefficient_MERISv1_MERISv2_of__B3:  %+10.4f %n", methodLeastSquaresClearLand[2][1]));
            out.write(String.format("RESIDUUM______LeastSquares_coefficient_MERISv1_MERISv2_of__B3:  %+10.4f %n", methodLeastSquaresClearLand[2][2]));
            out.write(String.format("RESIDUUM_1to1_LeastSquares_coefficient_MERISv1_MERISv2_of__B3:  %+10.4f %n", methodLeastSquaresClearLand[2][3]));
            out.write("\n");
            out.write(String.format("pearson_correlation_coefficient_MERISv1_MERISv2_of_________B4:  %+10.4f %n", correlationClearLand[3]));
            out.write(String.format("ALPHA0________LeastSquares_coefficient_MERISv1_MERISv2_of__B4:  %+10.4f %n", methodLeastSquaresClearLand[3][0]));
            out.write(String.format("ALPHA1________LeastSquares_coefficient_MERISv1_MERISv2_of__B4:  %+10.4f %n", methodLeastSquaresClearLand[3][1]));
            out.write(String.format("RESIDUUM______LeastSquares_coefficient_MERISv1_MERISv2_of__B4:  %+10.4f %n", methodLeastSquaresClearLand[3][2]));
            out.write(String.format("RESIDUUM_1to1_LeastSquares_coefficient_MERISv1_MERISv2_of__B4:  %+10.4f %n", methodLeastSquaresClearLand[3][3]));
            out.write("\n");
            out.write("\n");
            out.close();
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
        //System.out.printf("==> Empty Product:  %s\n", productName);
    }
}
