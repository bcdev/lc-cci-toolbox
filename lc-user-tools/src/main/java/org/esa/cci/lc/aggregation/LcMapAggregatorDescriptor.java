package org.esa.cci.lc.aggregation;

import com.bc.ceres.binding.PropertySet;
import org.esa.beam.binning.Aggregator;
import org.esa.beam.binning.AggregatorConfig;
import org.esa.beam.binning.AggregatorDescriptor;
import org.esa.beam.binning.VariableContext;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Marco Peters
 */
public class LcMapAggregatorDescriptor implements AggregatorDescriptor {

    private static final LCCS LCCS_CLASSES = LCCS.getInstance();

    public static final String NAME = "LC_MAP_AGGR";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public AggregatorConfig createConfig() {
        return new LcMapAggregatorConfig();
    }

    @Override
    public Aggregator createAggregator(VariableContext varCtx, AggregatorConfig aggregatorConfig) {

        PropertySet propertySet = aggregatorConfig.asPropertySet();
        boolean outputLCCSClasses = (Boolean) propertySet.getValue("outputLCCSClasses");
        int numMajorityClasses = (Integer) propertySet.getValue("numMajorityClasses");
        boolean outputPFTClasses = (Boolean) propertySet.getValue("outputPFTClasses");
        File userPFTConversionTable = (File) propertySet.getValue("userPFTConversionTable");
        AreaCalculator areaCalculator = (AreaCalculator) propertySet.getValue("areaCalculator");

        Lccs2PftLut pftLut = getPftLut(outputPFTClasses, userPFTConversionTable);

        String[] spatialFeatureNames = createSpatialFeatureNames();
        String[] outputFeatureNames = createOutputFeatureNames(outputLCCSClasses, numMajorityClasses, pftLut, spatialFeatureNames);
        return new LcMapAggregator(outputLCCSClasses, numMajorityClasses, areaCalculator, pftLut, spatialFeatureNames, outputFeatureNames);
    }

    @Override
    public String[] getSourceVarNames(AggregatorConfig aggregatorConfig) {
        return new String[]{((LcMapAggregatorConfig) aggregatorConfig).getSourceVarName()};
    }

    @Override
    public String[] getTargetVarNames(AggregatorConfig aggregatorConfig) {
        PropertySet propertySet = aggregatorConfig.asPropertySet();
        boolean outputLCCSClasses = (Boolean) propertySet.getValue("outputLCCSClasses");
        int numMajorityClasses = (Integer) propertySet.getValue("numMajorityClasses");
        boolean outputPFTClasses = (Boolean) propertySet.getValue("outputPFTClasses");
        File userPFTConversionTable = (File) propertySet.getValue("userPFTConversionTable");

        Lccs2PftLut pftLut = getPftLut(outputPFTClasses, userPFTConversionTable);
        String[] spatialFeatureNames = createSpatialFeatureNames();
        return createOutputFeatureNames(outputLCCSClasses, numMajorityClasses, pftLut, spatialFeatureNames);
    }

    private static Lccs2PftLut getPftLut(boolean outputPFTClasses, File userPFTConversionTable) {
        Lccs2PftLut pftLut = null;
        if (outputPFTClasses) {
            try {
                Lccs2PftLutBuilder lutBuilder = new Lccs2PftLutBuilder();
                lutBuilder.useScaleFactor(1 / 100.0f);
                if (userPFTConversionTable != null) {
                    InputStreamReader reader = new InputStreamReader(new FileInputStream(userPFTConversionTable));
                    lutBuilder = lutBuilder.useLccs2PftTable(reader);
                }
                pftLut = lutBuilder.create();
                final int numLccsClasses = LCCS.getInstance().getNumClasses();
                final int numConversionFactors = pftLut.getConversionFactors().length;
                if (numConversionFactors != numLccsClasses) {
                    final String msg = String.format("PFT conversion table not valid. Should have %d conversion factors but has %d",
                                                     numLccsClasses, numConversionFactors);
                    throw new IllegalStateException(msg);
                }
            } catch (IOException | Lccs2PftLutException e) {
                throw new IllegalStateException(e);
            }
        }
        return pftLut;
    }

    private static String[] createSpatialFeatureNames() {
        String[] spatialFeatureNames = new String[LCCS_CLASSES.getNumClasses()];
        short[] classValues = LCCS_CLASSES.getClassValues();
        for (int i = 0; i < spatialFeatureNames.length; i++) {
            spatialFeatureNames[i] = "class_area_" + classValues[i];
        }
        return spatialFeatureNames;
    }

    private static String[] createOutputFeatureNames(boolean outputLCCSClasses, int numMajorityClasses, Lccs2PftLut pftLut,
                                                     String[] spatialFeatureNames) {
        List<String> outputFeatureNames = new ArrayList<String>();
        if (outputLCCSClasses) {
            outputFeatureNames.addAll(Arrays.asList(spatialFeatureNames));
        }
        for (int i = 0; i < numMajorityClasses; i++) {
            outputFeatureNames.add("majority_class_" + (i + 1));
        }
        if (pftLut != null) {
            outputFeatureNames.addAll(Arrays.asList(pftLut.getPFTNames()));
        }
        return outputFeatureNames.toArray(new String[outputFeatureNames.size()]);
    }


}
