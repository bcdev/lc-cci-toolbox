package org.esa.cci.lc.qa;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.FlagCoding;
import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.descriptor.OperatorDescriptor;
import org.esa.beam.gpf.operators.standard.BandMathsOp;
import org.esa.beam.util.BitSetter;

import java.io.IOException;

/**
 * Class providing a QA for AVHRR L1b products
 *
 * @author boe
 */
@OperatorMetadata(alias = "lc.avhrr.qa")
public class AvhrrL1QaOp extends Operator {

    public static final String INVALID_EXPRESSION =
            "counts_1 <= 0 or counts_2 <= 0 or counts_3 <= 0 or counts_4 <= 0 or counts_5 <= 0 or sun_zenith > 75";
    public static final String ZERO_EXPRESSION =
            "counts_1 <= 0 or counts_2 <= 0 or counts_3 <= 0 or counts_4 <= 0 or counts_5 <= 0";
    public static final String HIGH_SZA_EXPRESSION =
            "sun_zenith > 75";
    public static final String NON_VEGETATED_LAND_EXPRESSION =
            "counts_1 > 0 and counts_2 > 0 and counts_3 > 0 and counts_4 > 0 and counts_5 > 0" +
            " and sun_zenith <= 75" +
            " and albedo_1 > 0 and albedo_2 > 0" +
            " and LAND==1" +
            " and (albedo_2 - albedo_1) / (albedo_2 + albedo_1) < -0.24";
    public static final String VEGETATED_OCEAN_EXPRESSION =
            "counts_1 > 0 and counts_2 > 0 and counts_3 > 0 and counts_4 > 0 and counts_5 > 0" +
            " and sun_zenith <= 75" +
            " and albedo_1 > 0 and albedo_2 > 0" +
            " and LAND==0" +
            " and (albedo_2 - albedo_1) / (albedo_2 + albedo_1) >= 0.15";  // was 0.01 for MERIS
    public static final String SMALL_COUNTS_EXPRESSION =
            "counts_1 > 0 and counts_2 > 0 and counts_3 > 0 and counts_4 > 0 and counts_5 > 0" +
            " and sun_zenith <= 75" +
            " and LAND==1" +
            " and (counts_2 < 40 or counts_1 < 50)";
    public static final int F_ZERO = 0;
    public static final int F_HIGHSZA = 1;
    public static final int F_COLOUR = 2;
    public static final int F_NONVEGLAND = 3;
    public static final int F_VEGWATER = 4;
    public static final int F_HSTRIPES = 5;
    public static final int F_VSTRIPES = 6;
    public static final int F_DUPLICATE = 7;

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
        String nonVegetatedLandExpression;
        String vegetatedOceanExpression;
        String smallCountsExpression;
        if ("land_water_fraction".equals(landFlagBandName)) {
            nonVegetatedLandExpression = NON_VEGETATED_LAND_EXPRESSION.replaceAll("LAND==1", "land_water_fraction<50");
            vegetatedOceanExpression = VEGETATED_OCEAN_EXPRESSION.replaceAll("LAND==0", "land_water_fraction>=50");
            smallCountsExpression = SMALL_COUNTS_EXPRESSION.replaceAll("LAND==1", "land_water_fraction<50");
        } else {
            nonVegetatedLandExpression = NON_VEGETATED_LAND_EXPRESSION;
            vegetatedOceanExpression = VEGETATED_OCEAN_EXPRESSION;
            smallCountsExpression = SMALL_COUNTS_EXPRESSION;
        }

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

        final short[] qaFlagBuffer = new short[height * width];
        boolean isBad = false;
        String cause = "";

        // check for invalid pixels with zero radiance in some band
        final int noOfZeroPixels = noOfMaskedPixels(sourceProduct, width, height, ZERO_EXPRESSION, qaFlagBuffer, (short) (1 << F_ZERO));

        // check for invalid pixels with zero radiance in some band
        final int noOfHighSzaPixels = noOfMaskedPixels(sourceProduct, width, height, HIGH_SZA_EXPRESSION, qaFlagBuffer, (short) (1 << F_HIGHSZA));

        // count invalid rows
        final int invalidDataRows = badDataRows(width, invalidRaster, 0, height);
        final int firstInvalidDataRow = this.firstBadLine;

        //if (invalidDataRows >= badDataRowsThreshold) {
        //    isBad = true;
        //    cause = invalidDataRows + " invalid lines";
        //}

