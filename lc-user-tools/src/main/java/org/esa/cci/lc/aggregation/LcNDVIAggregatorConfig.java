package org.esa.cci.lc.aggregation;

import org.esa.beam.binning.AggregatorConfig;
import org.esa.beam.framework.gpf.annotations.Parameter;

/**
 * @author Marco Peters
 */
class LcNDVIAggregatorConfig extends AggregatorConfig {

    @Parameter
    private String[] varNames;

    LcNDVIAggregatorConfig() {
        super(LcNDVIAggregatorDescriptor.NAME);
    }

    LcNDVIAggregatorConfig(String[] varNames) {
        super(LcNDVIAggregatorDescriptor.NAME);
        this.varNames = varNames;
    }

    @Override
    public String[] getVarNames() {
        return varNames;
    }
}
