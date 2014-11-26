package org.esa.cci.lc.io;

import org.esa.beam.binning.Aggregator;
import org.esa.beam.binning.BinningContext;
import org.esa.beam.binning.PlanetaryGrid;
import org.esa.beam.binning.TemporalBin;
import org.esa.beam.binning.WritableVector;
import org.esa.beam.binning.operator.BinWriter;
import org.esa.beam.binning.support.PlateCarreeGrid;
import org.esa.beam.dataio.netcdf.nc.NFileWriteable;
import org.esa.beam.dataio.netcdf.nc.NVariable;
import org.esa.beam.dataio.netcdf.nc.NWritableFactory;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.util.io.FileUtils;
import org.esa.beam.util.logging.BeamLogManager;
import org.esa.cci.lc.util.LcRegularGaussianGrid;
import org.geotools.geometry.jts.ReferencedEnvelope;
import ucar.ma2.DataType;

import java.awt.Dimension;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * @author Marco Peters
 */
public class LcBinWriter implements BinWriter {

    private static final float FILL_VALUE = Float.NaN;
    private final Map<String, String> lcProperties;
    private Logger logger;
    private String targetFilePath;
    private BinningContext binningContext;
    private PlanetaryGrid planetaryGrid;
    private ReferencedEnvelope region;

    public LcBinWriter(Map<String, String> lcProperties, ReferencedEnvelope region) {
        this.lcProperties = lcProperties;
        logger = BeamLogManager.getSystemLogger();
        this.region = region;
    }

    @Override
    public void setBinningContext(BinningContext binningContext) {
        this.binningContext = binningContext;
        if (region != null) {
            this.planetaryGrid = new RegionalPlanetaryGrid(binningContext.getPlanetaryGrid(), region);
        } else {
            this.planetaryGrid = binningContext.getPlanetaryGrid();
        }
    }

    @Override
    public void write(Map<String, String> metadataProperties, List<TemporalBin> temporalBins) throws IOException {
        NFileWriteable writeable = NWritableFactory.create(targetFilePath, "netcdf4");
        try {
            int sceneWidth = planetaryGrid.getNumCols(0);
            int sceneHeight = planetaryGrid.getNumRows();
            writeable.addDimension("lat", sceneHeight);
            writeable.addDimension("lon", sceneWidth);
            Dimension tileSize = new Dimension(128, 128);
            addGlobalAttributes(writeable);
            CoordinateEncoder coordinateEncoder = createCoordinateEncoder();
            coordinateEncoder.addCoordVars(writeable);
            ArrayList<NVariable> variables = addFeatureVariables(writeable, tileSize);
            writeable.create();
            fillVariables(temporalBins, variables, sceneWidth, sceneHeight);
            coordinateEncoder.fillCoordinateVars(writeable);
        } catch (Throwable e) {
            e.printStackTrace();
        } finally {
            writeable.close();
        }
    }

    private CoordinateEncoder createCoordinateEncoder() {
        if (planetaryGrid instanceof PlateCarreeGrid || planetaryGrid instanceof LcRegularGaussianGrid || planetaryGrid instanceof RegionalPlanetaryGrid) {
            return new RegularCoordinateEncoder(planetaryGrid);
        } else {
            throw new IllegalStateException("Unknown planetary grid");
        }
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
        String aggregationType = String.valueOf(lcProperties.remove("aggregationType"));
        writeable.addGlobalAttribute("title", String.format("ESA CCI Land Cover %s Aggregated", aggregationType));
        writeable.addGlobalAttribute("summary",
                                     "This dataset contains the global ESA CCI land cover products " +
                                     "which are spatially aggregated by the lc-user-tool.");

        writeable.addGlobalAttribute("type", lcProperties.remove("type"));
        writeable.addGlobalAttribute("id", lcProperties.remove("id"));

        LcWriterUtils.addGenericGlobalAttributes(writeable);
        LcWriterUtils.addSpecificGlobalAttributes(lcProperties.remove("spatialResolutionDegrees"),
                                                  lcProperties.remove("spatialResolution"),
                                                  lcProperties.remove("temporalCoverageYears"),
                                                  lcProperties.remove("temporalResolution"),
                                                  "Map".equals(aggregationType) ? "Y" : "D",
                                                  lcProperties.remove("startTime"),
                                                  lcProperties.remove("endTime"),
                                                  lcProperties.remove("version"),
                                                  lcProperties.remove("latMax"),
                                                  lcProperties.remove("latMin"),
                                                  lcProperties.remove("lonMin"),
                                                  lcProperties.remove("lonMax"),
                                                  writeable);

        // LC specific way of metadata provision
        for (Map.Entry<String, String> lcPropEentry : lcProperties.entrySet()) {
            writeable.addGlobalAttribute(lcPropEentry.getKey(), lcPropEentry.getValue());
        }

    }

