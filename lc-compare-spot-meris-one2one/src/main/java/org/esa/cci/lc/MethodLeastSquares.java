package org.esa.cci.lc;


import java.util.Arrays;

public class MethodLeastSquares {

    public static float[] getlinearLeastSquares(float[][] sourceData, int imageLength) {

        //  y =   alpha0   +    alpha1 * x
        float[] result = new float[3];
        Arrays.fill(result, 0.f);
        float meanX = 0.f; // x= SPOT VGT P or S1 array_index=1
        float meanY = 0.f; // y= Meris array_index=0

        float sumNumerator = 0.f;
        float sumDenominator = 0.f;

        float sumNumeratorResiduum = 0.f;
        float sumDenominatorResiduum = 0.f;

        float alpha0 = 0.f;
        float alpha1 = 0.f;
        float residuum = 0.f;

        for (int i = 0; i < imageLength; i++) {
            meanX += sourceData[1][i];
            meanY += sourceData[0][i];
        }
        meanX = meanX / imageLength;
        meanY = meanY / imageLength;

        for (int i = 0; i < imageLength; i++) {
            sumNumerator = sumNumerator + ((sourceData[1][i] - meanX) * (sourceData[0][i] - meanY));
            sumDenominator = sumDenominator + ((sourceData[1][i] - meanX) * (sourceData[1][i] - meanX));
        }

        //alpha1 = sumNumerator/sumDenominator;
        //alpha0 = meanY - alpha1*meanX;

        alpha1 = 1.0f;
        alpha0 = 0.0f;


        for (int i = 0; i < imageLength; i++) {
            sumNumeratorResiduum = sumNumeratorResiduum + (((alpha1 * sourceData[1][i] + alpha0) - meanY) * ((alpha1 * sourceData[1][i] + alpha0) - meanY));
            sumDenominatorResiduum = sumDenominatorResiduum + ((sourceData[0][i] - meanY) * (sourceData[0][i] - meanY));
        }

        residuum = sumNumeratorResiduum / sumDenominatorResiduum;

        result[0] = alpha0;
        result[1] = alpha1;
        result[2] = residuum;
        return result;
    }
}
