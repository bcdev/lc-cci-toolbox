package org.esa.cci.lc.aggregation;

import org.esa.snap.binning.AggregatorConfig;
import org.esa.snap.core.gpf.annotations.Parameter;

class LcMajorityAggregatorConfig extends AggregatorConfig {

    @Parameter
    private String sourceVarName;

    @Parameter
    private String targetVarName;

    LcMajorityAggregatorConfig() {
        super(LcMajorityAggregatorDescriptor.NAME);
    }

    LcMajorityAggregatorConfig(final String sourceVarName, final String targetVarName) {
        super(LcMajorityAggregatorDescriptor.NAME);
        this.sourceVarName = sourceVarName;
        this.targetVarName = targetVarName;
    }

    public String getSourceVarName() {
        return sourceVarName;
    }

    public String getTargetVarName() {
        return targetVarName;
    }
}
