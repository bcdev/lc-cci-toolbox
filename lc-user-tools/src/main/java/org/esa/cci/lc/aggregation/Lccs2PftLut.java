package org.esa.cci.lc.aggregation;

/**
 * @author Marco Peters
 */
public interface Lccs2PftLut {
    String getComment();

    String[] getPFTNames();

    float[][] getConversionFactors();
}
