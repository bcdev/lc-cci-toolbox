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

    public synchronized void writingLogFiles(int LcClassNumber,
                                             String LcClass,
                                             float latitude,
                                             float longitude) {


        try {
            FileWriter fstream = new FileWriter("E:/CCI_LC_Daten/ASSESSMENT/PVIR/" + LcClass + ".txt", true);
            BufferedWriter out = new BufferedWriter(fstream);
            out.write(String.valueOf(latitude) + " " + String.valueOf(longitude) + " "
                      + String.valueOf(LcClass) + " " + String.valueOf(LcClassNumber) + "\n");
            out.close();
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }

    }
}


