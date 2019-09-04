package org.esa.cci.lc.aggregation;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.Converter;
import org.esa.snap.binning.AggregatorConfig;
import org.esa.snap.core.gpf.annotations.Parameter;

/**
 * @author Marco Peters
 */
class LcWbAggregatorConfig extends AggregatorConfig {

    private static final String CLASS_BAND_NAME = "wb_class";

    @Parameter
    private int numMajorityClasses;
    @Parameter
    private boolean outputWbClasses;

    @Parameter(converter = AreaCalculatorConverter.class)
    private AreaCalculator areaCalculator;

    @Parameter
    private int numWbClasses;

    LcWbAggregatorConfig() {
        super(LcWbAggregatorDescriptor.NAME);
    }

    LcWbAggregatorConfig(boolean outputWbClasses, int numMajorityClasses, int numWbClasses, AreaCalculator areaCalculator) {
        super(LcWbAggregatorDescriptor.NAME);
        this.outputWbClasses = outputWbClasses;
        this.numMajorityClasses = numMajorityClasses;
        this.areaCalculator = areaCalculator;
        this.numWbClasses = numWbClasses;
    }

    public String getSourceVarName() {
        return CLASS_BAND_NAME;
    }

    public static class AreaCalculatorConverter implements Converter<AreaCalculator> {

        @Override
        public Class<? extends AreaCalculator> getValueType() {
            return AreaCalculator.class;
        }

        @Override
        public AreaCalculator parse(String s) throws ConversionException {
            throw new IllegalStateException("Not implemented.");
        }

        @Override
        public String format(AreaCalculator areaCalculator) {
            return areaCalculator.getClass().getName();
        }
    }
}
