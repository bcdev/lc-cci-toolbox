package org.esa.cci.lc.aggregation;

import org.esa.beam.binning.AggregatorConfig;
import org.esa.beam.framework.gpf.annotations.Parameter;

/**
 * @author Marco Peters
 */
class LcCondAggregatorConfig extends AggregatorConfig {

    @Parameter
    private String varName;

    @Parameter
    private FractionalAreaCalculator areaCalculator;


    LcCondAggregatorConfig() {
        super(LcCondAggregatorDescriptor.NAME);
    }

    LcCondAggregatorConfig(String varName, FractionalAreaCalculator areaCalculator) {
        super(LcMapAggregatorDescriptor.NAME);
        this.varName = varName;
        this.areaCalculator = areaCalculator;
    }

    @Override
    public String[] getVarNames() {
        return new String[]{varName};
    }
}
