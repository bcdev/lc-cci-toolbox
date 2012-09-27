package org.esa.cci.lc.aggregation;

import com.bc.ceres.binding.PropertySet;
import org.esa.beam.binning.Aggregator;
import org.esa.beam.binning.AggregatorConfig;
import org.esa.beam.binning.AggregatorDescriptor;
import org.esa.beam.binning.VariableContext;

/**
 * @author Marco Peters
 */
public class LcAggregatorDescriptor implements AggregatorDescriptor {

    public static final String NAME = "LC_AGGR";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public AggregatorConfig createAggregatorConfig() {
        return new LcAggregatorConfig();
    }

    @Override
    public Aggregator createAggregator(VariableContext varCtx, AggregatorConfig aggregatorConfig) {

        PropertySet propertySet = aggregatorConfig.asPropertySet();
        int numMajorityClasses = (Integer) propertySet.getValue("numMajorityClasses");
        int numGridRows = (Integer) propertySet.getValue("numGridRows");
        FractionalAreaCalculator areaCalculator = (FractionalAreaCalculator) propertySet.getValue("areaCalculator");

        return new LcAggregator(numMajorityClasses, numGridRows, areaCalculator);
    }
}
