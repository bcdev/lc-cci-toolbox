package org.esa.cci.lc.qa;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.datamodel.TiePointGrid;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.descriptor.OperatorDescriptor;
import org.esa.beam.gpf.operators.standard.BandMathsOp;

import java.io.IOException;

/**
 * Class providing a QA for MERIS L1b products: quality check fails if
 * - more than either the first or last 100 rows of the product are invalid, where 'invalid' means that more
 *   than a certain percentage (user parameter) of pixels in a row is flagged as INVALID
 * - the product contains more than a certain number (user parameter) of invalid rows elsewhere (not at top or bottom)
 *
 * The target product will be an 'empty' product (no bands) if the quality check passed. If the quality check failed,
 * the L1 flag band and one of the radiance bands (user parameter) will be written to the target product. In this case,
 * the extension '_QA_FAILED' is added to the product type for a later identification.
 *
 * @author olafd
 */
@OperatorMetadata(alias = "lc.meris.qa")
public class MerisL1QaOp extends Operator {

    @SourceProduct(alias = "l1b", description = "MERIS L1b (N1) product")
    private Product sourceProduct;

    @Parameter(defaultValue = "l1_flags")
    private String l1FlagBandName;

    @Parameter(defaultValue = "radiance_13")
    private String radianceOutputBandName;

    @Parameter(defaultValue = "l1_flags.INVALID")
    private String invalidExpression;

    @Parameter(defaultValue = "7")
    private int invalidMaskBitIndex;

    // percentage of bad data values per row as criterion for QA failure (100% means that whole row must be 'bad')
    @Parameter(defaultValue = "100.0")
    private float percentBadDataValuesThreshold;

    // threshold of rows identified as 'bad' as criterion for QA failure
    @Parameter(defaultValue = "1")
    private int badDataRowsThreshold;

    private static final int START_ROW_OFFSET_MAX = 100;
    private static final int END_ROW_OFFSET_MAX = 100;

