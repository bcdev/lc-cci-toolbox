package org.esa.cci.lc.aggregation;


import org.esa.cci.lc.io.LcBinWriter;
import org.esa.snap.binning.AggregatorConfig;
import org.esa.snap.binning.PlanetaryGrid;
import org.esa.snap.binning.aggregators.AggregatorAverage;
import org.esa.snap.binning.aggregators.AggregatorSum;
import org.esa.snap.binning.operator.BinningOp;
import org.esa.snap.binning.operator.VariableConfig;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.geotools.geometry.jts.ReferencedEnvelope;

import java.io.File;
import java.util.HashMap;

@OperatorMetadata(
        alias = "LC.Aggregate.Pft",
        internal = true,
        version = "4.7",
        authors = "Roman Shevchuk",
        copyright = "(c) 2022 by Brockmann Consult",
        description = "Allows to aggregate LC PFT products.",
        autoWriteDisabled = true)
public class LcPftAggregationOp extends AbstractLcAggregationOp {

    @Parameter(description = "The number of majority classes generated and added to the output.", defaultValue = "5",
            label = "Number of Majority Classes")
    private int numMajorityClasses;

    boolean outputTargetProduct;

    private static final String[] listPFTVariables = {"BARE","BUILT","GRASS_MAN","GRASS_NAT","SHRUBS_BD","SHRUBS_BE","SHRUBS_ND","SHRUBS_NE",
            "SNOWICE","TREES_BD","TREES_BE","TREES_ND","TREES_NE","WATER"};

    @Override
    public void initialize() throws OperatorException {
        super.initialize();
        Product source = getSourceProduct();
        final String planetaryGridClassName = getPlanetaryGridClassName();

        final HashMap<String, String> lcProperties = getLcProperties();
        String id = createTypeAndID();
        BinningOp binningOp;
        try {
            binningOp = new BinningOp();
            binningOp.setParameterDefaultValues();
        } catch (Exception e) {
            throw new OperatorException("Could not create binning operator.", e);
        }
        final ReferencedEnvelope regionEnvelope = getRegionEnvelope();
        if (regionEnvelope != null) {
            source = createSubset(source, regionEnvelope);
        }

        binningOp.setSourceProduct(source);
        initBinningOp(planetaryGridClassName, binningOp, id + ".nc");

        binningOp.setOutputTargetProduct(outputTargetProduct);
        binningOp.setParameter("outputBinnedData", true);
        binningOp.setBinWriter(new LcBinWriter(lcProperties, regionEnvelope));

    }


    private VariableConfig[] createVarConfigs(){
        VariableConfig[] variableConfigs = new VariableConfig[listPFTVariables.length];
        for (int i=0 ; i<listPFTVariables.length; i++) {
            variableConfigs[0] = new VariableConfig(listPFTVariables[i], listPFTVariables[i], "true");
        }
        return variableConfigs;
    }

    private void initBinningOp(String planetaryGridClassName, BinningOp binningOp, String outputFilename) {
        Product sourceProduct = getSourceProduct();
        //final String mapType = sourceProduct.getFileLocation() != null ? LcMapMetadata.mapTypeOf(sourceProduct.getFileLocation().getName()) : "unknown";
        int sceneWidth = sourceProduct.getSceneRasterWidth();
        int sceneHeight = sourceProduct.getSceneRasterHeight();
        final double sourceMapResolutionX = 180.0 / sceneHeight;
        final double sourceMapResolutionY = 360.0 / sceneWidth;
        PlanetaryGrid planetaryGrid = createPlanetaryGrid();
        AreaCalculator areaCalculator = new FractionalAreaCalculator(planetaryGrid, sourceMapResolutionX, sourceMapResolutionY);
        binningOp.setNumRows(getNumRows());
        binningOp.setSuperSampling(1);



        LcPftAggregatorConfig config = new LcPftAggregatorConfig("WATER", "WATER", 1d, false, false, areaCalculator );
        LcPftAggregatorConfig config2 = new LcPftAggregatorConfig("BARE", "BARE", 1d, false, false, areaCalculator );

        LcPftAggregatorConfig[] configs = createConfigs(areaCalculator);



        binningOp.setAggregatorConfigs(configs);

        //binningOp.setAggregatorConfigs(aggregatorConfigs);
        binningOp.setPlanetaryGridClass(planetaryGridClassName);
        binningOp.setOutputFile(getOutputFile() == null ? new File(getTargetDir(), outputFilename).getPath() : getOutputFile());
        binningOp.setOutputType(getOutputType() == null ? "Product" : getOutputType());
        binningOp.setOutputFormat("NetCDF4-CF");

        Product dummyTarget = binningOp.getTargetProduct();
        setTargetProduct(dummyTarget);
    }

    private LcPftAggregatorConfig[] createConfigs(AreaCalculator areaCalculator){
        LcPftAggregatorConfig[] configs = new LcPftAggregatorConfig[4];
        for (int i=0 ; i<4; i++) {
            LcPftAggregatorConfig config = new LcPftAggregatorConfig(listPFTVariables[i], listPFTVariables[i], 1d, false, false, areaCalculator);
            configs[i] = config;
        }
        return configs;
    }

    private String createTypeAndID(){

        return "ID";
    }


    /**
     * The Service Provider Interface (SPI) for the operator.
     * It provides operator meta-data and is a factory for new operator instances.
     */
    public static class Spi extends OperatorSpi {

        public Spi() {
            super(LcPftAggregationOp.class);
        }
    }
}
