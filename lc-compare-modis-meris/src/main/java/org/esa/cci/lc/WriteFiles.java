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
                             int counterClearLandData,
                             float[][] clearLandDataModisMerisB1,
                             float[][] clearLandDataModisMerisB2,
                             float[][] clearLandDataModisMerisB3,
                             float[][] clearLandDataModisMerisB4) {

        try {
            // todo Toc
            // FileWriter fstream = new FileWriter("E:/CCI_LC_Daten/PhaseII/MODIS/MOD09/MODIS_IMAGE_COMPLETT_VERGLEICH/" + productName + "_result.txt", true);
            FileWriter fstream = new FileWriter(
                    "E:/CCI_LC_Daten/PhaseII/MODIS/Modis_L1b/MODIS_IMAGE_COMPLETT_VERGLEICH/" + productName + "_result.txt", true);
            BufferedWriter out = new BufferedWriter(fstream);
            out.write(String.format("Scene_Width__________________________:  %+10d %n", sourceWidth));
            out.write(String.format("Scene_Height_________________________:  %+10d %n", sourceHeight));
            out.write(String.format("number_of_invalid_MODIS_MERIS________:  %+10d %n", counter_invalid));
            out.write(String.format("number_of_clear_land_MODIS_MERIS_____:  %+10d %n", counter_clear_land));
            out.write("\n");
            out.write("\n");
            out.write("clear_land \n");
            out.write(String.format("number of clear land pixel:  %+10d %n", counterClearLandData));
            out.write(String.format("pearson_correlation_coefficient_MODIS_and_MERIS_of____B1:  %+10.4f %n", correlationClearLand[0]));
            out.write(String.format("ALPHA0___LeastSquares_coefficient_MODIS_and_MERIS_of__B1:  %+10.4f %n", methodLeastSquaresClearLand[0][0]));
            out.write(String.format("ALPHA1___LeastSquares_coefficient_MODIS_and_MERIS_of__B1:  %+10.4f %n", methodLeastSquaresClearLand[0][1]));
            out.write(String.format("RESIDUUM_LeastSquares_coefficient_MODIS_and_MERIS_of__B1:  %+10.4f %n", methodLeastSquaresClearLand[0][2]));
            out.write("\n");
            out.write(String.format("pearson_correlation_coefficient_MODIS_and_MERIS_of____B2:  %+10.4f %n", correlationClearLand[1]));
            out.write(String.format("ALPHA0___LeastSquares_coefficient_MODIS_and_MERIS_of__B2:  %+10.4f %n", methodLeastSquaresClearLand[1][0]));
            out.write(String.format("ALPHA1___LeastSquares_coefficient_MODIS_and_MERIS_of__B2:  %+10.4f %n", methodLeastSquaresClearLand[1][1]));
            out.write(String.format("RESIDUUM_LeastSquares_coefficient_MODIS_and_MERIS_of__B2:  %+10.4f %n", methodLeastSquaresClearLand[1][2]));
            out.write("\n");
            out.write(String.format("pearson_correlation_coefficient_MODIS_and_MERIS_of____B3:  %+10.4f %n", correlationClearLand[2]));
            out.write(String.format("ALPHA0___LeastSquares_coefficient_MODIS_and_MERIS_of__B3:  %+10.4f %n", methodLeastSquaresClearLand[2][0]));
            out.write(String.format("ALPHA1___LeastSquares_coefficient_MODIS_and_MERIS_of__B3:  %+10.4f %n", methodLeastSquaresClearLand[2][1]));
            out.write(String.format("RESIDUUM_LeastSquares_coefficient_MODIS_and_MERIS_of__B3:  %+10.4f %n", methodLeastSquaresClearLand[2][2]));
            out.write("\n");
            out.write(String.format("pearson_correlation_coefficient_MODIS_and_MERIS_of____B4:  %+10.4f %n", correlationClearLand[3]));
            out.write(String.format("ALPHA0___LeastSquares_coefficient_MODIS_and_MERIS_of__B4:  %+10.4f %n", methodLeastSquaresClearLand[3][0]));
            out.write(String.format("ALPHA1___LeastSquares_coefficient_MODIS_and_MERIS_of__B4:  %+10.4f %n", methodLeastSquaresClearLand[3][1]));
            out.write(String.format("RESIDUUM_LeastSquares_coefficient_MODIS_and_MERIS_of__B4:  %+10.4f %n", methodLeastSquaresClearLand[3][2]));
            out.write("\n");
            out.write("\n");
//            out.write(String.format("clear_land_data: B1_MODIS  B6_7_MERIS B2_MODIS  B13_MERIS B3_MODIS B2_3_MERIS  B4_MODIS B5_MERIS: %+10d %n", counterClearLandData));
//            for (int i = 0; i < counterClearLandData; i++) {
//                out.write(String.format("%+10.3f %+10.3f %+10.3f %+10.3f %+10.3f %+10.3f %+10.3f %+10.3f %n",
//                        clearLandDataModisMerisB1[0][i], clearLandDataModisMerisB1[1][i],
//                        clearLandDataModisMerisB2[0][i], clearLandDataModisMerisB2[1][i],
//                        clearLandDataModisMerisB3[0][i], clearLandDataModisMerisB3[1][i],
//                        clearLandDataModisMerisB4[0][i], clearLandDataModisMerisB4[1][i]));
//            }

            out.close();
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
        //System.out.printf("==> Empty Product:  %s\n", productName);
    }
}
