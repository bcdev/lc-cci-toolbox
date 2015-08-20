package org.esa.cci.lc.qa;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.gpf.operators.standard.BandMathsOp;

import java.io.IOException;
import java.util.ArrayList;

import static org.esa.beam.util.Guardian.assertEquals;
import static org.esa.beam.util.Guardian.assertGreaterThan;

/**
 * Class providing a QA for AVHRR L1b products
 *
 * @author boe
 */
@OperatorMetadata(alias = "lc.avhrr.destitchingtable")
public class AvhrrL1DestitchingTableOp extends Operator {

    @SourceProduct(alias = "l1a", description = "AVHRR L1b product")
    private Product sourceProduct;
    @TargetProduct(description = "qa record in metadata")
    private Product qaProduct;

    @Parameter(defaultValue = "1.438833 * 927 / log(1. + 0.000011910659 * pow(927,3) / radiance_4) > 270 " +
            "and latitude >= -84.0 and latitude <= 84.0 and " +
            "X > 350 and X < 1698")
    private String filter;

    @Parameter(defaultValue = "0.5")
    private float gapLatitudeDelta;
    @Parameter(defaultValue = "8")
    private int minLinesSubset;
    @Parameter(defaultValue = "32400")
    private int reprojectedRasterWidth;

    static class Subset {
        int yMin;
        int yMax;
        double lonMin = 180;
        double lonMax = -180;
        double latMin = 90;
        double latMax = -90;
        double lonMinValid = 180;
        double lonMaxValid = -180;
        double latMinValid = 90;
        double latMaxValid = -90;
        int xSubset = -1;
        int ySubset = -1;
        int widthSubset = 0;
        int heightSubset = 0;
        Subset(int yMin, int yMax) {
            this.yMin = yMin;
            this.yMax = yMax;
        }
    }

    @Override
    public void initialize() throws OperatorException {

        final int width = sourceProduct.getSceneRasterWidth();
        final int height = sourceProduct.getSceneRasterHeight();
        qaProduct = new Product("AVHRR destitching metadata", "metadata", width, height);
        //ProductUtils.copyGeoCoding(sourceProduct, qaProduct);

        final ArrayList<Subset> subsets = new ArrayList<Subset>();
        determineSubsets(subsets);

        final StringBuilder record = new StringBuilder();
        for (Subset subset : subsets) {
            determineLonLatExtent(subset);
            determinePixelExtent(subset);

            if (subset.heightSubset < minLinesSubset) {
                continue;
            }
            if (record.length() != 0) {
                record.append('\n');
            }
            record.append(sourceProduct.getFileLocation().getName());
            record.append('\t');
            record.append(subset.yMin);
            record.append('\t');
            record.append(subset.yMax);
            record.append('\t');
            record.append(subset.xSubset);
            record.append('\t');
            record.append(subset.ySubset);
            record.append('\t');
            record.append(subset.widthSubset);
            record.append('\t');
            record.append(subset.heightSubset);
        }

        final String legend = String.format("%s\t%s\t%s\t%s\t%s\t%s\t%s",
                                            "Name", "yMin", "yMax", "x", "y", "width", "height");
        System.out.println(legend);
        System.out.println(record);
        // add QA record to metadata of output
        final MetadataElement qa = new MetadataElement("QA");
        qa.addAttribute(new MetadataAttribute("record", new ProductData.ASCII(record.toString()), false));
        qaProduct.getMetadataRoot().addElement(qa);

        setTargetProduct(qaProduct);
    }

    private void determineSubsets(ArrayList<Subset> subsets) {
        final int tiePointRasterWidth = sourceProduct.getTiePointGrid("longitude").getRasterWidth();
        final int tiePointRasterHeight = sourceProduct.getTiePointGrid("longitude").getRasterHeight();
        // we assume the tie point grid has the same lines as the product raster (but fewer columns)
        assertEquals("tie point raster height", tiePointRasterHeight, sourceProduct.getSceneRasterHeight());
        assertEquals("tie point raster height", sourceProduct.getTiePointGrid("latitude").getRasterHeight(), tiePointRasterHeight);
        assertEquals("tie point raster width", sourceProduct.getTiePointGrid("latitude").getRasterWidth(), tiePointRasterWidth);
        assertGreaterThan("tie point raster width", tiePointRasterWidth, 0);
        final double[] longitude = new double[tiePointRasterHeight * tiePointRasterWidth];
        final double[] latitude = new double[tiePointRasterHeight * tiePointRasterWidth];
        sourceProduct.getTiePointGrid("longitude").getPixels(0, 0, tiePointRasterWidth, tiePointRasterHeight, longitude);
        sourceProduct.getTiePointGrid("latitude").getPixels(0, 0, tiePointRasterWidth, tiePointRasterHeight, latitude);
        int yMin = 0;
        for (int y = 1; y < tiePointRasterHeight; ++y) {
            int pixelGapCount = 0;
            for (int x = 0; x < tiePointRasterWidth; ++x) {
                // compare taxicab distance to point in previous line with threshold
                if (Math.abs(longitude[y*tiePointRasterWidth+x] - longitude[(y-1)*tiePointRasterWidth+x])
                        + Math.abs(latitude[y*tiePointRasterWidth+x] - latitude[(y-1)*tiePointRasterWidth+x])
                        > gapLatitudeDelta) {
                    ++pixelGapCount;
                }
            }
            // consider as gap if at least half the points in the line show the gap
            if (pixelGapCount * 2 >= tiePointRasterWidth) {
                subsets.add(new Subset(yMin, y-1));
                yMin = y;
            }
        }
        // add final subset from last gap (or the beginning) to the end
        subsets.add(new Subset(yMin, tiePointRasterHeight - 1));
    }

