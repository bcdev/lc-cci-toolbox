package org.esa.cci.lc.aggregation;


import org.esa.cci.lc.io.LcBinWriter;
import org.esa.cci.lc.io.LcCdsBinWriter;
import org.esa.cci.lc.util.PlanetaryGridName;
import org.esa.snap.binning.AggregatorConfig;
import org.esa.snap.binning.PlanetaryGrid;
import org.esa.snap.binning.operator.BinningOp;
import org.esa.snap.binning.operator.VariableConfig;
import org.esa.snap.binning.support.PlateCarreeGrid;
import org.esa.snap.binning.support.RegularGaussianGrid;
import org.esa.snap.binning.support.SEAGrid;
import org.esa.snap.core.datamodel.MetadataElement;
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

    @Parameter(defaultValue = "2160")
    private int numRows;

    boolean outputTargetProduct;
    private  HashMap<String, String> lcProperties = new HashMap<>();
    private static final int METER_PER_DEGREE_At_EQUATOR = 111300;

    private static final String[] listPFTVariables = {"BARE","BUILT","GRASS-MAN","GRASS-NAT","SHRUBS-BD","SHRUBS-BE","SHRUBS-ND","SHRUBS-NE","WATER_INLAND",
            "SNOWICE","TREES-BD","TREES-BE","TREES-ND","TREES-NE","WATER","LAND","WATER_OCEAN"};

    @Override
    public void initialize() throws OperatorException {
        super.initialize();
        Product source = getSourceProduct();
        final String planetaryGridClassName = PlateCarreeGrid.class.getName();

        final HashMap<String, String> lcProperties = getLcProperties();
        getSourceProduct().getMetadataRoot().getElement("Global_Attributes").setAttributeString("parent_path",getSourceProduct().getFileLocation().getAbsolutePath());;
        final MetadataElement globalAttributes = source.getMetadataRoot().getElement("Global_Attributes");
        //addMetadataToLcProperties(globalAttributes);
        addGridNameToLcProperties(planetaryGridClassName);

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
        binningOp.setBinWriter(new LcCdsBinWriter(lcProperties, regionEnvelope,getSourceProduct().getMetadataRoot().getElement("global_attributes")));

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
        int sceneWidth = sourceProduct.getSceneRasterWidth();
        int sceneHeight = sourceProduct.getSceneRasterHeight();
        final double sourceMapResolutionX = 180.0 / sceneHeight;
        final double sourceMapResolutionY = 360.0 / sceneWidth;
        PlanetaryGrid planetaryGrid = createPlanetaryGrid();
        AreaCalculator areaCalculator = new FractionalAreaCalculator(planetaryGrid, sourceMapResolutionX, sourceMapResolutionY);
        binningOp.setNumRows(getNumRows());
        binningOp.setSuperSampling(1);



        //LcPftAggregatorConfig config = new LcPftAggregatorConfig("WATER", "WATER", 1d, false, false, areaCalculator );
        //LcPftAggregatorConfig config2 = new LcPftAggregatorConfig("BARE", "BARE", 1d, false, false, areaCalculator );
        //LcPftAggregatorConfig[] configs = {config};

        LcPftAggregatorConfig[] configs = createConfigs(areaCalculator);



        binningOp.setAggregatorConfigs(configs);

        //binningOp.setAggregatorConfigs(aggregatorConfigs);
        binningOp.setPlanetaryGridClass(planetaryGridClassName);
        binningOp.setOutputFile(getOutputFile() == null ? new File(getTargetDir(), outputFilename).getPath() : getOutputFile());
        binningOp.setOutputType(getOutputType() == null ? "Product" : getOutputType());
        binningOp.setOutputFormat("NetCDF4-LC-PFT-Aggregate");
        //binningOp.setOutputFormat("NetCDF4-CF");
        sourceProduct.getMetadataRoot().getElement("global_attributes").setAttributeString("parent_path",sourceProduct.getFileLocation().getAbsolutePath());
        Product dummyTarget = binningOp.getTargetProduct();
        setTargetProduct(dummyTarget);
    }

    private LcPftAggregatorConfig[] createConfigs(AreaCalculator areaCalculator){
        LcPftAggregatorConfig[] configs = new LcPftAggregatorConfig[listPFTVariables.length];
        for (int i=0 ; i<listPFTVariables.length; i++) {
            LcPftAggregatorConfig config = new LcPftAggregatorConfig(listPFTVariables[i], listPFTVariables[i], 1d, false, false, areaCalculator);
            configs[i] = config;
        }
        return configs;
    }

    private String createTypeAndID(){
        String id = getSourceProduct().getName().replace("ESACCI-LC-L4-PFT-Map-300m-P1Y","ESACCI-LC-L4-PFT-Map-300m-P1Y-aggregated");
        return id;
    }

    @Override
    protected void addMetadataToLcProperties(MetadataElement globalAttributes) {
        //String timeCoverageDuration = globalAttributes.getAttributeString("time_coverage_duration");
        //String timeCoverageResolution = globalAttributes.getAttributeString("time_coverage_resolution");
        //lcProperties.put("temporalCoverageYears", timeCoverageDuration.substring(1, timeCoverageDuration.length() - 1));
        //lcProperties.put("spatialResolutionNominal", globalAttributes.getAttributeString("spatial_resolution"));
        //lcProperties.put("temporalResolution", timeCoverageResolution.substring(1, timeCoverageResolution.length() - 1));
        //lcProperties.put("startTime", globalAttributes.getAttributeString("time_coverage_start"));
        //lcProperties.put("endTime", globalAttributes.getAttributeString("time_coverage_end"));
        //lcProperties.put("version", globalAttributes.getAttributeString("product_version"));
        //lcProperties.put("source", globalAttributes.getAttributeString("source"));
        lcProperties.put("history", globalAttributes.getAttributeString("history"));
        float resolutionDegree = getTargetSpatialResolution();
        lcProperties.put("spatialResolutionDegrees", String.format("%.6f", resolutionDegree));
        lcProperties.put("spatialResolution", String.valueOf((int) (METER_PER_DEGREE_At_EQUATOR * resolutionDegree)));
        ReferencedEnvelope regionEnvelope = getRegionEnvelope();
        if (regionEnvelope != null) {
            lcProperties.put("latMin", String.valueOf(regionEnvelope.getMinimum(1)));
            lcProperties.put("latMax", String.valueOf(regionEnvelope.getMaximum(1)));
            lcProperties.put("lonMin", String.valueOf(regionEnvelope.getMinimum(0)));
            lcProperties.put("lonMax", String.valueOf(regionEnvelope.getMaximum(0)));
        } else {
            lcProperties.put("latMin", globalAttributes.getAttributeString("geospatial_lat_min"));
            lcProperties.put("latMax", globalAttributes.getAttributeString("geospatial_lat_max"));
            lcProperties.put("lonMin", globalAttributes.getAttributeString("geospatial_lon_min"));
            lcProperties.put("lonMax", globalAttributes.getAttributeString("geospatial_lon_max"));
        }
        lcProperties.put("parent_path",getSourceProduct().getProduct().getFileLocation().getAbsolutePath());
    }

    private float getTargetSpatialResolution() {
        return 180.0f / getNumRows();
    }

    int getNumRows() {
        return numRows;
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
