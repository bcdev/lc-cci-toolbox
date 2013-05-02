package org.esa.cci.lc.aggregation;

import org.esa.beam.binning.Aggregator;
import org.esa.beam.binning.BinningContext;
import org.esa.beam.binning.PlanetaryGrid;
import org.esa.beam.binning.TemporalBin;
import org.esa.beam.binning.operator.BinWriter;
import org.esa.beam.binning.support.PlateCarreeGrid;
import org.esa.beam.dataio.netcdf.nc.NFileWriteable;
import org.esa.beam.dataio.netcdf.nc.NVariable;
import org.esa.beam.dataio.netcdf.nc.NWritableFactory;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.util.io.FileUtils;
import org.esa.beam.util.jai.JAIUtils;
import org.esa.beam.util.logging.BeamLogManager;
import ucar.ma2.DataType;

import java.awt.Dimension;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * @author Marco Peters
 */
class LcBinWriter implements BinWriter {

    private static final float FILL_VALUE = Float.NaN;
    private String targetFilePath;
    private Logger logger;
    private BinningContext binningContext;

    LcBinWriter() {
        logger = BeamLogManager.getSystemLogger();
    }

    @Override
    public void write(Map<String, String> metadataProperties, List<TemporalBin> temporalBins) throws IOException {
        NFileWriteable writeable = NWritableFactory.create(targetFilePath, "netcdf4");
        try {
            PlanetaryGrid planetaryGrid = binningContext.getPlanetaryGrid();
            int width = planetaryGrid.getNumCols(0);
            int height = planetaryGrid.getNumRows();
            writeable.addDimension("lon", width);
            writeable.addDimension("lat", height);
            Dimension tileSize = JAIUtils.computePreferredTileSize(width, height, 1);
            addGlobalAttributes(writeable);
            addCoordinateVariables(writeable, tileSize);
            ArrayList<NVariable> variables = addFeatureVariables(writeable, tileSize);
            fillVariables(temporalBins, variables, width, height);
        } catch (Throwable e) {
            e.printStackTrace();
        } finally {
            writeable.create();
            writeable.close();
        }
    }

    @Override
    public void setBinningContext(BinningContext binningContext) {
        this.binningContext = binningContext;
    }

    @Override
    public void setTargetFileTemplatePath(String targetFileTemplatePath) {
        targetFilePath = FileUtils.ensureExtension(targetFileTemplatePath, ".nc");
    }

    @Override
    public String getTargetFilePath() {
        return targetFilePath;
    }

    @Override
    public void setLogger(Logger logger) {
        this.logger = logger;
    }


    private void addGlobalAttributes(NFileWriteable writeable) throws IOException {
        writeable.addGlobalAttribute("title", "ESA CCI Land Cover Map");
        writeable.addGlobalAttribute("summary", "Fill in something meaningful."); // TODO
        writeable.addGlobalAttribute("project", "Climate Change Initiative - European Space Agency");
        writeable.addGlobalAttribute("references", "http://www.esa-landcover-cci.org/");
        writeable.addGlobalAttribute("source", "ESA CCI Land Cover Map product"); // TODO epoch, version, resolution
        writeable.addGlobalAttribute("history", "amorgos-4,0, lc-sdr-1.0, lc-sr-1.0, lc-classification-1.0, aggregation-tool-0.8");  // versions
        writeable.addGlobalAttribute("comment", ""); // TODO

        writeable.addGlobalAttribute("Conventions", "CF-1.6");
        writeable.addGlobalAttribute("standard_name_vocabulary", "NetCDF Climate and Forecast (CF) Standard Names version 21");
        writeable.addGlobalAttribute("keywords", "land cover classification,satellite,observation");     // TODO
        writeable.addGlobalAttribute("keywords_vocabulary", "NASA Global Change Master Directory (GCMD) Science Keywords");
        writeable.addGlobalAttribute("license", "ESA CCI Data Policy: free and open access");
        writeable.addGlobalAttribute("naming_authority", "org.esa-cci");
        writeable.addGlobalAttribute("cdm_data_type", "grid"); // todo
    }

    private void addCoordinateVariables(NFileWriteable writeable, Dimension tileSize) throws IOException {

        if (binningContext.getPlanetaryGrid() instanceof PlateCarreeGrid) {
            final NVariable lat = writeable.addVariable("lat", DataType.FLOAT, tileSize, "y x");
            lat.addAttribute("units", "degrees_north");
            lat.addAttribute("long_name", "latitude coordinate");
            lat.addAttribute("standard_name", "latitude");

            final NVariable lon = writeable.addVariable("lon", DataType.FLOAT, tileSize, "y x");
            lon.addAttribute("units", "degrees_east");
            lon.addAttribute("long_name", "longitude coordinate");
            lon.addAttribute("standard_name", "longitude");
        }
    }

    private ArrayList<NVariable> addFeatureVariables(NFileWriteable writeable, Dimension tileSize) throws IOException {
        final int aggregatorCount = binningContext.getBinManager().getAggregatorCount();
        final ArrayList<NVariable> featureVars = new ArrayList<NVariable>(60);
        for (int i = 0; i < aggregatorCount; i++) {
            final Aggregator aggregator = binningContext.getBinManager().getAggregator(i);
            final String[] featureNames = aggregator.getTemporalFeatureNames();
            for (String featureName : featureNames) {
                final NVariable featureVar = writeable.addVariable(featureName, DataType.FLOAT, tileSize, writeable.getDimensions());
                featureVar.addAttribute("_FillValue", FILL_VALUE);
                featureVars.add(featureVar);
            }
        }

        return featureVars;
    }

    private void fillVariables(List<TemporalBin> temporalBins, ArrayList<NVariable> variables, int width, int height) throws IOException {
        // todo write the actual data, coordinate data need to be written too.
        // Problem: The second chunk (tile) has a width of zero
        float[] line = new float[width];
        Arrays.fill(line, FILL_VALUE);
        ProductData dataLine = ProductData.createInstance(line);
        for (int y = 0; y < height; y++) {
            for (NVariable variable : variables) {
                variable.write(0, y, width, 1, false, dataLine);
            }

        }

    }


}
