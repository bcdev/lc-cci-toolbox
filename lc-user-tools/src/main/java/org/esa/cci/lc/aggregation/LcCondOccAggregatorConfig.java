package org.esa.cci.lc.aggregation;

import org.esa.beam.binning.AggregatorConfig;
import org.esa.beam.framework.gpf.annotations.Parameter;

/**
 * @author Marco Peters
 */
public class LcCondOccAggregatorConfig extends AggregatorConfig {

    @Parameter
    private String[] varNames;

    LcCondOccAggregatorConfig() {
        super(LcCondOccAggregatorDescriptor.NAME);
    }

    LcCondOccAggregatorConfig(String[] varNames) {
        super(LcCondOccAggregatorDescriptor.NAME);
        this.varNames = varNames;
    }

    @Override
    public String[] getVarNames() {
        return varNames;
    }
}
