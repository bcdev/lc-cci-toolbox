package org.esa.cci.lc.io;

import org.esa.cci.lc.util.CdsVariableWriter;
import org.esa.cci.lc.util.LcHelper;
import org.esa.snap.binning.*;
import org.esa.snap.binning.operator.BinWriter;
import org.esa.snap.binning.support.PlateCarreeGrid;
import org.esa.snap.binning.support.RegularGaussianGrid;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.core.util.io.FileUtils;
import org.esa.snap.core.util.logging.BeamLogManager;
import org.esa.snap.dataio.netcdf.ProfileWriteContext;
import org.esa.snap.dataio.netcdf.nc.NFileWriteable;
import org.esa.snap.dataio.netcdf.nc.NVariable;
import org.esa.snap.dataio.netcdf.nc.NWritableFactory;
import org.geotools.geometry.jts.ReferencedEnvelope;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.Variable;

import java.awt.*;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class LcCdsBinWriter implements BinWriter {

    private String targetFilePath;
    private PlanetaryGrid planetaryGrid;
    private MetadataElement element;
    private static final float FILL_VALUE = Float.NaN;
    private BinningContext binningContext;
    private Logger logger;
    private ReferencedEnvelope region;
    private  Map<String, String> lcProperties;


    public LcCdsBinWriter(Map<String, String> lcProperties, ReferencedEnvelope region,MetadataElement element) {
        this.lcProperties = lcProperties;
        this.element = element;
        logger = BeamLogManager.getSystemLogger();
        this.region = region;
    }



    @Override
    public void write(Map<String, String> metadataProperties, List<TemporalBin> temporalBins) throws IOException {
        NFileWriteable writeable = NWritableFactory.create(targetFilePath, "netcdf4");
        try {
            int sceneWidth = planetaryGrid.getNumCols(0);
            int sceneHeight = planetaryGrid.getNumRows();
            final Dimension tileSize = new Dimension(2025, 2025);
            writeable.addDimension("lat", sceneHeight);
            writeable.addDimension("lon", sceneWidth);
            writeable.addDimension("time", 1);
            writeable.addDimension("bounds", 2);
            writeLCGlobalAttribute(writeable,element);
            LcCdsNetCDF4WriterPlugin.addCustomVariable(writeable,"lon","lon",DataType.DOUBLE,null,element);
            LcCdsNetCDF4WriterPlugin.addCustomVariable(writeable,"lat","lat",DataType.DOUBLE,null,element);
            LcCdsNetCDF4WriterPlugin.addCustomVariable(writeable, "lat_bounds", "lat bounds", DataType.DOUBLE,null,element);
            LcCdsNetCDF4WriterPlugin.addCustomVariable(writeable, "lon_bounds", "lon bounds", DataType.DOUBLE,null,element);
            LcCdsNetCDF4WriterPlugin.addCustomVariable(writeable, "time_bounds", "time bounds", DataType.DOUBLE,null,element);
            LcCdsNetCDF4WriterPlugin.addCustomVariable(writeable, "time", "time", DataType.DOUBLE,null,element);
            ArrayList<NVariable> variables = addFeatureVariables(writeable, tileSize);
            writeable.create();
            CdsVariableWriter.timeWriter(writeable,element);
            Double latMin = element.getAttributeDouble("geospatial_lat_min");
            Double latMax = element.getAttributeDouble("geospatial_lat_max");
            Double lonMin = element.getAttributeDouble("geospatial_lon_min");
            Double lonMax = element.getAttributeDouble("geospatial_lon_max");
            CdsVariableWriter.lcLatLonCustomBoundsWriter(writeable,sceneHeight,sceneWidth,lonMin,lonMax,latMin,latMax);
            fillVariables(temporalBins, variables, sceneWidth, sceneHeight,writeable);
        } catch (Throwable e) {
            e.printStackTrace();
        } finally {
            writeable.close();
        }

    }

    private void fillVariables(List<TemporalBin> temporalBins, ArrayList<NVariable> variables, int sceneWidth, int sceneHeight, NFileWriteable writeable) throws IOException {
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
                lineY = writeDataLine(variables, sceneWidth, dataLines, lineY,writeable);
                lineY = writeEmptyLines(variables, sceneWidth, dataLines, lineY, binY,writeable);
                if (lineY % hundredthHeight == 0) {
                    logger.info(String.format("Line %d of %d done", lineY, sceneHeight));
                }
            }

            for (int i = 0; i < variables.size(); i++) {
                dataLines[i].setElemFloatAt(binX, resultVector.get(i));
            }
        }
        lineY = writeDataLine(variables, sceneWidth, dataLines, lineY, writeable);
        writeEmptyLines(variables, sceneWidth, dataLines, lineY, sceneHeight,writeable);
    }

    private int writeEmptyLines(ArrayList<NVariable> variables, int sceneWidth, ProductData.Float[] dataLines, int lastY, int y,NFileWriteable writeable) throws IOException {
        initDataLines(variables, sceneWidth, dataLines);
        for (; lastY < y; lastY++) {
            writeDataLine(variables, sceneWidth, dataLines, lastY,writeable);
        }
        return lastY;
    }

    private int writeDataLine(ArrayList<NVariable> variables, int sceneWidth, ProductData.Float[] dataLines, int y, NFileWriteable writeable) throws IOException {
        for (int i = 0; i < variables.size(); i++) {
            NVariable variable = variables.get(i);
            Variable netVariable = writeable.getWriter().findVariable(variable.getName());
            int[] origin = new int[]{0, y, 0};
            Array data = Array.factory(netVariable.getDataType(), new int[]{1,1,sceneWidth}, dataLines[i].getArray());
            try {
                writeable.getWriter().write(netVariable, origin, data);
            }
            catch (InvalidRangeException e) {
            }
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


    private ArrayList<NVariable> addFeatureVariables(NFileWriteable writeable, Dimension tileSize) throws IOException {
        final int aggregatorCount = binningContext.getBinManager().getAggregatorCount();
        final ArrayList<NVariable> featureVars = new ArrayList<>(60);
        for (int i = 0; i < aggregatorCount; i++) {
            final Aggregator aggregator = binningContext.getBinManager().getAggregator(i);
            final String[] featureNames = aggregator.getOutputFeatureNames();
            for (String featureName : featureNames) {
                final NVariable featureVar = writeable.addVariable(featureName, DataType.FLOAT, tileSize, "time lat lon");
                Attribute attribute = featureVar.addAttribute("_FillValue", FILL_VALUE);
                featureVars.add(featureVar);
            }
        }
        return featureVars;
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


    private void writeLCGlobalAttribute(NFileWriteable writeable, MetadataElement element) throws IOException {
        final Dimension tileSize = new Dimension(2025, 2025);
        String history = element.getAttributeString("history");

        LcCdsNetCDF4WriterPlugin.addGlobalAttribute(writeable, element, "id", null);
        LcCdsNetCDF4WriterPlugin.addGlobalAttribute(writeable, element, "title", "Land Cover Map of ESA CCI brokered by CDS");
        LcCdsNetCDF4WriterPlugin.addGlobalAttribute(writeable, element, "summary", "This dataset characterizes the land cover of a particular year (see time_coverage). The land cover was derived from the analysis of satellite data time series of the full period.");
        LcCdsNetCDF4WriterPlugin.addGlobalAttribute(writeable, element, "type", null);
        LcCdsNetCDF4WriterPlugin.addGlobalAttribute(writeable, element, "project", null);
        LcCdsNetCDF4WriterPlugin.addGlobalAttribute(writeable, element, "references", null);
        LcCdsNetCDF4WriterPlugin.addGlobalAttribute(writeable, element, "institution", "UCLouvain");
        LcCdsNetCDF4WriterPlugin.addGlobalAttribute(writeable, element, "contact", "https://www.ecmwf.int/en/about/contact-us/get-support");
        LcCdsNetCDF4WriterPlugin.addGlobalAttribute(writeable, element, "comment", null);
        LcCdsNetCDF4WriterPlugin.addGlobalAttribute(writeable, element, "Conventions", null);
        LcCdsNetCDF4WriterPlugin.addGlobalAttribute(writeable, element, "standard_name_vocabulary", null);
        LcCdsNetCDF4WriterPlugin.addGlobalAttribute(writeable, element, "keywords", null);
        LcCdsNetCDF4WriterPlugin.addGlobalAttribute(writeable, element, "keywords_vocabulary", null);
        LcCdsNetCDF4WriterPlugin.addGlobalAttribute(writeable, element, "license", null);
        LcCdsNetCDF4WriterPlugin.addGlobalAttribute(writeable, element, "naming_authority", null);
        LcCdsNetCDF4WriterPlugin.addGlobalAttribute(writeable, element, "cdm_data_type", null);
        LcCdsNetCDF4WriterPlugin.addGlobalAttribute(writeable, element, "TileSize", LcHelper.format(tileSize));
        LcCdsNetCDF4WriterPlugin.addGlobalAttribute(writeable, element, "tracking_id", UUID.randomUUID().toString());
        LcCdsNetCDF4WriterPlugin.addGlobalAttribute(writeable, element, "product_version", null);
        LcCdsNetCDF4WriterPlugin.addGlobalAttribute(writeable, element, "creation_date", LcWriterUtils.COMPACT_ISO_FORMAT.format(new Date()));
        LcCdsNetCDF4WriterPlugin.addGlobalAttribute(writeable, element, "creator_name", "UCLouvain");
        LcCdsNetCDF4WriterPlugin.addGlobalAttribute(writeable, element, "creator_url", "http://www.uclouvain.be/");
        LcCdsNetCDF4WriterPlugin.addGlobalAttribute(writeable, element, "creator_email", null);
        LcCdsNetCDF4WriterPlugin.addGlobalAttribute(writeable, element, "source", null);
        LcCdsNetCDF4WriterPlugin.addGlobalAttribute(writeable, element, "history", history + ",lc-user-tools-" + LcWriterUtils.getModuleVersion());
        LcCdsNetCDF4WriterPlugin.addGlobalAttribute(writeable, element, "time_coverage_start", null);
        LcCdsNetCDF4WriterPlugin.addGlobalAttribute(writeable, element, "time_coverage_end", null);
        LcCdsNetCDF4WriterPlugin.addGlobalAttribute(writeable, element, "time_coverage_duration", null);
        LcCdsNetCDF4WriterPlugin.addGlobalAttribute(writeable, element, "time_coverage_resolution", null);
        LcCdsNetCDF4WriterPlugin.addGlobalAttribute(writeable, element, "geospatial_lat_min", null);
        LcCdsNetCDF4WriterPlugin.addGlobalAttribute(writeable, element, "geospatial_lat_max", null);
        LcCdsNetCDF4WriterPlugin.addGlobalAttribute(writeable, element, "geospatial_lon_min", null);
        LcCdsNetCDF4WriterPlugin.addGlobalAttribute(writeable, element, "geospatial_lon_max", null);
        LcCdsNetCDF4WriterPlugin.addGlobalAttribute(writeable, element, "spatial_resolution", null);
        LcCdsNetCDF4WriterPlugin.addGlobalAttribute(writeable, element, "geospatial_lat_units", null);
        LcCdsNetCDF4WriterPlugin.addGlobalAttribute(writeable, element, "geospatial_lat_resolution", null);
        LcCdsNetCDF4WriterPlugin.addGlobalAttribute(writeable, element, "geospatial_lon_units", null);
        LcCdsNetCDF4WriterPlugin.addGlobalAttribute(writeable, element, "geospatial_lon_resolution", null);
    }






}