    @Override
    public void initialize() throws OperatorException {

        final int width = sourceProduct.getSceneRasterWidth();
        final int height = sourceProduct.getSceneRasterHeight();

        final Band b = sourceProduct.getBand(l1FlagBandName);
        if (b == null) {
            throw new OperatorException("Input product does not contain specified L1 flag band. - product QA failed.");
        }

        try {
            b.loadRasterData();
        } catch (IOException e) {
            System.out.println("Cannot load raster data for band '" + b.getName() + "' - product QA failed.");
            throw new OperatorException("Cannot load raster data for band '" + b.getName() + "' - product QA failed.");
        }
        final RasterDataNode sourceRaster = sourceProduct.getRasterDataNode(b.getName());

        boolean isBad = false;
        String cause = "";

        // check first rows of product
        final int yStart = firstGoodRow(width, height, sourceRaster);

        if (yStart >= START_ROW_OFFSET_MAX) {
            System.out.println("Too many invalid rows (" + yStart + ") at start of product - QA failed.");
            isBad = true;
            cause = "more than " + START_ROW_OFFSET_MAX + " initial invalid lines";
        }

        // check last rows of product
        final int yEnd = lastGoodRow(width, height, sourceRaster, yStart);

        if (yEnd <= height-END_ROW_OFFSET_MAX) {
            System.out.println("Too many invalid rows (" + yStart + ") at end of product - QA failed.");
            isBad = true;
            cause = "more than " + END_ROW_OFFSET_MAX + " final invalid lines";
        }

        // valid start and end row found in first and last 100 rows - now check all rows in between
        final int badDataRows = badDataRows(width, sourceRaster, yStart, yEnd);

        if (badDataRows >= badDataRowsThreshold) {
            // limit of allowed 'bad' rows is exceeded - QA failed
            isBad = true;
            cause = "more than " + badDataRowsThreshold + "% intermediate invalid lines";
        }

        final boolean productHasEmptyTiePoints = productHasEmptyTiepoints(sourceProduct);
        if (productHasEmptyTiePoints) {
            isBad = true;
            cause = "empty tie points";
        }

        final boolean productHasEmptyLatLonLines = productHasEmptyLatLonLines(sourceProduct);
        if (productHasEmptyLatLonLines) {
            isBad = true;
            cause = "empty lat/lon lines";
        }

        final int noOfVegetatedWaterPixels = noOfMaskedPixels(sourceProduct, width, 0, height, "l1_flags.WATER and !l1_flags.BRIGHT and !l1_flags.SUSPECT and !l1_flags.INVALID and (radiance_13 - radiance_8) / (radiance_13 - radiance_8) < 0.01");
        if (noOfVegetatedWaterPixels > width * height / 1000) {    // TODO relate to # water pixels
            isBad = true;
            cause = (noOfVegetatedWaterPixels * 1000 / width / height) + " ppm vegetated water";
        }

        final int noOfNonVegetatedLandPixels = noOfMaskedPixels(sourceProduct, width, 0, height, "l1_flags.LAND and !l1_flags.BRIGHT and !l1_flags.SUSPECT and !l1_flags.INVALID and (radiance_13 - radiance_8) / (radiance_13 - radiance_8) >= 0.01");
        if (noOfVegetatedWaterPixels > width * height / 1000) {    // TODO relate to # water pixels
            isBad = true;
            cause = (noOfVegetatedWaterPixels * 1000 / width / height) + " ppm non-vegetated land";
        }

        final int noOfValidZeroPixels1 = noOfMaskedPixels(sourceProduct, width, 0, 1, "!l1_flags.INVALID and ( radiance_1 <= 0 or radiance_2 <= 0 or radiance_3 <= 0 or radiance_4 <= 0 or radiance_5 <= 0 or radiance_6 <= 0 or radiance_7 <= 0 or radiance_8 <= 0 or radiance_9 <= 0 or radiance_10 <= 0 or radiance_11 <= 0 or radiance_12 <= 0 or radiance_13 <= 0 or radiance_14 <= 0 or radiance_15 <= 0");
        if (noOfValidZeroPixels1 > width / 2) {
            isBad = true;
            cause = noOfValidZeroPixels1 + " valid pixels with zero radiance in first line";
        }

        final int noOfValidZeroPixels9 = noOfMaskedPixels(sourceProduct, width, height - 1, height, "!l1_flags.INVALID and ( radiance_1 <= 0 or radiance_2 <= 0 or radiance_3 <= 0 or radiance_4 <= 0 or radiance_5 <= 0 or radiance_6 <= 0 or radiance_7 <= 0 or radiance_8 <= 0 or radiance_9 <= 0 or radiance_10 <= 0 or radiance_11 <= 0 or radiance_12 <= 0 or radiance_13 <= 0 or radiance_14 <= 0 or radiance_15 <= 0");
        if (noOfValidZeroPixels9 > width / 2) {
            isBad = true;
            cause = noOfValidZeroPixels9 + " valid pixels with zero radiance in last line";
        }

        final int noOfValidZeroPixels = noOfMaskedPixels(sourceProduct, width, 0, height, "!l1_flags.INVALID and ( radiance_1 <= 0 or radiance_2 <= 0 or radiance_3 <= 0 or radiance_4 <= 0 or radiance_5 <= 0 or radiance_6 <= 0 or radiance_7 <= 0 or radiance_8 <= 0 or radiance_9 <= 0 or radiance_10 <= 0 or radiance_11 <= 0 or radiance_12 <= 0 or radiance_13 <= 0 or radiance_14 <= 0 or radiance_15 <= 0");
        if (noOfValidZeroPixels > 2 * width) {
            isBad = true;
            cause = noOfValidZeroPixels + " valid pixels with zero radiance";
        }

        final OperatorDescriptor descriptor = getSpi().getOperatorDescriptor();
        final String aliasName = descriptor.getAlias() != null ? descriptor.getAlias() : descriptor.getName();
        final String qaRecord = String.format("%s\t%b\t%s\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%b\t%b\t%s",
                                              sourceProduct.getName(), !isBad, aliasName,
                                              height,
                                              noOfVegetatedWaterPixels, noOfNonVegetatedLandPixels, noOfValidZeroPixels1, noOfValidZeroPixels9, noOfValidZeroPixels,
                                              yStart, height-yEnd, badDataRows,
                                              productHasEmptyTiePoints, productHasEmptyLatLonLines,
                                              cause);
        System.out.println(qaRecord);
        final MetadataElement qa = new MetadataElement("QA");
        qa.addAttribute(new MetadataAttribute("record", new ProductData.ASCII(qaRecord), false));
        sourceProduct.getMetadataRoot().addElement(qa);
        setTargetProduct(sourceProduct);
    }

    private int noOfMaskedPixels(Product product, int width, int yStart, int yEnd, String bandMathExpression) throws OperatorException {
        try {
            BandMathsOp validZeroOp = BandMathsOp.createBooleanExpressionBand(bandMathExpression, product);
            Band validZeroBand = validZeroOp.getTargetProduct().getBandAt(0);
            int count = 0;
            int[] flags = new int[width];
            for (int y = yStart; y < yEnd; ++y) {
                validZeroBand.readPixels(0, y, width, 1, flags);
                for (int x = 0; x < width; ++x) {
                    if (flags[x] != 0) {
                        ++count;
                    }
                }
            }
            return count;
        } catch (IOException e) {
            throw new OperatorException("pixel count for '" + bandMathExpression + "' failed: " + e.getMessage(), e);
        }
    }

    private int badDataRows(int width, RasterDataNode sourceRaster, int yStart, int yEnd) {
        int y = yStart;
        int badDataRows = 0;
        while (y < yEnd) {
            int[] pixels = new int[width];
            pixels = sourceRaster.getPixels(0, y, width, 1, pixels);
            int x = 0;
            int badCount = 0;
            while (badCount * 100.0 / width < percentBadDataValuesThreshold && x < width) {
                final boolean isBadPixel = isInvalidMaskBitSet(pixels[x]);
                if (isBadPixel) {
                    badCount++;
                }
                x++;
            }
            final double percentBadDataValues = badCount * 100.0 / width;
            if (percentBadDataValues >= percentBadDataValuesThreshold) {
                // row is identified as 'bad'
                badDataRows++;
            }
            y++;
        }
        return badDataRows;
    }

