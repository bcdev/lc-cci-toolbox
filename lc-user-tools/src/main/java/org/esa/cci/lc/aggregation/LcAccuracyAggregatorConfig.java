package org.esa.cci.lc.aggregation;

import org.esa.snap.binning.AggregatorConfig;
import org.esa.snap.core.gpf.annotations.Parameter;

class LcAccuracyAggregatorConfig extends AggregatorConfig {

    @Parameter
    private String sourceVarName;

    @Parameter
    private String targetVarName;

    LcAccuracyAggregatorConfig() {
        super(LcAccuracyAggregatorDescriptor.NAME);
    }

    LcAccuracyAggregatorConfig(final String sourceVarName, final String targetVarName) {
        super(LcAccuracyAggregatorDescriptor.NAME);
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
