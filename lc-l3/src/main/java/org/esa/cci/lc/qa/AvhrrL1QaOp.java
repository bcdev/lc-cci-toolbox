package org.esa.cci.lc.qa;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.RasterDataNode;
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
 * Class providing a QA for AVHRR L1b products
 *
 * @author boe
 */
@OperatorMetadata(alias = "lc.avhrr.qa")
public class AvhrrL1QaOp extends Operator {

    public static final String INVALID_EXPRESSION =
            "counts_1 <= 0 or counts_2 <= 0 or counts_3 <= 0 or counts_4 <= 0 or counts_5 <= 0";
    public static final String NON_VEGETATED_LAND_EXPRESSION =
            "counts_1 > 0 and counts_2 > 0 and counts_3 > 0 and counts_4 > 0 and counts_5 > 0" +
            " and LAND==1" +
            " and (counts_2 - counts_1) / (counts_2 + counts_1) < -0.24";
    public static final String VEGETATED_OCEAN_EXPRESSION =
            "counts_1 > 0 and counts_2 > 0 and counts_3 > 0 and counts_4 > 0 and counts_5 > 0" +
            " and LAND==0" +
            " and (counts_2 - counts_1) / (counts_2 + counts_1) >= 0.01";
    public static final String SMALL_COUNTS_EXPRESSION =
            "counts_1 > 0 and counts_2 > 0 and counts_3 > 0 and counts_4 > 0 and counts_5 > 0" +
            " and LAND==1" +
            " and (counts_2 < 40 or counts_1 < 50)";

    @SourceProduct(alias = "l1b", description = "AVHRR L1b product with 'land' mask band")
    private Product sourceProduct;
    @Parameter(defaultValue = "LAND")
    private String landFlagBandName;

    // percentage of bad data values per row as criterion for QA failure (100% means that whole row must be 'bad')
    @Parameter(defaultValue = "100.0")
    private float percentBadDataValuesThreshold;
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

    // set by counting methods as a side effect, second return value
    private int firstBadLine = -1;

