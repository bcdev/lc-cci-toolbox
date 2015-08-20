package org.esa.cci.lc.qa;

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
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.descriptor.OperatorDescriptor;
import org.esa.beam.gpf.operators.standard.BandMathsOp;
import org.esa.beam.util.BitSetter;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Class providing a QA for AVHRR L1b products
 *
 * @author boe
 */
@OperatorMetadata(alias = "lc.avhrr.qa")
public class AvhrrL1QaOp extends Operator {

    public static final String NON_ZERO_TERM = "counts_1 > 0 and counts_2 > 0 and counts_3 > 0 and counts_4 > 0 and counts_5 > 0";
    public static final String ACCEPTED_SZA_TERM = "sun_zenith <= 75";
    public static final String LAND_TERM = "LAND==1";
    public static final String WATER_TERM = "LAND==0";
    public static final String VALID_LAND_TERM = NON_ZERO_TERM + " and " + ACCEPTED_SZA_TERM + " and " + LAND_TERM;
    public static final String VALID_WATER_TERM = NON_ZERO_TERM + " and " + ACCEPTED_SZA_TERM + " and " + WATER_TERM;

    public static final String ZERO_EXPRESSION =
            "counts_1 <= 0 or counts_2 <= 0 or counts_3 <= 0 or counts_4 <= 0 or counts_5 <= 0";
    public static final String HIGH_SZA_EXPRESSION =
            "sun_zenith > 75";
    public static final String INVALID_EXPRESSION =
            ZERO_EXPRESSION + " or " + HIGH_SZA_EXPRESSION;
    public static final String NON_VEGETATED_LAND_EXPRESSION =
            VALID_LAND_TERM +
            " and albedo_1 > 0 and albedo_2 > 0" +
            " and 1.438833 * 927 / log(1. + 0.000011910659 * pow(927,3) / radiance_4) > 270" + // no cloud at all
            " and (albedo_2 - albedo_1) / (albedo_2 + albedo_1) < -0.20";  // was -0.24 for MERIS
    public static final String VEGETATED_OCEAN_EXPRESSION =
            VALID_WATER_TERM +
            " and albedo_1 > 0 and albedo_2 > 0" +
            " and 1.438833 * 927 / log(1. + 0.000011910659 * pow(927,3) / radiance_4) > 270" + // no cloud at all
            " and (albedo_2 - albedo_1) / (albedo_2 + albedo_1) >= 0.15";  // was 0.01 for MERIS
//    public static final String NON_VEGETATED_LAND_EXPRESSION2 =
//            VALID_LAND_TERM +
//            " and albedo_1 > 0 and albedo_2 > 0" +
//            " and albedo_1 / albedo_2 < 0.9" +  // no cloud over water
//            " and (albedo_2 - albedo_1) / (albedo_2 + albedo_1) < -0.24";
//    public static final String VEGETATED_OCEAN_EXPRESSION2 =
//            VALID_WATER_TERM +
//            " and albedo_1 > 0 and albedo_2 > 0" +
//            " and albedo_1 / albedo_2 > 1.1" + // no cloud over land
//            " and (albedo_2 - albedo_1) / (albedo_2 + albedo_1) >= 0.15";  // was 0.01 for MERIS
//    public static final String NON_VEGETATED_LAND_EXPRESSION3 =
//            VALID_LAND_TERM +
//            " and albedo_1 > 0 and albedo_2 > 0" +
//            " and albedo_1 / albedo_2 > 1.1" +  // no cloud over land
//            " and (albedo_2 - albedo_1) / (albedo_2 + albedo_1) < -0.24";
//    public static final String VEGETATED_OCEAN_EXPRESSION3 =
//            VALID_WATER_TERM +
//            " and albedo_1 > 0 and albedo_2 > 0" +
//            " and albedo_1 / albedo_2 < 0.9" + // no cloud over water
//            " and (albedo_2 - albedo_1) / (albedo_2 + albedo_1) >= 0.15";  // was 0.01 for MERIS
    public static final String SMALL_COUNTS_EXPRESSION =
            VALID_LAND_TERM +
            " and (counts_2 < 40 or counts_1 < 50)";
    public static final String RRCT_LAND_CLOUD_EXPRESSION =
            VALID_LAND_TERM +
            " and albedo_1 / albedo_2 <= 1.1";
    public static final String RRCT_WATER_CLOUD_EXPRESSION =
            VALID_WATER_TERM +
            " and albedo_1 / albedo_2 >= 0.9";
    public static final String TGCT_LAND_CLOUD_EXPRESSION =
            VALID_LAND_TERM +
            " and 1.438833 * 927 / log(1. + 0.000011910659 * pow(927,3) / radiance_4) <= 244";
    public static final String TGCT_WATER_CLOUD_EXPRESSION =
            VALID_WATER_TERM +
            " and 1.438833 * 927 / log(1. + 0.000011910659 * pow(927,3) / radiance_4) <= 270";
    public static final int F_ZERO = 0;
    public static final int F_HIGHSZA = 1;
    public static final int F_COLOUR = 2;
    public static final int F_NONVEGLAND = 3;
    public static final int F_VEGWATER = 4;
    public static final int F_HSTRIPES = 5;
    public static final int F_VSTRIPES = 6;
    public static final int F_DUPLICATE = 7;
    public static final int F_RRCTLAND = 8;
    public static final int F_RRCTWATER = 9;
    public static final int F_TGCTLAND = 10;
    public static final int F_TGCTWATER = 11;
//    public static final int F_NONVEGLANDRRCTW = 12;
//    public static final int F_VEGWATERRRCTL = 13;
//    public static final int F_NONVEGLANDRRCTL = 14;
//    public static final int F_VEGWATERRRCTW = 15;
    public static final int F_NONVEGLANDLINE = 12;
    public static final int F_VEGWATERLINE = 13;

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
    @Parameter(defaultValue = "20")  // was 45 for MERIS
    private int stripeMinLength;
    @Parameter(defaultValue = "129")
    private int shortProductThreshold;
    @Parameter(defaultValue = "10")
    private int duplicateMinLength;
    @Parameter(defaultValue = "10")
    private int geoshiftLinesThreshold;
    @Parameter(defaultValue = "20")  // was 10 initially
    private int compactAreaMinSize;
    @Parameter(defaultValue = "2")
    private int compactAreaMaxGap;

