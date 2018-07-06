package org.esa.cci.lc.aggregation;

import org.esa.snap.binning.AggregatorConfig;
import org.esa.snap.core.gpf.annotations.Parameter;

/**
 * @author Marco Peters
 */
public class LcCondOccAggregatorConfig extends AggregatorConfig {

    @Parameter
    private String[] sourceVarNames;

    @Parameter
    private String[] targetVarNameTemplates;

    LcCondOccAggregatorConfig() {
        super(LcCondOccAggregatorDescriptor.NAME);
    }

    LcCondOccAggregatorConfig(String[] sourceVarNames, String[] targetVarNameTemplates) {
        super(LcCondOccAggregatorDescriptor.NAME);
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