    @Override
    public void initialize() throws OperatorException {

        final int width = sourceProduct.getSceneRasterWidth();
        final int height = sourceProduct.getSceneRasterHeight();

        final Band b = sourceProduct.getBand(landFlagBandName);
        if (b == null) {
            throw new OperatorException("Input product does not contain specified L1 flag band. - product QA failed.");
        }
        try {
            b.loadRasterData();
        } catch (IOException e) {
            System.out.println("Cannot load raster data for band '" + b.getName() + "' - product QA failed.");
            throw new OperatorException("Cannot load raster data for band '" + b.getName() + "' - product QA failed.");
        }
        final RasterDataNode landRaster = sourceProduct.getRasterDataNode(b.getName());

        BandMathsOp invalidOp = BandMathsOp.createBooleanExpressionBand(INVALID_EXPRESSION, sourceProduct);
        final Product invalidProduct = invalidOp.getTargetProduct();
        Band invalidBand = invalidProduct.getBandAt(0);
        try {
            invalidBand.loadRasterData();
        } catch (IOException e) {
            e.printStackTrace();
            throw new OperatorException("failed to load virtual band for " + INVALID_EXPRESSION, e);
        }
        final RasterDataNode invalidRaster = invalidProduct.getRasterDataNode(invalidBand.getName());

        boolean isBad = false;
        String cause = "";

        // check for invalid pixels with zero radiance in some band
        final int noOfZeroPixels = noOfMaskedPixels(sourceProduct, width, height, INVALID_EXPRESSION);
        final int firstZeroDataRow = this.firstBadLine;
        if (noOfZeroPixels > 3 * width) {
            isBad = true;
            cause = "zero: " + noOfZeroPixels + " pixels with zero radiance";
        }

        // count invalid rows
        final int invalidDataRows = badDataRows(width, invalidRaster, 0, height);
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
        final int noHorizontalStripePixels = noHorizontalStripePixels(sourceProduct, invalidRaster, width, height);
        final int firstHorizontalStripeDataRow = this.firstBadLine;
        if (noHorizontalStripePixels > stripesPixelThreshold) {
            isBad = true;
            cause = "stripes: " + noHorizontalStripePixels + " horizontal stripe pixels";
        }

        // check for vertical stripes by gradient between segments of adjacent columns
        final int noVerticalStripePixels = noVerticalStripePixels(sourceProduct, invalidRaster, width, height);
        if (noVerticalStripePixels > stripesPixelThreshold) {
            isBad = true;
            cause = "stripes: " + noVerticalStripePixels + " vertical stripe pixels";
        }

        // check for wrong colours by counting valid pixels with zero radiance in some band
        final int noOfSmallCountsPixels = noOfMaskedPixels(sourceProduct, width, height, SMALL_COUNTS_EXPRESSION);
        final int firstValidZeroDataRow = this.firstBadLine;
        if (noOfSmallCountsPixels > 3 * width) {
            isBad = true;
            cause = "colours: " + noOfSmallCountsPixels + " valid pixels with zero radiance";
        }

        // check for short products, number of lines
        if (height <= shortProductThreshold) {
            isBad = true;
            cause = "short product";
        }

        final OperatorDescriptor descriptor = getSpi().getOperatorDescriptor();
        final String aliasName = descriptor.getAlias() != null ? descriptor.getAlias() : descriptor.getName();
        final String legend = String.format("%s\t%s\t%s | \t%s\t%s\t%s | \t%s\t%s\t%s\t%s | \t%s\t%s\t%s\t%s\t%s | \t%s\t%s",
                                            "Name", "Lines", "QA",
                                            "InvalidLines", "ZeroPixels", "SmallValues",
                                            "NonVegLand", "VegWater", "HStripes", "VStripes",
                                            "FirstInvalidRow", "FirstZeroRadRow",
                                            "FirstNonVegLandRow", "FirstVegWaterRow", "FirstHorizontalStripesRow",
                                            "Passed", "Cause");
        final String qaRecord = String.format("%s\t%d\t%s | \t%d\t%d\t%d | \t%d\t%d\t%d\t%d | \t%d\t%d\t%d\t%d\t%d | \t%b\t%s",
                                              sourceProduct.getName(), height, aliasName,
                                              invalidDataRows, noOfZeroPixels, noOfSmallCountsPixels,
                                              noOfNonVegetatedLandPixels, noOfVegetatedWaterPixels, noHorizontalStripePixels, noVerticalStripePixels,
                                              firstInvalidDataRow, firstValidZeroDataRow,
                                              firstNonVegetatedLandDataRow, firstVegetatedWaterDataRow, firstHorizontalStripeDataRow,
                                              !isBad, cause);
        System.out.println(legend);
        System.out.println(qaRecord);
        final MetadataElement qa = new MetadataElement("QA");
        qa.addAttribute(new MetadataAttribute("record", new ProductData.ASCII(qaRecord), false));
        sourceProduct.getMetadataRoot().addElement(qa);
        setTargetProduct(sourceProduct);
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
                if (pixels[x] != 0) {
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

    private int noVerticalStripePixels(Product product, RasterDataNode invalidRaster, int width, int height) throws OperatorException {
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
                    invalidRaster.getPixels(0, y, width, 1, flags);
                    band.readPixels(0, y, width, 1, radiances);
                    for (int x=0; x<width-1; ++x) {
                        if (flags[x] == 0) {
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

    private int noHorizontalStripePixels(Product product, RasterDataNode invalidRaster, int width, int height) {
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
                invalidRaster.getPixels(0, 0, width, 1, flags0);
                band.readPixels(0, 0, width, 1, radiances0);
                for (int y = 1; y < height; ++y) {
                    invalidRaster.getPixels(0, y, width, 1, flags);
                    band.readPixels(0, y, width, 1, radiances);
                    for (int x=0; x<width-1; ++x) {
                        if (flags[x] == 0) {
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

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(AvhrrL1QaOp.class);
        }
    }
}

