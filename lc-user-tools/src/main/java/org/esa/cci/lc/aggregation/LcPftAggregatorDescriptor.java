package org.esa.cci.lc.aggregation;

import org.esa.snap.binning.Aggregator;
import org.esa.snap.binning.AggregatorConfig;
import org.esa.snap.binning.AggregatorDescriptor;
import org.esa.snap.binning.VariableContext;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Product;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
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
        if (!(aggregatorConfig instanceof LcPftAggregatorConfig)) {
            throw new IllegalStateException("!(aggregatorConfig instanceof LcPftAggregatorConfig)");
        }
        LcPftAggregatorConfig pftConf = (LcPftAggregatorConfig) aggregatorConfig;
        boolean outputLCCSClasses = pftConf.isOutputLCCSClasses();
        int numMajorityClasses = pftConf.getNumMajorityClasses();
        boolean outputPFTClasses = pftConf.isOutputPFTClasses();
        URL userPFTConversionTable = pftConf.getUserPFTConversionTable();
        URL additionalUserMap = pftConf.getAdditionalUserMap();
        boolean outputUserMapClasses = pftConf.isOutputUserMapClasses();
        URL additionalUserMapPFTConversionTable = pftConf.getAdditionalUserMapPFTConversionTable();
        AreaCalculator areaCalculator = pftConf.getAreaCalculator();

        Product additionalUserMapProduct;
        try {
            additionalUserMapProduct = additionalUserMap != null ? ProductIO.readProduct(new File(additionalUserMap.toURI())) : null;
        } catch (IOException | URISyntaxException e) {
            throw new IllegalStateException("Could not read additional user map product", e);
        }

        Lccs2PftLut pftLut = getPftLut(outputPFTClasses, userPFTConversionTable, additionalUserMapPFTConversionTable);

        String[] spatialFeatureNames = createSpatialFeatureNames(outputUserMapClasses, additionalUserMapPFTConversionTable);
        String[] outputFeatureNames = createOutputFeatureNames();
        // todo - actuallly these are to many parameters. Pass directly the configuration (mp - 20151130)
        return new LcPftAggregator(outputLCCSClasses, numMajorityClasses, additionalUserMapProduct, outputUserMapClasses,
                areaCalculator, pftLut,
                spatialFeatureNames, outputFeatureNames);
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
        LcPftAggregatorConfig config = (LcPftAggregatorConfig) aggregatorConfig;
        return createOutputFeatureNames();
    }

    private static String[] createSpatialFeatureNames(boolean outputUserMapClasses, URL additionalUserMapPFTConversionTable) {
        String[] spatialFeatureNames = new String[LCCS_CLASSES.getNumClasses() + (outputUserMapClasses ? 1 : 0)];
        int[] classValues = LCCS_CLASSES.getClassValues();
        for (int i = 0; i < classValues.length; i++) {
            spatialFeatureNames[i] = "class_area_" + classValues[i];
        }
        if (outputUserMapClasses || additionalUserMapPFTConversionTable != null) {
            spatialFeatureNames[spatialFeatureNames.length - 1] = "user_map";
        }
        return spatialFeatureNames;
    }

    private static String[] createOutputFeatureNames() {
        List<String> outputFeatureNames = new ArrayList<>();

        outputFeatureNames.addAll(Arrays.asList(listPFTVariables));

        return outputFeatureNames.toArray(new String[outputFeatureNames.size()]);
    }

    private static Lccs2PftLut getPftLut(boolean outputPFTClasses, URL userPFTConversionTable, URL additionalUserMapPFTConversionTable) {
        Lccs2PftLut pftLut = null;
        if (outputPFTClasses) {
            try {
                Lccs2PftLutBuilder lutBuilder = new Lccs2PftLutBuilder();
                lutBuilder.useScaleFactor(1 / 100.0f);
                if (userPFTConversionTable != null) {
                    InputStreamReader reader = new InputStreamReader(userPFTConversionTable.openStream());
                    lutBuilder = lutBuilder.useLccs2PftTable(reader);
                }
                if (additionalUserMapPFTConversionTable != null) {
                    InputStream inputStream = additionalUserMapPFTConversionTable.openStream();
                    InputStreamReader additionalReader = new InputStreamReader(inputStream);
                    lutBuilder = lutBuilder.useAdditionalUserMap(additionalReader);
                }
                pftLut = lutBuilder.create();
            } catch (IOException | Lccs2PftLutException e) {
                throw new IllegalStateException(e);
            }
        }
        return pftLut;
    }
}
