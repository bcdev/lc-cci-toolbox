package org.esa.cci.lc.aggregation;

import com.bc.ceres.binding.PropertySet;
import org.esa.snap.binning.Aggregator;
import org.esa.snap.binning.AggregatorConfig;
import org.esa.snap.binning.AggregatorDescriptor;
import org.esa.snap.binning.VariableContext;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LcPftAggregatorDescriptor implements AggregatorDescriptor {

    private static final LCCS LCCS_CLASSES = LCCS.getInstance();

    public static final String NAME = "LC_PFT_AGGR";
    private static final String[] listPFTVariables = {"BARE","BUILT","GRASS-MAN","GRASS-NAT","SHRUBS-BD","SHRUBS-BE","SHRUBS-ND","SHRUBS-NE","WATER_INLAND",
            "SNOWICE","TREES-BD","TREES-BE","TREES-ND","TREES-NE","WATER","LAND","WATER_OCEAN"};

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public AggregatorConfig createConfig() {
        return new LcPftAggregatorConfig();
    }

    @Override
    public Aggregator createAggregator(VariableContext varCtx, AggregatorConfig aggregatorConfig) {

        PropertySet propertySet = aggregatorConfig.asPropertySet();
        int numMajorityClasses = propertySet.getValue("numMajorityClasses");
        AreaCalculator areaCalculator = propertySet.getValue("areaCalculator");

        String[] spatialFeatureNames;
        spatialFeatureNames = createSpatialFeatureNames();


        String[] outputFeatureNames = createOutputFeatureNames();
        return new LcPftAggregator(numMajorityClasses, areaCalculator, spatialFeatureNames, outputFeatureNames);
    }

    @Override
    public String[] getSourceVarNames(AggregatorConfig aggregatorConfig) {
        if (!(aggregatorConfig instanceof LcPftAggregatorConfig)) {
            throw new IllegalStateException("!(aggregatorConfig instanceof LcMapAggregatorConfig)");
        }
        return listPFTVariables;
    }

    @Override
    public String[] getTargetVarNames(AggregatorConfig aggregatorConfig) {
        return createSpatialFeatureNames();
    }

    private static String[] createSpatialFeatureNames() {
        String[] spatialFeatureNames = new String[listPFTVariables.length];

        for (int i = 0; i < listPFTVariables.length; i++) {
            spatialFeatureNames[i] = "class_area_" + listPFTVariables[i];
        }
        return spatialFeatureNames;
    }

    private static String[] createOutputFeatureNames() {
        List<String> outputFeatureNames = new ArrayList<>();

        outputFeatureNames.addAll(Arrays.asList(listPFTVariables));

        return outputFeatureNames.toArray(new String[outputFeatureNames.size()]);
    }

}