        // check for horizontal stripes by gradient between segments of subsequent lines
        final int noHorizontalStripePixels = noHorizontalStripePixels(sourceProduct, invalidRaster, width, height, qaFlagBuffer, (short) (1 << F_HSTRIPES));
        final int firstHorizontalStripeDataRow = this.firstBadLine;
        if (noHorizontalStripePixels > stripesPixelThreshold) {
            isBad = true;
            cause = "stripes: " + noHorizontalStripePixels + " horizontal stripe pixels";
        }

        // check for duplicate lines by comparison between segments of subsequent lines
        final int noDuplicateLinesPixels = noDuplicateLinesPixels(sourceProduct, invalidRaster, width, height, qaFlagBuffer, (short) (1 << F_DUPLICATE));
        final int firstDuplicateLinesDataRow = this.firstBadLine;
        if (noDuplicateLinesPixels > stripesPixelThreshold) {
            isBad = true;
            cause = "duplicate: " + noDuplicateLinesPixels + " pixels of duplicate lines";
        }

        // check for geo-shift by counting non-vegetated land pixels
        final int noOfNonVegetatedLandPixels = noOfMaskedPixels(sourceProduct, width, height, nonVegetatedLandExpression, qaFlagBuffer, (short) (1 << F_NONVEGLAND));
        final int firstNonVegetatedLandDataRow = this.firstBadLine;
        if (noOfNonVegetatedLandPixels > width * geoshiftLinesThreshold) {
            isBad = true;
            cause = "geocoding: " + noOfNonVegetatedLandPixels  + " pixels non-vegetated land";
        }

        // check for geo-shift by counting vegetated water pixels
        final int noOfVegetatedWaterPixels = noOfMaskedPixels(sourceProduct, width, height, vegetatedOceanExpression, qaFlagBuffer, (short) (1 << F_VEGWATER));
        final int firstVegetatedWaterDataRow = this.firstBadLine;
        if (noOfVegetatedWaterPixels > width * geoshiftLinesThreshold) {
            isBad = true;
            cause = "geocoding: " + noOfVegetatedWaterPixels  + " pixels vegetated water";
        }

        // check for wrong colours by counting valid pixels with zero radiance in some band
        final int noOfSmallCountsPixels = noOfMaskedPixels(sourceProduct, width, height, smallCountsExpression, qaFlagBuffer, (short) (1 << F_COLOUR));
        final int firstValidZeroDataRow = this.firstBadLine;
        if (noOfSmallCountsPixels > 3 * width) {
            isBad = true;
            cause = "colours: " + noOfSmallCountsPixels + " pixels with small radiance in some band";
        }

        // check for vertical stripes by gradient between segments of adjacent columns
        final int noVerticalStripePixels = noVerticalStripePixels(sourceProduct, invalidRaster, width, height, qaFlagBuffer, (short) (1 << F_VSTRIPES));
        if (noVerticalStripePixels > stripesPixelThreshold) {
            isBad = true;
            cause = "stripes: " + noVerticalStripePixels + " vertical stripe pixels";
        }

        // check for short products, number of lines
        if (height <= shortProductThreshold) {
            isBad = true;
            cause = "short product";
        }

