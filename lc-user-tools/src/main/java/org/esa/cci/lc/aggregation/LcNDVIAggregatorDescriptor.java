package org.esa.cci.lc.aggregation;

import org.esa.beam.binning.Aggregator;
import org.esa.beam.binning.AggregatorConfig;
import org.esa.beam.binning.AggregatorDescriptor;
import org.esa.beam.binning.VariableContext;

/**
 * @author Marco Peters
 */
public class LcNDVIAggregatorDescriptor implements AggregatorDescriptor {

    public static final String NAME = "LC_NDVI_AGGR";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public AggregatorConfig createConfig() {
        return new LcNDVIAggregatorConfig();
    }

    @Override
    public Aggregator createAggregator(VariableContext varCtx, AggregatorConfig aggregatorConfig) {
        return new LcNDVIAggregator(varCtx,
                                    getSourceVarNames(aggregatorConfig),
                                    getTargetVarNames(aggregatorConfig));
    }

    @Override
    public String[] getSourceVarNames(AggregatorConfig aggregatorConfig) {
        return ((LcNDVIAggregatorConfig) aggregatorConfig).getSourceVarNames();
    }

    @Override
    public String[] getTargetVarNames(AggregatorConfig aggregatorConfig) {
        String[] targetVarNameTemplates = ((LcNDVIAggregatorConfig) aggregatorConfig).getTargetVarNameTemplates();
        return createFeatureNames(getSourceVarNames(aggregatorConfig), targetVarNameTemplates);
    }

    private static String[] createFeatureNames(String[] sourceVarNames, String[] targetVarNameTemplates) {
        String[] featureNames = new String[targetVarNameTemplates.length];
        featureNames[0] = String.format(targetVarNameTemplates[0], sourceVarNames[0]);
        featureNames[1] = String.format(targetVarNameTemplates[1], sourceVarNames[1]);
        return featureNames;
    }

}
