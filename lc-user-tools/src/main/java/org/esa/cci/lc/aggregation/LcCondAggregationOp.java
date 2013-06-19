package org.esa.cci.lc.aggregation;

import org.esa.beam.binning.AggregatorConfig;
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
import org.esa.beam.framework.gpf.experimental.Output;
import org.esa.beam.util.Debug;
import org.esa.cci.lc.io.LcBinWriter;
import org.esa.cci.lc.io.LcCondMetadata;
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
        alias = "LCCCI.Aggregate.Cond",
        version = "0.6",
        authors = "Marco Peters",
        copyright = "(c) 2013 by Brockmann Consult",
        description = "Allows to aggregate LC cond products.")
public class LcCondAggregationOp extends AbstractLcAggregationOp implements Output {

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

        HashMap<String, String> lcProperties = getLcProperties();
        lcProperties.put("aggregationType", "Condition");

        MetadataElement globalAttributes = getSourceProduct().getMetadataRoot().getElement("Global_Attributes");
        addMetadataToLcProperties(globalAttributes);
        LcCondMetadata lcCondMetadata = new LcCondMetadata(getSourceProduct());
        lcProperties.put("condition", lcCondMetadata.getCondition());
        lcProperties.put("startYear", lcCondMetadata.getStartYear());
        lcProperties.put("endYear", lcCondMetadata.getEndYear());
        lcProperties.put("startDate", lcCondMetadata.getStartDate());


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
        String sourceFileName = getSourceProduct().getFileLocation().getName();
        AggregatorConfig aggregatorConfig;
        Product sourceProduct = getSourceProduct();
        if (sourceFileName.toUpperCase().contains("NDVI")) {
            String[] variableNames = Arrays.copyOf(sourceProduct.getBandNames(), 3);
            aggregatorConfig = new LcNDVIAggregatorConfig(variableNames);
        } else {
            String[] variableNames = Arrays.copyOf(sourceProduct.getBandNames(), 2);
            aggregatorConfig = new LcCondOccAggregatorConfig(variableNames);
        }

        BinningConfig binningConfig = new BinningConfig();
        binningConfig.setNumRows(getNumRows());
        binningConfig.setSuperSampling(1);
        binningConfig.setAggregatorConfigs(aggregatorConfig);
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


    /**
     * The Service Provider Interface (SPI) for the operator.
     * It provides operator meta-data and is a factory for new operator instances.
     */
    public static class Spi extends OperatorSpi {

        public Spi() {
            super(LcCondAggregationOp.class);
        }
    }

}
