package org.esa.cci.lc.aggregation;

import com.bc.ceres.binding.PropertySet;
import org.esa.beam.binning.Aggregator;
import org.esa.beam.binning.AggregatorConfig;
import org.esa.beam.binning.AggregatorDescriptor;
import org.esa.beam.binning.VariableContext;

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
    public AggregatorConfig createAggregatorConfig() {
        return new LcAggregatorConfig();
    }

    @Override
    public Aggregator createAggregator(VariableContext varCtx, AggregatorConfig aggregatorConfig) {

        PropertySet propertySet = aggregatorConfig.asPropertySet();
        int numMajorityClasses = (Integer) propertySet.getValue("numMajorityClasses");
        int numGridRows = (Integer) propertySet.getValue("numGridRows");
        boolean outputPFTClasses = (Boolean) propertySet.getValue("outputPFTClasses");
        FractionalAreaCalculator areaCalculator = (FractionalAreaCalculator) propertySet.getValue("areaCalculator");

        PftLut pftLut = null;
        if (outputPFTClasses) {
            try {
                InputStream resourceAsStream = LcAggregator.class.getResourceAsStream("Example_PFT_LUT.csv");
                InputStreamReader reader = new InputStreamReader(resourceAsStream);
                pftLut = PftLut.load(reader);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
        return new LcAggregator(numMajorityClasses, numGridRows, areaCalculator, pftLut);
    }
}
