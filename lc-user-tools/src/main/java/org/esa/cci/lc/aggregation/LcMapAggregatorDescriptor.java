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
        if (!(aggregatorConfig instanceof LcMapAggregatorConfig)) {
            throw new IllegalStateException("!(aggregatorConfig instanceof LcMapAggregatorConfig)");
        }
        LcMapAggregatorConfig mapConf = (LcMapAggregatorConfig) aggregatorConfig;
        boolean outputLCCSClasses = mapConf.isOutputLCCSClasses();
        int numMajorityClasses = mapConf.getNumMajorityClasses();
        boolean outputPFTClasses = mapConf.isOutputPFTClasses();
        URL userPFTConversionTable = mapConf.getUserPFTConversionTable();
        URL additionalUserMap = mapConf.getAdditionalUserMap();
        boolean outputUserMapClasses = mapConf.isOutputUserMapClasses();
        URL additionalUserMapPFTConversionTable = mapConf.getAdditionalUserMapPFTConversionTable();
        AreaCalculator areaCalculator = mapConf.getAreaCalculator();

        Product additionalUserMapProduct;
        try {
            additionalUserMapProduct = additionalUserMap != null ? ProductIO.readProduct(new File(additionalUserMap.toURI())) : null;
        } catch (IOException | URISyntaxException e) {
            throw new IllegalStateException("Could not read additional user map product", e);
        }

        Lccs2PftLut pftLut = getPftLut(outputPFTClasses, userPFTConversionTable, additionalUserMapPFTConversionTable);

        String[] spatialFeatureNames = createSpatialFeatureNames(outputUserMapClasses, additionalUserMapPFTConversionTable);
        String[] outputFeatureNames = createOutputFeatureNames(outputLCCSClasses, outputUserMapClasses, numMajorityClasses, pftLut, spatialFeatureNames);
        // todo - actuallly these are to many parameters. Pass directly the configuration (mp - 20151130)
        return new LcMapAggregator(outputLCCSClasses, numMajorityClasses, additionalUserMapProduct, outputUserMapClasses,
                                   areaCalculator, pftLut,
                                   spatialFeatureNames, outputFeatureNames);
    }

    @Override
    public String[] getSourceVarNames(AggregatorConfig aggregatorConfig) {
        if (!(aggregatorConfig instanceof LcMapAggregatorConfig)) {
            throw new IllegalStateException("!(aggregatorConfig instanceof LcMapAggregatorConfig)");
        }
        return new String[]{((LcMapAggregatorConfig) aggregatorConfig).getSourceVarName()};
    }

    @Override
    public String[] getTargetVarNames(AggregatorConfig aggregatorConfig) {
        if (!(aggregatorConfig instanceof LcMapAggregatorConfig)) {
            throw new IllegalStateException("!(aggregatorConfig instanceof LcMapAggregatorConfig)");
        }
        LcMapAggregatorConfig mapConf = (LcMapAggregatorConfig) aggregatorConfig;
        boolean outputLCCSClasses = mapConf.isOutputLCCSClasses();
        int numMajorityClasses = mapConf.getNumMajorityClasses();
        boolean outputPFTClasses = mapConf.isOutputPFTClasses();
        URL userPFTConversionTable = mapConf.getUserPFTConversionTable();
        boolean outputUserMapClasses = mapConf.isOutputUserMapClasses();
        URL additionalUserMapPFTConversionTable = mapConf.getAdditionalUserMapPFTConversionTable();

        Lccs2PftLut pftLut = getPftLut(outputPFTClasses, userPFTConversionTable, additionalUserMapPFTConversionTable);
        String[] spatialFeatureNames = createSpatialFeatureNames(outputUserMapClasses, additionalUserMapPFTConversionTable);
        return createOutputFeatureNames(outputLCCSClasses, outputUserMapClasses, numMajorityClasses, pftLut, spatialFeatureNames);
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

    private static String[] createOutputFeatureNames(boolean outputLCCSClasses, boolean outputUserMapClasses, int numMajorityClasses, Lccs2PftLut pftLut,
                                                     String[] spatialFeatureNames) {
        List<String> outputFeatureNames = new ArrayList<>();
        if (outputLCCSClasses) {
            final int numClasses = LCCS_CLASSES.getClassValues().length;
            outputFeatureNames.addAll(Arrays.asList(spatialFeatureNames).subList(0, numClasses));
        }
        if (outputUserMapClasses) {
            outputFeatureNames.add("user_map");
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
