package org.esa.cci.lc.aggregation;

import org.esa.beam.binning.AggregatorConfig;
import org.esa.beam.framework.gpf.annotations.Parameter;

/**
 * @author Marco Peters
 */
class LcBinIndexAggregatorConfig extends AggregatorConfig {

    @Parameter
    private String varName;


    LcBinIndexAggregatorConfig() {
        super(LcBinIndexAggregatorDescriptor.NAME);
    }

    LcBinIndexAggregatorConfig(String varName) {
        super(LcBinIndexAggregatorDescriptor.NAME);
        this.varName = varName;
    }

    @Override
    public String[] getVarNames() {
        return new String[]{varName};
    }

}
