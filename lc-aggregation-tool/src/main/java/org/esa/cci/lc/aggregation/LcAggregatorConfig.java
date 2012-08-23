package org.esa.cci.lc.aggregation;

import org.esa.beam.binning.operator.AggregatorConfig;
import org.esa.beam.framework.gpf.annotations.Parameter;

/**
 * @author Norman Fomferra
 */
public class LcAggregatorConfig extends AggregatorConfig {
    @Parameter
    int numMajorityClasses;

    public LcAggregatorConfig(int numMajorityClasses) {
        super(LcAggregatorDescriptor.NAME);
        this.numMajorityClasses = numMajorityClasses;
    }
}
