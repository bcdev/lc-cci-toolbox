package org.esa.cci.lc.aggregation;

import org.esa.beam.binning.AggregatorConfig;
import org.esa.beam.framework.gpf.annotations.Parameter;

/**
 * @author Marco Peters
 */
class LcNDVIAggregatorConfig extends AggregatorConfig {

    @Parameter
    private FractionalAreaCalculator areaCalculator;


    LcNDVIAggregatorConfig() {
        super(LcNDVIAggregatorDescriptor.NAME);
    }

    LcNDVIAggregatorConfig(FractionalAreaCalculator areaCalculator) {
        super(LcMapAggregatorDescriptor.NAME);
        this.areaCalculator = areaCalculator;
    }

    @Override
    public String[] getVarNames() {
        return new String[]{"ndvi_mean", "ndvi_std", "ndvi_nYearObs"};
    }
}
