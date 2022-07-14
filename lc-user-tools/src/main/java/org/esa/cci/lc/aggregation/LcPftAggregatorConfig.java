package org.esa.cci.lc.aggregation;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.Converter;
import org.esa.snap.binning.AggregatorConfig;
import org.esa.snap.core.gpf.annotations.Parameter;


public class LcPftAggregatorConfig extends AggregatorConfig {

    private static final String CLASS_BAND_NAME = "pft_class";

    @Parameter
    private int numMajorityClasses;
    @Parameter
    private boolean outputPftClasses;
    @Parameter(converter = AreaCalculatorConverter.class)
    private AreaCalculator areaCalculator;


    LcPftAggregatorConfig() {
        super(LcPftAggregatorDescriptor.NAME);
    }

    LcPftAggregatorConfig( int numMajorityClasses, AreaCalculator areaCalculator) {
        super(LcPftAggregatorDescriptor.NAME);
        //this.outputPftClasses = outputPftClasses;
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
