/*
 * Copyright (C) 2014 by Array Systems Computing Inc. http://www.array.ca
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */
package org.esa.cci.lc.elevation;

import com.bc.ceres.glevel.support.AbstractMultiLevelSource;
import com.bc.ceres.glevel.support.DefaultMultiLevelImage;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.VirtualBand;
import org.esa.beam.framework.dataop.dem.ElevationModel;
import org.esa.beam.framework.dataop.dem.ElevationModelDescriptor;
import org.esa.beam.framework.dataop.dem.ElevationModelRegistry;
import org.esa.beam.framework.dataop.resamp.ResamplingFactory;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.jai.ImageManager;
import org.esa.beam.jai.RasterDataNodeSampleOpImage;
import org.esa.beam.jai.ResolutionLevel;
import org.esa.beam.util.ProductUtils;

import java.awt.image.RenderedImage;
import java.util.HashMap;
import java.util.Map;

/**
 * CreateElevationBandOp adds an elevation band to a product
 */

@OperatorMetadata(alias = "AddElevation",
        category = "Raster/DEM Tools",
        authors = "Jun Lu, Luis Veci, Martin Boettcher",
        copyright = "Copyright (C) 2014 by Array Systems Computing Inc. and Brockmann Consult GmbH",
        description = "Creates a DEM band")
public final class AddElevationOp extends Operator {

    @SourceProduct(alias = "source")
    private Product sourceProduct;
    @TargetProduct
    private Product targetProduct;

    @Parameter(description = "The digital elevation model.", defaultValue = "GETASSE30", label = "Digital Elevation Model")
    private String demName = "GETASSE30";

    @Parameter(description = "The elevation band name.", defaultValue = "elevation", label = "Elevation Band Name")
    private String elevationBandName = "elevation";

    @Parameter(defaultValue = ResamplingFactory.BILINEAR_INTERPOLATION_NAME,
               label = "Resampling Method")
    private String resamplingMethod = ResamplingFactory.BILINEAR_INTERPOLATION_NAME;

    private ElevationModel dem = null;
    private double noDataValue = 0;

    private final Map<Band, Band> sourceRasterMap = new HashMap<Band, Band>(10);

    /**
     * Initializes this operator and sets the one and only target product.
     * <p>The target product can be either defined by a field of type {@link Product} annotated with the
     * {@link TargetProduct TargetProduct} annotation or
     * by calling {@link #setTargetProduct} method.</p>
     * <p>The framework calls this method after it has created this operator.
     * Any client code that must be performed before computation of tile data
     * should be placed here.</p>
     *
     * @throws OperatorException If an error occurs during operator initialisation.
     * @see #getTargetProduct()
     */
    @Override
    public void initialize() throws OperatorException {

        try {

            final ElevationModelRegistry elevationModelRegistry = ElevationModelRegistry.getInstance();
            final ElevationModelDescriptor demDescriptor = elevationModelRegistry.getDescriptor(demName);
            if (demDescriptor == null) {
                throw new OperatorException("The DEM '" + demName + "' is not supported.");
            }
            if (demDescriptor.isInstallingDem()) {
                throw new OperatorException("The DEM '" + demName + "' is currently being installed.");
            }
            if (!demDescriptor.isDemInstalled()) {
                if (! demDescriptor.installDemFiles(null)) {
                    throw new OperatorException("The DEM '\" + demName + \"' is not installed.");
                }
            }
            dem = demDescriptor.createDem(ResamplingFactory.createResampling(resamplingMethod));
            noDataValue = dem.getDescriptor().getNoDataValue();

            createTargetProduct();

        } catch (Throwable e) {
            if (e instanceof OperatorException) {
                throw (OperatorException) e;
            } else {
                throw new OperatorException(getId() + ": " + e.getMessage(), e);
            }
        }
    }

    /**
     * Create target product.
     */
    void createTargetProduct() {

        targetProduct = new Product(sourceProduct.getName(),
                sourceProduct.getProductType(),
                sourceProduct.getSceneRasterWidth(),
                sourceProduct.getSceneRasterHeight());
        ProductUtils.copyProductNodes(sourceProduct, targetProduct);

        for (Band band : sourceProduct.getBands()) {
            if (band.getName().equalsIgnoreCase(elevationBandName))
                throw new OperatorException("Band " + elevationBandName + " already exists. Try another name.");
            if (band instanceof VirtualBand) {
                final VirtualBand sourceBand = (VirtualBand) band;
                final VirtualBand targetBand = new VirtualBand(sourceBand.getName(),
                        sourceBand.getDataType(),
                        sourceBand.getRasterWidth(),
                        sourceBand.getRasterHeight(),
                        sourceBand.getExpression());
                ProductUtils.copyRasterDataNodeProperties(sourceBand, targetBand);
                targetProduct.addBand(targetBand);
                sourceRasterMap.put(targetBand, band);
            } else {
                final Band targetBand = ProductUtils.copyBand(band.getName(), sourceProduct, targetProduct, false);
                targetBand.setSourceImage(band.getSourceImage());
                sourceRasterMap.put(targetBand, band);
            }
        }

        Band elevationBand = targetProduct.addBand(elevationBandName, ProductData.TYPE_INT16);
        elevationBand.setSynthetic(true);
        elevationBand.setNoDataValue(noDataValue);
        elevationBand.setNoDataValueUsed(true);
        elevationBand.setUnit("m");
        elevationBand.setDescription(dem.getDescriptor().getName());
        elevationBand.setSourceImage(createElevationSourceImage(dem, targetProduct.getGeoCoding(), elevationBand));
    }

    private static class ElevationSourceImage extends RasterDataNodeSampleOpImage {
        private final ElevationModel dem;
        private final GeoCoding geoCoding;
        private double noDataValue;

        public ElevationSourceImage(ElevationModel dem, GeoCoding geoCoding, Band band, ResolutionLevel level) {
            super(band, level);
            this.dem = dem;
            this.geoCoding = geoCoding;
            noDataValue = band.getNoDataValue();
        }

        @Override
        protected double computeSample(int sourceX, int sourceY) {
            GeoPos geoPos = geoCoding.getGeoPos(new PixelPos(sourceX + 0.5f, sourceY + 0.5f), null);
            try {
                return dem.getElevation(geoPos);
            } catch (Exception e) {
                return noDataValue;
            }
        }
    }

    private static RenderedImage createElevationSourceImage(final ElevationModel dem, final GeoCoding geoCoding, final Band band) {
        return new DefaultMultiLevelImage(new AbstractMultiLevelSource(ImageManager.getMultiLevelModel(band)) {
            @Override
            protected RenderedImage createImage(final int level) {
                return new ElevationSourceImage(dem, geoCoding, band, ResolutionLevel.create(getModel(), level));
            }
        });
    }


    /**
     * The SPI is used to register this operator in the graph processing framework
     * via the SPI configuration file
     * {@code META-INF/services/org.esa.snap.core.gpf.OperatorSpi}.
     * This class may also serve as a factory for new operator instances.
     *
     * @see OperatorSpi#createOperator()
     * @see OperatorSpi#createOperator(Map, Map)
     */
    public static class Spi extends OperatorSpi {
        public Spi() {
            super(AddElevationOp.class);
        }
    }
}
