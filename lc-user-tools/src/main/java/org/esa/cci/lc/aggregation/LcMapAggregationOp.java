package org.esa.cci.lc.aggregation;

import org.esa.beam.binning.AggregatorConfig;
import org.esa.beam.binning.PlanetaryGrid;
import org.esa.beam.binning.operator.BinningOp;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.cci.lc.io.LcBinWriter;
import org.esa.cci.lc.io.LcMapMetadata;
import org.esa.cci.lc.io.LcMapTiffReader;
import org.esa.cci.lc.util.LcHelper;
import org.esa.cci.lc.util.PlanetaryGridName;
import org.geotools.geometry.jts.ReferencedEnvelope;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;

/**
 * The LC map and conditions products are delivered in a full spatial resolution version, both as global
 * files and as regional subsets, in a Plate Carree projection. However, climate models may need products
 * associated with a coarser spatial resolution, over specific areas (e.g. for regional climate models)
 * and/or in another projection. This Operator implementation provides this functionality.
 *
 * @author Marco Peters
 */
@OperatorMetadata(
        alias = "LCCCI.Aggregate.Map",
        internal = true,
        version = "3.15",
        authors = "Marco Peters",
        copyright = "(c) 2014 by Brockmann Consult",
        description = "Allows to aggregate LC map products.",
        autoWriteDisabled = true)
public class LcMapAggregationOp extends AbstractLcAggregationOp {

    @Parameter(description = "Whether or not to add LCCS classes to the output.",
            label = "Output LCCS Classes", defaultValue = "true")
    private boolean outputLCCSClasses;

    @Parameter(description = "The number of majority classes generated and added to the output.", defaultValue = "5",
            label = "Number of Majority Classes")
    private int numMajorityClasses;

    @Parameter(description = "Whether or not to add PFT classes to the output.",
            label = "Output PFT Classes", defaultValue = "true")
    private boolean outputPFTClasses;

    @Parameter(description = "The user defined conversion table from LCCS to PFTs. " +
            "If not given, the standard LC-CCI table is used.",
            label = "User Defined PFT Conversion Table")
    private File userPFTConversionTable;

    @Parameter(description = "A map containing additional classes which can be used to refine " +
            "the conversion from LCCS to PFT classes",
            label = "Additional User Map")
    private File additionalUserMap;

    @Parameter(description = "Whether or not to add the classes of the user map to the output. " +
            "This option is only applicable if the additional user map is given too.",
            label = "Output User Map Classes", defaultValue = "false")
    private boolean outputUserMapClasses;

    @Parameter(description = "The conversion table from LCCS to PFTs considering the additional user map. " +
            "This option is only applicable if the additional user map is given too.",
            label = "Additional User Map PFT Conversion Table")
    private File additionalUserMapPFTConversionTable;

    @Parameter(description = "Whether or not to add the accuracy variable (for multi-year maps) to the output.",
            label = "Output Accuracy Value", defaultValue = "true")
    private boolean outputAccuracy;

    @Parameter(description = "Whether or not to add the change count variable (for yearly maps) to the output.",
            label = "Output Change Count Value", defaultValue = "true")
    private boolean outputChangeCount;

    boolean outputTargetProduct;

    @Override
    public void initialize() throws OperatorException {
        super.initialize();
        validateInputSettings();

        Product source = getSourceProduct();
        final String planetaryGridClassName = getPlanetaryGridClassName();
        final String mapType = source.getFileLocation() != null ? LcMapMetadata.mapTypeOf(source.getFileLocation().getName()) : "unknown";
        final MetadataElement globalAttributes = source.getMetadataRoot().getElement("Global_Attributes");
        final HashMap<String, String> lcProperties = getLcProperties();
        LcHelper.addPFTTableInfoToLcProperties(lcProperties, outputPFTClasses, userPFTConversionTable, additionalUserMapPFTConversionTable);
        addAggregationTypeToLcProperties("Map");
        addGridNameToLcProperties(planetaryGridClassName);
        addMetadataToLcProperties(globalAttributes);

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

        String id = createTypeAndID(lcProperties, mapType);
        initBinningOp(planetaryGridClassName, binningOp, id + ".nc");
        binningOp.setSourceProduct(source);
        binningOp.setOutputTargetProduct(outputTargetProduct);
        binningOp.setParameter("outputBinnedData", true);
        binningOp.setBinWriter(new LcBinWriter(lcProperties, regionEnvelope));

        Product dummyTarget = binningOp.getTargetProduct();
        setTargetProduct(dummyTarget);
    }

