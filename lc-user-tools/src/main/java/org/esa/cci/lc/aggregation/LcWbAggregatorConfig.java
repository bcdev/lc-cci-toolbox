package org.esa.cci.lc.aggregation;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.Converter;
import org.esa.beam.binning.AggregatorConfig;
import org.esa.beam.framework.gpf.annotations.Parameter;

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


    LcWbAggregatorConfig() {
        super(LcWbAggregatorDescriptor.NAME);
    }

    LcWbAggregatorConfig(boolean outputWbClasses, int numMajorityClasses, AreaCalculator areaCalculator) {
        super(LcWbAggregatorDescriptor.NAME);
        this.outputWbClasses = outputWbClasses;
        this.numMajorityClasses = numMajorityClasses;
        this.areaCalculator = areaCalculator;
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
