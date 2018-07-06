package org.esa.cci.lc.aggregation;

import com.bc.ceres.binding.PropertySet;
import org.esa.snap.binning.Aggregator;
import org.esa.snap.binning.AggregatorConfig;
import org.esa.snap.binning.AggregatorDescriptor;
import org.esa.snap.binning.VariableContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Marco Peters
 */
public class LcWbAggregatorDescriptor implements AggregatorDescriptor {

    public static final String NAME = "LC_WB_AGGR";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public AggregatorConfig createConfig() {
        return new LcWbAggregatorConfig();
    }

    @Override
    public Aggregator createAggregator(VariableContext varCtx, AggregatorConfig aggregatorConfig) {

        PropertySet propertySet = aggregatorConfig.asPropertySet();
        int numMajorityClasses = propertySet.getValue("numMajorityClasses");
        boolean outputWbClasses = propertySet.getValue("outputWbClasses");
        AreaCalculator areaCalculator = propertySet.getValue("areaCalculator");

        String[] spatialFeatureNames = createSpatialFeatureNames();
        String[] outputFeatureNames = createOutputFeatureNames(outputWbClasses, numMajorityClasses, spatialFeatureNames);
        return new LcWbAggregator(numMajorityClasses, outputWbClasses, areaCalculator, spatialFeatureNames, outputFeatureNames);
    }

    @Override
    public String[] getSourceVarNames(AggregatorConfig aggregatorConfig) {
        return new String[]{((LcWbAggregatorConfig) aggregatorConfig).getSourceVarName()};
    }

    @Override
    public String[] getTargetVarNames(AggregatorConfig aggregatorConfig) {
        PropertySet propertySet = aggregatorConfig.asPropertySet();
        int numMajorityClasses = propertySet.getValue("numMajorityClasses");
        boolean outputWbClasses = propertySet.getValue("outputWbClasses");
        String[] spatialFeatureNames = createSpatialFeatureNames();
        return createOutputFeatureNames(outputWbClasses, numMajorityClasses, spatialFeatureNames);
    }

    private static String[] createSpatialFeatureNames() {
        return new String[]{"class_area_invalid", "class_area_terrestrial", "class_area_water"};
    }

    private static String[] createOutputFeatureNames(boolean outputWbClasses, int numMajorityClasses,
                                                     String[] spatialFeatureNames) {
        List<String> outputFeatureNames = new ArrayList<>();
        if (outputWbClasses) {
            outputFeatureNames.addAll(Arrays.asList(spatialFeatureNames));
        }
        for (int i = 1; i <= numMajorityClasses; i++) {
            outputFeatureNames.add("majority_class_" + i);
        }
        return outputFeatureNames.toArray(new String[outputFeatureNames.size()]);
    }


}
