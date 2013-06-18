package org.esa.cci.lc.io;

import org.esa.beam.dataio.netcdf.DefaultNetCdfWriter;
import org.esa.beam.dataio.netcdf.NullProfilePartWriter;
import org.esa.beam.dataio.netcdf.ProfileWriteContext;
import org.esa.beam.dataio.netcdf.metadata.ProfileInitPartWriter;
import org.esa.beam.dataio.netcdf.metadata.ProfilePartWriter;
import org.esa.beam.dataio.netcdf.metadata.profiles.beam.BeamBandPart;
import org.esa.beam.dataio.netcdf.metadata.profiles.beam.BeamInitialisationPart;
import org.esa.beam.dataio.netcdf.metadata.profiles.beam.BeamNetCdf4WriterPlugIn;
import org.esa.beam.dataio.netcdf.nc.NFileWriteable;
import org.esa.beam.dataio.netcdf.nc.NVariable;
import org.esa.beam.dataio.netcdf.util.Constants;
import org.esa.beam.dataio.netcdf.util.DataTypeUtils;
import org.esa.beam.dataio.netcdf.util.ReaderUtils;
import org.esa.beam.framework.dataio.ProductWriter;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.jai.ImageManager;
import ucar.ma2.ArrayByte;
import ucar.ma2.DataType;

import java.awt.Dimension;
import java.io.IOException;
import java.text.MessageFormat;

/**
 * @author Martin Boettcher
 */
public class LcMapNetCdf4WriterPlugIn extends BeamNetCdf4WriterPlugIn {

    public static final String FORMAT_NAME = "NetCDF4-LC-Map";
    public static final byte[] LCCS_CLASS_FLAG_VALUES = new byte[]{
            0, 10, 11,
            12, 20, 30,
            40, 50, 60,
            61, 62, 70,
            71, 72, 80,
            81, 82, 90,
            100, 110, 120,
            (byte) 130, (byte) 140, (byte) 150,
            (byte) 160, (byte) 170, (byte) 180,
            (byte) 190, (byte) 200, (byte) 210,
            (byte) 220
    };
    public static final String LCCS_CLASS_FLAG_MEANINGS = "no_data cropland_rainfed cropland_rainfed_herbaceous_cover " +
                                                          "cropland_rainfed_tree_or_shrub_cover cropland_irrigated mosaic_cropland " +
                                                          "mosaic_natural_vegetation tree_broadleaved_evergreen_closed_to_open tree_broadleaved_deciduous_closed_to_open " +
                                                          "tree_broadleaved_deciduous_closed tree_broadleaved_deciduous_open tree_needleleaved_evergreen_closed_to_open " +
                                                          "tree_needleleaved_evergreen_closed tree_needleleaved_evergreen_open tree_needleleaved_deciduous_closed_to_open " +
                                                          "tree_needleleaved_deciduous_closed tree_needleleaved_deciduous_open tree_mixed " +
                                                          "mosaic_tree_and_shrub mosaic_herbaceous shrubland " +
                                                          "grassland lichens_and_mosses sparse_vegetation " +
                                                          "tree_cover_flooded_fresh_or_brakish_water tree_cover_flooded_saline_water shrub_or_herbaceous_cover_flooded " +
                                                          "urban bare_areas water " +
                                                          "snow_and_ice";

    @Override
    public String[] getFormatNames() {
        return new String[]{FORMAT_NAME};
    }

    @Override
    public ProductWriter createWriterInstance() {
        return new DefaultNetCdfWriter(this);
    }

    @Override
    public ProfilePartWriter createMetadataPartWriter() {
        return new NullProfilePartWriter();
    }

    @Override
    public ProfileInitPartWriter createInitialisationPartWriter() {
        return new LcMapInitialisationPart();
    }

    @Override
    public ProfilePartWriter createDescriptionPartWriter() {
        return new NullProfilePartWriter();
    }

    class LcMapInitialisationPart extends BeamInitialisationPart {

