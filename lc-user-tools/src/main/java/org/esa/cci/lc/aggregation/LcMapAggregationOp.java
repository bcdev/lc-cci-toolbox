package org.esa.cci.lc.aggregation;

import org.esa.beam.binning.CompositingType;
import org.esa.beam.binning.PlanetaryGrid;
import org.esa.beam.binning.operator.BinningConfig;
import org.esa.beam.binning.operator.BinningOp;
import org.esa.beam.binning.operator.FormatterConfig;
import org.esa.beam.binning.support.PlateCarreeGrid;
import org.esa.beam.binning.support.ReducedGaussianGrid;
import org.esa.beam.binning.support.RegularGaussianGrid;
import org.esa.beam.binning.support.SEAGrid;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.experimental.Output;
import org.esa.beam.util.Debug;
import org.esa.cci.lc.io.LcBinWriter;
import org.esa.cci.lc.io.LcMapTiffReader;
import org.esa.cci.lc.util.LcHelper;

import java.io.File;
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
        version = "0.6",
        authors = "Marco Peters",
        copyright = "(c) 2013 by Brockmann Consult",
        description = "Allows to aggregate LC map products.")
public class LcMapAggregationOp extends AbstractLcAggregationOp implements Output {

    private static final String VALID_EXPRESSION_PATTERN = "processed_flag == %d && (current_pixel_state == %d || current_pixel_state == %d)";

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

    FormatterConfig formatterConfig;
    boolean outputTargetProduct;

    @Override
    public void initialize() throws OperatorException {
        Debug.setEnabled(true);
        ensureTargetDir();
        validateInputSettings();
        final PlanetaryGrid planetaryGrid = createPlanetaryGrid();
        appendGridNameProperty(planetaryGrid);
        BinningConfig binningConfig = createBinningConfig(planetaryGrid);

        if (formatterConfig == null) {
            formatterConfig = createDefaultFormatterConfig();
        }

        HashMap<String, Object> lcProperties = getLcProperties();
        appendPFTProperty(lcProperties);
        lcProperties.put("aggregationType", "Map");

        MetadataElement globalAttributes = getSourceProduct().getMetadataRoot().getElement("Global_Attributes");
        addMetadataToLcProperties(globalAttributes);

        BinningOp binningOp;
        try {
            binningOp = new BinningOp();
        } catch (Exception e) {
            throw new OperatorException("Could not create binning operator.", e);
        }
        binningOp.setSourceProduct(getSourceProduct());
        binningOp.setOutputTargetProduct(outputTargetProduct);
        binningOp.setParameter("outputBinnedData", true);
        binningOp.setBinWriter(new LcBinWriter(lcProperties));
        binningOp.setBinningConfig(binningConfig);
        binningOp.setFormatterConfig(formatterConfig);

        Product dummyTarget = binningOp.getTargetProduct();
        setTargetProduct(dummyTarget);

    }

    private void appendPFTProperty(HashMap<String, Object> lcProperties) {
        if (outputPFTClasses) {
            if (userPFTConversionTable != null) {
                lcProperties.put("pft_table",
                                 String.format("User defined PFT conversion table used (%s).", userPFTConversionTable.getName())); // TODO
            } else {
                lcProperties.put("pft_table", "LCCCI conform PFT conversion table used.");
            }
        } else {
            lcProperties.put("pft_table", "No PFT computed.");
        }
    }

    private void appendGridNameProperty(PlanetaryGrid planetaryGrid) {
        final String gridName;
        int numRows = getNumRows();
        if (planetaryGrid instanceof RegularGaussianGrid) {
            gridName = "Regular gaussian grid (N" + numRows / 2 + ")";
            getLcProperties().put("grid_name", gridName);
        } else if (planetaryGrid instanceof PlateCarreeGrid) {
            getLcProperties().put("grid_name", String.format("Geographic lat lon grid (cell size: %.6f°)", 180.0 / numRows));
        } else {
            throw new OperatorException("The grid '" + planetaryGrid.getClass().getName() + "' is not a valid grid.");
        }
    }

