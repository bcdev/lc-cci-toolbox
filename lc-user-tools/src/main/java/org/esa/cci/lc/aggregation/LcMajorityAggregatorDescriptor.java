package org.esa.cci.lc.aggregation;

import org.esa.beam.binning.Aggregator;
import org.esa.beam.binning.AggregatorConfig;
import org.esa.beam.binning.AggregatorDescriptor;
import org.esa.beam.binning.VariableContext;

public class LcMajorityAggregatorDescriptor implements AggregatorDescriptor {

    public static final String NAME = "LC_MAJORITY_AGGR";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public AggregatorConfig createConfig() {
        return new LcMajorityAggregatorConfig();
    }

    @Override
    public Aggregator createAggregator(VariableContext varCtx, AggregatorConfig aggregatorConfig) {
        return new LcMajorityAggregator(varCtx,
                                        getSourceVarNames(aggregatorConfig),
                                        getTargetVarNames(aggregatorConfig));
    }

    @Override
    public String[] getSourceVarNames(AggregatorConfig aggregatorConfig) {
        LcMajorityAggregatorConfig config = (LcMajorityAggregatorConfig) aggregatorConfig;
        return new String[]{config.getSourceVarName()};
    }

    @Override
    public String[] getTargetVarNames(AggregatorConfig aggregatorConfig) {
        LcMajorityAggregatorConfig config = (LcMajorityAggregatorConfig) aggregatorConfig;
        return new String[]{config.getTargetVarName()};
    }
}
