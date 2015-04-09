package org.esa.cci.lc.conversion;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.experimental.Output;
import org.esa.beam.gpf.operators.standard.WriteOp;
import org.esa.beam.util.Debug;
import org.esa.cci.lc.io.LcCondMetadata;
import org.esa.cci.lc.io.LcConditionNetCdf4WriterPlugIn;
import org.esa.cci.lc.io.LcMapMetadata;
import org.esa.cci.lc.io.LcMapNetCdf4WriterPlugIn;
import org.esa.cci.lc.io.LcWbMetadata;
import org.esa.cci.lc.io.LcWbNetCdf4WriterPlugIn;

import java.io.File;

/**
 * This operator converts the LC CCI GeoTIFF files of a map product or a condition product
 * into NetCDF4 with CF and LC metadata and LC CCI file names.
 *
 * @author Martin Böttcher
 */
@OperatorMetadata(
        alias = "LCCCI.Convert",
        version = "3.8",
        internal = true,
        authors = "Martin Böttcher, Marco Peters",
        copyright = "(c) 2015 by Brockmann Consult",
        description = "Converts LC CCI GeoTiff Map products to NetCDF4 with CF and LC metadata and file names")
public class LcConversionOp extends Operator implements Output {

    private static final String LC_MAP_FORMAT = LcMapNetCdf4WriterPlugIn.FORMAT_NAME;
    private static final String LC_WB_FORMAT = LcWbNetCdf4WriterPlugIn.FORMAT_NAME;
    private static final String LC_CONDITION_FORMAT = LcConditionNetCdf4WriterPlugIn.FORMAT_NAME;

    @SourceProduct(description = "LC CCI map conversion input.", optional = false)
    private Product sourceProduct;
    @Parameter(description = "The target directory. Default is the directory of the source product.")
    private File targetDir;
    @Parameter(description = "Version of the target file. Replacing the one given by the source product")
    private String targetVersion;

    @Override
    public void initialize() throws OperatorException {
        Debug.setEnabled(true);

        File sourceFile = sourceProduct.getFileLocation();


        String typeString;
        String id;
        String outputFormat;
        if (sourceFile.getName().startsWith("ESACCI-LC-L4-LCCS-Map")) {
            outputFormat = LC_MAP_FORMAT;
            final LcMapMetadata metadata = new LcMapMetadata(sourceProduct);
            typeString = String.format("ESACCI-LC-L4-LCCS-Map-%s-P%sY",
                                       metadata.getSpatialResolution(),
                                       metadata.getTemporalResolution());
            id = String.format("%s-%s-v%s",
                               typeString,
                               metadata.getEpoch(),
                               targetVersion != null ? targetVersion : metadata.getVersion());
        } else if (sourceFile.getName().startsWith("ESACCI-LC-L4-WB-Map")) {
            outputFormat = LC_WB_FORMAT;
            final LcWbMetadata metadata = new LcWbMetadata(sourceProduct);
            typeString = String.format("ESACCI-LC-L4-WB-Map-%s-P%sY",
                                       metadata.getSpatialResolution(),
                                       metadata.getTemporalResolution());
            id = String.format("%s-%s-v%s",
                               typeString,
                               metadata.getEpoch(),
                               targetVersion != null ? targetVersion : metadata.getVersion());
        } else {
            outputFormat = LC_CONDITION_FORMAT;
            LcCondMetadata metadata = new LcCondMetadata(sourceProduct);
            String temporalCoverageYears = String.valueOf(Integer.parseInt(metadata.getEndYear()) - Integer.parseInt(metadata.getStartYear()) + 1);
            typeString = String.format("ESACCI-LC-L4-%s-Cond-%s-P%sY%sD",
                                       metadata.getCondition(),
                                       metadata.getSpatialResolution(),
                                       temporalCoverageYears,
                                       metadata.getTemporalResolution());
            id = String.format("%s-%s-v%s",
                               typeString,
                               metadata.getStartDate(),
                               targetVersion != null ? targetVersion : metadata.getVersion());
        }

        // setting the id in order to hand over to the writer
        MetadataElement metadataRoot = sourceProduct.getMetadataRoot();
        metadataRoot.setAttributeString("type", typeString);
        metadataRoot.setAttributeString("id", id);
        if (targetVersion != null) {
            metadataRoot.setAttributeString("version", targetVersion);
        }

        if (targetDir == null) {
            targetDir = sourceFile.getParentFile();
        }

        File targetFile = new File(targetDir, String.format(id + ".nc"));
        WriteOp writeOp = new WriteOp(sourceProduct, targetFile, outputFormat);
        writeOp.setClearCacheAfterRowWrite(true);
        // If execution order is not set to SCHEDULE_BAND_ROW_COLUMN a Java heap space error occurs multiple times
        // if only 2GB of heap space is available:
        // Exception in thread "SunTileScheduler0Standard2" java.lang.OutOfMemoryError: Java heap space
        System.setProperty("beam.gpf.executionOrder", "SCHEDULE_BAND_ROW_COLUMN");
        writeOp.writeProduct(ProgressMonitor.NULL);
        setTargetProduct(new Product("foo", "dummy", 2, 2));
    }


    /**
     * The Service Provider Interface (SPI) for the operator.
     * It provides operator meta-data and is a factory for new operator instances.
     */
    public static class Spi extends OperatorSpi {

        public Spi() {
            super(LcConversionOp.class);
        }

    }

}
