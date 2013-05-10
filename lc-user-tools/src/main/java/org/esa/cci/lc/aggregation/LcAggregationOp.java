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

import java.io.File;
import java.util.HashMap;

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

    @Parameter(description = "The target file location.", defaultValue = "target.nc")
    private File targetFile;

    @Parameter(description = "Defines the projection method for the target product.", notNull = true,
               valueSet = {"GEOGRAPHIC_LAT_LON", "REGULAR_GAUSSIAN_GRID"})
    private ProjectionMethod projectionMethod;
//    @Parameter(description = "Size of a pixel in X-direction in degree.", defaultValue = "0.1", unit = "°")
//    private double pixelSizeX;
//    @Parameter(description = "Size of a pixel in Y-direction in degree.", defaultValue = "0.1", unit = "°")
//    private double pixelSizeY;
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
        validateParameters();
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
        formatterConfig.setOutputFile(targetFile.getAbsolutePath());
        formatterConfig.setOutputType("Product");
        return formatterConfig;
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
        if (ProjectionMethod.GEOGRAPHIC_LAT_LON.equals(projectionMethod)) {
            planetaryGrid = new PlateCarreeGrid(numRows);
        } else if (ProjectionMethod.REGULAR_GAUSSIAN_GRID.equals(projectionMethod)) {
            planetaryGrid = new RegularGaussianGrid(numRows);
        } else if (ProjectionMethod.REDUCED_GAUSSIAN_GRID.equals(projectionMethod)) {
            planetaryGrid = new ReducedGaussianGrid(numRows);
        } else {
            planetaryGrid = new SEAGrid(numRows);
        }
        return planetaryGrid;
    }

    File getTargetFile() {
        return targetFile;
    }

    void setTargetFile(File targetFile) {
        this.targetFile = targetFile;
    }

    ProjectionMethod getProjectionMethod() {
        return projectionMethod;
    }

    void setProjectionMethod(ProjectionMethod projectionMethod) {
        this.projectionMethod = projectionMethod;
    }

//    double getPixelSizeX() {
//        return pixelSizeX;
//    }
//
//    void setPixelSizeX(double pixelSizeX) {
//        this.pixelSizeX = pixelSizeX;
//    }
//
//    double getPixelSizeY() {
//        return pixelSizeY;
//    }
//
//    void setPixelSizeY(double pixelSizeY) {
//        this.pixelSizeY = pixelSizeY;
//    }

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

    private void validateParameters() {
        if (targetFile == null) {
            throw new OperatorException("No target file specified");
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

}