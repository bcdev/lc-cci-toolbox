package org.esa.cci.lc.aggregation;

import org.esa.beam.binning.AggregatorConfig;
import org.esa.beam.framework.gpf.annotations.Parameter;

/**
 * @author Marco Peters
 */
class LcAggregatorConfig extends AggregatorConfig {

    @Parameter
    private String varName;

    @Parameter
    private boolean outputLCCSClasses;
    @Parameter
    private int numMajorityClasses;
    @Parameter
    private boolean outputPFTClasses;

    @Parameter
    private FractionalAreaCalculator areaCalculator;


    LcAggregatorConfig() {
        super(LcAggregatorDescriptor.NAME);
    }

    LcAggregatorConfig(String varName, boolean outputLCCSClasses, int numMajorityClasses,
                       boolean outputPFTClasses, FractionalAreaCalculator areaCalculator) {
        super(LcAggregatorDescriptor.NAME);
        this.varName = varName;
        this.outputLCCSClasses = outputLCCSClasses;
        this.numMajorityClasses = numMajorityClasses;
        this.outputPFTClasses = outputPFTClasses;
        this.areaCalculator = areaCalculator;
    }

    @Override
    public String[] getVarNames() {
        return new String[]{varName};
    }
}
