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

    public static final String NON_VEGETATED_LAND_EXPRESSION =
            "l1_flags.LAND_OCEAN and !l1_flags.BRIGHT and !l1_flags.SUSPECT and !l1_flags.INVALID " +
                    "and (radiance_13 - radiance_8) / (radiance_13 + radiance_8) < -0.24";
    public static final String VEGETATED_OCEAN_EXPRESSION =
            "!l1_flags.LAND_OCEAN and !l1_flags.BRIGHT and !l1_flags.SUSPECT and !l1_flags.INVALID " +
                    "and (radiance_13 - radiance_8) / (radiance_13 + radiance_8) >= 0.01";
    public static final String VALID_ZERO_EXPRESSION =
            "!l1_flags.INVALID and ( radiance_1 <= 0 or radiance_2 <= 0 or radiance_3 <= 0 " +
                    "or radiance_4 <= 0 or radiance_5 <= 0 or radiance_6 <= 0 or radiance_7 <= 0 " +
                    "or radiance_8 <= 0 or radiance_9 <= 0 or radiance_10 <= 0 or radiance_11 <= 0 " +
                    "or radiance_12 <= 0 or radiance_13 <= 0 or radiance_14 <= 0 or radiance_15 <= 0 )";

    @SourceProduct(alias = "l1b", description = "MERIS L1b (N1) product")
    private Product sourceProduct;

    @Parameter(defaultValue = "l1_flags")
    private String l1FlagBandName;

    @Parameter(defaultValue = "l1_flags.INVALID")
    private String invalidExpression;

    @Parameter(defaultValue = "7")
    private int invalidMaskBitIndex;
    @Parameter(defaultValue = "0")
    private int cosmeticMaskBitIndex;

    // percentage of bad data values per row as criterion for QA failure (100% means that whole row must be 'bad')
    @Parameter(defaultValue = "100.0")
    private float percentBadDataValuesThreshold;
    // threshold of rows identified as 'bad' as criterion for QA failure
    @Parameter(defaultValue = "32")
    private int badDataRowsThreshold;
    @Parameter(defaultValue = "55.0")
    private float stripeGradientThreshold;
    @Parameter(defaultValue = "45")
    private int stripeMinLength;
    @Parameter(defaultValue = "129")
    private int shortProductThreshold;
    @Parameter(defaultValue = "1000")
    private int stripesPixelThreshold;
    @Parameter(defaultValue = "3")
    private int geoshiftLinesThreshold;
    @Parameter(defaultValue = "12")
    private int cosmeticPercentThreshold;

    // set by counting methods as a side effect, second return value
    private int firstBadLine = -1;

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

        // count cosmetic pixels
        final int noCosmeticPixels = noCosmeticPixels(sourceProduct, sourceRaster, width, height);
        final int firstCosmeticDataRow = this.firstBadLine;
        if (noCosmeticPixels > height * width / 100 * cosmeticPercentThreshold) {
            isBad = true;
            cause = "cosmetic: " + noCosmeticPixels + " valid cosmetic pixels";
        }

        // count invalid rows
        final int invalidDataRows = badDataRows(width, sourceRaster, 0, height);
        final int firstInvalidDataRow = this.firstBadLine;

        if (invalidDataRows >= badDataRowsThreshold) {
            isBad = true;
            cause = invalidDataRows + " invalid lines";
        }

        // check for geo-shift by counting non-vegetated land pixels
        final int noOfNonVegetatedLandPixels = noOfMaskedPixels(sourceProduct, width, height, NON_VEGETATED_LAND_EXPRESSION);
        final int firstNonVegetatedLandDataRow = this.firstBadLine;
        if (noOfNonVegetatedLandPixels > width * geoshiftLinesThreshold) {
            isBad = true;
            cause = "geocoding: " + noOfNonVegetatedLandPixels  + " pixels non-vegetated land";
        }

        // check for geo-shift by counting vegetated water pixels
        final int noOfVegetatedWaterPixels = noOfMaskedPixels(sourceProduct, width, height, VEGETATED_OCEAN_EXPRESSION);
        final int firstVegetatedWaterDataRow = this.firstBadLine;
        if (noOfVegetatedWaterPixels > width * geoshiftLinesThreshold) {
            isBad = true;
            cause = "geocoding: " + noOfVegetatedWaterPixels  + " pixels vegetated water";
        }

        // check for horizontal stripes by gradient between segments of subsequent lines
        final int noHorizontalStripePixels = noHorizontalStripePixels(sourceProduct, sourceRaster, width, height);
        final int firstHorizontalStripeDataRow = this.firstBadLine;
        if (noHorizontalStripePixels > stripesPixelThreshold) {
            isBad = true;
            cause = "stripes: " + noHorizontalStripePixels + " horizontal stripe pixels";
        }

        // check for vertical stripes by gradient between segments of adjacent columns
        final int noVerticalStripePixels = noVerticalStripePixels(sourceProduct, sourceRaster, width, height);
        if (noVerticalStripePixels > stripesPixelThreshold) {
            isBad = true;
            cause = "stripes: " + noVerticalStripePixels + " vertical stripe pixels";
        }

        // check for wrong colours by counting valid pixels with zero radiance in some band
        final int noOfValidZeroPixels = noOfMaskedPixels(sourceProduct, width, height, VALID_ZERO_EXPRESSION);
        final int firstValidZeroDataRow = this.firstBadLine;
        if (noOfValidZeroPixels > 3 * width) {
            isBad = true;
            cause = "colours: " + noOfValidZeroPixels + " valid pixels with zero radiance";
        }

        // check for empty tie points (relevant after AMORGOS)
        final boolean productHasEmptyTiePoints = productHasEmptyTiepoints(sourceProduct);
        if (productHasEmptyTiePoints) {
            isBad = true;
            cause = "empty tie points";
        }

        // check for empty lat/lon lines (relevant after AMORGOS)
        final boolean productHasEmptyLatLonLines = productHasEmptyLatLonLines(sourceProduct);
        if (productHasEmptyLatLonLines) {
            isBad = true;
            cause = "empty lat/lon lines";
        }

        // check for short products, number of lines
        if (height <= shortProductThreshold) {
            isBad = true;
            cause = "short product";
        }

        final OperatorDescriptor descriptor = getSpi().getOperatorDescriptor();
        final String aliasName = descriptor.getAlias() != null ? descriptor.getAlias() : descriptor.getName();
        final String legend = String.format("%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s",
                                            "Name", "Lines", "QA",
                                            "InvalidLines", "VegWater", "NonVegLand", "ZeroRad", "Cosmetic", "VStripes", "HStripes",
                                            "EmptyTiePoints",
                                            "FirstInvalidRow", "FirstVegWaterRow", "FirstNonVegLandRow", "FirstZeroRadRow", "FirstCosmeticDataRow", "FirstHorizontalStripesRow",
                                            "Passed", "Cause");
        final String qaRecord = String.format("%s\t%d\t%s\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%b\t%d\t%d\t%d\t%d\t%d\t%d\t%b\t%s",
                                              sourceProduct.getName(), height, aliasName,
                                              invalidDataRows, noOfVegetatedWaterPixels, noOfNonVegetatedLandPixels, noOfValidZeroPixels, noCosmeticPixels, noVerticalStripePixels, noHorizontalStripePixels,
                                              productHasEmptyTiePoints || productHasEmptyLatLonLines,
                                              firstInvalidDataRow, firstVegetatedWaterDataRow, firstNonVegetatedLandDataRow, firstValidZeroDataRow, firstCosmeticDataRow, firstHorizontalStripeDataRow,
                                              !isBad, cause);
        System.out.println(legend);
        System.out.println(qaRecord);
        final MetadataElement qa = new MetadataElement("QA");
        qa.addAttribute(new MetadataAttribute("record", new ProductData.ASCII(qaRecord), false));
        sourceProduct.getMetadataRoot().addElement(qa);
        setTargetProduct(sourceProduct);
    }

    private int noCosmeticPixels(Product sourceProduct, RasterDataNode sourceRaster, int width, int height) {
        int cosmetic = 0;
        this.firstBadLine = -1;
        int[] flags = new int[width];
        for (int y = 0; y < height; ++y) {
            sourceRaster.getPixels(0, y, width, 1, flags);
            for (int x=0; x<width; ++x) {
                if (isCosmeticMaskBitSet(flags[x]) && ! isInvalidMaskBitSet(flags[x])) {
                    ++cosmetic;
                    if (this.firstBadLine < 0) {
                        this.firstBadLine = y;
                    }
                }
            }
        }
        return cosmetic;
    }

    private int noOfVerticalStripes(Product product, RasterDataNode sourceRaster, int width, int height) throws OperatorException {
        try {
            int stripes = 0;
            int[] counts = new int[width-1];  // counter of stripe length up to current line
            int[] lines = new int[width-1];  // marker up to which line a stripe has been recognised
            for (int x=0; x<width-1; ++x) {
                lines[x] = -1;
            }
            float[] radiances = new float[width];
            int[] flags = new int[width];
            Band[] bands = product.getBands();
            for (int y = 0; y < height; ++y) {
                sourceRaster.getPixels(0, y, width, 1, flags);
                for (Band band : bands) {
                    if (! band.getDisplayName().startsWith("radiance")) {
                        continue;
                    }
                    band.readPixels(0, y, width, 1, radiances);
                    for (int x=0; x<width-1; ++x) {
                        if (!isInvalidMaskBitSet(flags[x]) && !isInvalidMaskBitSet(flags[x+1])
                                && !isCosmeticMaskBitSet(flags[x]) && !isCosmeticMaskBitSet(flags[x + 1])
                                && Math.abs(radiances[x]-radiances[x+1]) > stripeGradientThreshold) {
                            lines[x] = y;
                        }
                    }
                }
                for (int x=0; x<width-1; ++x) {
                    if (lines[x] == y) {
                        ++counts[x];
                    } else {
                        if (counts[x] >= stripeMinLength) {
                            ++stripes;
                        }
                        counts[x] = 0;
                    }
                }
            }
            for (int x=0; x<width-1; ++x) {
                if (counts[x] >= stripeMinLength) {
                    ++stripes;
                }
            }
            return stripes;
        } catch (IOException e) {
            throw new OperatorException(e.getMessage(), e);
        }
    }

    private int noVerticalStripePixels(Product product, RasterDataNode sourceRaster, int width, int height) throws OperatorException {
        try {
            int stripes = 0;
            int[] counts = new int[width-1];  // counter of stripe length up to current line
            float[] gradients = new float[width-1];
            for (int x=0; x<width-1; ++x) {
                gradients[x] = 0.0f;
            }
            float[] radiances = new float[width];
            int[] flags = new int[width];
            Band[] bands = product.getBands();
            for (Band band : bands) {
                if (! band.getDisplayName().startsWith("radiance")) {
                    continue;
                }
                for (int y = 0; y < height; ++y) {
                    sourceRaster.getPixels(0, y, width, 1, flags);
                    band.readPixels(0, y, width, 1, radiances);
                    for (int x=0; x<width-1; ++x) {
                        if (! isInvalidMaskBitSet(flags[x]) && ! isInvalidMaskBitSet(flags[x+1])
                                && ! isCosmeticMaskBitSet(flags[x]) && ! isCosmeticMaskBitSet(flags[x+1])) {
                            float gradient = radiances[x]-radiances[x+1];
                            if (gradient >= stripeGradientThreshold) {
                                if (gradients[x] > 0.0f) {
                                    ++counts[x];
                                } else {
                                    if (counts[x] >= stripeMinLength) {
                                        stripes += counts[x];
                                    }
                                    counts[x] = 1;
                                    gradients[x] = 1.0f;
                                }
                            } else if (gradient <= -stripeGradientThreshold) {
                                if (gradients[x] < 0.0f) {
                                    ++counts[x];
                                } else {
                                    if (counts[x] >= stripeMinLength) {
                                        stripes += counts[x];
                                    }
                                    counts[x] = 1;
                                    gradients[x] = -1.0f;
                                }
                            } else {
                                if (counts[x] >= stripeMinLength) {
                                    stripes += counts[x];
                                }
                                gradients[x] = 0.0f;
                                counts[x] = 0;
                            }
                        } else {
                            if (counts[x] >= stripeMinLength) {
                                stripes += counts[x];
                            }
                            gradients[x] = 0.0f;
                            counts[x] = 0;
                        }
                    }
                }
                for (int x=0; x<width-1; ++x) {
                    if (counts[x] >= stripeMinLength) {
                        stripes += counts[x];
                    }
                }
            }
            return stripes;
        } catch (IOException e) {
            throw new OperatorException(e.getMessage(), e);
        }
    }

    private int noHorizontalStripePixels(Product product, RasterDataNode sourceRaster, int width, int height) {
        try {
            int stripes = 0;
            int length = 0;
            this.firstBadLine = -1;
            float gradient0 = 0.0f;
            float[] radiances0 = new float[width];
            float[] radiances = new float[width];
            int[] flags0 = new int[width];
            int[] flags = new int[width];
            Band[] bands = product.getBands();
            for (Band band : bands) {
                if (! band.getDisplayName().startsWith("radiance")) {
                    continue;
                }
                sourceRaster.getPixels(0, 0, width, 1, flags0);
                band.readPixels(0, 0, width, 1, radiances0);
                for (int y = 1; y < height; ++y) {
                    sourceRaster.getPixels(0, y, width, 1, flags);
                    band.readPixels(0, y, width, 1, radiances);
                    for (int x=0; x<width-1; ++x) {
                        if (! isInvalidMaskBitSet(flags[x]) && ! isInvalidMaskBitSet(flags0[x])
                                && ! isCosmeticMaskBitSet(flags[x]) && ! isCosmeticMaskBitSet(flags0[x])) {
                            float gradient = radiances[x]-radiances0[x];
                            if (gradient >= stripeGradientThreshold) {
                                if (gradient0 > 0.0f) {
                                    ++length;
                                } else {
                                    if (length >= stripeMinLength) {
                                        stripes += length;
                                        if (this.firstBadLine == -1 || y < this.firstBadLine) {
                                            this.firstBadLine = y;
                                        }
                                    }
                                    length = 1;
                                    gradient0 = 1.0f;
                                }
                            } else if (gradient <= -stripeGradientThreshold) {
                                if (gradient0 < 0.0f) {
                                    ++length;
                                } else {
                                    if (length >= stripeMinLength) {
                                        stripes += length;
                                        if (this.firstBadLine == -1 || y < this.firstBadLine) {
                                            this.firstBadLine = y;
                                        }
                                    }
                                    length = 1;
                                    gradient0 = -1.0f;
                                }
                            } else {
                                if (length >= stripeMinLength) {
                                    stripes += length;
                                    if (this.firstBadLine == -1 || y < this.firstBadLine) {
                                        this.firstBadLine = y;
                                    }
                                }
                                gradient0 = 0.0f;
                                length = 0;
                            }
                        } else {
                            if (length >= stripeMinLength) {
                                stripes += length;
                                if (this.firstBadLine == -1 || y < this.firstBadLine) {
                                    this.firstBadLine = y;
                                }
                            }
                            gradient0 = 0.0f;
                            length = 0;
                        }
                    }
                    if (length >= stripeMinLength) {
                        stripes += length;
                        if (this.firstBadLine == -1 || y < this.firstBadLine) {
                            this.firstBadLine = y;
                        }
                    }
                    length = 0;
                    // switch lines
                    final int[] flags1 = flags0;
                    flags0 = flags;
                    flags = flags1;
                    final float[] radiances1 = radiances0;
                    radiances0 = radiances;
                    radiances = radiances1;
                }
            }
            return stripes;
        } catch (IOException e) {
            throw new OperatorException(e.getMessage(), e);
        }
    }

    private int noOfMaskedPixels(Product product, int width, int height, String bandMathExpression) throws OperatorException {
        try {
            BandMathsOp toBeCountedOp = BandMathsOp.createBooleanExpressionBand(bandMathExpression, product);
            Band toBeCountedBand = toBeCountedOp.getTargetProduct().getBandAt(0);
            int count = 0;
            this.firstBadLine = -1;
            int[] flags = new int[width];
            for (int y = 0; y < height; ++y) {
                toBeCountedBand.readPixels(0, y, width, 1, flags);
                int lineCount = 0;
                for (int x = 0; x < width; ++x) {
                    if (flags[x] != 0) {
                        ++count;
                        ++lineCount;
                    }
                }
                if (this.firstBadLine < 0 && lineCount * 100 / width >= 1) {
                    this.firstBadLine = y;
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
        this.firstBadLine = -1;
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
                if (this.firstBadLine < 0) {
                    this.firstBadLine = y;
                }
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

    private boolean isCosmeticMaskBitSet(int pixel) {
        return ((pixel >> cosmeticMaskBitIndex) & 1) != 0;
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

