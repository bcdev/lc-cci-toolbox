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
    private int numMajorityClasses;
    @Parameter
    private int numGridRows;

    @Parameter
    private FractionalAreaCalculator areaCalculator;


    LcAggregatorConfig() {
        super(LcAggregatorDescriptor.NAME);
    }

    LcAggregatorConfig(String varName, int numMajorityClasses, int numGridRows,
                       FractionalAreaCalculator areaCalculator) {
        super(LcAggregatorDescriptor.NAME);
        this.numMajorityClasses = numMajorityClasses;
        this.numGridRows = numGridRows;
        this.varName = varName;
        this.areaCalculator = areaCalculator;
    }

    @Override
    public String[] getVarNames() {
        return new String[]{varName};
    }
}