    private String createTypeAndID(HashMap<String, String> lcProperties, String mapType) {
        String spatialResolutionNominal = lcProperties.get("spatialResolutionNominal");
        String temporalResolution = lcProperties.get("temporalResolution");
        String version = lcProperties.get("version");
        String typeString = String.format("ESACCI-LC-L4-LCCS-%s-%s-P%sY", mapType, spatialResolutionNominal, temporalResolution);
        int startYear = Integer.parseInt(lcProperties.get("startTime").substring(0, 4));
        int endYear = Integer.parseInt(lcProperties.get("endTime").substring(0, 4));
        String epoch = String.valueOf((endYear + startYear) / 2);

        int numRows = getNumRows();
        String aggrResolution = getGridName().equals(PlanetaryGridName.GEOGRAPHIC_LAT_LON)
                ? String.format(Locale.ENGLISH, "aggregated-%.6fDeg", 180.0 / numRows)
                : String.format(Locale.ENGLISH, "aggregated-N" + numRows / 2);
        final String regionIdentifier = getRegionIdentifier();
        lcProperties.put("type", typeString);
        String id;
        if (regionIdentifier != null) {
            id = String.format("%s-%s-%s-%s-v%s", typeString, aggrResolution, regionIdentifier, epoch, version);
        } else {
            id = String.format("%s-%s-%s-v%s", typeString, aggrResolution, epoch, version);
        }
        lcProperties.put("id", id);
        return id;

    }

    private void initBinningOp(String planetaryGridClassName, BinningOp binningOp, String outputFilename) {

        Product sourceProduct = getSourceProduct();
        final String mapType = sourceProduct.getFileLocation() != null ? LcMapMetadata.mapTypeOf(sourceProduct.getFileLocation().getName()) : "unknown";
        int sceneWidth = sourceProduct.getSceneRasterWidth();
        int sceneHeight = sourceProduct.getSceneRasterHeight();
        final double sourceMapResolutionX = 180.0 / sceneHeight;
        final double sourceMapResolutionY = 360.0 / sceneWidth;
        PlanetaryGrid planetaryGrid = createPlanetaryGrid();
        AreaCalculator areaCalculator = new FractionalAreaCalculator(planetaryGrid, sourceMapResolutionX, sourceMapResolutionY);

        binningOp.setNumRows(getNumRows());
        binningOp.setSuperSampling(1);
        URL userPFTConversionTableUrl = convertFileToUrl(userPFTConversionTable);
        URL additionalUserMapUrl = convertFileToUrl(additionalUserMap);
        URL additionalUserMapPFTConversionUrl = convertFileToUrl(additionalUserMapPFTConversionTable);
        LcMapAggregatorConfig lcMapAggregatorConfig = new LcMapAggregatorConfig(outputLCCSClasses, numMajorityClasses,
                                                                                outputPFTClasses, userPFTConversionTableUrl,
                                                                                additionalUserMapUrl, outputUserMapClasses,
                                                                                additionalUserMapPFTConversionUrl,
                                                                                areaCalculator);
        AggregatorConfig[] aggregatorConfigs;
        if (outputAccuracy && sourceProduct.containsBand("algorithmic_confidence_level")) {
            final String accuracyVariable = "Map".equals(mapType) ? "algorithmic_confidence_level" : "label_confidence_level";
            final LcAccuracyAggregatorConfig lcAccuracyAggregatorConfig = new LcAccuracyAggregatorConfig(accuracyVariable, "confidence");
            aggregatorConfigs = new AggregatorConfig[]{lcMapAggregatorConfig, lcAccuracyAggregatorConfig};
        } else if (outputChangeCount && sourceProduct.containsBand("change_count")) {
            final String majorityVariable = "change_count";
            final LcMajorityAggregatorConfig lcMajorityAggregatorConfig = new LcMajorityAggregatorConfig(majorityVariable, "change_count");
            aggregatorConfigs = new AggregatorConfig[]{lcMapAggregatorConfig, lcMajorityAggregatorConfig};
        } else {
            aggregatorConfigs = new AggregatorConfig[]{lcMapAggregatorConfig};
        }
        binningOp.setAggregatorConfigs(aggregatorConfigs);
        binningOp.setPlanetaryGridClass(planetaryGridClassName);
        binningOp.setOutputFile(getOutputFile() == null ? new File(getTargetDir(), outputFilename).getPath() : getOutputFile());
        binningOp.setOutputType(getOutputType() == null ? "Product" : getOutputType());
        binningOp.setOutputFormat(getOutputFormat());
    }

