package org.esa.cci.lc;

import javax.media.jai.Histogram;
import javax.media.jai.Interpolation;
import javax.media.jai.ROI;
import javax.media.jai.ROIShape;
import javax.media.jai.RenderedOp;
import javax.media.jai.operator.AbsoluteDescriptor;
import javax.media.jai.operator.ExtremaDescriptor;
import javax.media.jai.operator.FileLoadDescriptor;
import javax.media.jai.operator.FileStoreDescriptor;
import javax.media.jai.operator.HistogramDescriptor;
import javax.media.jai.operator.ScaleDescriptor;
import javax.media.jai.operator.SubtractDescriptor;
import javax.media.jai.operator.TranslateDescriptor;
import java.awt.Rectangle;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Hello world!
 */
public class ImageReg {
    private static final Interpolation INTERPOLATION = Interpolation.getInstance(Interpolation.INTERP_BICUBIC);
    private static final String OUTPUT_FILE_NAME = "image-reg-output.csv";

    public static void main(String[] args) throws IOException {
        if (args.length != 4) {
            System.out.println("Usage: ImageReg <image1> <image2> <offset> <octaveCount>");
            System.exit(1);
        }
        String image1Path = args[0];
        String image2Path = args[1];
        int offset = Integer.parseInt(args[2]);
        int octaveCount = Integer.parseInt(args[3]);
        RenderedOp source1 = FileLoadDescriptor.create(image1Path, null, null, null);
        RenderedOp source2 = FileLoadDescriptor.create(image2Path, null, null, null);

        PrintWriter writer = new PrintWriter(new FileWriter(OUTPUT_FILE_NAME));
        for (int k = 0; k < octaveCount; k++) {
            if (source1.getWidth() * source1.getHeight() <= 1) {
                break;
            }

            String title = "Octave " + k + ": image 1 is " + source1.getWidth() + " x " + source1.getHeight() + ", image 2 is " + source2.getWidth() + " x " + source2.getHeight();

            System.out.println(title);

            writer.println();
            writer.println(title);
            writer.println();
            writer.print("dx");
            writer.print("\t");
            writer.print("dy");
            for (int i = 0; i < source1.getNumBands(); i++) {
                writer.print("\t");
                writer.print("Mean." + i);
                writer.print("\t");
                writer.print("StdDev." + i);
                writer.print("\t");
                writer.print("P90." + i);
                writer.print("\t");
                writer.print("Entropy." + i);
            }
            writer.println();

            Rectangle image1Region = new Rectangle(source1.getWidth(), source1.getHeight());

            for (int dy = -offset; dy <= offset; dy++) {
                for (int dx = -offset; dx <= offset; dx++) {

                    Rectangle shiftedImage2Region = new Rectangle(dx, dy, source2.getWidth(), source2.getHeight());
                    Rectangle intersection = shiftedImage2Region.intersection(image1Region);
                    if (intersection.isEmpty()) {
                        continue;
                    }

                    ROI roi = new ROIShape(intersection);
                    RenderedOp translatedOp = TranslateDescriptor.create(source2, (float) dx, (float) dy, INTERPOLATION, null);
                    RenderedOp subtractedOp = AbsoluteDescriptor.create(SubtractDescriptor.create(source1, translatedOp, null), null);
                    RenderedOp extremaOp = ExtremaDescriptor.create(subtractedOp, roi, 1, 1, false, 1, null);
                    double[][] extrema = (double[][]) extremaOp.getProperty("extrema");
                    double[] mins = extrema[0];
                    double[] maxs = extrema[1];
                    if (!checkValidMinMax(subtractedOp, mins, maxs)) {
                        continue;
                    }

                    RenderedOp histogramOp = HistogramDescriptor.create(extremaOp, roi, 1, 1, new int[]{256}, mins, maxs, null);
                    Histogram histogram = (Histogram) histogramOp.getProperty("histogram");

                    writer.print(dx);
                    writer.print("\t");
                    writer.print(dy);
                    for (int i = 0; i < histogram.getNumBands(); i++) {
                        writer.print("\t");
                        writer.print(histogram.getMean()[i]);
                        writer.print("\t");
                        writer.print(histogram.getStandardDeviation()[i]);
                        writer.print("\t");
                        writer.print(histogram.getPTileThreshold(0.9)[i]);
                        writer.print("\t");
                        writer.print(histogram.getEntropy()[i]);
                    }
                    writer.println();
                }
            }
            source1 = ScaleDescriptor.create(source1, 0.5F, 0.5F, 0.0F, 0.0F, INTERPOLATION, null);
            source2 = ScaleDescriptor.create(source2, 0.5F, 0.5F, 0.0F, 0.0F, INTERPOLATION, null);
        }
        writer.close();

        System.out.println("Output written to " + OUTPUT_FILE_NAME);

    }

    private static boolean checkValidMinMax(RenderedOp subtractedOp, double[] mins, double[] maxs) {
        boolean valid = true;
        for (int i = 0; i < subtractedOp.getNumBands(); i++) {
            double min = mins[i];
            double max = maxs[i];
            valid = min < max;
            if (!valid) {
                break;
            }
        }
        return valid;
    }
}
