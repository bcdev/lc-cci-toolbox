package org.esa.cci.lc;

import org.esa.beam.util.io.FileUtils;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYTextAnnotation;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.TextAnchor;

import java.awt.Color;
import java.awt.Font;
import java.awt.geom.Ellipse2D;
import java.io.File;
import java.io.IOException;

/**
 * @author Marco Peters
 */
public class ScatterPlotExporter {

    private final JFreeChart chart;
    private int imageWidth;
    private int imageHeight;

    public static XYDataset createDataset(float[][] clearLandDataB0, float[] lineParameters) {

        XYSeries series1 = new XYSeries("Series 1");
        float[] xData = clearLandDataB0[1];
        float[] yData = clearLandDataB0[0];
        for (int i = 0; i < xData.length; i++) {
            series1.add(xData[i], yData[i]);

        }
        XYSeries series2 = new XYSeries("Series 2");
        // y = m*x + n
        series2.add(0.0, lineParameters[1] * 0.0 + lineParameters[0]);
        series2.add(1.0, lineParameters[1] * 1.0 + lineParameters[0]);
        XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(series1);
        dataset.addSeries(series2);
        return dataset;
    }

    public ScatterPlotExporter(int imageWidth, int imageHeight) {
        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;

        boolean showLegend = false;
        boolean showTooltips = false;
        boolean useUrls = false;
        XYDataset dataset = null;
        chart = ChartFactory.createScatterPlot("Scatter Plot", "x", "y", dataset, PlotOrientation.VERTICAL,
                                               showLegend, showTooltips, useUrls);
        XYPlot plot = (XYPlot) chart.getPlot();

        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        // first series (bunch of points) shall be drawn as points individually
        renderer.setSeriesLinesVisible(0, false);
        renderer.setSeriesShape(0, createCircle(1));
        Color lightBlue = Color.BLUE.brighter().brighter();
        Color series1Color = new Color(lightBlue.getRed(), lightBlue.getGreen(), lightBlue.getBlue(), 50);
        renderer.setSeriesPaint(0, series1Color);
        renderer.setSeriesShapesFilled(0, true);
        renderer.setSeriesShapesVisible(0, true);
        // second series (two points) shall be connected with a line
        renderer.setSeriesShape(1, createCircle(1));
        renderer.setSeriesLinesVisible(1, true);
        renderer.setSeriesPaint(1, Color.BLACK);
        plot.setRenderer(renderer);

//        domain axis is the X axis
        plot.getDomainAxis().setAutoRange(false);
        plot.getDomainAxis().setRange(0, 1);

        // range axis is the Y axis
        plot.getRangeAxis().setAutoRange(false);
        plot.getRangeAxis().setRange(0, 1);

    }

    public void export(float[][] scatter, float[] line, String xAxisLabel, String yAxisLabel, File file, String surfaceType) throws IOException {
        XYDataset dataset = createDataset(scatter, line);
        saveChart("Comparison of reflectance values of SPOT_VGT_P and SPOT_VGT_S1", xAxisLabel, yAxisLabel,
                  dataset, line, file, surfaceType);
    }

    private void saveChart(String title, String xAxisLabel, String yAxisLabel, XYDataset dataset, float[] lineParam,
                           File file, String surfaceType) throws IOException {
        File theFile = FileUtils.ensureExtension(file, ".png");
        chart.setTitle(title);
        XYPlot plot = (XYPlot) chart.getPlot();
        plot.getDomainAxis().setLabel(xAxisLabel);
        plot.getRangeAxis().setLabel(yAxisLabel);
        plot.setDataset(dataset);
        String label1 = String.format("y = %4.3f x + %4.3f", lineParam[1], lineParam[0]);
        String label2 = String.format("residuum = %4.3f", lineParam[2]);
        String label3 = String.format("number = %4.0f (%s)", lineParam[3], surfaceType);
        XYTextAnnotation annotation1 = createLineAnnotation(0.6, 0.3, label1);
        XYTextAnnotation annotation2 = createLineAnnotation(0.6, 0.25, label2);
        XYTextAnnotation annotation3 = createLineAnnotation(0.6, 0.2, label3);
        plot.getRenderer().addAnnotation(annotation1);
        plot.getRenderer().addAnnotation(annotation2);
        plot.getRenderer().addAnnotation(annotation3);

        ChartUtilities.saveChartAsPNG(theFile, chart, imageWidth, imageHeight);
    }

    private XYTextAnnotation createLineAnnotation(double x, double y, String text) {
        XYTextAnnotation annotation = new XYTextAnnotation(text, x, y);
        annotation.setTextAnchor(TextAnchor.CENTER);
        Font font = annotation.getFont();
        annotation.setFont(font.deriveFont(12));
        annotation.setPaint(Color.BLACK);
        return annotation;
    }

    private Ellipse2D createCircle(int radius) {
        return new Ellipse2D.Double(-radius, -radius, radius * 2, radius * 2);
    }

}
