package org.esa.cci.lc.aggregation;

import org.esa.snap.binning.Aggregator;
import org.esa.snap.binning.AggregatorConfig;
import org.esa.snap.binning.AggregatorDescriptor;
import org.esa.snap.binning.VariableContext;

public class LcAccuracyAggregatorDescriptor implements AggregatorDescriptor {

    public static final String NAME = "LC_ACCURACY_AGGR";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public AggregatorConfig createConfig() {
        return new LcAccuracyAggregatorConfig();
    }

    @Override
    public Aggregator createAggregator(VariableContext varCtx, AggregatorConfig aggregatorConfig) {
        return new LcAccuracyAggregator(varCtx,
                                        getSourceVarNames(aggregatorConfig),
                                        getTargetVarNames(aggregatorConfig),
                                        ((LcAccuracyAggregatorConfig)aggregatorConfig).getRowRatio());
    }

    @Override
    public String[] getSourceVarNames(AggregatorConfig aggregatorConfig) {
        LcAccuracyAggregatorConfig config = (LcAccuracyAggregatorConfig) aggregatorConfig;
        return new String[]{config.getSourceVarName()};
    }

    @Override
    public String[] getTargetVarNames(AggregatorConfig aggregatorConfig) {
        LcAccuracyAggregatorConfig config = (LcAccuracyAggregatorConfig) aggregatorConfig;
        return new String[]{config.getTargetVarName()};
    }
}
