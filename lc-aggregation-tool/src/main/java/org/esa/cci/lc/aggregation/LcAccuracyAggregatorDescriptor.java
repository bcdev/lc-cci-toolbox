package org.esa.cci.lc.aggregation;

import org.esa.beam.binning.Aggregator;
import org.esa.beam.binning.AggregatorConfig;
import org.esa.beam.binning.AggregatorDescriptor;
import org.esa.beam.binning.VariableContext;

public class LcAccuracyAggregatorDescriptor implements AggregatorDescriptor {

    public static final String NAME = "LC_ACCURACY_AGGR";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public AggregatorConfig createAggregatorConfig() {
        return new LcAccuracyAggregatorConfig();
    }

    @Override
    public Aggregator createAggregator(VariableContext varCtx, AggregatorConfig aggregatorConfig) {
        return new LcAccuracyAggregator(varCtx, aggregatorConfig.getVarNames());
    }
}
