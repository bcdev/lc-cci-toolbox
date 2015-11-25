package org.esa.cci.lc.aggregation;

import org.esa.beam.binning.AggregatorConfig;
import org.esa.beam.binning.PlanetaryGrid;
import org.esa.beam.binning.operator.BinningOp;
import org.esa.beam.binning.support.PlateCarreeGrid;
import org.esa.beam.binning.support.RegularGaussianGrid;
import org.esa.beam.binning.support.SEAGrid;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.cci.lc.io.LcBinWriter;
import org.esa.cci.lc.io.LcMapMetadata;
import org.esa.cci.lc.io.LcMapTiffReader;
import org.esa.cci.lc.util.PlanetaryGridName;
import org.geotools.geometry.jts.ReferencedEnvelope;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
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
        version = "3.8",
        authors = "Marco Peters",
        copyright = "(c) 2014 by Brockmann Consult",
        description = "Allows to aggregate LC map products.",
        autoWriteDisabled = true)
public class LcMapAggregationOp extends AbstractLcAggregationOp {

    @Parameter(description = "Whether or not to add LCCS classes to the output.",
               label = "Output LCCS classes", defaultValue = "true")
    private boolean outputLCCSClasses;

    @Parameter(description = "The number of majority classes generated and added to the output.", defaultValue = "5",
               label = "Number of majority classes")
    private int numMajorityClasses;

    @Parameter(description = "Whether or not to add PFT classes to the output.",
               label = "Output PFT classes", defaultValue = "true")
    private boolean outputPFTClasses;

    @Parameter(description = "The user defined conversion table from LCCS to PFTs. " +
                             "If not given, the standard LC-CCI table is used.",
               label = "User defined PFT conversion table")
    private File userPFTConversionTable;

    @Parameter(description = "Whether or not to add the accuracy variable to the output.",
               label = "Output accuracy value", defaultValue = "true")
    private boolean outputAccuracy;

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
        addPFTTableToLcProperties(lcProperties);
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

    private void addPFTTableToLcProperties(HashMap<String, String> lcProperties) {
        if (outputPFTClasses) {
            if (userPFTConversionTable != null) {
                lcProperties.put("pft_table",
                                 String.format("User defined PFT conversion table used (%s).", userPFTConversionTable.getName()));
                try {
                    final FileReader fileReader = new FileReader(userPFTConversionTable);
                    final Lccs2PftLutBuilder lutBuilder = new Lccs2PftLutBuilder().useLccs2PftTable(fileReader);
                    lutBuilder.useScaleFactor(1 / 100.0f);
                    Lccs2PftLut pftLut = lutBuilder.create();
                    if (pftLut.getComment() != null) {
                        lcProperties.put("pft_table_comment", pftLut.getComment());
                    }
                } catch (IOException | Lccs2PftLutException e) {
                    throw new OperatorException("Could not read specified PFT table.", e);
                }
            } else {
                lcProperties.put("pft_table", "LCCCI conform PFT conversion table used.");
            }
        } else {
            lcProperties.put("pft_table", "No PFT computed.");
        }
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
        LcMapAggregatorConfig lcMapAggregatorConfig = new LcMapAggregatorConfig(outputLCCSClasses, numMajorityClasses,
                                                                                outputPFTClasses, userPFTConversionTable,
                                                                                areaCalculator);
        AggregatorConfig[] aggregatorConfigs;
        if (outputAccuracy) {
            final String accuracyVariable = "Map".equals(mapType) ? "algorithmic_confidence_level" : "label_confidence_level";
            final LcAccuracyAggregatorConfig lcAccuracyAggregatorConfig = new LcAccuracyAggregatorConfig(accuracyVariable, "confidence");
            aggregatorConfigs = new AggregatorConfig[]{lcMapAggregatorConfig, lcAccuracyAggregatorConfig};
        } else {
            aggregatorConfigs = new AggregatorConfig[]{lcMapAggregatorConfig};
        }
        binningOp.setAggregatorConfigs(aggregatorConfigs);
        binningOp.setPlanetaryGridClass(planetaryGridClassName);
        binningOp.setOutputFile(getOutputFile() == null ? new File(getTargetDir(), outputFilename).getPath() : getOutputFile());
        binningOp.setOutputType(getOutputType() == null ? "Product" : getOutputType());
        binningOp.setOutputFormat(getOutputFormat());
    }

    private PlanetaryGrid createPlanetaryGrid() {
        PlanetaryGrid planetaryGrid;
        PlanetaryGridName gridName = getGridName();
        int numRows = getNumRows();
        if (PlanetaryGridName.GEOGRAPHIC_LAT_LON.equals(gridName)) {
            planetaryGrid = new PlateCarreeGrid(numRows);
        } else if (PlanetaryGridName.REGULAR_GAUSSIAN_GRID.equals(gridName)) {
            planetaryGrid = new RegularGaussianGrid(numRows);
//        } else if (PlanetaryGridName.REDUCED_GAUSSIAN_GRID.equals(gridName)) {   // not yet supported
//            planetaryGrid = new ReducedGaussianGrid(numRows);
        } else {
            planetaryGrid = new SEAGrid(numRows);
        }
        return planetaryGrid;
    }

    public boolean isOutputLCCSClasses() {
        return outputLCCSClasses;
    }

    public void setOutputLCCSClasses(boolean outputLCCSClasses) {
        this.outputLCCSClasses = outputLCCSClasses;
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
