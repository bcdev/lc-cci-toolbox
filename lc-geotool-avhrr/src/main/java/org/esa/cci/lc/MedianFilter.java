package org.esa.cci.lc;

import java.util.Arrays;


public class MedianFilter implements Filter {

    static final int ALLOWED_5FILTERED_THRESHOLD = 15;
    static final int ALLOWED_3FILTERED_THRESHOLD = 6;


    @Override
    public void compute(double[] sourceData,
                        int sourceWidth,
                        int sourceHeight,
                        int[] flagArray,
                       /* Tile targetBandFilter,*/
                        int KernelRadius) {

        int KernelSize = KernelRadius * 2 + 1;
        double[][] KernelData = new double[KernelSize][KernelSize];
        double[] KernelData1Dim = new double[KernelSize * KernelSize];

        double[] preparedData = new double[sourceData.length];
        System.arraycopy(sourceData, 0, preparedData, 0, sourceData.length);


        for (int y = KernelRadius; y < sourceHeight - KernelRadius; y++) {
            for (int x = KernelRadius; x < sourceWidth - KernelRadius; x++) {

                if (!Double.isNaN(sourceData[y * (sourceWidth) + x])) {

                    int k = 0;
                    //int counter3 = 0;
                    for (int i = 0; i < KernelSize; i++) {
                        for (int j = 0; j < KernelSize; j++) {
                            KernelData[i][j]
                                  = sourceData[(y - KernelRadius + j) * (sourceWidth) + x - KernelRadius + i];
                            KernelData1Dim[k] = KernelData[i][j];
                            k++;
                        }
                    }

                            Arrays.sort(KernelData1Dim);
                            preparedData[y * (sourceWidth) + x]
                                    = KernelData1Dim[((KernelSize * KernelSize) - 1) / 2];
                            /*median*/


                    }else
                    preparedData[y * (sourceWidth) + x]=Double.NaN;
                }
            }

        /*org.esa.beam.FrontsOperator.Image2ImageRegistration(preparedData, sourceWidth, sourceHeight,
                targetBandFilter, org.esa.beam.FrontsOperator.maxShiftRadius);*/

        System.arraycopy(preparedData, 0, sourceData, 0, sourceData.length);

    }

    private int searchExtremum(int kernelradius, double[][] kernelData, int kernelSize) {
        double inputMin;
        double inputMax;
        inputMin = +Double.MAX_VALUE;
        inputMax = -Double.MAX_VALUE;

        for (int i = 0; i < kernelSize; i++) {
            for (int j = 0; j < kernelSize; j++) {
                inputMin = Math.min(inputMin, kernelData[i][j]);
                inputMax = Math.max(inputMax, kernelData[i][j]);
            }
        }

        if (inputMin == kernelData[kernelradius][kernelradius] || inputMax == kernelData[kernelradius][kernelradius]) {
            return 1;
        } else {
            return 0;
        }
    }


}
