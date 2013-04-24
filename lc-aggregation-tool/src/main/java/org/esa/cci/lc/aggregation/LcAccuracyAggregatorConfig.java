package org.esa.cci.lc.aggregation;

import org.esa.beam.binning.AggregatorConfig;
import org.esa.beam.framework.gpf.annotations.Parameter;

class LcAccuracyAggregatorConfig extends AggregatorConfig {

    @Parameter
    private String varName;

    LcAccuracyAggregatorConfig() {
        super(LcAccuracyAggregatorDescriptor.NAME);
    }

    LcAccuracyAggregatorConfig(final String varName) {
        super(LcAccuracyAggregatorDescriptor.NAME);
        this.varName = varName;
    }

    @Override
    public String[] getVarNames() {
        return new String[]{varName};
    }
}
