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
import org.esa.beam.dataio.netcdf.metadata.profiles.beam.BeamNetCdf4WriterPlugIn;
import org.esa.beam.framework.dataio.ProductIOPlugInManager;
import org.esa.beam.framework.dataio.ProductSubsetDef;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.experimental.Output;
import org.esa.beam.util.Debug;
import org.esa.beam.util.io.FileUtils;

import java.io.File;
import java.io.IOException;

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
        copyright = "(c) 2012 by Brockmann Consult",
        description = "Allows to re-project, aggregate and subset LC map and conditions products.")
public class LCAggregationOp extends Operator implements Output {

    public static final String NETCDF4_BEAM_FORMAT_STRING = "NetCDF4-BEAM";

    private static final String VALID_EXPRESSION_PATTERN = "processed_flag == %d && (current_pixel_state == %d || current_pixel_state == %d)";


    @SourceProduct(description = "LC CCI map or conditions product.", optional = false)
    private Product sourceProduct;

    @Parameter(description = "The target file location.", defaultValue = "target.nc")
    private File targetFile;

    @Parameter(description = "Defines the projection method for the target product.",
               valueSet = {"GEOGRAPHIC_LAT_LON", "ROTATED_LAT_LON", "REGULAR_GAUSSIAN_GRID", "REDUCED_GAUSSIAN_GRID"},
               defaultValue = "GEOGRAPHIC_LAT_LON")
    private ProjectionMethod projectionMethod;

    @Parameter(description = "Size of a pixel in X-direction in degree.", defaultValue = "0.1", unit = "°")
    private double pixelSizeX;
    @Parameter(description = "Size of a pixel in Y-direction in degree.", defaultValue = "0.1", unit = "°")
    private double pixelSizeY;

    @Parameter(description = "The western longitude.", interval = "[-180,180]", defaultValue = "-15.0", unit = "°")
    private double westBound;
    @Parameter(description = "The northern latitude.", interval = "[-90,90]", defaultValue = "75.0", unit = "°")
    private double northBound;
    @Parameter(description = "The eastern longitude.", interval = "[-180,180]", defaultValue = "30.0", unit = "°")
    private double eastBound;
    @Parameter(description = "The southern latitude.", interval = "[-90,90]", defaultValue = "35.0", unit = "°")
    private double southBound;

    @Parameter(description = "Whether or not to add LCCS classes to the output.",
               label = "Output LCCS classes", defaultValue = "true")
    private boolean outputLCCSClasses;

    @Parameter(description = "The number of majority classes generated and added to the output.", defaultValue = "5")
    private int numberOfMajorityClasses;

    @Parameter(description = "Whether or not to add PFT classes to the output.",
               label = "Output PFT classes", defaultValue = "true")
    private boolean outputPFTClasses;

    @Parameter(description = "The user defined conversion table from LCCS to PFTs. " +
                             "If not given, the standard LC-CCI table is used.",
               label = "User defined PFT conversion table")
    private File userPFTConversionTable;

    @Parameter(defaultValue = "2160")
    private int numRows;

    FormatterConfig formatterConfig;
    private static final String CLASS_BAND_NAME = "lccs_class";

