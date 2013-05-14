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
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.experimental.Output;
import org.esa.beam.util.Debug;
import org.esa.cci.lc.conversion.LcMapTiffReader;
import org.esa.cci.lc.util.LcHelper;

import java.io.File;
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
            alias = "LCCCI.Aggregate",
            version = "0.5",
            authors = "Marco Peters",
            copyright = "(c) 2013 by Brockmann Consult",
            description = "Allows to aggregate LC map and condition products.")
public class LcAggregationOp extends Operator implements Output {

    private static final String VALID_EXPRESSION_PATTERN = "processed_flag == %d && (current_pixel_state == %d || current_pixel_state == %d)";
    private static final String CLASS_BAND_NAME = "lccs_class";

    @SourceProduct(description = "LC CCI map or conditions product.", optional = false)
    private Product sourceProduct;

    @Parameter(description = "The target directory.")
    private File targetDir;

    @Parameter(description = "Defines the grid for the target product.", notNull = true,
               valueSet = {"GEOGRAPHIC_LAT_LON", "REGULAR_GAUSSIAN_GRID"})
    private PlanetaryGridName gridName;

    @Parameter(defaultValue = "2160")
    private int numRows;

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
    private final HashMap<String, String> lcProperties;

    public LcAggregationOp() {
        lcProperties = new HashMap<String, String>();
    }

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

        appendPFTProperty(lcProperties);

        BinningOp binningOp;
        try {
            binningOp = new BinningOp();
        } catch (Exception e) {
            throw new OperatorException("Could not create binning operator.", e);
        }
        binningOp.setSourceProduct(sourceProduct);
        binningOp.setOutputTargetProduct(outputTargetProduct);
        binningOp.setParameter("outputBinnedData", true);
        binningOp.setBinWriter(new LcBinWriter(lcProperties));
        binningOp.setBinningConfig(binningConfig);
        binningOp.setFormatterConfig(formatterConfig);

        Product dummyTarget = binningOp.getTargetProduct();
        setTargetProduct(dummyTarget);

        // todo - useless code; Product is not written again
        //        LCCS lccs = LCCS.getInstance();
        //        int[] classValues = lccs.getClassValues();
        //        String[] classDescriptions = lccs.getClassDescriptions();
        //        for (int i = 0; i < classValues.length; i++) {
        //            int classValue = classValues[i];
        //            Band band = targetProduct.getBand("class_area_" + classValue);
        //            band.setDescription(classDescriptions[i]);
        //        }
        //        setTargetProduct(targetProduct);
    }

    private void appendPFTProperty(HashMap<String, String> lcProperties) {
        if (outputPFTClasses) {
            if (userPFTConversionTable != null) {
                lcProperties.put("pft_table", String.format("User defined PFT conversion table used (%s).", userPFTConversionTable.getName())); // TODO
            } else {
                lcProperties.put("pft_table", "LCCCI conform PFT conversion table used.");
            }
        } else {
            lcProperties.put("pft_table", "No PFT computed.");
        }
    }

    private void appendGridNameProperty(PlanetaryGrid planetaryGrid) {
        final String gridName;
        if (planetaryGrid instanceof RegularGaussianGrid) {
            gridName = "Regular gaussian grid (N" + numRows / 2 + ")";
            lcProperties.put("grid_name", gridName);
        } else if (planetaryGrid instanceof PlateCarreeGrid) {
            lcProperties.put("grid_name", String.format("Geographic lat lon grid (cell size: %3f°)", 180.0 / numRows));
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
        return new File(targetDir, getTargetFileName()).getPath();
    }

    private String getTargetFileName() {
        final String insertion = gridName.equals(PlanetaryGridName.GEOGRAPHIC_LAT_LON)
                                 ? String.format(Locale.ENGLISH, "aggregated-%.6fDeg", 180.0 / numRows)
                                 : String.format(Locale.ENGLISH, "aggregated-N" + numRows / 2);
        final String sourceFileName = sourceProduct.getFileLocation().getName();
        return LcHelper.getTargetFileName(insertion, sourceFileName);
    }

    private BinningConfig createBinningConfig(final PlanetaryGrid planetaryGrid) {
        int sceneWidth = sourceProduct.getSceneRasterWidth();
        int sceneHeight = sourceProduct.getSceneRasterHeight();
        FractionalAreaCalculator areaCalculator = new FractionalAreaCalculator(planetaryGrid,
                                                                               sceneWidth, sceneHeight);
        LcAggregatorConfig lcAggregatorConfig = new LcAggregatorConfig(CLASS_BAND_NAME,
                                                                       outputLCCSClasses, numMajorityClasses,
                                                                       outputPFTClasses, userPFTConversionTable,
                                                                       areaCalculator);
        final LcAccuracyAggregatorConfig lcAccuracyAggregatorConfig = new LcAccuracyAggregatorConfig("algorithmic_confidence_level");

        BinningConfig binningConfig = new BinningConfig();
        int processed = 1;
        int clearLand = 1;
        int clearWater = 2;
        String validExpr = String.format(VALID_EXPRESSION_PATTERN, processed, clearLand, clearWater);
        binningConfig.setMaskExpr(validExpr);
        binningConfig.setNumRows(numRows);
        binningConfig.setSuperSampling(1);
        binningConfig.setAggregatorConfigs(lcAggregatorConfig, lcAccuracyAggregatorConfig);
        binningConfig.setPlanetaryGrid(planetaryGrid.getClass().getName());
        binningConfig.setCompositingType(CompositingType.BINNING);
        return binningConfig;
    }

    private PlanetaryGrid createPlanetaryGrid() {
        PlanetaryGrid planetaryGrid;
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

    void ensureTargetDir() {
        if (targetDir == null) {
            final File fileLocation = sourceProduct.getFileLocation();
            if (fileLocation != null) {
                targetDir = fileLocation.getParentFile();
            }
        }
    }

    public File getTargetDir() {
        return targetDir;
    }

    public void setTargetDir(File targetDir) {
        this.targetDir = targetDir;
    }

    PlanetaryGridName getGridName() {
        return gridName;
    }

    void setGridName(PlanetaryGridName gridName) {
        this.gridName = gridName;
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

    int getNumRows() {
        return numRows;
    }

    void setNumRows(int numRows) {
        this.numRows = numRows;
    }

    private void validateInputSettings() {
        if (targetDir == null) {
            throw new OperatorException("The parameter 'targetDir' must be given.");
        }
        if (!targetDir.isDirectory()) {
            throw new OperatorException("The target directory does not exist or is not a directory.");
        }
        if (numMajorityClasses == 0 && !outputLCCSClasses && !outputPFTClasses) {
            throw new OperatorException("Either LCCS classes, majority classes or PFT classes have to be selected.");
        }
        LCCS lccs = LCCS.getInstance();
        if (numMajorityClasses > lccs.getNumClasses()) {
            throw new OperatorException("Number of Majority classes exceeds number of LC classes.");
        }
        if (numRows < 2 || numRows % 2 != 0) {
            throw new OperatorException("Number of rows must be greater than 2 and must be an even number.");
        }
        if (PlanetaryGridName.REGULAR_GAUSSIAN_GRID.equals(gridName)) {
            numRows *= 2;
        }
        for (String variableName : LcMapTiffReader.LC_VARIABLE_NAMES) {
            if (!sourceProduct.containsBand(variableName)) {
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
            super(LcAggregationOp.class);
        }
    }

    // for test cases only
    void setSourceProd(Product sourceProduct) {
        this.sourceProduct = sourceProduct;
    }
}