    // set by counting methods as a side effect, second return value
    private int numCompactPixels = -1;

    @Override
    public void initialize() throws OperatorException {

        final int width = sourceProduct.getSceneRasterWidth();
        final int height = sourceProduct.getSceneRasterHeight();

        final Band b = sourceProduct.getBand(landFlagBandName);
        if (b == null) {
            throw new OperatorException("Input product does not contain specified L1 flag band. - product QA failed.");
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

        final int[] qaFlagBuffer = new int[height * width];
        boolean isBad = false;
        String cause = "";

        // check for invalid pixels with zero radiance in some band
        final int noOfZeroPixels = noOfMaskedPixels(sourceProduct, width, height, ZERO_EXPRESSION, qaFlagBuffer, 1 << F_ZERO);

        // check for invalid pixels with zero radiance in some band
        final int noOfHighSzaPixels = noOfMaskedPixels(sourceProduct, width, height, HIGH_SZA_EXPRESSION, qaFlagBuffer, 1 << F_HIGHSZA);

        // count invalid rows
        final int invalidDataRows = badDataRows(width, invalidRaster, 0, height);

        // check for horizontal stripes by gradient between segments of subsequent lines
        final int noHorizontalStripePixels = noHorizontalStripePixels(sourceProduct, invalidRaster, width, height, qaFlagBuffer, 1 << F_HSTRIPES);
        if (noHorizontalStripePixels > 20 * width) {
            isBad = true;
            cause = "stripes: " + noHorizontalStripePixels + " horizontal stripe pixels";
        }

        // check for duplicate lines by comparison between segments of subsequent lines
        final int noDuplicateLinesPixels = noDuplicateLinesPixels(sourceProduct, invalidRaster, width, height, qaFlagBuffer, 1 << F_DUPLICATE);
        if (noDuplicateLinesPixels > 30 * width) {
            isBad = true;
            cause = "duplicate: " + noDuplicateLinesPixels + " pixels of duplicate lines";
        }

        // check for geo-shift by counting non-vegetated land pixels
        final int noOfNonVegetatedLandPixels = noOfMaskedPixels(sourceProduct, width, height, NON_VEGETATED_LAND_EXPRESSION, qaFlagBuffer, 1 << F_NONVEGLAND, 1 << F_NONVEGLANDLINE);
        final int numCompactNonVegetatedLandPixels = this.numCompactPixels;
        if (numCompactNonVegetatedLandPixels > width * geoshiftLinesThreshold) {
            isBad = true;
            cause = "geocoding: " + noOfNonVegetatedLandPixels  + " pixels non-vegetated land";
        }

        // check for geo-shift by counting vegetated water pixels
        final int noOfVegetatedWaterPixels = noOfMaskedPixels(sourceProduct, width, height, VEGETATED_OCEAN_EXPRESSION, qaFlagBuffer, 1 << F_VEGWATER, 1 << F_VEGWATERLINE);
        final int numCompactVegetatedWaterPixels = this.numCompactPixels;
        if (numCompactVegetatedWaterPixels > width * geoshiftLinesThreshold) {
            isBad = true;
            cause = "geocoding: " + noOfVegetatedWaterPixels  + " pixels vegetated water";
        }

        final int noOfRrctLandPixels = noOfMaskedPixels(sourceProduct, width, height, RRCT_LAND_CLOUD_EXPRESSION, qaFlagBuffer, 1 << F_RRCTLAND);
        final int noOfRrctWaterPixels = noOfMaskedPixels(sourceProduct, width, height, RRCT_WATER_CLOUD_EXPRESSION, qaFlagBuffer, 1 << F_RRCTWATER);
        final int noOfTgctLandPixels = noOfMaskedPixels(sourceProduct, width, height, TGCT_LAND_CLOUD_EXPRESSION, qaFlagBuffer, 1 << F_TGCTLAND);
        final int noOfTgctWaterPixels = noOfMaskedPixels(sourceProduct, width, height, TGCT_WATER_CLOUD_EXPRESSION, qaFlagBuffer, 1 << F_TGCTWATER);
//        final int noOfNonVegetatedLandRrctWPixels = noOfMaskedPixels(sourceProduct, width, height, NON_VEGETATED_LAND_EXPRESSION2, qaFlagBuffer, 1 << F_NONVEGLANDRRCTW);
//        final int noOfVegetatedWaterRrctLPixels = noOfMaskedPixels(sourceProduct, width, height, VEGETATED_OCEAN_EXPRESSION2, qaFlagBuffer, 1 << F_VEGWATERRRCTL);
//        final int noOfNonVegetatedLandRrctLPixels = noOfMaskedPixels(sourceProduct, width, height, NON_VEGETATED_LAND_EXPRESSION3, qaFlagBuffer, 1 << F_NONVEGLANDRRCTL);
//        final int noOfVegetatedWaterRrctWPixels = noOfMaskedPixels(sourceProduct, width, height, VEGETATED_OCEAN_EXPRESSION3, qaFlagBuffer, 1 << F_VEGWATERRRCTW);

        // check for wrong colours by counting valid pixels with zero radiance in some band
        final int noOfSmallCountsPixels = noOfMaskedPixels(sourceProduct, width, height, SMALL_COUNTS_EXPRESSION, qaFlagBuffer, 1 << F_COLOUR);
        if (noOfSmallCountsPixels > 50 * width) {  // was 3*width initially
            isBad = true;
            cause = "colours: " + noOfSmallCountsPixels + " pixels with small radiance in some band";
        }

        // check for vertical stripes by gradient between segments of adjacent columns
        final int noVerticalStripePixels = noVerticalStripePixels(sourceProduct, invalidRaster, width, height, qaFlagBuffer, 1 << F_VSTRIPES);
        if (noVerticalStripePixels > 4 * height) {
            isBad = true;
            cause = "stripes: " + noVerticalStripePixels + " vertical stripe pixels";
        }

        // check for short products, number of lines
        if (height <= shortProductThreshold) {
            isBad = true;
            cause = "short product";
        }

        final int flaggedPixels = noFlaggedPixels(qaFlagBuffer, height, width, 1 << F_ZERO | 1 << F_HIGHSZA | 1 << F_COLOUR | 1 << F_HSTRIPES | 1 << F_VSTRIPES | 1 << F_DUPLICATE);

        final OperatorDescriptor descriptor = getSpi().getOperatorDescriptor();
        final String aliasName = descriptor.getAlias() != null ? descriptor.getAlias() : descriptor.getName();
        final String legend = String.format("%s\t%s\t%s | \t%s\t%s\t%s\t%s | \t%s\t%s\t%s | \t%s\t%s\t%s\t%s | \t%s\t%s",
                                            "Name", "Lines", "QA",
                                            "Valid%", "ZeroPixels", "HighSzaPixels", "SmallValues",
                                            "HStripes", "VStripes", "Duplicates",
                                            "NonVegL", "NVLArea", "VegW", "VWArea",
                                            "Passed", "Cause");
        final String qaRecord = String.format("%s\t%d\t%s | \t%d\t%d\t%d\t%d | \t%d\t%d\t%d | \t%d\t%d\t%d\t%d | \t%b\t%s",
                                              sourceProduct.getName(), height, aliasName,
                                              (height*width-flaggedPixels)*100l/height/width, noOfZeroPixels, noOfHighSzaPixels, noOfSmallCountsPixels,
                                              noHorizontalStripePixels, noVerticalStripePixels, noDuplicateLinesPixels,
                                              noOfNonVegetatedLandPixels, numCompactNonVegetatedLandPixels,
                                              noOfVegetatedWaterPixels, numCompactVegetatedWaterPixels,
                                              !isBad, cause);
        System.out.println(legend);
        System.out.println(qaRecord);
        // add QA record to metadata of output
        final MetadataElement qa = new MetadataElement("QA");
        qa.addAttribute(new MetadataAttribute("record", new ProductData.ASCII(qaRecord), false));
        sourceProduct.getMetadataRoot().addElement(qa);
        // add QA flag band and flag coding
        final Band qaFlagBand = sourceProduct.addBand("qa_flags", ProductData.TYPE_INT32);
        qaFlagBand.setData(ProductData.createInstance(qaFlagBuffer));
        FlagCoding flagCoding = createFlagCoding("qa_flags");
        qaFlagBand.setSampleCoding(flagCoding);
        sourceProduct.getFlagCodingGroup().add(flagCoding);

        sourceProduct.addMask("M_ZERO", "qa_flags.F_ZERO", "all counts zero", Color.cyan, 0.2f);
        sourceProduct.addMask("M_COLOUR", "qa_flags.F_COLOUR", "counts 1 or 2 are low (below 50)", Color.cyan, 0.4f);
        sourceProduct.addMask("M_DUPLICATE", "qa_flags.F_DUPLICATE", "duplicate line (continuous duplication of counts)", Color.cyan, 0.6f);
        sourceProduct.addMask("M_HIGHSZA", "qa_flags.F_HIGHSZA", "SZA is above threshold (default=75)", Color.orange, 0.5f);
        sourceProduct.addMask("M_NONVEGLAND", "qa_flags.F_NONVEGLAND", "land but NDVI < threshold (default -0.2)", Color.magenta, 0.3f);
        sourceProduct.addMask("M_VEGWATER", "qa_flags.F_VEGWATER", "water but NDVI > threshold (default 0.15)", Color.magenta, 0.3f);
        sourceProduct.addMask("M_HSTRIPES", "qa_flags.F_HSTRIPES", "horizontal stripes (continuous count gradient)", Color.red, 0.5f);
        sourceProduct.addMask("M_VSTRIPES", "qa_flags.F_VSTRIPES", "vertical stripes (continuous count gradient)", Color.red, 0.5f);
        sourceProduct.addMask("M_NONVEGLANDLINE", "qa_flags.F_NONVEGLANDLINE", "land, no water cloud, but NDVI < threshold (default -0.2), larger line segment", Color.magenta, 0.7f);
        sourceProduct.addMask("M_VEGWATERLINE", "qa_flags.F_VEGWATERLINE", "water, no land cloud, but NDVI > threshold (default 0.15), larger line segment", Color.magenta, 0.7f);

        setTargetProduct(sourceProduct);
    }

    private int noFlaggedPixels(int[] qaFlagBuffer, int height, int width, int flag) {
        int result = 0;
        for (int y=0; y<height; ++y) {
            for (int x=0; x<width; ++x) {
                if ((qaFlagBuffer[y*width+x] & flag) != 0) {
                    ++result;
                }
            }
        }
        return result;
    }

    public static FlagCoding createFlagCoding(String flagIdentifier) {
        FlagCoding flagCoding = new FlagCoding(flagIdentifier);
        flagCoding.addFlag("F_ZERO", BitSetter.setFlag(0, F_ZERO), "all counts zero");
        flagCoding.addFlag("F_HIGHSZA", BitSetter.setFlag(0, F_HIGHSZA), "SZA is above threshold (default=75)");
        flagCoding.addFlag("F_COLOUR", BitSetter.setFlag(0, F_COLOUR), "counts 1 or 2 are low (below 50)");
        flagCoding.addFlag("F_NONVEGLAND", BitSetter.setFlag(0, F_NONVEGLAND), "land but NDVI < threshold (default -0.2)");
        flagCoding.addFlag("F_VEGWATER", BitSetter.setFlag(0, F_VEGWATER), "water but NDVI > threshold (default 0.15)");
        flagCoding.addFlag("F_HSTRIPES", BitSetter.setFlag(0, F_HSTRIPES), "horizontal stripes (continuous count gradient)");
        flagCoding.addFlag("F_VSTRIPES", BitSetter.setFlag(0, F_VSTRIPES), "vertical stripes (continuous count gradient)");
        flagCoding.addFlag("F_DUPLICATE", BitSetter.setFlag(0, F_DUPLICATE), "duplicate line (continuous duplication of counts)");
        flagCoding.addFlag("F_RRCTLAND", BitSetter.setFlag(0, F_RRCTLAND), "cloud over land");
        flagCoding.addFlag("F_RRCTWATER", BitSetter.setFlag(0, F_RRCTWATER), "cloud over water");
        flagCoding.addFlag("F_TGCTLAND", BitSetter.setFlag(0, F_TGCTLAND), "cloud over land");
        flagCoding.addFlag("F_TGCTWATER", BitSetter.setFlag(0, F_TGCTWATER), "cloud over water");
//        flagCoding.addFlag("F_NONVEGLANDRRCTW", BitSetter.setFlag(0, F_NONVEGLANDRRCTW), "land, no water cloud, but NDVI < threshold (default -0.24)");
//        flagCoding.addFlag("F_VEGWATERRRCTL", BitSetter.setFlag(0, F_VEGWATERRRCTL), "water, no land cloud, but NDVI > threshold (default 0.15)");
//        flagCoding.addFlag("F_NONVEGLANDRRCTL", BitSetter.setFlag(0, F_NONVEGLANDRRCTL), "land, no land cloud, but NDVI < threshold (default -0.24)");
//        flagCoding.addFlag("F_VEGWATERRRCTW", BitSetter.setFlag(0, F_VEGWATERRRCTW), "water, no water cloud, but NDVI > threshold (default 0.15)");
        flagCoding.addFlag("F_NONVEGLANDLINE", BitSetter.setFlag(0, F_NONVEGLANDLINE), "land, no water cloud, but NDVI < threshold (default -0.2), larger line segment");
        flagCoding.addFlag("F_VEGWATERLINE", BitSetter.setFlag(0, F_VEGWATERLINE), "water, no land cloud, but NDVI > threshold (default 0.15), larger line segment");
        return flagCoding;
    }

    private int noOfMaskedPixels(Product product, int width, int height, String bandMathExpression, int[] qaFlagBuffer, int flagBit) throws OperatorException {
        return noOfMaskedPixels(product, width, height, bandMathExpression, qaFlagBuffer, flagBit, 0);
    }
    private int noOfMaskedPixels(Product product, int width, int height, String bandMathExpression, int[] qaFlagBuffer, int flagBit, int compactFlagBit) throws OperatorException {
        try {
            if ("land_water_fraction".equals(landFlagBandName)) {
                bandMathExpression = bandMathExpression.replaceAll(LAND_TERM, "land_water_fraction<50").
                                                        replaceAll(WATER_TERM, "land_water_fraction>=50");
            }
            BandMathsOp toBeCountedOp = BandMathsOp.createBooleanExpressionBand(bandMathExpression, product);
            Band toBeCountedBand = toBeCountedOp.getTargetProduct().getBandAt(0);
            int count = 0;
            this.numCompactPixels = 0;
            int[] flags = new int[width];
            for (int y = 0; y < height; ++y) {
                toBeCountedBand.readPixels(0, y, width, 1, flags);
                int lineCount = 0;
                int compactX = -1;
                int compactCount = 0;
                boolean compactLineToBeFlagged = false;
                for (int x = 0; x < width; ++x) {
                    if (flags[x] != 0) {
                        ++count;
                        ++lineCount;
                        qaFlagBuffer[y*width+x] |= flagBit;
                        ++compactCount;
                        compactX = x;
                    } else if (x > compactX + compactAreaMaxGap + 1) {
                        if (compactCount > compactAreaMinSize) {
                            this.numCompactPixels += compactCount;
                            compactLineToBeFlagged = true;
                        }
                        compactCount = 0;
                    }
                }
                if (compactCount > compactAreaMinSize) {
                    this.numCompactPixels += compactCount;
                    compactLineToBeFlagged = true;
                }
                if (compactLineToBeFlagged && compactFlagBit > 0) {
                    for (int x = 0; x < width; ++x) {
                        qaFlagBuffer[y*width+x] |= compactFlagBit;
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
                if (pixels[x] != 0) {
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

    private int noVerticalStripePixels(Product product, RasterDataNode invalidRaster, int width, int height, int[] qaFlagBuffer, int flagBit) throws OperatorException {
        try {
            int stripes = 0;
            int noBands = 0;
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
                int[] counts = new int[width-1];  // counter of stripe length up to current line
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
                                            qaFlagBuffer[y1*width+x+1] |= flagBit;
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
                                            qaFlagBuffer[y1*width+x+1] |= flagBit;
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
                                        qaFlagBuffer[y1*width+x+1] |= flagBit;
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
                                    qaFlagBuffer[y1*width+x+1] |= flagBit;
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
                            qaFlagBuffer[y1*width+x+1] |= flagBit;
                        }
                    }
                }
            }
            return stripes / noBands;
        } catch (IOException e) {
            throw new OperatorException(e.getMessage(), e);
        }
    }

    private int noHorizontalStripePixels(Product product, RasterDataNode invalidRaster, int width, int height, int[] qaFlagBuffer, int flagBit) {
        try {
            int stripes = 0;
            int length = 0;
            int noBands = 0;
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
                                            qaFlagBuffer[(y-1)*width+x1] |= flagBit;
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
                                            qaFlagBuffer[(y-1)*width+x1] |= flagBit;
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
                                        qaFlagBuffer[(y-1)*width+x1] |= flagBit;
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
                                    qaFlagBuffer[(y-1)*width+x1] |= flagBit;
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
                            qaFlagBuffer[(y-1)*width+x1] |= flagBit;
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

    private int noDuplicateLinesPixels(Product product, RasterDataNode invalidRaster, int width, int height, int[] qaFlagBuffer, int flagBit) {
        try {
            final List<Band> bands = new ArrayList<Band>(5);
            for (Band band : product.getBands()) {
                if (band.getDisplayName().startsWith("counts")) {
                    bands.add(band);
                }
            }
            final int numBands = bands.size();

            int result = 0;
            float[][] radiances0 = new float[numBands][width];
            float[][] radiances = new float[numBands][width];
            int[] flags0 = new int[width];
            int[] flags = new int[width];
            // initialise with first line data
            invalidRaster.getPixels(0, 0, width, 1, flags0);
            for (int b = 0; b < numBands; ++b) {
                bands.get(b).readPixels(0, 0, width, 1, radiances0[b]);
            }
            // line loop
            for (int y = 1; y < height; ++y) {
                int numDuplicates = 0;
                int validDuplicates = 0;
                // read line for all count bands
                invalidRaster.getPixels(0, y, width, 1, flags);
                for (int b = 0; b < numBands; ++b) {
                    bands.get(b).readPixels(0, y, width, 1, radiances[b]);
                }
                // pixel loop
                for (int x=0; x<width-1; ++x) {
                    // check flag and compare pixel with previous line pixel, all bands
                    if (isSpectrumEqual(radiances, radiances0, x)) {
                        ++numDuplicates;
                        if (flags[x] != 0 || flags0[x] != 0) {
                            ++validDuplicates;
                        }
                    } else {
                        if (numDuplicates >= duplicateMinLength && validDuplicates > 0) {
                            result += validDuplicates;
                            for (int x1=x-numDuplicates; x1<x; ++x1) {
                                qaFlagBuffer[y*width+x1] |= flagBit;
                                qaFlagBuffer[(y-1)*width+x1] |= flagBit;
                            }
                        }
                        numDuplicates = 0;
                        validDuplicates = 0;
                    }
                }
                // handle duplicates at end of line
                if (numDuplicates >= duplicateMinLength && validDuplicates > 0) {
                    result += validDuplicates;
                    for (int x1=width-numDuplicates; x1<width; ++x1) {
                        qaFlagBuffer[y*width+x1] |= flagBit;
                        qaFlagBuffer[(y-1)*width+x1] |= flagBit;
                    }
                }
                // switch lines
                final int[] flags1 = flags0;
                flags0 = flags;
                flags = flags1;
                final float[][] radiances1 = radiances0;
                radiances0 = radiances;
                radiances = radiances1;
            }
            return result;
        } catch (IOException e) {
            throw new OperatorException(e.getMessage(), e);
        }
    }

    private static boolean isSpectrumEqual(float[][] radiances, float[][] radiances0, int x) {
        for (int b = 0; b < radiances.length; ++b) {
            if (radiances[b][x] != radiances0[b][x]) {
                return false;
            }
        }
        return true;
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(AvhrrL1QaOp.class);
        }
    }
}

