package org.esa.cci.lc.aggregation;

import org.esa.beam.binning.Aggregator;
import org.esa.beam.binning.AggregatorConfig;
import org.esa.beam.binning.AggregatorDescriptor;
import org.esa.beam.binning.VariableContext;

public class LcMedianAggregatorDescriptor implements AggregatorDescriptor {

    public static final String NAME = "LC_Median_AGGR";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public AggregatorConfig createAggregatorConfig() {
        return new LcMedianAggregatorConfig();
    }

    @Override
    public Aggregator createAggregator(VariableContext varCtx, AggregatorConfig aggregatorConfig) {
        return new LcMedianAggregator(varCtx, aggregatorConfig.getVarNames());
    }
}