    private URL convertFileToUrl(File file) {
        if (file != null) {
            try {
                return file.toURI().toURL();
            } catch (MalformedURLException e) {
                throw new OperatorException("Can not convert file to URL.", e);
            }
        }
        return null;
    }

    public boolean isOutputLCCSClasses() {
        return outputLCCSClasses;
    }

    public void setOutputLCCSClasses(boolean outputLCCSClasses) {
        this.outputLCCSClasses = outputLCCSClasses;
    }

    public boolean isOutputUserMapClasses() {
        return outputUserMapClasses;
    }

    public void setOutputUserMapClasses(boolean outputUserMapClasses) {
        this.outputUserMapClasses = outputUserMapClasses;
    }

    int getNumMajorityClasses() {
        return numMajorityClasses;
    }

    void setNumMajorityClasses(int numMajorityClasses) {
        this.numMajorityClasses = numMajorityClasses;
    }

    boolean isOutputPFTClasses() {
        return outputPFTClasses;
    }

    void setOutputPFTClasses(boolean outputPFTClasses) {
        this.outputPFTClasses = outputPFTClasses;
    }

    protected void validateInputSettings() {
        super.validateInputSettings();
        if (numMajorityClasses == 0 && !outputLCCSClasses && !outputPFTClasses) {
            throw new OperatorException("Either LCCS classes, majority classes or PFT classes have to be selected.");
        }
        if (userPFTConversionTable != null && !userPFTConversionTable.isFile()) {
            throw new OperatorException(String.format("The path to the PFT conversion table is not valid [%s].",
                                                      userPFTConversionTable));
        }
        LCCS lccs = LCCS.getInstance();
        if (numMajorityClasses > lccs.getNumClasses()) {
            throw new OperatorException("Number of majority classes exceeds number of LC classes.");
        }
        final String[] lcVariableNames = Arrays.copyOf(LcMapTiffReader.LC_VARIABLE_NAMES, 1);
        for (String variableName : lcVariableNames) {
            if (!getSourceProduct().containsBand(variableName)) {
                throw new OperatorException(String.format("Missing band '%s' in source product.", variableName));
            }
        }
        if (outputUserMapClasses && additionalUserMap == null) {
            throw new OperatorException("If the user map classes shall be included the user map file must be specified too.");
        }
        if (additionalUserMapPFTConversionTable != null) {
            if (!additionalUserMapPFTConversionTable.isFile()) {
                throw new OperatorException(String.format("The path to the additional user map PFT conversion table is not valid [%s].",
                                                          additionalUserMap));
            }
            if (additionalUserMap == null) {
                throw new OperatorException("The additional user map conversion table is specified but not the user map file.");
            }
        }
        if (additionalUserMap != null && !additionalUserMap.isFile()) {
            throw new OperatorException(String.format("The path to the additional user map is not valid [%s].",
                                                      additionalUserMap));
        }

    }

    /**
     * The Service Provider Interface (SPI) for the operator.
     * It provides operator meta-data and is a factory for new operator instances.
     */
    public static class Spi extends OperatorSpi {

        public Spi() {
            super(LcMapAggregationOp.class);
        }
    }

}
