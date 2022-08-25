package org.esa.cci.lc.aggregation;

import com.bc.ceres.binding.PropertySet;
import org.esa.snap.binning.*;
import org.esa.snap.core.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
            return new LcPftAggregator(varCtx, config.varName, targetName, weightCoeff, outputCounts, outputSums);
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
            return outputSums ?
                    AbstractAggregator.createFeatureNames(targetName, "sum", "sum_sq", "weights", outputCounts ? "counts" : null) :
                    AbstractAggregator.createFeatureNames(targetName, "mean", "sigma", outputCounts ? "counts" : null);
        }
}


