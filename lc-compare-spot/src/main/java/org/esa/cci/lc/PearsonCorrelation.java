package org.esa.cci.lc;


public class PearsonCorrelation {

    public static float getPearsonCorrelation(float[][] scores, int imageLength) {
        float result = 0.f;
        float sum_sq_x = 0.f;
        float sum_sq_y = 0.f;
        float sum_coproduct = 0.f;
        float mean_x = scores[0][0];
        float mean_y = scores[1][0];

        for (int i = 2; i < imageLength; i += 1) {
            float sweep = Float.valueOf(i - 1) / i;
            float delta_x = scores[0][i - 1] - mean_x;
            float delta_y = scores[1][i - 1] - mean_y;
            sum_sq_x += delta_x * delta_x * sweep;
            sum_sq_y += delta_y * delta_y * sweep;
            sum_coproduct += delta_x * delta_y * sweep;
            mean_x += delta_x / i;
            mean_y += delta_y / i;
        }
        float pop_sd_x = (float) Math.sqrt(sum_sq_x / imageLength);
        float pop_sd_y = (float) Math.sqrt(sum_sq_y / imageLength);
        float cov_x_y = sum_coproduct / imageLength;
        result = cov_x_y / (pop_sd_x * pop_sd_y);
        return result;
    }
}

