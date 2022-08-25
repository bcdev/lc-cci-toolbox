package org.esa.cci.lc.aggregation;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.Converter;
import org.esa.snap.binning.AggregatorConfig;
import org.esa.snap.core.gpf.annotations.Parameter;


class LcPftAggregatorConfig extends AggregatorConfig {


        @Parameter(label = "Source band name", notEmpty = true, notNull = true, description = "The source band used for aggregation.")
        String varName;
        @Parameter(label = "Target band name prefix (optional)", description = "The name prefix for the resulting bands. If empty, the source band name is used.")
        String targetName;
        @Parameter(label = "Weight coefficient", defaultValue = "1.0",
                description = "The number of spatial observations to the power of this value \n" +
                        "will define the value for weighting the sums. Zero means observation count weighting is disabled.")
        Double weightCoeff;
        @Parameter(defaultValue = "false",
                description = "If true, the result will include the count of all valid values.")
        Boolean outputCounts;
        @Parameter(defaultValue = "false",
                description = "If true, the result will include the sum of all values.")
        Boolean outputSums;

        public LcPftAggregatorConfig() {
            this(null, null, null, null, null);
        }

        public LcPftAggregatorConfig(String varName) {
            this(varName, null, null, null, null);
        }

        public LcPftAggregatorConfig(String varName, String targetName, Double weightCoeff, Boolean outputCounts, Boolean outputSums) {
            //AggregatorConfig.NAME;
            this.varName = varName;
            this.targetName = targetName;
            this.weightCoeff = weightCoeff;
            this.outputCounts = outputCounts;
            this.outputSums = outputSums;
        }
}

