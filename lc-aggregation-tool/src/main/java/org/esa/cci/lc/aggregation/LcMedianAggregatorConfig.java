package org.esa.cci.lc.aggregation;

import org.esa.beam.binning.AggregatorConfig;
import org.esa.beam.framework.gpf.annotations.Parameter;

class LcMedianAggregatorConfig extends AggregatorConfig {

    @Parameter
    private String varName;

    LcMedianAggregatorConfig() {
        super(LcMedianAggregatorDescriptor.NAME);
    }

    LcMedianAggregatorConfig(final String varName) {
        super(LcMedianAggregatorDescriptor.NAME);
        this.varName = varName;
    }

    @Override
    public String[] getVarNames() {
        return new String[]{varName};
    }
}