    @Override
    public void initialize() throws OperatorException {
        Debug.setEnabled(true);
        validateParameters();

        Product inputProduct = sourceProduct;

        ProductIOPlugInManager plugInManager = ProductIOPlugInManager.getInstance();
        if (!plugInManager.getWriterPlugIns(NETCDF4_BEAM_FORMAT_STRING).hasNext()) {
            plugInManager.addWriterPlugIn(new BeamNetCdf4WriterPlugIn());
        }

        BinningConfig binningConfig = createBinningConfig(inputProduct);
        if (formatterConfig == null) {
            formatterConfig = createDefaultFormatterConfig();
        }

        BinningOp binningOp;
        try {
            binningOp = new BinningOp();
        } catch (Exception e) {
            throw new OperatorException("Could not create binning operator.", e);
        }
        binningOp.setSourceProduct(inputProduct);
        binningOp.setParameter("outputBinnedData", false);
        binningOp.setBinningConfig(binningConfig);
        binningOp.setFormatterConfig(formatterConfig);

        Product binningTarget = binningOp.getTargetProduct();

        ProductSubsetDef subsetDef = new ProductSubsetDef();
        subsetDef.setRegion(0, 0, 2, 2);
        Product dummyTarget = null;
        try {
            dummyTarget = binningTarget.createSubset(subsetDef, "dummy", "dummy");
        } catch (IOException e) {
            throw new OperatorException(e);
        }
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

    FormatterConfig createDefaultFormatterConfig() {
        final FormatterConfig formatterConfig = new FormatterConfig();
        formatterConfig.setOutputFormat(NETCDF4_BEAM_FORMAT_STRING);
        targetFile = FileUtils.ensureExtension(targetFile, ".nc");
        formatterConfig.setOutputFile(targetFile.getAbsolutePath());
        formatterConfig.setOutputType("Product");
        return formatterConfig;
    }

    private BinningConfig createBinningConfig(Product product) {
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

        int sceneWidth = sourceProduct.getSceneRasterWidth();
        int sceneHeight = sourceProduct.getSceneRasterHeight();
        FractionalAreaCalculator areaCalculator = new FractionalAreaCalculator(planetaryGrid,
                                                                               sceneWidth, sceneHeight);
        LcAggregatorConfig lcAggregatorConfig = new LcAggregatorConfig(CLASS_BAND_NAME,
                                                                       outputLCCSClasses, numberOfMajorityClasses,
                                                                       outputPFTClasses, userPFTConversionTable,
                                                                       areaCalculator);
        final LcMedianAggregatorConfig lcMedianAggregatorConfig = new LcMedianAggregatorConfig("algorithmic_confidence_level");

        BinningConfig binningConfig = new BinningConfig();
        int processed = 1;
        int clearLand = 1;
        int clearWater = 2;
        String validExpr = String.format(VALID_EXPRESSION_PATTERN, processed, clearLand, clearWater);
        binningConfig.setMaskExpr(validExpr);
        binningConfig.setNumRows(numRows);
        binningConfig.setSuperSampling(1);
        binningConfig.setAggregatorConfigs(lcAggregatorConfig, lcMedianAggregatorConfig);
        binningConfig.setPlanetaryGrid(planetaryGrid.getClass().getName());
        binningConfig.setCompositingType(CompositingType.BINNING);
        return binningConfig;
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

    double getPixelSizeX() {
        return pixelSizeX;
    }

    void setPixelSizeX(double pixelSizeX) {
        this.pixelSizeX = pixelSizeX;
    }

    double getPixelSizeY() {
        return pixelSizeY;
    }

    void setPixelSizeY(double pixelSizeY) {
        this.pixelSizeY = pixelSizeY;
    }

    double getWestBound() {
        return westBound;
    }

    void setWestBound(double westBound) {
        this.westBound = westBound;
    }

    double getNorthBound() {
        return northBound;
    }

    void setNorthBound(double northBound) {
        this.northBound = northBound;
    }

    double getEastBound() {
        return eastBound;
    }

    void setEastBound(double eastBound) {
        this.eastBound = eastBound;
    }

    double getSouthBound() {
        return southBound;
    }

    void setSouthBound(double southBound) {
        this.southBound = southBound;
    }


    public boolean isOutputLCCSClasses() {
        return outputLCCSClasses;
    }

    public void setOutputLCCSClasses(boolean outputLCCSClasses) {
        this.outputLCCSClasses = outputLCCSClasses;
    }

    int getNumberOfMajorityClasses() {
        return numberOfMajorityClasses;
    }

    void setNumberOfMajorityClasses(int numberOfMajorityClasses) {
        this.numberOfMajorityClasses = numberOfMajorityClasses;
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
        if (westBound >= eastBound) {
            throw new OperatorException("West bound must be western of east bound.");
        }
        if (northBound <= southBound) {
            throw new OperatorException("North bound must be northern of south bound.");
        }
        if (numberOfMajorityClasses == 0 && !outputLCCSClasses && !outputPFTClasses) {
            throw new OperatorException("Either LCCS classes, majority classes or PFT classes have to be selected.");
        }
        LCCS lccs = LCCS.getInstance();
        if (numberOfMajorityClasses > lccs.getNumClasses()) {
            throw new OperatorException("Number of Majority classes exceeds number of LC classes.");
        }
        if (numRows < 2 || numRows % 2 != 0) {
            throw new OperatorException("Number of rows must be greater than 2 and must be an even number.");
        }
    }

    /**
     * The Service Provider Interface (SPI) for the operator.
     * It provides operator meta-data and is a factory for new operator instances.
     */
    public static class Spi extends OperatorSpi {

        public Spi() {
            super(LCAggregationOp.class);
        }
    }

}