        final OperatorDescriptor descriptor = getSpi().getOperatorDescriptor();
        final String aliasName = descriptor.getAlias() != null ? descriptor.getAlias() : descriptor.getName();
        final String legend = String.format("%s\t%s\t%s | \t%s\t%s\t%s\t%s | \t%s\t%s\t%s\t%s\t%s | \t%s\t%s",
                                            "Name", "Lines", "QA",
                                            "InvalidLines", "ZeroPixels", "HighSzaPixels", "SmallValues",
                                            "NonVegLand", "VegWater", "HStripes", "VStripes", "Duplicates",
                                            //"FirstInvalidRow", "FirstZeroRadRow",
                                            //"FirstNonVegLandRow", "FirstVegWaterRow", "FirstHorizontalStripesRow",
                                            "Passed", "Cause");
        final String qaRecord = String.format("%s\t%d\t%s | \t%d\t%d\t%d\t%d | \t%d\t%d\t%d\t%d\t%d | \t%b\t%s",
                                              sourceProduct.getName(), height, aliasName,
                                              invalidDataRows, noOfZeroPixels, noOfHighSzaPixels, noOfSmallCountsPixels,
                                              noOfNonVegetatedLandPixels, noOfVegetatedWaterPixels, noHorizontalStripePixels, noVerticalStripePixels, noDuplicateLinesPixels,
                                              //firstInvalidDataRow, firstZeroDataRow,
                                              //firstNonVegetatedLandDataRow, firstVegetatedWaterDataRow, firstHorizontalStripeDataRow,
                                              !isBad, cause);
        System.out.println(legend);
        System.out.println(qaRecord);
        // add QA record to metadata of output
        final MetadataElement qa = new MetadataElement("QA");
        qa.addAttribute(new MetadataAttribute("record", new ProductData.ASCII(qaRecord), false));
        sourceProduct.getMetadataRoot().addElement(qa);
        // add QA flag band and flag coding
        final Band qaFlagBand = sourceProduct.addBand("qa_flags", ProductData.TYPE_INT16);
        qaFlagBand.setData(ProductData.createInstance(qaFlagBuffer));
        FlagCoding flagCoding = createFlagCoding("qa_flags");
        qaFlagBand.setSampleCoding(flagCoding);
        sourceProduct.getFlagCodingGroup().add(flagCoding);