    private ArrayList<NVariable> addFeatureVariables(NFileWriteable writeable, Dimension tileSize) throws IOException {
        final int aggregatorCount = binningContext.getBinManager().getAggregatorCount();
        final ArrayList<NVariable> featureVars = new ArrayList<NVariable>(60);
        for (int i = 0; i < aggregatorCount; i++) {
            final Aggregator aggregator = binningContext.getBinManager().getAggregator(i);
            final String[] featureNames = aggregator.getOutputFeatureNames();
            for (String featureName : featureNames) {
                final NVariable featureVar = writeable.addVariable(featureName, DataType.FLOAT, tileSize, writeable.getDimensions());
                featureVar.addAttribute("_FillValue", FILL_VALUE);
                featureVars.add(featureVar);
            }
        }

        return featureVars;
    }

    private void fillVariables(List<TemporalBin> temporalBins, ArrayList<NVariable> variables, int sceneWidth, int sceneHeight) throws IOException {
        final Iterator<TemporalBin> iterator = temporalBins.iterator();

        ProductData.Float[] dataLines = new ProductData.Float[variables.size()];
        initDataLines(variables, sceneWidth, dataLines);

        int lineY = 0;
        int hundredthHeight = Math.max(sceneHeight / 100, sceneHeight);
        int binX;
        int binY;
        while (iterator.hasNext()) {
            final TemporalBin temporalBin = iterator.next();
            long binIndex = temporalBin.getIndex();
            if (planetaryGrid instanceof RegionalPlanetaryGrid) {
                final RegionalPlanetaryGrid regionalGrid = (RegionalPlanetaryGrid) planetaryGrid;
                if (!regionalGrid.isBinIndexInRegionalGrid(binIndex)) {
                    continue;
                }
                int baseGridWidth = regionalGrid.getGlobalGrid().getNumCols(0);
                binX = (int) (binIndex % baseGridWidth);
                binY = (int) (binIndex / baseGridWidth);
                binX = binX - regionalGrid.getColumnOffset();
                binY = binY - regionalGrid.getRowOffset();
            } else {
                binX = (int) (binIndex % sceneWidth);
                binY = (int) (binIndex / sceneWidth);
            }
            final WritableVector resultVector = temporalBin.toVector();
            if (binY != lineY) {
                lineY = writeDataLine(variables, sceneWidth, dataLines, lineY);
                lineY = writeEmptyLines(variables, sceneWidth, dataLines, lineY, binY);
                if (lineY % hundredthHeight == 0) {
                    logger.info(String.format("Line %d of %d done", lineY, sceneHeight));
                }
            }

            for (int i = 0; i < variables.size(); i++) {
                dataLines[i].setElemFloatAt(binX, resultVector.get(i));
            }
        }
        lineY = writeDataLine(variables, sceneWidth, dataLines, lineY);
        writeEmptyLines(variables, sceneWidth, dataLines, lineY, sceneHeight);
    }

    private int writeEmptyLines(ArrayList<NVariable> variables, int sceneWidth, ProductData.Float[] dataLines, int lastY, int y) throws IOException {
        initDataLines(variables, sceneWidth, dataLines);
        for (; lastY < y; lastY++) {
            writeDataLine(variables, sceneWidth, dataLines, lastY);
        }
        return lastY;
    }

    private int writeDataLine(ArrayList<NVariable> variables, int sceneWidth, ProductData.Float[] dataLines, int y) throws IOException {
        for (int i = 0; i < variables.size(); i++) {
            NVariable variable = variables.get(i);
            variable.write(0, y, sceneWidth, 1, false, dataLines[i]);
        }
        return y + 1;
    }

    private void initDataLines(ArrayList<NVariable> variables, int sceneWidth, ProductData.Float[] dataLines) {
        for (int i = 0; i < variables.size(); i++) {
            if (dataLines[i] != null) {
                Arrays.fill(dataLines[i].getArray(), FILL_VALUE);
            } else {
                float[] line = new float[sceneWidth];
                Arrays.fill(line, FILL_VALUE);
                dataLines[i] = new ProductData.Float(line);
            }
        }
    }

}
