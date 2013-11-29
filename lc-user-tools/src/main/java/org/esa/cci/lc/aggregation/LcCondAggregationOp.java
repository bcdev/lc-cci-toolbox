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
import org.esa.cci.lc.io.LcBinWriter;
import org.esa.cci.lc.io.LcCondMetadata;
import org.geotools.geometry.jts.ReferencedEnvelope;

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
        version = "3.3",
        authors = "Marco Peters",
        copyright = "(c) 2013 by Brockmann Consult",
        description = "Allows to aggregate LC condition products.")
public class LcCondAggregationOp extends AbstractLcAggregationOp implements Output {

    FormatterConfig formatterConfig;
    boolean outputTargetProduct;

    @Override
    public void initialize() throws OperatorException {
        super.initialize();
        validateInputSettings();
        final PlanetaryGrid planetaryGrid = createPlanetaryGrid();
        BinningConfig binningConfig = createBinningConfig(planetaryGrid);

        HashMap<String, String> lcProperties = getLcProperties();
        addAggregationTypeToLcProperties("Condition");
        addGridNameToLcProperties(planetaryGrid);

        MetadataElement globalAttributes = getSourceProduct().getMetadataRoot().getElement("Global_Attributes");
        addMetadataToLcProperties(globalAttributes);
        LcCondMetadata lcCondMetadata = new LcCondMetadata(getSourceProduct());
        lcProperties.put("condition", lcCondMetadata.getCondition());
        lcProperties.put("startYear", lcCondMetadata.getStartYear());
        lcProperties.put("endYear", lcCondMetadata.getEndYear());
        lcProperties.put("startDate", lcCondMetadata.getStartDate());

        String id = createId(lcProperties);
        if (formatterConfig == null) {
            formatterConfig = createDefaultFormatterConfig(id);
        }


        BinningOp binningOp;
        try {
            binningOp = new BinningOp();
        } catch (Exception e) {
            throw new OperatorException("Could not create binning operator.", e);
        }
        Product source = getSourceProduct();
        final ReferencedEnvelope regionEnvelope = getRegionEnvelope();
        if (regionEnvelope != null) {
            source = createSubset(source, regionEnvelope);
        }

        binningOp.setSourceProduct(source);
        binningOp.setOutputTargetProduct(outputTargetProduct);
        binningOp.setParameter("outputBinnedData", true);
        binningOp.setBinWriter(new LcBinWriter(lcProperties, regionEnvelope));
        binningOp.setBinningConfig(binningConfig);
        binningOp.setFormatterConfig(formatterConfig);

        Product dummyTarget = binningOp.getTargetProduct();
        setTargetProduct(dummyTarget);
    }

    private String createId(HashMap<String, String> lcProperties) {

        String condition = lcProperties.remove("condition");
        String startYear = lcProperties.remove("startYear");
        String endYear = lcProperties.remove("endYear");
        String startDate = lcProperties.remove("startDate");
        String spatialResolutionNominal = lcProperties.get("spatialResolutionNominal");
        String temporalResolution = lcProperties.get("temporalResolution");
        String version = lcProperties.get("version");

        String temporalCoverageYears = String.valueOf(Integer.parseInt(endYear) - Integer.parseInt(startYear) + 1);
        String typeString = String.format("ESACCI-LC-L4-%s-Cond-%sm-P%sY%sD", condition, spatialResolutionNominal,
                                          temporalCoverageYears, temporalResolution);
        int numRows = getNumRows();
        String aggrResolution = getGridName().equals(PlanetaryGridName.GEOGRAPHIC_LAT_LON)
                                ? String.format(Locale.ENGLISH, "aggregated-%.6fDeg", 180.0 / numRows)
                                : String.format(Locale.ENGLISH, "aggregated-N" + numRows / 2);
        lcProperties.put("type", typeString);
        String id;
        final String regionIdentifier = getRegionIdentifier();
        if (regionIdentifier != null) {
            id = String.format("%s-%s-%s-%s-v%s", typeString, aggrResolution, regionIdentifier, startDate, version);
        } else {
            id = String.format("%s-%s-%s-v%s", typeString, aggrResolution, startDate, version);
        }
        lcProperties.put("id", id);
        return id;
    }

    private BinningConfig createBinningConfig(final PlanetaryGrid planetaryGrid) {
        String sourceFileName = getSourceProduct().getFileLocation().getName();
        AggregatorConfig aggregatorConfig;
        Product sourceProduct = getSourceProduct();
        if (sourceFileName.toUpperCase().contains("NDVI")) {
            final String[] ndviBandNames = sourceProduct.getBandNames();
            String[] variableNames = new String[3];
            variableNames[0] = ndviBandNames[0]; // ndvi_mean
            variableNames[1] = ndviBandNames[1]; // ndvi_std
            variableNames[2] = ndviBandNames[3]; // ndvi_nYearObs
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
