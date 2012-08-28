package org.esa.cci.lc.aggregation;

import org.esa.beam.binning.operator.BinningConfig;
import org.esa.beam.binning.operator.BinningOp;
import org.esa.beam.binning.operator.FormatterConfig;
import org.esa.beam.binning.operator.VariableConfig;
import org.esa.beam.dataio.netcdf.metadata.profiles.beam.BeamNetCdf4WriterPlugIn;
import org.esa.beam.framework.dataio.ProductIOPlugInManager;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.util.Debug;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.crs.DefaultGeographicCRS;

import java.awt.geom.Rectangle2D;

/**
 * The LC map and conditions products are delivered in a full spatial resolution version, both as global
 * files and as regional subsets, in a Plate Carree projection. However, climate models may need products
 * associated with a coarser spatial resolution, over specific areas (e.g. for regional climate models)
 * and/or in another projection. This Operator implementation provides this functionality.
 *
 * @author Marco Peters
 */
@OperatorMetadata(
        alias = "Aggregate",
        version = "0.1",
        authors = "Marco Peters",
        copyright = "(c) 2012 by Brockmann Consult",
        description = "Allows to re-project, aggregate and subset LC map and conditions products.")
public class AggregationOp extends Operator {

    @SourceProduct(description = "LC CCI map or conditions product", optional = false)
    private Product sourceProduct;

    @Parameter(description = "Defines the projection method for the target product.",
               valueSet = {"GAUSSIAN_GRID", "GEOGRAPHIC_LAT_LON", "ROTATED_LAT_LON"}, defaultValue = "GAUSSIAN_GRID")
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

    @Parameter(description = "Whether or not to add majority classes and the fractional area to the output.",
               defaultValue = "true")
    private boolean outputMajorityClasses;

    @Parameter(description = "The number of majority classes generated and added to the output.", defaultValue = "5")
    private int numberOfMajorityClasses;

    @Parameter(description = "Whether or not to add PFT classes to the output.", defaultValue = "true")
    private boolean outputPFTClasses;

    @Parameter(defaultValue = "216")
    private int numRows;

    FormatterConfig formatterConfig;

    private BeamNetCdf4WriterPlugIn beamNetCdf4WriterPlugIn;

    @Override
    public void initialize() throws OperatorException {
        Debug.setEnabled(true);
        validateParameters();
        // validateSourceProduct();

        ProductIOPlugInManager plugInManager = ProductIOPlugInManager.getInstance();
        if (!plugInManager.getWriterPlugIns("NetCDF4-BEAM").hasNext()) {
            beamNetCdf4WriterPlugIn = new BeamNetCdf4WriterPlugIn();
            plugInManager.addWriterPlugIn(beamNetCdf4WriterPlugIn);
        }

        BinningConfig binningConfig = createBinningConfig(sourceProduct.getBandAt(0).getName());
        if (formatterConfig == null) {
            formatterConfig = getFormatterConfig();
        }

        BinningOp binningOp = new BinningOp();
        binningOp.setSourceProduct(sourceProduct);
        binningOp.setParameter("outputBinnedData", false);
        binningOp.setBinningConfig(binningConfig);
        binningOp.setFormatterConfig(formatterConfig);

        setTargetProduct(binningOp.getTargetProduct());
    }


    @Override
    public void dispose() {
        super.dispose();
        if (beamNetCdf4WriterPlugIn != null) {
            ProductIOPlugInManager.getInstance().removeWriterPlugIn(beamNetCdf4WriterPlugIn);
        }
    }

    FormatterConfig getFormatterConfig() {
        final FormatterConfig formatterConfig = new FormatterConfig();
        formatterConfig.setOutputFormat("NetCDF4-BEAM");
        formatterConfig.setOutputFile("target.nc");
        formatterConfig.setOutputType("Product");
        return formatterConfig;
    }

    private BinningConfig createBinningConfig(String lcClassBandName) {
        VariableConfig variableConfig = new VariableConfig(lcClassBandName, null);
        LcAggregatorConfig lcAggregatorConfig = new LcAggregatorConfig(variableConfig.getName(),
                                                                       numberOfMajorityClasses);
        BinningConfig binningConfig = new BinningConfig();
        binningConfig.setMaskExpr("true");
        binningConfig.setNumRows(numRows);
        binningConfig.setSuperSampling(1);
        binningConfig.setAggregatorConfigs(lcAggregatorConfig);
        return binningConfig;
    }

    private ReferencedEnvelope createTargetEnvelope() {
        try {
            final Rectangle2D bounds = new Rectangle2D.Double();
            bounds.setFrameFromDiagonal(westBound, northBound, eastBound, southBound);
            final ReferencedEnvelope boundsEnvelope = new ReferencedEnvelope(bounds, DefaultGeographicCRS.WGS84);
            return boundsEnvelope.transform(DefaultGeographicCRS.WGS84, true);
        } catch (Exception e) {
            throw new OperatorException(e);
        }
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

    boolean isOutputMajorityClasses() {
        return outputMajorityClasses;
    }

    void setOutputMajorityClasses(boolean outputMajorityClasses) {
        this.outputMajorityClasses = outputMajorityClasses;
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
        if (westBound >= eastBound) {
            throw new OperatorException("West bound must be western of east bound.");
        }
        if (northBound <= southBound) {
            throw new OperatorException("North bound must be northern of south bound.");
        }
        if (!outputMajorityClasses && !outputPFTClasses) {
            throw new OperatorException("Nothing to process. Majority classes and/or " +
                                        "PFT classes must be selected for output.");
        }
    }

    /**
     * The Service Provider Interface (SPI) for the operator.
     * It provides operator meta-data and is a factory for new operator instances.
     */
    public static class Spi extends OperatorSpi {

        public Spi() {
            super(AggregationOp.class);
        }
    }

}
