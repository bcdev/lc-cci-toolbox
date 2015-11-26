package org.esa.cci.lc.aggregation;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.Converter;
import org.esa.beam.binning.AggregatorConfig;
import org.esa.beam.framework.gpf.annotations.Parameter;

import java.io.File;

/**
 * @author Marco Peters
 */
class LcMapAggregatorConfig extends AggregatorConfig {

    private static final String CLASS_BAND_NAME = "lccs_class";

    @Parameter
    private boolean outputLCCSClasses;
    @Parameter
    private int numMajorityClasses;
    @Parameter
    private boolean outputPFTClasses;
    @Parameter
    private File userPFTConversionTable;
    @Parameter
    private File additionalUserMap;
    @Parameter
    private boolean outputUserMapClasses;
    @Parameter
    private File additionalUserMapPFTConversionTable;

    @Parameter(converter = AreaCalculatorConverter.class)
    private AreaCalculator areaCalculator;


    LcMapAggregatorConfig() {
        super(LcMapAggregatorDescriptor.NAME);
    }

    LcMapAggregatorConfig(boolean outputLCCSClasses, int numMajorityClasses,
                          boolean outputPFTClasses, File userPFTConversionTable,
                          File additionalUserMap, boolean outputUserMapClasses,
                          File additionalUserMapPFTConversionTable,
                          AreaCalculator areaCalculator) {
        super(LcMapAggregatorDescriptor.NAME);
        this.outputLCCSClasses = outputLCCSClasses;
        this.numMajorityClasses = numMajorityClasses;
        this.outputPFTClasses = outputPFTClasses;
        this.userPFTConversionTable = userPFTConversionTable;
        this.additionalUserMap = additionalUserMap;
        this.outputUserMapClasses = outputUserMapClasses;
        this.additionalUserMapPFTConversionTable = additionalUserMapPFTConversionTable;
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
