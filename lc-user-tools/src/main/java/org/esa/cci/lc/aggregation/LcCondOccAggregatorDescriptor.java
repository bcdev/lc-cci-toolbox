package org.esa.cci.lc.aggregation;

import org.esa.beam.binning.Aggregator;
import org.esa.beam.binning.AggregatorConfig;
import org.esa.beam.binning.AggregatorDescriptor;
import org.esa.beam.binning.VariableContext;

/**
 * @author Marco Peters
 */
public class LcCondOccAggregatorDescriptor implements AggregatorDescriptor {

    public static final String NAME = "LC_COND_OCC_AGGR";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public AggregatorConfig createConfig() {
        return new LcCondOccAggregatorConfig();
    }

    @Override
    public Aggregator createAggregator(VariableContext varCtx, AggregatorConfig aggregatorConfig) {
        return new LcCondOccAggregator(varCtx, aggregatorConfig.getVarNames());
    }
}