    FormatterConfig createDefaultFormatterConfig() {
        final FormatterConfig formatterConfig = new FormatterConfig();
        formatterConfig.setOutputFile(getTargetFilePath());
        formatterConfig.setOutputType("Product");
        return formatterConfig;
    }

    private String getTargetFilePath() {
        return new File(getTargetDir(), getTargetFileName()).getPath();
    }

    private String getTargetFileName() {
        int numRows = getNumRows();
        final String insertion = getGridName().equals(PlanetaryGridName.GEOGRAPHIC_LAT_LON)
                                 ? String.format(Locale.ENGLISH, "aggregated-%.6fDeg", 180.0 / numRows)
                                 : String.format(Locale.ENGLISH, "aggregated-N" + numRows / 2);
        final String sourceFileName = getSourceProduct().getFileLocation().getName();
        return LcHelper.getTargetFileName(insertion, sourceFileName);
    }

    private BinningConfig createBinningConfig(final PlanetaryGrid planetaryGrid) {
        Product sourceProduct = getSourceProduct();
        int sceneWidth = sourceProduct.getSceneRasterWidth();
        int sceneHeight = sourceProduct.getSceneRasterHeight();
        FractionalAreaCalculator areaCalculator = new FractionalAreaCalculator(planetaryGrid,
                                                                               sceneWidth, sceneHeight);
        LcMapAggregatorConfig lcMapAggregatorConfig = new LcMapAggregatorConfig(outputLCCSClasses, numMajorityClasses,
                                                                                outputPFTClasses, userPFTConversionTable,
                                                                                areaCalculator);
        final LcAccuracyAggregatorConfig lcAccuracyAggregatorConfig = new LcAccuracyAggregatorConfig("algorithmic_confidence_level");

        BinningConfig binningConfig = new BinningConfig();
        int processed = 1;
        int clearLand = 1;
        int clearWater = 2;
        String validExpr = String.format(VALID_EXPRESSION_PATTERN, processed, clearLand, clearWater);
        binningConfig.setMaskExpr(validExpr);
        binningConfig.setNumRows(getNumRows());
        binningConfig.setSuperSampling(1);
        binningConfig.setAggregatorConfigs(lcMapAggregatorConfig, lcAccuracyAggregatorConfig);
        binningConfig.setPlanetaryGrid(planetaryGrid.getClass().getName());
        binningConfig.setCompositingType(CompositingType.BINNING);
        return binningConfig;
    }

    private PlanetaryGrid createPlanetaryGrid() {
        PlanetaryGrid planetaryGrid;
        PlanetaryGridName gridName = getGridName();
        int numRows = getNumRows();
        if (PlanetaryGridName.GEOGRAPHIC_LAT_LON.equals(gridName)) {
            planetaryGrid = new PlateCarreeGrid(numRows);
        } else if (PlanetaryGridName.REGULAR_GAUSSIAN_GRID.equals(gridName)) {
            planetaryGrid = new RegularGaussianGrid(numRows);
        } else if (PlanetaryGridName.REDUCED_GAUSSIAN_GRID.equals(gridName)) {
            planetaryGrid = new ReducedGaussianGrid(numRows);
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
            throw new OperatorException("Number of Majority classes exceeds number of LC classes.");
        }
        final String[] lcVariableNames = Arrays.copyOf(LcMapTiffReader.LC_VARIABLE_NAMES, 5);
        for (String variableName : lcVariableNames) {
            if (!getSourceProduct().containsBand(variableName)) {
                throw new OperatorException(String.format("Missing band '%s' in source product.", variableName));
            }
        }
    }

    static boolean onlyOneIsTrue(boolean b1, boolean b2, boolean b3) {
        int count = 0;
        count += b1 ? 1 : 0;
        count += b2 ? 1 : 0;
        count += b3 ? 1 : 0;
        return count == 1;
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
