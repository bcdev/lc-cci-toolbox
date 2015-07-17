package org.esa.cci.lc;

public class AvhrrGeoToolCorrelation {


    public static double getPearsonCorrelation1(double[] scores1, double[] scores2) {
        double result = 0.;
        double sum_sq_x = 0.;
        double sum_sq_y = 0.;
        double sum_coproduct = 0.;
        double mean_x = scores1[0];
        double mean_y = scores2[0];

        for (int i = 2; i < scores1.length + 1; i += 1) {
            double sweep = Double.valueOf(i - 1) / i;
            double delta_x = scores1[i - 1] - mean_x;
            double delta_y = scores2[i - 1] - mean_y;
            sum_sq_x += delta_x * delta_x * sweep;
            sum_sq_y += delta_y * delta_y * sweep;
            sum_coproduct += delta_x * delta_y * sweep;
            mean_x += delta_x / i;
            mean_y += delta_y / i;
        }
        double pop_sd_x = (double) Math.sqrt(sum_sq_x / scores1.length);
        double pop_sd_y = (double) Math.sqrt(sum_sq_y / scores1.length);
        double cov_x_y = sum_coproduct / scores1.length;
        result = cov_x_y / (pop_sd_x * pop_sd_y);
        return result;
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
