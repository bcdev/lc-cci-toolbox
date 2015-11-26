package org.esa.cci.lc.aggregation;

class AdditionalMapPftLut implements Lccs2PftLut {

    public static Lccs2PftLut load() {
        return new AdditionalMapPftLut();
    }

    private AdditionalMapPftLut() {
        throw new IllegalStateException("Not implemented yet!");
    }

    @Override
    public String getComment() {
        return null;
    }

    @Override
    public String[] getPFTNames() {
        return new String[0];
    }

    @Override
    public float[][] getConversionFactors() {
        return new float[0][];
    }

    @Override
    public float[] getConversionFactors(int lccsClass) {
        return new float[0];
    }

    @Override
    public float[] getConversionFactors(int lccsClass, int additionalUserClass) {
        return new float[0];
    }
}
