package org.esa.cci.lc;

public interface Filter {

    void compute(double[] sourceData,
                 int sourceWidth,
                 int sourceHeight,
                 int[] flagArray,
                 /*Tile targetBandFilter,*/
                 int filterKernelRadius);
}

