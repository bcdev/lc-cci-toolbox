package org.esa.cci.lc.aggregation;

import org.esa.beam.binning.AggregatorConfig;
import org.esa.beam.framework.gpf.annotations.Parameter;

/**
 * @author Norman Fomferra
 */
public class LcAggregatorConfig extends AggregatorConfig {

    @Parameter
    private String varName;

    @Parameter
    int numMajorityClasses;


    public LcAggregatorConfig() {
        super(LcAggregatorDescriptor.NAME);
    }

    LcAggregatorConfig(String varName, int numMajorityClasses) {
        super(LcAggregatorDescriptor.NAME);
        this.numMajorityClasses = numMajorityClasses;
        this.varName = varName;
    }

    @Override
    public String[] getVarNames() {
        return new String[]{varName};
    }
}