    private int lastGoodRow(int width, int height, RasterDataNode sourceRaster, int yStart) {
        int yEnd = height-1;
        boolean lastGoodRowFound = false;
        while (!lastGoodRowFound && yEnd > yStart) {
            int[] pixels = new int[width];
            pixels = sourceRaster.getPixels(0, yEnd, width, 1, pixels);
            int x = 0;
            while (!lastGoodRowFound && x < width) {
                lastGoodRowFound = !isInvalidMaskBitSet(pixels[x]);
                x++;
            }
            yEnd--;
        }
        return yEnd;
    }

    private int firstGoodRow(int width, int height, RasterDataNode sourceRaster) {
        int yStart = 0;
        boolean firstGoodRowFound = false;
        while (!firstGoodRowFound && yStart < height-1) {
            int[] pixels = new int[width];
            pixels = sourceRaster.getPixels(0, yStart, width, 1, pixels);
            int x = 0;
            while (!firstGoodRowFound && x < width) {
                firstGoodRowFound = !isInvalidMaskBitSet(pixels[x]);
                x++;
            }
            yStart++;
        }
        return yStart;
    }

    private boolean isInvalidMaskBitSet(int pixel) {
        return ((pixel >> invalidMaskBitIndex) & 1) != 0;
    }

    private static boolean productHasSuspectLines(Product product) throws IOException {
        BandMathsOp mathop1 = null;
        BandMathsOp mathop2 = null;
        try {
            mathop1 = BandMathsOp.createBooleanExpressionBand("l1_flags.INVALID", product);
            Band invalid = mathop1.getTargetProduct().getBandAt(0);

            mathop2 = BandMathsOp.createBooleanExpressionBand("l1_flags.SUSPECT", product);
            Band suspect = mathop2.getTargetProduct().getBandAt(0);

            int width = product.getSceneRasterWidth();
            int height = product.getSceneRasterHeight();

            int[] invalidFlags = new int[width];
            int[] suspectFlags = new int[width];
            for (int y = 0; y < height; y++) {
                invalid.readPixels(0, y, width, 1, invalidFlags);
                suspect.readPixels(0, y, width, 1, suspectFlags);
                if (isWholeLineSuspect(invalidFlags, suspectFlags)) {
                    return true;
                }
            }
            return false;
        } catch (OperatorException ignore) {
            return false;
        } finally {
            if (mathop1 != null) {
                mathop1.dispose();
            }
            if (mathop2 != null) {
                mathop2.dispose();
            }
        }
    }

    static boolean isWholeLineSuspect(int[] invalidFlags, int[] suspectFlags) {
        int state = 0;
        for (int i = 0; i < invalidFlags.length; i++) {
            boolean isInvalid = invalidFlags[i] != 0;
            boolean isSuspect = suspectFlags[i] != 0;
            if ((state == 0 || state == 1) && isInvalid && !isSuspect) {
                state = 1;
            } else if ((state == 1 || state == 2) && !isInvalid && isSuspect) {
                state = 2;
            } else if (state == 3 && i == (invalidFlags.length-1)) {
                return true;
            } else if ((state == 2 || state == 3) && isInvalid && !isSuspect) {
                state = 3;
            } else {
                return false;
            }
        }
        return false;
    }

    private static boolean productHasEmptyTiepoints(Product sourceProduct) {
        // "AMORGOS" can produce products that are corrupted.
        // All tie point grids contain only zeros, check the first one,
        // if the product has one.
        TiePointGrid[] tiePointGrids = sourceProduct.getTiePointGrids();
        if (tiePointGrids != null && tiePointGrids.length > 0) {
            TiePointGrid firstGrid = tiePointGrids[0];
            float[] tiePoints = firstGrid.getTiePoints();
            for (float tiePoint : tiePoints) {
                if (tiePoint != 0.0f) {
                    return false;
                }
            }
            // all values are zero
            return true;
        }
        return false;
    }

    private static boolean productHasEmptyLatLonLines(Product sourceProduct) {
        TiePointGrid latitude = sourceProduct.getTiePointGrid("latitude");
        TiePointGrid longitude = sourceProduct.getTiePointGrid("longitude");
        if (latitude == null || longitude == null) {
            return false;
        }
        float[] latData = (float[]) latitude.getDataElems();
        float[] lonData = (float[]) longitude.getDataElems();

        int width = latitude.getRasterWidth();
        int height = latitude.getRasterHeight();

        for (int y = 0; y < height; y++) {
            if (isLineZero(latData, width, y) && isLineZero(lonData, width, y)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isLineZero(float[] floatData, int width, int y) {
        for (int x = 0; x < width; x++) {
            if (floatData[(y * width + x)] != 0) {
                return false;
            }
        }
        return true;
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(MerisL1QaOp.class);
        }
    }
}