        @Override
        public void writeProductBody(ProfileWriteContext ctx, Product product) throws IOException {

            final LcMapMetadata lcMapMetadata = new LcMapMetadata(product);
            final String spatialResolution = lcMapMetadata.getSpatialResolution();
            final String temporalResolution = lcMapMetadata.getTemporalResolution();
            final String epoch = lcMapMetadata.getEpoch();
            final String version = lcMapMetadata.getVersion();

            final GeoCoding geoCoding = product.getGeoCoding();
            final GeoPos upperLeft = geoCoding.getGeoPos(new PixelPos(0, 0), null);
            final GeoPos lowerRight = geoCoding.getGeoPos(new PixelPos(product.getSceneRasterWidth(), product.getSceneRasterHeight()), null);
            final float latMax = upperLeft.getLat();
            final float latMin = lowerRight.getLat();
            final float lonMin = upperLeft.getLon();
            final float lonMax = lowerRight.getLon();

            final NFileWriteable writeable = ctx.getNetcdfFileWriteable();

            // global attributes
            writeable.addGlobalAttribute("title", "ESA CCI Land Cover Map");
            writeable.addGlobalAttribute("summary",
                                         "This dataset contains the global ESA CCI land cover classification map derived from satellite data of one epoch.");
            LcWriterUtils.addGenericGlobalAttributes(writeable);

            //writeable.addGlobalAttribute("platform", platform);
            //writeable.addGlobalAttribute("sensor", sensor);
            // we use the name in order to transfer the identifier from the LcSubsetOp to this class
            String regionIdentifier = product.getName();
            writeable.addGlobalAttribute("type", MessageFormat.format("ESACCI-LC-L4-LCCS-Map-{0}m-P{1}Y-{2}",
                                                                      spatialResolution,
                                                                      temporalResolution,
                                                                      regionIdentifier));
            writeable.addGlobalAttribute("id", MessageFormat.format("ESACCI-LC-L4-LCCS-Map-{0}m-P{1}Y-{4}-{2}-v{3}",
                                                                    spatialResolution,
                                                                    temporalResolution,
                                                                    epoch,
                                                                    version,
                                                                    regionIdentifier));

            String spatialResolutionDegrees = "300".equals(spatialResolution) ? "0.002778" : "0.011112";
            String startYear = String.valueOf(Integer.parseInt(epoch) - Integer.parseInt(temporalResolution) / 2);
            String startTime = startYear + "0101";
            String endYear = String.valueOf(Integer.parseInt(epoch) + Integer.parseInt(temporalResolution) / 2);
            String endTime = endYear + "1231";
            final int temporalCoverageYears;
            try {
                temporalCoverageYears = Integer.parseInt(endYear) - Integer.parseInt(startYear) + 1;
            } catch (NumberFormatException ex) {
                throw new RuntimeException("cannot parse " + startYear + " and " + endYear + " as year numbers", ex);
            }

            LcWriterUtils.addSpecificGlobalAttribute(spatialResolutionDegrees, spatialResolution, temporalCoverageYears, temporalResolution,
                                                     startTime, endTime,
                                                     version, latMax, latMin, lonMin, lonMax, writeable
            );

            final Dimension tileSize = ImageManager.getPreferredTileSize(product);
            writeable.addGlobalAttribute("TileSize", tileSize.height + ":" + tileSize.width);
            //TODO writeable.addDimension("time", 1);
            writeable.addDimension("lat", product.getSceneRasterHeight());
            writeable.addDimension("lon", product.getSceneRasterWidth());
        }

    }

