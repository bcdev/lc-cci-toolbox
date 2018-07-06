package org.esa.cci.lc.aggregation;

import org.esa.snap.binning.Aggregator;
import org.esa.snap.binning.AggregatorConfig;
import org.esa.snap.binning.AggregatorDescriptor;
import org.esa.snap.binning.VariableContext;

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
        String[] sourceVarNames = getSourceVarNames(aggregatorConfig);
        String[] targetVarNameTemplates = getTargetVarNames(aggregatorConfig);
        return new LcCondOccAggregator(varCtx,
                                       sourceVarNames,
                                       createFeatureNames(sourceVarNames, targetVarNameTemplates));
    }

    @Override
    public String[] getSourceVarNames(AggregatorConfig aggregatorConfig) {
        LcCondOccAggregatorConfig config = (LcCondOccAggregatorConfig) aggregatorConfig;
        return config.getSourceVarNames();
    }

    @Override
    public String[] getTargetVarNames(AggregatorConfig aggregatorConfig) {
        LcCondOccAggregatorConfig config = (LcCondOccAggregatorConfig) aggregatorConfig;
        return config.getTargetVarNameTemplates();
    }

    private static String[] createFeatureNames(String[] varNames, String[] targetVarNameTemplates) {
        String[] featureNames = new String[targetVarNameTemplates.length];
        featureNames[0] = String.format(targetVarNameTemplates[0], varNames[0]);
        featureNames[1] = String.format(targetVarNameTemplates[1], varNames[0]);
        featureNames[2] = String.format(targetVarNameTemplates[2], varNames[1]);
        return featureNames;
    }

}
