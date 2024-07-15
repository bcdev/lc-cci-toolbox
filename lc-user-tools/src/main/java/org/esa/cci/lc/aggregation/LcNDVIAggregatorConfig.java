package org.esa.cci.lc.aggregation;

import org.esa.snap.binning.AggregatorConfig;
import org.esa.snap.core.gpf.annotations.Parameter;

/**
 * @author Marco Peters
 */
class LcNDVIAggregatorConfig extends AggregatorConfig {

    @Parameter
    private String[] sourceVarNames;

    @Parameter
    private String[] targetVarNameTemplates;

    LcNDVIAggregatorConfig() {
        super(LcNDVIAggregatorDescriptor.NAME);
    }

    LcNDVIAggregatorConfig(String[] sourceVarNames, String[] targetVarNameTemplates) {
        super(LcNDVIAggregatorDescriptor.NAME);
        this.sourceVarNames = sourceVarNames;
        this.targetVarNameTemplates = targetVarNameTemplates;
    }

    public String[] getSourceVarNames() {
        return sourceVarNames;
    }

    public String[] getTargetVarNameTemplates() {
        return targetVarNameTemplates;
    }
}
