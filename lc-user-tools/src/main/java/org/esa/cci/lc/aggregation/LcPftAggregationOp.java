package org.esa.cci.lc.aggregation;


import org.esa.cci.lc.io.LcBinWriter;
import org.esa.snap.binning.AggregatorConfig;
import org.esa.snap.binning.PlanetaryGrid;
import org.esa.snap.binning.operator.BinningOp;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.geotools.geometry.jts.ReferencedEnvelope;

import java.io.File;
import java.util.HashMap;

@OperatorMetadata(
        alias = "LC.Aggregate.Pft",
        internal = true,
        version = "4.7",
        authors = "Roman Shevchuk",
        copyright = "(c) 2022 by Brockmann Consult",
        description = "Allows to aggregate LC PFT products.",
        autoWriteDisabled = true)
public class LcPftAggregationOp extends AbstractLcAggregationOp {


    boolean outputTargetProduct;


    @Override
    public void initialize() throws OperatorException {
        super.initialize();
        Product source = getSourceProduct();
        final String planetaryGridClassName = getPlanetaryGridClassName();

        final HashMap<String, String> lcProperties = getLcProperties();
        String id = createTypeAndID();
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

        binningOp.setSourceProduct(source);
        initBinningOp(planetaryGridClassName, binningOp, id + ".nc");

        binningOp.setOutputTargetProduct(outputTargetProduct);
        binningOp.setParameter("outputBinnedData", true);
        binningOp.setBinWriter(new LcBinWriter(lcProperties, regionEnvelope));

    }


    private void initBinningOp(String planetaryGridClassName, BinningOp binningOp, String outputFilename) {
        Product sourceProduct = getSourceProduct();
        //final String mapType = sourceProduct.getFileLocation() != null ? LcMapMetadata.mapTypeOf(sourceProduct.getFileLocation().getName()) : "unknown";
        int sceneWidth = sourceProduct.getSceneRasterWidth();
        int sceneHeight = sourceProduct.getSceneRasterHeight();
        final double sourceMapResolutionX = 180.0 / sceneHeight;
        final double sourceMapResolutionY = 360.0 / sceneWidth;
        PlanetaryGrid planetaryGrid = createPlanetaryGrid();
        AreaCalculator areaCalculator = new FractionalAreaCalculator(planetaryGrid, sourceMapResolutionX, sourceMapResolutionY);
        binningOp.setNumRows(getNumRows());
        binningOp.setSuperSampling(1);

        LcPftAggregatorConfig lcPftAggregatorConfig = new LcPftAggregatorConfig(areaCalculator);
        AggregatorConfig[] aggregatorConfigs;
        aggregatorConfigs = new AggregatorConfig[]{lcPftAggregatorConfig};


        binningOp.setAggregatorConfigs(aggregatorConfigs);
        binningOp.setPlanetaryGridClass(planetaryGridClassName);
        binningOp.setOutputFile(getOutputFile() == null ? new File(getTargetDir(), outputFilename).getPath() : getOutputFile());
        binningOp.setOutputType(getOutputType() == null ? "Product" : getOutputType());
        binningOp.setOutputFormat(getOutputFormat());

        Product dummyTarget = binningOp.getTargetProduct();
        setTargetProduct(dummyTarget);
    }



    private String createTypeAndID(){

        return "ID";
    }


    /**
     * The Service Provider Interface (SPI) for the operator.
     * It provides operator meta-data and is a factory for new operator instances.
     */
    public static class Spi extends OperatorSpi {

        public Spi() {
            super(LcPftAggregationOp.class);
        }
    }
}
