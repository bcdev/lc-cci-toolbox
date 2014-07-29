package org.esa.cci.lc;

import java.util.Arrays;

/**
 * Created with IntelliJ IDEA.
 * User: grit
 * Date: 29.01.13
 * Time: 17:48
 * To change this template use File | Settings | File Templates.
 */
public class EdgeOperator {

    static byte homogenValue = 0;
    static int testValue = 100;
    static byte edgeValue = 1;

    public byte[] computeEdge(int[] sourceData,
                              int sourceWidth,
                              int sourceHeight) {


        int sourceLength = sourceWidth * sourceHeight;


        byte[] edgeData = new byte[sourceLength];
        Arrays.fill(edgeData, homogenValue);
        int count = 0;

        int[][] fillArray = new int[3][3];

        for (int y = 0; y < sourceHeight; y++) {
            for (int x = 0; x < sourceWidth; x++) {

                for (int i = -1; i <= 1; i++) {
                    for (int j = -1; j <= 1; j++) {
                        fillArray[i + 1][j + 1] = testValue;

                        if (x + i >= 0 && x + i < sourceWidth && y + j >= 0 && y + j < sourceHeight) {

                            fillArray[i + 1][j + 1] = sourceData[(y + j) * (sourceWidth) + (x + i)];
                        }
                    }
                }

                //System.out.printf("x, y:  %d %d  \n", x,y);
                //System.out.printf("%d %d %d  \n", fillArray[0][0],fillArray[1][0], fillArray[2][0]);
                //System.out.printf("%d %d %d  \n", fillArray[0][1],fillArray[1][1], fillArray[2][1]);
                //System.out.printf("%d %d %d  \n", fillArray[0][2],fillArray[1][2], fillArray[2][2]);

                count = 0;
                for (int ii = -1; ii <= 1; ii++) {
                    for (int jj = -1; jj <= 1; jj++) {
                        if (fillArray[1][1] != fillArray[ii + 1][jj + 1] && fillArray[ii + 1][jj + 1] != testValue) {
                            count = count + 1;
                            //System.out.printf("x, y:  %d %d %d \n", x,y, count);
                        }
                    }
                }
                //System.out.printf("x, y count:  %d %d %d \n", x,y, count);

                if (count > 0) {

                    edgeData[y * (sourceWidth) + x] = edgeValue;
                    // System.out.printf("x, y edge:  %d %d %d \n", x,y, edgeData[y * (sourceWidth) + x]);
                }

                //System.out.printf("x, y edge:  %d %d %d \n", x,y, edgeData[y * (sourceWidth) + x]);
            }
        }

        return edgeData;

    }
}