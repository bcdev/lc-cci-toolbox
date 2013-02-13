package org.esa.cci.lc.conversion;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.dataio.netcdf.metadata.profiles.beam.BeamNetCdf4WriterPlugIn;
import org.esa.beam.framework.dataio.ProductIOPlugInManager;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.experimental.Output;
import org.esa.beam.gpf.operators.standard.WriteOp;
import org.esa.beam.jai.ImageManager;
import org.esa.beam.util.Debug;

import java.io.File;

/**
 * This operator converts the LC CCI GeoTIFF files of a map product or a condition product
 * into NetCDF4 with CF and LC metadata and LC CCI file names.
 *
 * @author Martin Böttcher
 */
@OperatorMetadata(
        alias = "LCCCI.Convert",
        version = "0.5",
        authors = "Martin Böttcher",
        copyright = "(c) 2013 by Brockmann Consult",
        description = "Converts LC CCI GeoTiff Map products to NetCDF4 with CF and LC metadata and file names")
public class LcConversionOp extends Operator implements Output {

    @SourceProduct(description = "LC CCI map conversion input.", optional = false)
    private Product sourceProduct;

    @Parameter(description = "Target file location.", defaultValue = "")
    private File targetFile;

    private static final String NETCDF4_LC_MAP_FORMAT = "NetCDF4-LC-Map";

    @Override
    public void initialize() throws OperatorException {
        Debug.setEnabled(true);

        if (targetFile == null) {
            targetFile = new File(sourceProduct.getFileLocation().getParent(), "dummy");
        }

        WriteOp writeOp = new WriteOp(sourceProduct, targetFile, NETCDF4_LC_MAP_FORMAT);
        writeOp.setClearCacheAfterRowWrite(true);
        setTargetProduct(writeOp.getTargetProduct());

        writeOp.writeProduct(ProgressMonitor.NULL);
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

    public void setTargetFile(File targetFile) {
        this.targetFile = targetFile;
    }

    public void setSourceProduct(Product sourceProduct) {
        this.sourceProduct = sourceProduct;
    }
}
