package org.esa.cci.lc.conversion;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.MetadataAttribute;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.common.WriteOp;
import org.esa.snap.core.util.Debug;
import org.esa.cci.lc.io.LcCondMetadata;
import org.esa.cci.lc.io.LcConditionNetCdf4WriterPlugIn;
import org.esa.cci.lc.io.LcMapMetadata;
import org.esa.cci.lc.io.LcMapNetCdf4WriterPlugIn;
import org.esa.cci.lc.io.LcWbMetadata;
import org.esa.cci.lc.io.LcWbNetCdf4WriterPlugIn;
import org.esa.cci.lc.util.LcHelper;


import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This operator converts the LC CCI GeoTIFF files of a map product or a condition product
 * into NetCDF4 with CF and LC metadata and LC CCI file names.
 *
 * @author Martin Böttcher
 */
@OperatorMetadata(
        alias = "LCCCI.Convert",
        internal = true,
        version = "4.1-SNAPSHOT",
        authors = "Martin Böttcher, Marco Peters",
        copyright = "(c) 2015 by Brockmann Consult",
        description = "Converts LC CCI GeoTiff Map products to NetCDF4 with CF and LC metadata and file names",
        autoWriteDisabled = true
)
public class LcConversionOp extends Operator {

    private static final String LC_MAP_FORMAT = LcMapNetCdf4WriterPlugIn.FORMAT_NAME;
    private static final String LC_WB_FORMAT = LcWbNetCdf4WriterPlugIn.FORMAT_NAME;
    private static final String LC_CONDITION_FORMAT = LcConditionNetCdf4WriterPlugIn.FORMAT_NAME;
    private static final String LC_CDS_FILENAME_FORMAT = "ESACCI-LC-L4-LCCS-Map-300m-P1Y-(....)-v2.0.7b.nc";
    private static final String BA_CDS_FILENAME_FORMAT = "(........)-ESACCI-L4_FIRE-BA-MODIS-fv5.0.nc";

    @SourceProduct(description = "LC CCI map conversion input.", optional = false)
    private Product sourceProduct;
    @Parameter(description = "The target directory. Default is the directory of the source product.")
    private File targetDir;
    @Parameter(description = "Version of the target file. Replacing the one given by the source product")
    private String targetVersion;
    @Parameter(description = "Format of the output file: lccci,lccds,bacds,clcds,ppcds.",defaultValue = "lccci")
    private String format;

    @Override
    public void initialize() throws OperatorException {
        Debug.setEnabled(true);

        final File sourceFile = sourceProduct.getFileLocation();

        String typeString;
        String id;
        String outputFormat;
        if ("lccci".equals(format) && sourceFile.getName().startsWith("ESACCI-LC-L4-LCCS-Map")) {
            outputFormat = LC_MAP_FORMAT;
            final LcMapMetadata metadata = new LcMapMetadata(sourceProduct);
            typeString = String.format("ESACCI-LC-L4-LCCS-%s-%s-P%sY",
                                       metadata.getMapType(),
                                       metadata.getSpatialResolution(),
                                       metadata.getTemporalResolution());
            id = String.format("%s-%s-v%s",
                               typeString,
                               metadata.getEpoch(),
                               targetVersion != null ? targetVersion : metadata.getVersion());
        } else if ("lccci".equals(format) && sourceFile.getName().startsWith("ESACCI-LC-L4-WB-Map")) {
            outputFormat = LC_WB_FORMAT;
            final LcWbMetadata metadata = new LcWbMetadata(sourceProduct);
            typeString = String.format("ESACCI-LC-L4-WB-Map-%s-P%sY",
                                       metadata.getSpatialResolution(),
                                       metadata.getTemporalResolution());
            id = String.format("%s-%s-v%s",
                               typeString,
                               metadata.getEpoch(),
                               targetVersion != null ? targetVersion : metadata.getVersion());
        }
        else if  ("lccci".equals(format)) {
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
        else if ("lccds".equals(format) || "bacds".equals(format))  {
            outputFormat = "NetCDF4-LC-CDS";
            id = sourceFile.getName();
            sourceProduct.getMetadataRoot().getElement("global_attributes").setAttributeString("parent_path", sourceProduct.getFileLocation().getAbsolutePath());

            if ("lccds".equals(format)) {
                Pattern p = Pattern.compile(LC_CDS_FILENAME_FORMAT);
                final Matcher m = p.matcher(sourceFile.getName());
                if (!m.matches()) {
                    throw new IllegalArgumentException("input file name " + sourceFile.getName() + " does not match pattern " + LC_CDS_FILENAME_FORMAT);
                }


                typeString = sourceProduct.getMetadataRoot().getElement("global_attributes").getAttributeString("type");
            }
            else {
                Pattern p = Pattern.compile(BA_CDS_FILENAME_FORMAT);
                final Matcher m = p.matcher(sourceFile.getName());
                if (!m.matches()) {
                    throw new IllegalArgumentException("input file name " + sourceFile.getName() + " does not match pattern " + BA_CDS_FILENAME_FORMAT);
                }
                typeString="burned_area";
                id=id+"cds";
            }
            String productVersion=sourceProduct.getMetadataRoot().getElement("global_attributes").getAttributeString("product_version");
            if (targetVersion==null) {
                targetVersion=productVersion+"cds";
            }
            id=id.replace(productVersion+"b.nc",targetVersion);
            id=id.replaceFirst(".nc","");
        }
        else if ("ppcds".equals(format)) {
            typeString="pixel_product";
            id=sourceFile.getName().replace("-LC.tif","cds");
            outputFormat = "NetCDF4-LC-CDS";
            sourceProduct.getMetadataRoot().getElement("global_attributes").setAttributeString("parent_path", sourceProduct.getFileLocation().getAbsolutePath());

            try
            {
                 sourceProduct = ProductIO.readProduct(sourceFile,"LC_CDS_TIFF");
            }
            catch (IOException e){}
        }
        else {
            throw  new OperatorException("Unknown format "+format);
        }
        // setting the id in order to hand over to the writer
        MetadataElement metadataRoot = sourceProduct.getMetadataRoot();
        if (metadataRoot==null){
            throw new OperatorException(sourceProduct.getName()+" lacks metadataRoot, reader is "+sourceProduct.getProductReader());
        }
        if (metadataRoot.getElement("global_attributes")==null){
            throw new OperatorException(sourceProduct.getName()+" lacks global_attributes, reader is "+sourceProduct.getProductReader());
        }
        metadataRoot.getElement("global_attributes").setAttributeString("type", typeString);
        metadataRoot.getElement("global_attributes").setAttributeString("id", id);
        if (targetVersion != null) {
            metadataRoot.getElement("global_attributes").setAttributeString("product_version", targetVersion);
        }

        if (targetDir == null) {
            targetDir = sourceFile.getParentFile();
        }

        File targetFile = new File(targetDir, id + ".nc");
        sourceProduct.setPreferredTileSize(LcHelper.TILE_SIZE);
        WriteOp writeOp = new WriteOp(sourceProduct, targetFile, outputFormat);
        writeOp.setWriteEntireTileRows(false);
        writeOp.setClearCacheAfterRowWrite(true);
        // If execution order is not set to SCHEDULE_BAND_ROW_COLUMN a Java heap space error occurs multiple times
        // if only 2GB of heap space is available:
        // Exception in thread "SunTileScheduler0Standard2" java.lang.OutOfMemoryError: Java heap space
        System.setProperty("snap.gpf.executionOrder", "SCHEDULE_BAND_ROW_COLUMN");
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
