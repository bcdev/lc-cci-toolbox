package org.esa.cci.lc.aggregation;

import com.bc.ceres.binding.PropertySet;
import org.esa.beam.binning.Aggregator;
import org.esa.beam.binning.AggregatorConfig;
import org.esa.beam.binning.AggregatorDescriptor;
import org.esa.beam.binning.VariableContext;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * @author Marco Peters
 */
public class LcAggregatorDescriptor implements AggregatorDescriptor {

    public static final String NAME = "LC_AGGR";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public AggregatorConfig createConfig() {
        return new LcAggregatorConfig();
    }

    @Override
    public Aggregator createAggregator(VariableContext varCtx, AggregatorConfig aggregatorConfig) {

        PropertySet propertySet = aggregatorConfig.asPropertySet();
        boolean outputLCCSClasses = (Boolean) propertySet.getValue("outputLCCSClasses");
        int numMajorityClasses = (Integer) propertySet.getValue("numMajorityClasses");
        boolean outputPFTClasses = (Boolean) propertySet.getValue("outputPFTClasses");
        File userPFTConversionTable = (File) propertySet.getValue("userPFTConversionTable");
        FractionalAreaCalculator areaCalculator = (FractionalAreaCalculator) propertySet.getValue("areaCalculator");

        PftLut pftLut = null;
        if (outputPFTClasses) {
            try {
                InputStream resourceAsStream;
                if (userPFTConversionTable != null) {
                    resourceAsStream = new FileInputStream(userPFTConversionTable);
                } else {
                    resourceAsStream = LcAggregator.class.getResourceAsStream("Default_LCCS2PFT_LUT.csv");
                }
                InputStreamReader reader = new InputStreamReader(resourceAsStream);
                pftLut = PftLut.load(reader);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
        return new LcAggregator(outputLCCSClasses, numMajorityClasses, areaCalculator, pftLut);
    }
}
