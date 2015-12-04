package org.esa.cci.lc.aggregation;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

/**
 * A builder for setting up a {@link Lccs2PftLut Lccs2Pft-Look-Up-Table}
 */
public class Lccs2PftLutBuilder {

    public static final String DEFAULT_LCCS2_PFT_LUT_FILENAME = "Default_LCCS2PFT_LUT.csv";

    static final float DEFAULT_SCALE_FACTOR = 1.0f;

    private Reader lutReader;
    private float scaleFactor;
    private Reader additionalLutReader;

    public Lccs2PftLutBuilder() {
        this.lutReader = null;
        additionalLutReader = null;
        this.scaleFactor = DEFAULT_SCALE_FACTOR;
    }

    /**
     * @param lccs2PftLutReader reader which is used to read in the Lccs2Pft-Look-Up-Table.
     * @return the current builder
     */
    public Lccs2PftLutBuilder useLccs2PftTable(Reader lccs2PftLutReader) {
        lutReader = lccs2PftLutReader;
        return this;
    }

    public Lccs2PftLutBuilder useAdditionalUserMap(Reader additionalMapLutReader) {
        additionalLutReader = additionalMapLutReader;
        return this;
    }

    /**
     * @param scaleFactor used to scale the values read from the table. Default is {@code 1.0}.
     * @return the current builder
     */
    public Lccs2PftLutBuilder useScaleFactor(float scaleFactor) {
        this.scaleFactor = scaleFactor;
        return this;
    }

    public Lccs2PftLut create() throws Lccs2PftLutException {
        final Reader reader = getLutReader();

        Lccs2PftLut lut = BasicPftLut.load(reader, getScaleFactor());
        if (additionalLutReader != null) {
            lut = AdditionalMapPftLut.create(lut, additionalLutReader, scaleFactor);
        }
        return lut;
    }

    private Reader getLutReader() {
        if (this.lutReader == null) {
            final InputStream inputStream = Lccs2PftLutBuilder.class.getResourceAsStream(Lccs2PftLutBuilder.DEFAULT_LCCS2_PFT_LUT_FILENAME);
            return new InputStreamReader(inputStream);
        } else {
            return this.lutReader;
        }
    }

    private Reader getAdditionalLutReader() {
        return additionalLutReader;
    }

    private float getScaleFactor() {
        return scaleFactor;
    }

}
