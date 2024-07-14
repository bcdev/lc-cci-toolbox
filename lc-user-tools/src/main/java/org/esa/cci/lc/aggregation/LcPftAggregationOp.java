package org.esa.cci.lc.aggregation;


import org.esa.cci.lc.io.LcCdsBinWriter;
import org.esa.cci.lc.util.LcHelper;
import org.esa.snap.binning.PlanetaryGrid;
import org.esa.snap.binning.operator.BinningOp;
import org.esa.snap.binning.support.PlateCarreeGrid;
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

    @Parameter(description = "Output chunk size in format height:width, defaults to 2025:2025", defaultValue = "2025:2025")
    private String outputTileSize;

    boolean outputTargetProduct;
    private static final int METER_PER_DEGREE_AT_EQUATOR = 111300;

    private static final String[] listPFTVariables = {"BARE","BUILT","GRASS-MAN","GRASS-NAT","SHRUBS-BD","SHRUBS-BE","SHRUBS-ND","SHRUBS-NE","WATER_INLAND",
            "SNOWICE","TREES-BD","TREES-BE","TREES-ND","TREES-NE","WATER","LAND","WATER_OCEAN"};

    /**
     * Creates a chain of SubsetOp and BinnningOp, parameterises binning, forwards global attributes of input or subset
     * @throws OperatorException
     */
    @Override
    public void initialize() throws OperatorException {
        super.initialize();
        final String planetaryGridClassName = PlateCarreeGrid.class.getName();

        Product source = getSourceProduct();
        MetadataElement globalAttributes = source.getMetadataRoot().getElement("Global_Attributes");
        final String parent_path = source.getFileLocation().getAbsolutePath();
        final String id = createTypeAndID();

        final HashMap<String, String> lcProperties = getLcProperties();
        //addMetadataToLcProperties(globalAttributes);
        lcProperties.put(LcHelper.PROP_NAME_TILE_SIZE, outputTileSize);
        addGridNameToLcProperties(planetaryGridClassName);

        final ReferencedEnvelope regionEnvelope = getRegionEnvelope();
        if (regionEnvelope != null) {
            source = createSubset(source, regionEnvelope);
            globalAttributes = source.getMetadataRoot().getElement("Global_Attributes");
        }
        globalAttributes.setAttributeString("parent_path", parent_path);

        BinningOp binningOp;
        try {
            binningOp = new BinningOp();
            binningOp.setParameterDefaultValues();
        } catch (Exception e) {
            throw new OperatorException("Could not create binning operator.", e);
        }
        binningOp.setSourceProduct(source);
        final double sourceMapResolutionX = 180.0 / source.getSceneRasterHeight();
        final double sourceMapResolutionY = 360.0 / source.getSceneRasterWidth();
        PlanetaryGrid planetaryGrid = createPlanetaryGrid();
        AreaCalculator areaCalculator = new FractionalAreaCalculator(planetaryGrid, sourceMapResolutionX, sourceMapResolutionY);
        LcPftAggregatorConfig[] configs = createConfigs(areaCalculator);
        binningOp.setAggregatorConfigs(configs);
        binningOp.setPlanetaryGridClass(planetaryGridClassName);
        binningOp.setNumRows(getNumRows());
        binningOp.setSuperSampling(1);
        binningOp.setOutputFile(getOutputFile() == null ? new File(getTargetDir(), id + ".nc").getPath() : getOutputFile());
        binningOp.setOutputType(getOutputType() == null ? "Product" : getOutputType());
        binningOp.setOutputFormat("NetCDF4-LC-PFT-Aggregate");
        //sourceProduct.getMetadataRoot().getElement("global_attributes").setAttributeString("parent_path",sourceProduct.getFileLocation().getAbsolutePath());
        Product dummyTarget = binningOp.getTargetProduct();
        setTargetProduct(dummyTarget);
        binningOp.setOutputTargetProduct(outputTargetProduct);
        binningOp.setParameter("outputBinnedData", true);
        binningOp.setBinWriter(new LcCdsBinWriter(lcProperties, regionEnvelope, globalAttributes));
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
        return getSourceProduct().getName().replace("ESACCI-LC-L4-PFT-Map-300m-P1Y",
                                                    "ESACCI-LC-L4-PFT-Map-300m-P1Y-aggregated");
    }

    @Override
    protected void addMetadataToLcProperties(MetadataElement globalAttributes) {
        final HashMap<String, String> lcProperties = getLcProperties();
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
        lcProperties.put("spatialResolution", String.valueOf((int) (METER_PER_DEGREE_AT_EQUATOR * resolutionDegree)));
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
