package org.esa.cci.lc.aggregation;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.Converter;
import org.esa.snap.binning.AggregatorConfig;
import org.esa.snap.core.gpf.annotations.Parameter;

import java.net.URL;

public class LcPftAggregatorConfig extends AggregatorConfig {

    @Parameter
    private boolean outputLCCSClasses;
    @Parameter
    private int numMajorityClasses;
    @Parameter
    private boolean outputPFTClasses;
    @Parameter
    private URL userPFTConversionTable;
    @Parameter
    private URL additionalUserMap;
    @Parameter
    private boolean outputUserMapClasses;
    @Parameter
    private URL additionalUserMapPFTConversionTable;

    @Parameter(converter = LcPftAggregatorConfig.AreaCalculatorConverter.class)

    private AreaCalculator areaCalculator;


    LcPftAggregatorConfig() {
        super(LcPftAggregatorDescriptor.NAME);
    }

    LcPftAggregatorConfig(AreaCalculator areaCalculator) {
        super(LcPftAggregatorDescriptor.NAME);
        this.areaCalculator = areaCalculator;
    }

    private static final String[] listPFTVariables = {"BARE","BUILT","GRASS-MAN","GRASS-NAT","SHRUBS-BD","SHRUBS-BE","SHRUBS-ND","SHRUBS-NE","WATER_INLAND",
            "SNOWICE","TREES-BD","TREES-BE","TREES-ND","TREES-NE","WATER","LAND","WATER_OCEAN"};

    public String[] getSourceVarName() {
        return listPFTVariables;
    }


    public URL getAdditionalUserMap() {
        return additionalUserMap;
    }

    public URL getAdditionalUserMapPFTConversionTable() {
        return additionalUserMapPFTConversionTable;
    }

    public AreaCalculator getAreaCalculator() {
        return areaCalculator;
    }

    public int getNumMajorityClasses() {
        return numMajorityClasses;
    }

    public boolean isOutputLCCSClasses() {
        return outputLCCSClasses;
    }

    public boolean isOutputPFTClasses() {
        return outputPFTClasses;
    }

    public boolean isOutputUserMapClasses() {
        return outputUserMapClasses;
    }

    public URL getUserPFTConversionTable() {
        return userPFTConversionTable;
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
