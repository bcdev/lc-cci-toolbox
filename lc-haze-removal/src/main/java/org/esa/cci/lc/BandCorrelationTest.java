package org.esa.cci.lc;

/**
 * Created by grit on 23.06.2014.
 */
public class BandCorrelationTest {

    /**
     * Created by IntelliJ IDEA.
     * User: grit
     * Date: 31.10.11
     * Time: 17:09
     * To change this template use File | Settings | File Templates.
     */


    public static double getPearsonCorrelation(double[] sourceData1, double[] sourceData2, int[] flagArray) {


        double[] scores1 = new double[sourceData1.length];
        double[] scores2 = new double[sourceData2.length];
        int counterValid = 0;

        for (int i = 0; i < sourceData1.length; i += 1) {
            if (flagArray[i] == PreparingOfSourceBand.CLEAR_LAND_FLAG
                && !Double.isNaN(sourceData1[i]) && !Double.isNaN(sourceData2[i])) {
                scores1[counterValid] = sourceData1[i];
                scores2[counterValid] = sourceData2[i];
                counterValid = counterValid + 1;
            }
        }

        double result = 0.;
        double mean_x = 0.;
        double mean_y = 0.;
        double delta_x = 0.;
        double delta_y = 0.;
        double sum_xx = 0.;
        double sum_yy = 0.;
        double sum_xy = 0.;
        double TINY = 1.0e-20;
        for (int i = 0; i < counterValid; i += 1) { // mean
            mean_x += scores1[i];
            mean_y += scores2[i];
        }

        mean_x /= counterValid;
        mean_y /= counterValid;


        for (int i = 0; i < counterValid; i += 1) { // correlation coefficient
            delta_x = scores1[i] - mean_x;
            delta_y = scores2[i] - mean_y;
            sum_xx += delta_x * delta_x;
            sum_yy += delta_y * delta_y;
            sum_xy += delta_x * delta_y;
        }
        result = sum_xy / (Math.sqrt(sum_xx * sum_yy) + TINY);
        return result;
    }
}



