package org.esa.cci.lc;

import java.io.BufferedWriter;
import java.io.FileWriter;

/**
 * Created by IntelliJ IDEA.
 * User: michael
 * Date: 06.09.11
 * Time: 14:51
 * To change this template use File | Settings | File Templates.
 */
public class WriteLogFiles {

    static final int PANORAMEFFECT_FLAG = 2000;

    public void writingLogFiles(String productName) {

        //int sourceLength = sourceWidth * sourceHeight;
        //System.out.printf("============>>>>    count:  %d   %s   %s\n", count, RunType, StatisticFrontOperator.MEAN);
        if (WaterBodyCompareOperator.maxKernelRadius == 0) {
            //System.out.printf("i am here at 0\n");
            try {
                FileWriter fstream = new FileWriter("F:/FrontsAATSR/AuxiliaryLogs/emptyProducts.txt", true);
                BufferedWriter out = new BufferedWriter(fstream);
                out.write(productName + "\n");
                out.close();
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
            }
            //System.out.printf("==> Empty Product:  %s\n", productName);
        } else {
            try {
                //System.out.printf("i am here at writing\n");
                FileWriter fstream = new FileWriter("...../goodProducts.txt", true);
                BufferedWriter out = new BufferedWriter(fstream);
                out.write(productName
                          + " "
                          + WaterBodyCompareOperator.maxKernelRadius
                          + " "
                          + WaterBodyCompareOperator.minKernelRadius
                          + " "
                          + WaterBodyCompareOperator.convolutionFilterKernelRadius + "\n");
                out.close();
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
            }
        }
    }


    private String addZeroTo1000(int i) {
        String A = "";
        if (i < 1000) {
            A = "0";
            if (i < 100) {
                A = "00";
                if (i < 10) {
                    A = "000";
                }
            }
        }
        return A;
    }
}

