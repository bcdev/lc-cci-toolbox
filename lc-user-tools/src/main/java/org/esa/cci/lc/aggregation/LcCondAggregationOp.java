package org.esa.cci.lc.aggregation;

import org.esa.beam.binning.AggregatorConfig;
import org.esa.beam.binning.operator.BinningOp;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.cci.lc.io.LcBinWriter;
import org.esa.cci.lc.io.LcCondMetadata;
import org.esa.cci.lc.util.PlanetaryGridName;
import org.geotools.geometry.jts.ReferencedEnvelope;

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
        internal = true,
        version = "3.15",
        authors = "Marco Peters",
        copyright = "(c) 2014 by Brockmann Consult",
        description = "Allows to aggregate LC condition products.",
        autoWriteDisabled = true)
public class LcCondAggregationOp extends AbstractLcAggregationOp {

    boolean outputTargetProduct;

    @Override
    public void initialize() throws OperatorException {
        super.initialize();
        validateInputSettings();
        final String planetaryGridClassName = getPlanetaryGridClassName();

        HashMap<String, String> lcProperties = getLcProperties();
        addAggregationTypeToLcProperties("Condition");
        addGridNameToLcProperties(planetaryGridClassName);

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
            binningOp.setParameterDefaultValues();
        } catch (Exception e) {
            throw new OperatorException("Could not create binning operator.", e);
        }
        Product source = getSourceProduct();
        final ReferencedEnvelope regionEnvelope = getRegionEnvelope();
        if (regionEnvelope != null) {
            source = createSubset(source, regionEnvelope);
        }

        String id = createTypeAndId(lcProperties);
        initBinningOp(planetaryGridClassName, binningOp, id + ".nc");
        binningOp.setSourceProduct(source);
        binningOp.setOutputTargetProduct(outputTargetProduct);
        binningOp.setParameter("outputBinnedData", true);
        binningOp.setBinWriter(new LcBinWriter(lcProperties, regionEnvelope));

        Product dummyTarget = binningOp.getTargetProduct();
        setTargetProduct(dummyTarget);
    }

    private String createTypeAndId(HashMap<String, String> lcProperties) {

        String condition = lcProperties.remove("condition");
        String startYear = lcProperties.remove("startYear");
        String endYear = lcProperties.remove("endYear");
        String startDate = lcProperties.remove("startDate");
        String spatialResolutionNominal = lcProperties.get("spatialResolutionNominal");
        String temporalResolution = lcProperties.get("temporalResolution");
        String version = lcProperties.get("version");

        String temporalCoverageYears = String.valueOf(Integer.parseInt(endYear) - Integer.parseInt(startYear) + 1);
        String typeString = String.format("ESACCI-LC-L4-%s-Cond-%s-P%sY%sD", condition, spatialResolutionNominal,
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

    private void initBinningOp(String planetaryGridClassName, BinningOp binningOp, String outputFileName) {
        String sourceFileName = getSourceProduct().getFileLocation().getName();
        AggregatorConfig aggregatorConfig;
        Product sourceProduct = getSourceProduct();
        String maskExpression = null;
        if (isSourceNDVI(sourceFileName)) {
            maskExpression = "ndvi_status == 1";
            final String[] ndviBandNames = sourceProduct.getBandNames();
            String[] variableNames = new String[2];
            variableNames[0] = ndviBandNames[0]; // ndvi_mean
            variableNames[1] = ndviBandNames[3]; // ndvi_nYearObs
            String[] featureNameTemplates = {"%s_mean", "%s_sum"};
            aggregatorConfig = new LcNDVIAggregatorConfig(variableNames, featureNameTemplates);
        } else {
            if (isSourceSnow(sourceFileName)) {
                maskExpression = "snow_occ >= 0 && snow_occ <= 100";
            }

            if (isSourceBA(sourceFileName)) {
                maskExpression = "ba_occ >= 0 && ba_occ <= 100";
            }
            String[] variableNames = Arrays.copyOf(sourceProduct.getBandNames(), 2);
            String[] featureNameTemplates = {"%s_proportion_area", "%s_mean_frequency", "%s_sum"};
            aggregatorConfig = new LcCondOccAggregatorConfig(variableNames, featureNameTemplates);
        }

        binningOp.setMaskExpr(maskExpression);
        binningOp.setNumRows(getNumRows());
        binningOp.setAggregatorConfigs(aggregatorConfig);
        binningOp.setPlanetaryGridClass(planetaryGridClassName);
        binningOp.setOutputFile(getOutputFile() == null ? new File(getTargetDir(), outputFileName).getPath() : getOutputFile());
        binningOp.setOutputType(getOutputType() == null ? "Product" : getOutputType());
        binningOp.setOutputFormat(getOutputFormat());
    }

    private boolean isSourceBA(String sourceFileName) {
        return sourceFileName.toUpperCase().contains("BA");
    }

    private boolean isSourceSnow(String sourceFileName) {
        return sourceFileName.toUpperCase().contains("SNOW");
    }

    private boolean isSourceNDVI(String sourceFileName) {
        return sourceFileName.toUpperCase().contains("NDVI");
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
