package org.esa.cci.lc.aggregation;

import org.esa.beam.framework.datamodel.CrsGeoCoding;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.util.math.MathUtils;
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
    Product source;

    @Parameter(description = "Defines the projection method for the target product.",
               valueSet = {"GAUSSIAN_GRID", "GEOGRAPHIC_LAT_LON", "ROTATED_LAT_LON"}, defaultValue = "GAUSSIAN_GRID")
    ProjectionMethod projectionMethod;

    @Parameter(description = "Size of a pixel in X-direction in degree.", defaultValue = "0.1", unit = "°")
    double pixelSizeX;
    @Parameter(description = "Size of a pixel in Y-direction in degree.", defaultValue = "0.1", unit = "°")
    double pixelSizeY;

    @Parameter(description = "The western longitude.", interval = "[-180,180]", defaultValue = "-15.0", unit = "°")
    double westBound;
    @Parameter(description = "The northern latitude.", interval = "[-90,90]", defaultValue = "75.0", unit = "°")
    double northBound;
    @Parameter(description = "The eastern longitude.", interval = "[-180,180]", defaultValue = "30.0", unit = "°")
    double eastBound;
    @Parameter(description = "The southern latitude.", interval = "[-90,90]", defaultValue = "35.0", unit = "°")
    double southBound;

    @Parameter(description = "Whether or not to add majority classes and the fractional area to the output.",
               defaultValue = "true")
    boolean outputMajorityClasses;
    // todo (mp, 26.07.12) - set value range if max. classes is fixed or add validator which uses input to define maximum.
    @Parameter(description = "The number of majority classes generated and added to the output.", defaultValue = "5")
    int numberOfMajorityClasses;

    @Parameter(description = "Whether or not to add PFT classes to the output.", defaultValue = "true")
    boolean outputPFTClasses;
    // todo (mp, 26.07.12) - set value range if max. classes is fixed or add validator which uses input to define maximum.
    // todo (mp, 26.07.12) - meaningful default value?
    @Parameter(description = "The number of PFT classes generated and added to the output.", defaultValue = "3")
    int numberOfPFTClasses;

    @Override
    public void initialize() throws OperatorException {
        validateParameters();
        // validateSourceProduct();
        setTargetProduct(createTargetProduct());
    }

    private Product createTargetProduct() {
        final ReferencedEnvelope targetEnvelope = createTargetEnvelope();
        final int sceneWidth = MathUtils.floorInt(targetEnvelope.getSpan(0) / pixelSizeX);
        final int sceneHeight = MathUtils.floorInt(targetEnvelope.getSpan(1) / pixelSizeY);
        Product targetProduct = new Product("Aggregated LC Product", "LC_AGG", sceneWidth, sceneHeight);
        final CrsGeoCoding geoCoding = createGeoCoding(targetEnvelope, sceneWidth, sceneHeight);
        targetProduct.setGeoCoding(geoCoding);
        return targetProduct;
    }


    private CrsGeoCoding createGeoCoding(ReferencedEnvelope targetEnvelope, int sceneWidth, int sceneHeight) {
        // todo (mp, 26.07.12) - Currently only creating WGS84 CRS, needs to be extended.
        if (projectionMethod != ProjectionMethod.GEOGRAPHIC_LAT_LON) {
            throw new IllegalStateException("Only Geographic Lat/Lon implemented as projection method.");
        }
        try {
            return new CrsGeoCoding(DefaultGeographicCRS.WGS84, sceneWidth, sceneHeight,
                                    targetEnvelope.getMinimum(0), targetEnvelope.getMaximum(1),
                                    pixelSizeX, pixelSizeY);
        } catch (Exception e) {
            throw new OperatorException(e);
        }
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

    int getNumberOfPFTClasses() {
        return numberOfPFTClasses;
    }

    void setNumberOfPFTClasses(int numberOfPFTClasses) {
        this.numberOfPFTClasses = numberOfPFTClasses;
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
