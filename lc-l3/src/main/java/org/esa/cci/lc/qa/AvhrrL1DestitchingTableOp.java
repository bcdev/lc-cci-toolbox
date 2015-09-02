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
import org.esa.beam.gpf.operators.standard.SubsetOp;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.resources.geometry.XRectangle2D;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.ArrayList;

import static org.esa.beam.util.Guardian.assertEquals;
import static org.esa.beam.util.Guardian.assertGreaterThan;

/**
 * Operator that converts an AVHRR L1B 1km product into a QA metadata record of gap-free subsets.
 * Some AVHRR products are stitched and fall apart into two or more subsets.
 * The AVHRR product will run through a GPT graph of subset - reproject - subset later.
 * This operator determines the parameters for this graph.
 *
 * This operator determines the gaps resulting in line subsets.
 * Each subset is searched for its valid extent, dropping its border of 350 pixels,
 * pixels with latitude values below -84 or above 84 degrees, and cloudy pixels.
 *
 * The operator adds a metadata string "QA" with a record for each subset. The record contains
 * inputPath outputName yMin yLen x y width height
 * with yMin yLen for the first subsetting and the other parameters for the second after reprojection.
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
    @Parameter(defaultValue = "350")
    private int marginWidth;
    @Parameter(defaultValue = "200")
    private int subsetMinLines;
    @Parameter(defaultValue = "true")
    private boolean cutAtAntimeridian;
    @Parameter(defaultValue = "-60")
    private float cutAtLatitude;

    /**
     * A Subset describes a continuous set of lines of the input product with no spatial gaps
     * between subsequent lines.
     */
    static class Subset {
        static final int WEST = -1; // between -180 and -30 longitude
        static final int GLOBE = 0; // no anti-meridian crossing
        static final int EAST = 1;  // between -30 and 180 longitude
        int hemisphere;
        int yMin;
        int numLines;
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
        Subset(int yMin, int numLines, int hemisphere) {
            this.yMin = yMin;
            this.numLines = numLines;
            this.hemisphere = hemisphere;
        }
    }

    /**
     * Operator implementation method that determines the gaps and computes the extent of each subset
     * in the target projection and the extent of the valid part of the subset.
     * @throws OperatorException
     */
    @Override
    public void initialize() throws OperatorException {

        final int width = sourceProduct.getSceneRasterWidth();
        final int height = sourceProduct.getSceneRasterHeight();
        final String name = sourceProduct.getFileLocation().getName();
        final String inputPath = "/calvalus/eodata/AVHRR_L1B/noaa" + name.substring(2,4) +
                "/19" + name.substring(8,10) + "/" + name.substring(4,6) + "/" + name.substring(6,8) +
                "/" + name;

        // find gaps and create subsets, at least one
        final ArrayList<Subset> subsets = new ArrayList<Subset>();
        determineSubsets(subsets);

        // determine extent and write one line per subset for later concatenation
        final StringBuilder accu = new StringBuilder();
        for (Subset subset : subsets) {
            determineBoundingBox(subset);
            if (determineValidBox(subset)) {
                determineValidPixelSubsetAfterReprojection(subset);
                formatSubsetRecord(subset, inputPath, name.substring(0, 23), accu);
            }
        }

        final String legend = String.format("%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s",
                                            "Name", "output", "yMin", "yLen", "x", "y", "width", "height");
        System.out.println(legend);
        System.out.println(accu);

        // add QA accu to metadata of output
        qaProduct = new Product(inputPath, "metadata", width, height);
        //ProductUtils.copyGeoCoding(sourceProduct, qaProduct);
        final MetadataElement qa = new MetadataElement("QA");
        qa.addAttribute(new MetadataAttribute("record", new ProductData.ASCII(accu.toString()), false));
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
        final float[] longitude = sourceProduct.getTiePointGrid("longitude").getTiePoints();
        final float[] latitude = sourceProduct.getTiePointGrid("latitude").getTiePoints();
        int yMin = 0;
        boolean antiMeridianCrossed = false;
        for (int y = 1; y < tiePointRasterHeight; ++y) {
            int pixelGapCount = 0;
            for (int x = 0; x < tiePointRasterWidth; ++x) {
                // compare taxicab distance to point in previous line with threshold
                if (Math.abs(longitude[y*tiePointRasterWidth+x] - longitude[(y-1)*tiePointRasterWidth+x])
                        + Math.abs(latitude[y*tiePointRasterWidth+x] - latitude[(y-1)*tiePointRasterWidth+x])
                        > gapLatitudeDelta) {
                    ++pixelGapCount;
                }
                // check whether subsequent pixels in a row are on different sides of the antimeridian
                if (! antiMeridianCrossed
                        && x > 0
                        && ((longitude[(y-1)*tiePointRasterWidth+x-1] < -90 && longitude[(y-1)*tiePointRasterWidth+x] >= 90)
                            || (longitude[(y-1)*tiePointRasterWidth+x-1] >= 90 && longitude[(y-1)*tiePointRasterWidth+x] < -90))) {
                    antiMeridianCrossed = true;
                    System.out.println("antimeridian crossed y=" + y + " x=" + x);
                }
            }
            // consider as gap if at least half the points in the line show the gap
            if (pixelGapCount * 2 >= tiePointRasterWidth) {
                final int numLines = y - yMin;
                if (numLines >= subsetMinLines) {
                    if (cutAtAntimeridian && antiMeridianCrossed) {
                        subsets.add(new Subset(yMin, numLines, Subset.WEST));
                        subsets.add(new Subset(yMin, numLines, Subset.EAST));
                    } else {
                        subsets.add(new Subset(yMin, numLines, Subset.GLOBE));
                    }
                }
                antiMeridianCrossed = false;
                yMin = y;
            }
        }
        // add final subset from last gap (or the beginning) to the end
        final int numLines = tiePointRasterHeight - yMin;
        if (subsets.isEmpty() || numLines >= subsetMinLines) {
            if (cutAtAntimeridian && antiMeridianCrossed) {
                subsets.add(new Subset(yMin, numLines, Subset.WEST));
                subsets.add(new Subset(yMin, numLines, Subset.EAST));
            } else {
                subsets.add(new Subset(yMin, numLines, Subset.GLOBE));
            }
        }
    }

    private void determineBoundingBox(Subset subset) {
        // determine extent the same way as SubsetOp and ReprojectionOp
        final SubsetOp subsetOp = new SubsetOp();
        subsetOp.setParameterDefaultValues();
        subsetOp.setSourceProduct(sourceProduct);
        subsetOp.setRegion(new Rectangle(350, subset.yMin, 1348, subset.numLines));
        final Product subsetProduct = subsetOp.getTargetProduct();
        Rectangle2D mapBoundary;
        try {
            final CoordinateReferenceSystem sourceCrs = subsetProduct.getGeoCoding().getImageCRS();
            final CoordinateReferenceSystem targetCrs = CRS.decode("EPSG:4326", true);
            final int sourceW = subsetProduct.getSceneRasterWidth();
            final int sourceH = subsetProduct.getSceneRasterHeight();
            final Rectangle2D rect = XRectangle2D.createFromExtremums(0.5, 0.5, sourceW - 0.5, sourceH - 0.5);
            int pointsPerSide = Math.max(sourceH, sourceW) / 10;
            pointsPerSide = Math.max(9, pointsPerSide);
            final ReferencedEnvelope sourceEnvelope = new ReferencedEnvelope(rect, sourceCrs);
            final ReferencedEnvelope targetEnvelope = sourceEnvelope.transform(targetCrs, true, pointsPerSide);
            double minX = targetEnvelope.getMinX();
            double width = targetEnvelope.getWidth();
            if (subsetProduct.getGeoCoding().isCrossingMeridianAt180()) {
                minX = -180.0;
                width = 360;
            }
            mapBoundary = new Rectangle2D.Double(minX, targetEnvelope.getMinY(), width, targetEnvelope.getHeight());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        subset.lonMin = mapBoundary.getX();
        subset.lonMax = mapBoundary.getX() + mapBoundary.getWidth();
        subset.latMin = mapBoundary.getY();
        subset.latMax = mapBoundary.getY() + mapBoundary.getHeight();
        // print out extends,
        System.out.println("lonMin " + subset.lonMin + " lonMax " + subset.lonMax + " latMin " + subset.latMin + " latMax " + subset.latMax);
    }

    private boolean determineValidBox(Subset subset) {
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
        for (int y=subset.yMin; y<subset.yMin+subset.numLines; ++y) {
            isValid = validRaster.getPixels(0, y, width, 1, isValid);
            for (int x=marginWidth; x<width-marginWidth; ++x) {
                pixelPos.setLocation(x+0.5f, y+0.5f);
                geoCoding.getGeoPos(pixelPos, geoPos);
                if ((subset.hemisphere == Subset.WEST && geoPos.getLon() >= -30.0f)
                    || (subset.hemisphere == Subset.EAST && geoPos.getLon() < -30.0f)
                    || geoPos.getLat() < cutAtLatitude) {
                    continue;
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
                    subset.latMaxValid = geoPos.getLat();
                }
            }
        }

        System.out.println("valMin " + subset.lonMinValid + " valMax " + subset.lonMaxValid + " valMin " + subset.latMinValid + " valMax " + subset.latMaxValid);
        // clip valid subset to reprojected input
        if (subset.hemisphere == Subset.WEST) {
            subset.lonMinValid = -180.0;
        }
        if (subset.hemisphere == Subset.EAST) {
            subset.lonMaxValid = 180.0;
        }
        if (subset.lonMinValid < subset.lonMin) {
            subset.lonMinValid = subset.lonMin;
        }
        if (subset.lonMaxValid > subset.lonMax) {
            subset.lonMaxValid = subset.lonMax;
        }
        if (subset.latMinValid < subset.latMin) {
            subset.latMinValid = subset.latMin;
        }
        if (subset.latMaxValid > subset.latMax) {
            subset.latMaxValid = subset.latMax;
        }
        return subset.lonMinValid < subset.lonMaxValid && subset.latMinValid < subset.latMaxValid;
    }

    private void determineValidPixelSubsetAfterReprojection(Subset subset) {
        // conversion to subset of valid image within complete reprojected image
        subset.xSubset = (int) ((subset.lonMinValid - subset.lonMin) * reprojectedRasterWidth / 360.0 + 0.5);
        subset.ySubset = (int) ((subset.latMax - subset.latMaxValid) * reprojectedRasterWidth / 360.0 + 0.5);
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

    private void formatSubsetRecord(Subset subset, String inputPath, String outputBasename, StringBuilder accu) {
        // the first line gets its inputPath prefix during aggregation
        if (accu.length() != 0) {
            accu.append('\n');
            accu.append(inputPath);
            accu.append('\t');
        }
        accu.append(outputBasename);
        accu.append("_Line");
        accu.append(subset.yMin);
        if (subset.hemisphere == Subset.WEST) {
            accu.append("W");
        } else if (subset.hemisphere == Subset.EAST) {
            accu.append("E");
        }
        accu.append(".l1b");
        accu.append('\t');
        accu.append(subset.yMin);
        accu.append('\t');
        accu.append(subset.numLines);
        accu.append('\t');
        accu.append(subset.xSubset);
        accu.append('\t');
        accu.append(subset.ySubset);
        accu.append('\t');
        accu.append(subset.widthSubset);
        accu.append('\t');
        accu.append(subset.heightSubset);
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(AvhrrL1DestitchingTableOp.class);
        }
    }
}