    @Override
    public ProfilePartWriter createBandPartWriter() {
        return new BeamBandPart() {
            @Override
            public void preEncode(ProfileWriteContext ctx, Product p) throws IOException {

                final NFileWriteable ncFile = ctx.getNetcdfFileWriteable();
                java.awt.Dimension tileSize = ImageManager.getPreferredTileSize(p);
                StringBuilder ancillaryVariables = new StringBuilder();
                for (Band band : p.getBands()) {
                    if ("processed_flag".equals(band.getName()) ||
                        "current_pixel_state".equals(band.getName()) ||
                        "observation_count".equals(band.getName()) ||
                        "algorithmic_confidence_level".equals(band.getName()) ||
                        "overall_confidence_level".equals(band.getName())) {
                        if (ancillaryVariables.length() > 0) {
                            ancillaryVariables.append(' ');
                        }
                        ancillaryVariables.append(band.getName());
                    }
                }
                for (Band band : p.getBands()) {
                    if ("lccs_class".equals(band.getName())) {
                        addLccsClassVariable(ncFile, band, tileSize, ancillaryVariables.toString());
                    } else if ("processed_flag".equals(band.getName())) {
                        addProcessedFlagVariable(ncFile, band, tileSize);
                    } else if ("current_pixel_state".equals(band.getName())) {
                        addCurrentPixelStateVariable(ncFile, band, tileSize);
                    } else if ("observation_count".equals(band.getName())) {
                        addObservationCountVariable(ncFile, band, tileSize);
                    } else if ("algorithmic_confidence_level".equals(band.getName())) {
                        addAlgorithmicConfidenceLevelVariable(ncFile, band, tileSize);
                    } else if ("overall_confidence_level".equals(band.getName())) {
                        addOverallConfidenceLevelVariable(ncFile, band, tileSize);
                    }
                }
            }

            private void addLccsClassVariable(NFileWriteable ncFile, Band band, Dimension tileSize, String ancillaryVariables) throws IOException {
                final DataType ncDataType = DataTypeUtils.getNetcdfDataType(band.getDataType());
                final String variableName = ReaderUtils.getVariableName(band);
                final NVariable variable = ncFile.addVariable(variableName, ncDataType, false, tileSize, ncFile.getDimensions());
                final ArrayByte.D1 valids = new ArrayByte.D1(LCCS_CLASS_FLAG_VALUES.length);
                for (int i = 0; i < LCCS_CLASS_FLAG_VALUES.length; ++i) {
                    valids.set(i, LCCS_CLASS_FLAG_VALUES[i]);
                }
                variable.addAttribute("long_name", band.getDescription());
                variable.addAttribute("standard_name", "land_cover_lccs");
                variable.addAttribute("flag_values", valids);
                variable.addAttribute("flag_meanings", LCCS_CLASS_FLAG_MEANINGS);
                variable.addAttribute("valid_min", 1);
                variable.addAttribute("valid_max", 240);
                variable.addAttribute("_Unsigned", "true");
                variable.addAttribute(Constants.FILL_VALUE_ATT_NAME, (byte) 0);
                if (ancillaryVariables.length() > 0) {
                    variable.addAttribute("ancillary_variables", ancillaryVariables);
                }
            }

            private void addProcessedFlagVariable(NFileWriteable ncFile, Band band, Dimension tileSize) throws IOException {
                final DataType ncDataType = DataTypeUtils.getNetcdfDataType(band.getDataType());
                final String variableName = ReaderUtils.getVariableName(band);
                final NVariable variable = ncFile.addVariable(variableName, ncDataType, tileSize, ncFile.getDimensions());
                final byte[] FLAG_VALUES = new byte[]{0, 1};
                final String FLAG_MEANINGS = "not_processed processed";
                final ArrayByte.D1 valids = new ArrayByte.D1(FLAG_VALUES.length);
                for (int i = 0; i < FLAG_VALUES.length; ++i) {
                    valids.set(i, FLAG_VALUES[i]);
                }
                variable.addAttribute("long_name", band.getDescription());
                variable.addAttribute("standard_name", "land_cover_lccs status_flag");
                variable.addAttribute("flag_values", valids);
                variable.addAttribute("flag_meanings", FLAG_MEANINGS);
                variable.addAttribute("valid_min", 0);
                variable.addAttribute("valid_max", 1);
                if (ncDataType == DataType.SHORT) {
                    variable.addAttribute(Constants.FILL_VALUE_ATT_NAME, (short) -1);
                } else {
                    variable.addAttribute(Constants.FILL_VALUE_ATT_NAME, (byte) -1);
                }
            }

            private void addCurrentPixelStateVariable(NFileWriteable ncFile, Band band, Dimension tileSize) throws IOException {
                final DataType ncDataType = DataTypeUtils.getNetcdfDataType(band.getDataType());
                final String variableName = ReaderUtils.getVariableName(band);
                final NVariable variable = ncFile.addVariable(variableName, ncDataType, tileSize, ncFile.getDimensions());
                final byte[] FLAG_VALUES = new byte[]{0, 1, 2, 3, 4, 5};
                final String FLAG_MEANINGS = "invalid clear_land clear_water clear_snow_ice cloud cloud_shadow";
                final ArrayByte.D1 valids = new ArrayByte.D1(FLAG_VALUES.length);
                for (int i = 0; i < FLAG_VALUES.length; ++i) {
                    valids.set(i, FLAG_VALUES[i]);
                }
                variable.addAttribute("long_name", band.getDescription());
                variable.addAttribute("standard_name", "land_cover_lccs status_flag");
                variable.addAttribute("flag_values", valids);
                variable.addAttribute("flag_meanings", FLAG_MEANINGS);
                variable.addAttribute("valid_min", 0);
                variable.addAttribute("valid_max", 5);
                if (ncDataType == DataType.SHORT) {
                    variable.addAttribute(Constants.FILL_VALUE_ATT_NAME, (short) -1);
                } else {
                    variable.addAttribute(Constants.FILL_VALUE_ATT_NAME, (byte) -1);
                }
            }

            private void addObservationCountVariable(NFileWriteable ncFile, Band band, Dimension tileSize) throws IOException {
                final DataType ncDataType = DataTypeUtils.getNetcdfDataType(band.getDataType());
                final String variableName = ReaderUtils.getVariableName(band);
                final NVariable variable = ncFile.addVariable(variableName, ncDataType, tileSize, ncFile.getDimensions());
                variable.addAttribute("long_name", band.getDescription());
                variable.addAttribute("standard_name", "land_cover_lccs number_of_observations");
                variable.addAttribute("valid_min", 0);
                variable.addAttribute("valid_max", 32767);
                if (ncDataType == DataType.SHORT) {
                    variable.addAttribute(Constants.FILL_VALUE_ATT_NAME, (short) -1);
                } else {
                    variable.addAttribute(Constants.FILL_VALUE_ATT_NAME, (byte) -1);
                }
            }

            private void addAlgorithmicConfidenceLevelVariable(NFileWriteable ncFile, Band band, Dimension tileSize) throws IOException {
                final DataType ncDataType = DataTypeUtils.getNetcdfDataType(band.getDataType());
                final String variableName = ReaderUtils.getVariableName(band);
                final NVariable variable = ncFile.addVariable(variableName, ncDataType, tileSize, ncFile.getDimensions());
                variable.addAttribute("long_name", band.getDescription());
                variable.addAttribute("standard_name", "land_cover_lccs status_flag");
                variable.addAttribute("valid_min", 0);
                variable.addAttribute("valid_max", 100);
                variable.addAttribute("scale_factor", 0.01f);
                if (ncDataType == DataType.SHORT) {
                    variable.addAttribute(Constants.FILL_VALUE_ATT_NAME, (short) -1);
                } else {
                    variable.addAttribute(Constants.FILL_VALUE_ATT_NAME, (byte) -1);
                }
            }

            private void addOverallConfidenceLevelVariable(NFileWriteable ncFile, Band band, Dimension tileSize) throws IOException {
                final DataType ncDataType = DataTypeUtils.getNetcdfDataType(band.getDataType());
                final String variableName = ReaderUtils.getVariableName(band);
                final NVariable variable = ncFile.addVariable(variableName, ncDataType, tileSize, ncFile.getDimensions());
                final byte[] FLAG_VALUES = new byte[]{0, 1, 2};
                final String FLAG_MEANINGS = "doubtful reasonable certain";
                final ArrayByte.D1 valids = new ArrayByte.D1(FLAG_VALUES.length);
                for (int i = 0; i < FLAG_VALUES.length; ++i) {
                    valids.set(i, FLAG_VALUES[i]);
                }
                variable.addAttribute("long_name", band.getDescription());
                variable.addAttribute("standard_name", "land_cover_lccs status_flag");
                variable.addAttribute("flag_values", valids);
                variable.addAttribute("flag_meanings", FLAG_MEANINGS);
                variable.addAttribute("valid_min", 0);
                variable.addAttribute("valid_max", 1);
                if (ncDataType == DataType.SHORT) {
                    variable.addAttribute(Constants.FILL_VALUE_ATT_NAME, (short) -1);
                } else {
                    variable.addAttribute(Constants.FILL_VALUE_ATT_NAME, (byte) -1);
                }
            }
        };
    }

}
