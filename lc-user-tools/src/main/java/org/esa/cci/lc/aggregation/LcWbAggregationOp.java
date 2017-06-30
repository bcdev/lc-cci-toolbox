package org.esa.cci.lc.aggregation;

import org.esa.beam.binning.PlanetaryGrid;
import org.esa.beam.binning.operator.BinningOp;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.cci.lc.io.LcBinWriter;
import org.esa.cci.lc.util.PlanetaryGridName;
import org.geotools.geometry.jts.ReferencedEnvelope;

import java.io.File;
import java.util.HashMap;
import java.util.Locale;

/**
 * The LC map and conditions products are delivered in a full spatial resolution version, both as global
 * files and as regional subsets, in a Plate Carree projection. However, climate models may need products
 * associated with a coarser spatial resolution, over specific areas (e.g. for regional climate models)
 * and/or in another projection. This Operator implementation provides this functionality.
 *
 * @author Marco Peters, Martin Boettcher
 */
@OperatorMetadata(
        alias = "LCCCI.Aggregate.WB",
        internal = true,
        version = "3.13",
        authors = "Marco Peters, Martin Boettcher",
        copyright = "(c) 2015 by Brockmann Consult",
        description = "Allows to aggregate LC WB products.",
        autoWriteDisabled = true)
public class LcWbAggregationOp extends AbstractLcAggregationOp {

    @Parameter(description = "Whether or not to add the WB class areas to the output.",
            label = "Output WB Class Areas", defaultValue = "true")
    private boolean outputWbClasses;

    @Parameter(description = "The number of majority classes generated and added to the output.", defaultValue = "2",
            label = "Number of Majority Classes")
    private int numMajorityClasses;

    boolean outputTargetProduct;
    private String outputFormat;
    private String outputFile;
    private String outputType;

    @Override
    public void initialize() throws OperatorException {
        super.initialize();
        validateInputSettings();
        final String planetaryGridClassName = getPlanetaryGridClassName();

        HashMap<String, String> lcProperties = getLcProperties();
        addAggregationTypeToLcProperties("WB");
        addGridNameToLcProperties(planetaryGridClassName);
        MetadataElement globalAttributes = getSourceProduct().getMetadataRoot().getElement("Global_Attributes");
        addMetadataToLcProperties(globalAttributes);

        BinningOp binningOp;
        try {
            binningOp = new BinningOp();
            binningOp.setParameterDefaultValues();
        } catch (Exception e) {
            throw new OperatorException("Could not create binning operator.", e);
        }
        Product source = getSourceProduct();
        final ReferencedEnvelope regionEnvelope = getRegionEnvelope();
        if (regionEnvelope != null) {
            source = createSubset(source, regionEnvelope);
        }

        String id = createTypeAndID(lcProperties);
        initBinningOp(planetaryGridClassName, binningOp, id + ".nc");
        binningOp.setSourceProduct(source);
        binningOp.setOutputTargetProduct(outputTargetProduct);
        binningOp.setParameter("outputBinnedData", true);
        binningOp.setBinWriter(new LcBinWriter(lcProperties, regionEnvelope));

        Product dummyTarget = binningOp.getTargetProduct();
        setTargetProduct(dummyTarget);

    }

    private String createTypeAndID(HashMap<String, String> lcProperties) {
        String spatialResolutionNominal = lcProperties.get("spatialResolutionNominal");
        String temporalResolution = lcProperties.get("temporalResolution");
        String version = lcProperties.get("version");
        String typeString = String.format("ESACCI-LC-L4-WB-Map-%s-P%sY", spatialResolutionNominal, temporalResolution);
        int startYear = Integer.parseInt(lcProperties.get("startTime").substring(0, 4));
        String epoch = String.valueOf(startYear);

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
        int sceneWidth = sourceProduct.getSceneRasterWidth();
        int sceneHeight = sourceProduct.getSceneRasterHeight();
        final double sourceMapResolutionX = 180.0 / sceneHeight;
        final double sourceMapResolutionY = 360.0 / sceneWidth;
        PlanetaryGrid planetaryGrid = createPlanetaryGrid();
        AreaCalculator areaCalculator = new FractionalAreaCalculator(planetaryGrid, sourceMapResolutionX, sourceMapResolutionY);

        binningOp.setNumRows(getNumRows());
        binningOp.setSuperSampling(1);
        LcWbAggregatorConfig lcWbAggregatorConfig = new LcWbAggregatorConfig(outputWbClasses, numMajorityClasses, areaCalculator);
        binningOp.setAggregatorConfigs(lcWbAggregatorConfig);
        binningOp.setPlanetaryGridClass(planetaryGridClassName);
        binningOp.setOutputFile(outputFile == null ? new File(getTargetDir(), outputFilename).getPath() : outputFile);
        binningOp.setOutputType(outputType == null ? "Product" : outputType);
        binningOp.setOutputFormat(outputFormat);
    }

    void setOutputFormat(String outputFormat) {
        this.outputFormat = outputFormat;
    }

    void setOutputFile(String outputFile) {
        this.outputFile = outputFile;
    }

    void setOutputType(String outputType) {
        this.outputType = outputType;
    }

    int getNumMajorityClasses() {
        return numMajorityClasses;
    }

    void setNumMajorityClasses(int numMajorityClasses) {
        this.numMajorityClasses = numMajorityClasses;
    }

    public boolean isOutputWbClasses() {
        return outputWbClasses;
    }

    public void setOutputWbClasses(boolean outputWbClasses) {
        this.outputWbClasses = outputWbClasses;
    }

    protected void validateInputSettings() {
        super.validateInputSettings();
        if (numMajorityClasses == 0 && !outputWbClasses) {
            throw new OperatorException("Either WB classes or majority classes must be selected.");
        }
        if (numMajorityClasses > 3) {
            throw new OperatorException("Number of majority classes exceeds number of WB classes.");
        }
        ensureSourceProductContainsBand("wb_class");
    }

    private void ensureSourceProductContainsBand(String lcVariableName) {
        if (!getSourceProduct().containsBand(lcVariableName)) {
            throw new OperatorException(String.format("Missing band '%s' in source product.", lcVariableName));
        }
    }

    /**
     * The Service Provider Interface (SPI) for the operator.
     * It provides operator meta-data and is a factory for new operator instances.
     */
    public static class Spi extends OperatorSpi {

        public Spi() {
            super(LcWbAggregationOp.class);
        }
    }

}
