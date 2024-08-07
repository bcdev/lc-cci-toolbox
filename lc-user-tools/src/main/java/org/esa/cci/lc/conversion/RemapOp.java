package org.esa.cci.lc.conversion;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.gpf.common.WriteOp;
import org.esa.snap.core.util.io.FileUtils;
import org.esa.cci.lc.util.LcHelper;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

/**
 * This operator converts the lCCS classes to PFT classes staying without resampling them onto a different grid.
 *
 * @author Marco Peters
 */
@OperatorMetadata(
        alias = "LCCCI.Remap",
        internal = true,
        version = "3.15",
        authors = "Marco Peters",
        copyright = "(c) 2015 by Brockmann Consult",
        description = "Remaps the LCCS classes to pft classes on the same grid as the input.",
        autoWriteDisabled = true
)
public class RemapOp extends Operator {

    @SourceProduct()
    private Product sourceProduct;

    @TargetProduct(description = "The target product containing the pft classes.")
    private Product targetProduct;

    @Parameter(description = "The user defined conversion table from LCCS to PFTs. " +
            "If not given, the standard LC-CCI table is used.",
            label = "User Defined PFT Conversion Table")
    private File userPFTConversionTable;

    @Parameter(description = "A map containing additional classes which can be used to refine " +
            "the conversion from LCCS to PFT classes")
    private File additionalUserMap;

    @Parameter(description = "The conversion table from LCCS to PFTs considering the additional user map. " +
            "This option is only applicable if the additional user map is given too.",
            label = "Additional User Map PFT Conversion Table")
    private File additionalUserMapPFTConversionTable;

    @Parameter(description = "Format of the output file: lccci,lccds",defaultValue = "lccci")
    private String format;

    @Override
    public void initialize() throws OperatorException {
        final HashMap<String, Object> parameters = new HashMap<>();
        parameters.put("userPFTConversionTable", userPFTConversionTable);
        parameters.put("additionalUserMapPFTConversionTable", additionalUserMapPFTConversionTable);
        final HashMap<String, Product> productMap = new HashMap<>();
        productMap.put("sourceProduct", sourceProduct);
        if (additionalUserMap != null) {
            try {
                productMap.put("additionalUserMap", ProductIO.readProduct(additionalUserMap));
            } catch (IOException ioe) {
                throw new OperatorException("Not able to read additionalUserMap", ioe);
            }
        }
        targetProduct = GPF.createProduct("LCCCI.RemapIntern", parameters, productMap);

        writeTarget();
    }

    private void writeTarget() {
        final String targetFileName = FileUtils.getFilenameWithoutExtension(sourceProduct.getFileLocation()) + "_updated.nc";
        final String targetDir = sourceProduct.getFileLocation().getParent();
        //final String formatName = "NetCDF4-LC-Map";


        String formatName = "NetCDF4-LC-Map";
        if (format.equals("lccds")){
             formatName = "NetCDF4-LC-CDS";
            targetProduct.getMetadataRoot().getElement("global_attributes").setAttributeString("parent_path", sourceProduct.getFileLocation().getAbsolutePath());
        }


        File targetFile = new File(targetDir, targetFileName);
        targetProduct.setPreferredTileSize(LcHelper.TILE_SIZE);
        WriteOp writeOp = new WriteOp(targetProduct, targetFile, formatName);
        writeOp.setClearCacheAfterRowWrite(true);
        writeOp.setWriteEntireTileRows(false);
        System.setProperty("snap.gpf.executionOrder", "SCHEDULE_ROW_COLUMN_BAND");
        writeOp.writeProduct(ProgressMonitor.NULL);
    }

    @Override
    public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        throw new IllegalStateException("Should not come here!");
    }

    /**
     * The Service Provider Interface (SPI) for the operator.
     * It provides operator meta-data and is a factory for new operator instances.
     */
    public static class Spi extends OperatorSpi {

        public Spi() {
            super(RemapOp.class);
        }
    }

}