    private void determineLonLatExtent(Subset subset) {
        BandMathsOp validOp = BandMathsOp.createBooleanExpressionBand(filter, sourceProduct);
        final Product validProduct = validOp.getTargetProduct();
        Band validBand = validProduct.getBandAt(0);
        try {
            validBand.loadRasterData();
        } catch (IOException e) {
            e.printStackTrace();
            throw new OperatorException("failed to load virtual band for " + filter, e);
        }
        final RasterDataNode validRaster = validProduct.getRasterDataNode(validBand.getName());

        final GeoCoding geoCoding = sourceProduct.getGeoCoding();
        final PixelPos pixelPos = new PixelPos();
        final GeoPos geoPos = new GeoPos();
        final int width = sourceProduct.getSceneRasterWidth();
        int[] isValid = new int[width];

        // determine image extend and extend of valid pixels
        for (int y=subset.yMin; y<=subset.yMax; ++y) {
            isValid = validRaster.getPixels(0, y, width, 1, isValid);
            for (int x=0; x<width; ++x) {
                pixelPos.setLocation(x, y);
                geoCoding.getGeoPos(pixelPos, geoPos);
                if (geoPos.getLon() < subset.lonMin) {
                    subset.lonMin = geoPos.getLon();
                }
                if (geoPos.getLon() > subset.lonMax) {
                    subset.lonMax = geoPos.getLon();
                }
                if (geoPos.getLat() < subset.latMin) {
                    subset.latMin = geoPos.getLat();
                }
                if (geoPos.getLat() > subset.latMax) {
                    subset.latMax = geoPos.getLat();
                }
                if (isValid[x] == 1.0 && geoPos.getLon() < subset.lonMinValid) {
                    subset.lonMinValid = geoPos.getLon();
                }
                if (isValid[x] == 1.0 && geoPos.getLon() > subset.lonMaxValid) {
                    subset.lonMaxValid = geoPos.getLon();
                }
                if (isValid[x] == 1.0 && geoPos.getLat() < subset.latMinValid) {
                    subset.latMinValid = geoPos.getLat();
                }
                if (isValid[x] == 1.0 && geoPos.getLat() > subset.latMaxValid) {
                    if (geoPos.getLat() > 84.0) {
                        System.out.println(y + " " + x + " " + geoPos.getLat());
                    }
                    subset.latMaxValid = geoPos.getLat();
                }
            }
        }
    }

    private void determinePixelExtent(Subset subset) {
        // conversion to subset of valid image within complete reprojected image
        subset.xSubset = (int) ((subset.lonMinValid - subset.lonMin) * reprojectedRasterWidth / 360.0 + 0.5);
        subset.ySubset = (int) ((subset.latMinValid - subset.latMin) * reprojectedRasterWidth / 360.0 + 0.5);
        subset.widthSubset = (int) ((subset.lonMaxValid - subset.lonMinValid) * reprojectedRasterWidth / 360 + 0.5);
        subset.heightSubset = (int) ((subset.latMaxValid - subset.latMinValid) * reprojectedRasterWidth / 360 + 0.5);
        // clipping in case geocoding is not strictly between -180 and 180
        if (subset.xSubset + subset.widthSubset > reprojectedRasterWidth) {
            subset.xSubset = 0;
            subset.widthSubset = reprojectedRasterWidth;
        }
        if (subset.ySubset + subset.heightSubset > reprojectedRasterWidth / 2) {
            subset.ySubset = 0;
            subset.heightSubset = reprojectedRasterWidth / 2;
        }
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(AvhrrL1DestitchingTableOp.class);
        }
    }
}

