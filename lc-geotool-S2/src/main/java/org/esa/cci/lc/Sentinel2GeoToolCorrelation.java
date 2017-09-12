package org.esa.cci.lc;

/**
 * Created by IntelliJ IDEA.
 * User: grit
 * Date: 31.10.11
 * Time: 17:09
 * To change this template use File | Settings | File Templates.
 */
public class Sentinel2GeoToolCorrelation {


    public static float getPearsonCorrelation1(float[] scores1, float[] scores2) {
        float result = 0.f;
        float sum_sq_x = 0.f;
        float sum_sq_y = 0.f;
        float sum_coproduct = 0.f;
        float mean_x = scores1[0];
        float mean_y = scores2[0];

        for (int i = 2; i < scores1.length + 1; i += 1) {
            float sweep = Float.valueOf(i - 1) / i;
            float delta_x = scores1[i - 1] - mean_x;
            float delta_y = scores2[i - 1] - mean_y;
            sum_sq_x += delta_x * delta_x * sweep;
            sum_sq_y += delta_y * delta_y * sweep;
            sum_coproduct += delta_x * delta_y * sweep;
            mean_x += delta_x / i;
            mean_y += delta_y / i;
        }
        float pop_sd_x = (float) Math.sqrt(sum_sq_x / scores1.length);
        float pop_sd_y = (float) Math.sqrt(sum_sq_y / scores1.length);
        float cov_x_y = sum_coproduct / scores1.length;
        result = cov_x_y / (pop_sd_x * pop_sd_y);
        return result;
    }

    public static float getPearsonCorrelation1(float[] scores1, float[] scores2, int validLength) {
        float result = 0.f;
        float sum_sq_x = 0.f;
        float sum_sq_y = 0.f;
        float sum_coproduct = 0.f;
        float mean_x = scores1[0];
        float mean_y = scores2[0];

        for (int i = 2; i < validLength + 1; i += 1) {
            float sweep = Float.valueOf(i - 1) / i;
            float delta_x = scores1[i - 1] - mean_x;
            float delta_y = scores2[i - 1] - mean_y;
            sum_sq_x += delta_x * delta_x * sweep;
            sum_sq_y += delta_y * delta_y * sweep;
            sum_coproduct += delta_x * delta_y * sweep;
            mean_x += delta_x / i;
            mean_y += delta_y / i;
        }
        float pop_sd_x = (float) Math.sqrt(sum_sq_x / validLength);
        float pop_sd_y = (float) Math.sqrt(sum_sq_y / validLength);
        float cov_x_y = sum_coproduct / validLength;
        result = cov_x_y / (pop_sd_x * pop_sd_y);
        return Math.abs(result);
    }

    public static double getPearsonCorrelation2(double[] scores1, double[] scores2) {
        double result = 0.;
        double mean_x = 0.;
        double mean_y = 0.;
        double delta_x = 0.;
        double delta_y = 0.;
        double sum_xx = 0.;
        double sum_yy = 0.;
        double sum_xy = 0.;
        double TINY = 1.0e-20;
        for (int i = 0; i < scores1.length; i += 1) { // mean
            mean_x += scores1[i];
            mean_y += scores2[i];
        }

        mean_x /= scores1.length;
        mean_y /= scores1.length;


        for (int i = 0; i < scores1.length; i += 1) { // correlation coefficient
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
