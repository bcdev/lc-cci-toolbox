package org.esa.cci.lc.aggregation;

import org.esa.beam.binning.AggregatorConfig;

/**
 * @author Marco Peters
 */
class LcNDVIAggregatorConfig extends AggregatorConfig {

    LcNDVIAggregatorConfig() {
        super(LcNDVIAggregatorDescriptor.NAME);
    }

    @Override
    public String[] getVarNames() {
        return new String[]{"ndvi_mean", "ndvi_std", "ndvi_nYearObs"};
    }
}
