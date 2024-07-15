package org.esa.cci.lc.aggregation;

import org.esa.snap.binning.*;
import org.esa.snap.core.util.StringUtils;


public class LcPftAggregatorDescriptor implements AggregatorDescriptor {


        public static final String NAME = "PFT_AGG";

        @Override
        public String getName() {
            return NAME;
        }

        @Override
        public Aggregator createAggregator(VariableContext varCtx, AggregatorConfig aggregatorConfig) {
            LcPftAggregatorConfig config = (LcPftAggregatorConfig) aggregatorConfig;
            String targetName = StringUtils.isNotNullAndNotEmpty(config.targetName) ? config.targetName : config.varName;
            double weightCoeff = config.weightCoeff != null ? config.weightCoeff : 0.0;
            boolean outputCounts = config.outputCounts != null ? config.outputCounts : false;
            boolean outputSums = config.outputSums != null ? config.outputSums : false;
            AreaCalculator areaCalculator = config.areaCalculator;
            return new LcPftAggregator(varCtx, config.varName, targetName, weightCoeff, outputCounts, outputSums, areaCalculator);
        }

        @Override
        public AggregatorConfig createConfig() {
            return new LcPftAggregatorConfig();
        }

        @Override
        public String[] getSourceVarNames(AggregatorConfig aggregatorConfig) {
            LcPftAggregatorConfig config = (LcPftAggregatorConfig) aggregatorConfig;
            return new String[]{config.varName};
        }

        @Override
        public String[] getTargetVarNames(AggregatorConfig aggregatorConfig) {
            LcPftAggregatorConfig config = (LcPftAggregatorConfig) aggregatorConfig;
            String targetName = StringUtils.isNotNullAndNotEmpty(config.targetName) ? config.targetName : config.varName;
            boolean outputCounts = config.outputCounts != null ? config.outputCounts : false;
            boolean outputSums = config.outputSums != null ? config.outputSums : false;
            return AbstractAggregator.createFeatureNames(targetName, "mean-2", "sigma-2", outputCounts ? "counts" : null);
        }
}


