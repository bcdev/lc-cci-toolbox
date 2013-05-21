package org.esa.cci.lc.conversion;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.experimental.Output;
import org.esa.beam.gpf.operators.standard.WriteOp;
import org.esa.beam.util.Debug;

import java.io.File;
import java.text.MessageFormat;

/**
 * This operator converts the LC CCI GeoTIFF files of a map product or a condition product
 * into NetCDF4 with CF and LC metadata and LC CCI file names.
 *
 * @author Martin Böttcher
 */
@OperatorMetadata(
        alias = "LCCCI.Convert",
        version = "0.5",
        internal = true,
        authors = "Martin Böttcher, Marco Peters",
        copyright = "(c) 2013 by Brockmann Consult",
        description = "Converts LC CCI GeoTiff Map products to NetCDF4 with CF and LC metadata and file names")
public class LcConversionOp extends Operator implements Output {

    private static final String LC_MAP_FORMAT = LcMapNetCdf4WriterPlugIn.FORMAT_NAME;
    private static final String LC_CONDITION_FORMAT = LcConditionNetCdf4WriterPlugIn.FORMAT_NAME;

    @SourceProduct(description = "LC CCI map conversion input.", optional = false)
    private Product sourceProduct;

    @Override
    public void initialize() throws OperatorException {
        Debug.setEnabled(true);

        final String lcOutputFilename;
        File sourceFile = sourceProduct.getFileLocation();

        String outputFormat;
        if (sourceFile.getName().startsWith("ESACCI_LC_Map")) {
            outputFormat = LC_MAP_FORMAT;
            final LcMapMetadata metadata = new LcMapMetadata(sourceProduct);
            lcOutputFilename = MessageFormat.format("ESACCI-LC-L4-LCCS-Map-{0}m-P{1}Y-{2}-v{3}.nc",
                                                    metadata.getSpatialResolution(),
                                                    metadata.getTemporalResolution(),
                                                    metadata.getEpoch(),
                                                    metadata.getVersion());
        } else {
            outputFormat = LC_CONDITION_FORMAT;
            LcCondMetadata metadata = new LcCondMetadata(sourceProduct);
            lcOutputFilename = MessageFormat.format("ESACCI-LC-L4-{0}-Cond-{1}m-P{2}D-{3}-{4}-{5}-v{6}.nc",
                                                    metadata.getCondition(),
                                                    metadata.getSpatialResolution(),
                                                    metadata.getTemporalResolution(),
                                                    metadata.getStartYear(),
                                                    metadata.getEndYear(),
                                                    metadata.getWeekNumber(),
                                                    metadata.getVersion());
        }

        File targetFile = new File(sourceFile.getParent(), lcOutputFilename);
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
