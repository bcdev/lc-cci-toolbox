package org.esa.cci.lc.aggregation;

import org.esa.beam.binning.AggregatorConfig;
import org.esa.beam.framework.gpf.annotations.Parameter;

import java.io.File;

/**
 * @author Marco Peters
 */
class LcMapAggregatorConfig extends AggregatorConfig {

    private static final String CLASS_BAND_NAME = "lccs_class";

    @Parameter
    private boolean outputLCCSClasses;
    @Parameter
    private int numMajorityClasses;
    @Parameter
    private boolean outputPFTClasses;
    @Parameter
    private File userPFTConversionTable;

    @Parameter
    private AreaCalculator areaCalculator;


    LcMapAggregatorConfig() {
        super(LcMapAggregatorDescriptor.NAME);
    }

    LcMapAggregatorConfig(boolean outputLCCSClasses, int numMajorityClasses,
                          boolean outputPFTClasses, File userPFTConversionTable,
                          AreaCalculator areaCalculator) {
        super(LcMapAggregatorDescriptor.NAME);
        this.outputLCCSClasses = outputLCCSClasses;
        this.numMajorityClasses = numMajorityClasses;
        this.outputPFTClasses = outputPFTClasses;
        this.userPFTConversionTable = userPFTConversionTable;
        this.areaCalculator = areaCalculator;
    }

    @Override
    public String[] getVarNames() {
        return new String[]{CLASS_BAND_NAME};
    }
}