        setTargetProduct(sourceProduct);
    }

    public static FlagCoding createFlagCoding(String flagIdentifier) {
        FlagCoding flagCoding = new FlagCoding(flagIdentifier);
        flagCoding.addFlag("F_ZERO", BitSetter.setFlag(0, F_ZERO), "all counts zero");
        flagCoding.addFlag("F_HIGHSZA", BitSetter.setFlag(0, F_HIGHSZA), "SZA is above threshold (default=75)");
        flagCoding.addFlag("F_COLOUR", BitSetter.setFlag(0, F_COLOUR), "counts 1 or 2 are low (below 50)");
        flagCoding.addFlag("F_NONVEGLAND", BitSetter.setFlag(0, F_NONVEGLAND), "land but NDVI < threshold (default -0.24)");
        flagCoding.addFlag("F_VEGWATER", BitSetter.setFlag(0, F_VEGWATER), "water but NDVI > threshold (default 0.15)");
        flagCoding.addFlag("F_HSTRIPES", BitSetter.setFlag(0, F_HSTRIPES), "horizontal stripes (continuous count gradient)");
        flagCoding.addFlag("F_VSTRIPES", BitSetter.setFlag(0, F_VSTRIPES), "vertical stripes (continuous count gradient)");
        flagCoding.addFlag("F_DUPLICATE", BitSetter.setFlag(0, F_DUPLICATE), "duplicate line (continuous duplication of counts)");
        return flagCoding;
    }

    private int noOfMaskedPixels(Product product, int width, int height, String bandMathExpression, short[] qaFlagBuffer, short flagBit) throws OperatorException {
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
                        qaFlagBuffer[y*width+x] |= flagBit;
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

    private int noVerticalStripePixels(Product product, RasterDataNode invalidRaster, int width, int height, short[] qaFlagBuffer, short flagBit) throws OperatorException {
        try {
            int stripes = 0;
            int noBands = 0;
            int[] counts = new int[width-1];  // counter of stripe length up to current line
            float[] gradients = new float[width-1];
            for (int x=0; x<width-1; ++x) {
                gradients[x] = 0.0f;
            }
            float[] radiances = new float[width];
            int[] flags = new int[width];
            Band[] bands = product.getBands();
            for (Band band : bands) {
                if (! band.getDisplayName().startsWith("counts")) {
                    continue;
                }
                ++noBands;
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
                                        for (int y1=y-counts[x]; y1<y; ++y1) {
                                            qaFlagBuffer[y1*width+x] |= flagBit;
                                        }
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
                                        for (int y1=y-counts[x]; y1<y; ++y1) {
                                            qaFlagBuffer[y1*width+x] |= flagBit;
                                        }
                                    }
                                    counts[x] = 1;
                                    gradients[x] = -1.0f;
                                }
                            } else {
                                if (counts[x] >= stripeMinLength) {
                                    stripes += counts[x];
                                    for (int y1=y-counts[x]; y1<y; ++y1) {
                                        qaFlagBuffer[y1*width+x] |= flagBit;
                                    }
                                }
                                gradients[x] = 0.0f;
                                counts[x] = 0;
                            }
                        } else {
                            if (counts[x] >= stripeMinLength) {
                                stripes += counts[x];
                                for (int y1=y-counts[x]; y1<y; ++y1) {
                                    qaFlagBuffer[y1*width+x] |= flagBit;
                                }
                            }
                            gradients[x] = 0.0f;
                            counts[x] = 0;
                        }
                    }
                }
                for (int x=0; x<width-1; ++x) {
                    if (counts[x] >= stripeMinLength) {
                        stripes += counts[x];
                        for (int y1=height-counts[x]; y1<height; ++y1) {
                            qaFlagBuffer[y1*width+x] |= flagBit;
                        }
                    }
                }
            }
            return stripes / noBands;
        } catch (IOException e) {
            throw new OperatorException(e.getMessage(), e);
        }
    }

    private int noHorizontalStripePixels(Product product, RasterDataNode invalidRaster, int width, int height, short[] qaFlagBuffer, short flagBit) {
        try {
            int stripes = 0;
            int length = 0;
            int noBands = 0;
            this.firstBadLine = -1;
            float gradient0 = 0.0f;
            float[] radiances0 = new float[width];
            float[] radiances = new float[width];
            int[] flags0 = new int[width];
            int[] flags = new int[width];
            Band[] bands = product.getBands();
            for (Band band : bands) {
                if (! band.getDisplayName().startsWith("counts")) {
                    continue;
                }
                ++noBands;
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
                                        for (int x1=x-length; x1<x; ++x1) {
                                            qaFlagBuffer[y*width+x1] |= flagBit;
                                        }
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
                                        for (int x1=x-length; x1<x; ++x1) {
                                            qaFlagBuffer[y*width+x1] |= flagBit;
                                        }
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
                                    for (int x1=x-length; x1<x; ++x1) {
                                        qaFlagBuffer[y*width+x1] |= flagBit;
                                    }
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
                                for (int x1=x-length; x1<x; ++x1) {
                                    qaFlagBuffer[y*width+x1] |= flagBit;
                                }
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
                        for (int x1=width-length; x1<width; ++x1) {
                            qaFlagBuffer[y*width+x1] |= flagBit;
                        }
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
            return stripes / noBands;
        } catch (IOException e) {
            throw new OperatorException(e.getMessage(), e);
        }
    }

    private int noDuplicateLinesPixels(Product product, RasterDataNode invalidRaster, int width, int height, short[] qaFlagBuffer, short flagBit) {
        try {
            int stripes = 0;
            int length = 0;
            int noBands = 0;
            this.firstBadLine = -1;
            float[] radiances0 = new float[width];
            float[] radiances = new float[width];
            int[] flags0 = new int[width];
            int[] flags = new int[width];
            Band[] bands = product.getBands();
            for (Band band : bands) {
                if (! band.getDisplayName().startsWith("counts")) {
                    continue;
                }
                ++noBands;
                invalidRaster.getPixels(0, 0, width, 1, flags0);
                band.readPixels(0, 0, width, 1, radiances0);
                for (int y = 1; y < height; ++y) {
                    invalidRaster.getPixels(0, y, width, 1, flags);
                    band.readPixels(0, y, width, 1, radiances);
                    for (int x=0; x<width-1; ++x) {
                        if (flags[x] == 0 && flags0[x] == 0) {
                            if (radiances[x] == radiances0[x]) {
                                ++length;
                            } else {
                                if (length >= stripeMinLength) {
                                    stripes += length;
                                    for (int x1=x-length; x1<x; ++x1) {
                                        qaFlagBuffer[y*width+x1] |= flagBit;
                                    }
                                    if (this.firstBadLine == -1 || y < this.firstBadLine) {
                                        this.firstBadLine = y;
                                    }
                                }
                                length = 0;
                            }
                        } else {
                            if (length >= stripeMinLength) {
                                stripes += length;
                                    for (int x1=x-length; x1<x; ++x1) {
                                        qaFlagBuffer[y*width+x1] |= flagBit;
                                    }
                                if (this.firstBadLine == -1 || y < this.firstBadLine) {
                                    this.firstBadLine = y;
                                }
                            }
                            length = 0;
                        }
                    }
                    if (length >= stripeMinLength) {
                        stripes += length;
                        for (int x1=width-length; x1<width; ++x1) {
                            qaFlagBuffer[y*width+x1] |= flagBit;
                        }
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
            return stripes / noBands;
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

