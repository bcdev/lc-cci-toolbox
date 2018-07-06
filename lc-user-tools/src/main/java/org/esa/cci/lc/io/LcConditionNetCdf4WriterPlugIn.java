package org.esa.cci.lc.io;

import org.esa.snap.dataio.netcdf.DefaultNetCdfWriter;
import org.esa.snap.dataio.netcdf.NullProfilePartWriter;
import org.esa.snap.dataio.netcdf.ProfileWriteContext;
import org.esa.snap.dataio.netcdf.metadata.ProfileInitPartWriter;
import org.esa.snap.dataio.netcdf.metadata.ProfilePartWriter;
import org.esa.snap.dataio.netcdf.metadata.profiles.beam.BeamBandPart;
import org.esa.snap.dataio.netcdf.metadata.profiles.beam.BeamInitialisationPart;
import org.esa.snap.dataio.netcdf.metadata.profiles.beam.BeamNetCdf4WriterPlugIn;
import org.esa.snap.dataio.netcdf.nc.NFileWriteable;
import org.esa.snap.dataio.netcdf.nc.NVariable;
import org.esa.snap.dataio.netcdf.util.Constants;
import org.esa.snap.dataio.netcdf.util.DataTypeUtils;
import org.esa.snap.dataio.netcdf.util.ReaderUtils;
import org.esa.snap.core.dataio.ProductWriter;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Product;
import ucar.ma2.Array;
import ucar.ma2.DataType;

import java.awt.Dimension;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

/**
 * @author Martin Boettcher
 */
public class LcConditionNetCdf4WriterPlugIn extends BeamNetCdf4WriterPlugIn {

    public static final String FORMAT_NAME = "NetCDF4-LC-Condition";

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
    public ProfilePartWriter createDescriptionPartWriter() {
        return new NullProfilePartWriter();
    }

    @Override
    public ProfileInitPartWriter createInitialisationPartWriter() {
        return new LcCondInitialisationPart();
    }

    class LcCondInitialisationPart extends BeamInitialisationPart {

        private final SimpleDateFormat COMPACT_ISO_FORMAT = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");

        LcCondInitialisationPart() {
            COMPACT_ISO_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
        }

        @Override
        public void writeProductBody(ProfileWriteContext ctx, Product product) throws IOException {

            final NFileWriteable writeable = ctx.getNetcdfFileWriteable();
            final LcCondMetadata metadata = new LcCondMetadata(product);

            final String condition = metadata.getCondition();
            final String startYear = metadata.getStartYear();
            final String endYear = metadata.getEndYear();
            final String startDate = metadata.getStartDate();
            final String spatialResolution = metadata.getSpatialResolution();
            final String temporalResolution = metadata.getTemporalResolution();
            final String version = metadata.getVersion();

            final String spatialResolutionDegrees = "500m".equals(spatialResolution) ? "0.005556" : "0.011112";
            final String temporalCoverageYears = getTemporalCoverageYears(startYear, endYear);

            final String startTime = startDate;
            final String endTime = endYear + startDate.substring(4);
            final GeoCoding geoCoding = product.getSceneGeoCoding();
            final GeoPos upperLeft = geoCoding.getGeoPos(new PixelPos(0.0f, 0.0f), null);
            final GeoPos lowerRight = geoCoding.getGeoPos(new PixelPos(product.getSceneRasterWidth(), product.getSceneRasterHeight()), null);
            final String latMax = String.valueOf(upperLeft.getLat());
            final String latMin = String.valueOf(lowerRight.getLat());
            final String lonMin = String.valueOf(upperLeft.getLon());
            final String lonMax = String.valueOf(lowerRight.getLon());

            // global attributes
            writeable.addGlobalAttribute("title", "ESA CCI Land Cover Condition " + condition);
            writeable.addGlobalAttribute("summary",
                                         "This dataset contains the global ESA CCI land cover condition derived from satellite data of a range of years.");
            writeable.addGlobalAttribute("type", metadata.getType());
            writeable.addGlobalAttribute("id", metadata.getId());

            final Dimension tileSize = product.getPreferredTileSize();
            LcWriterUtils.addGenericGlobalAttributes(writeable, String.format("%d:%d", tileSize.width, tileSize.height));
            LcWriterUtils.addSpecificGlobalAttributes("SPOT VGT, MODIS, ...",
                                                      "lc-condition-1.0",
                                                      spatialResolutionDegrees, spatialResolution,
                                                      temporalCoverageYears, temporalResolution, "D",
                                                      startTime, endTime,
                                                      version, latMax, latMin, lonMin, lonMax, writeable, "University catholique de Louvain");

            writeable.addDimension("lat", product.getSceneRasterHeight());
            writeable.addDimension("lon", product.getSceneRasterWidth());
        }

        private String getTemporalCoverageYears(String startYear, String endYear) {
            final String temporalCoverageYears;
            try {
                temporalCoverageYears = String.valueOf(Integer.parseInt(endYear) - Integer.parseInt(startYear) + 1);
            } catch (NumberFormatException ex) {
                throw new RuntimeException("cannot parse " + startYear + " and " + endYear + " as year numbers", ex);
            }
            return temporalCoverageYears;
        }

    }

    @Override
    public ProfilePartWriter createBandPartWriter() {
        return new BeamBandPart() {
            private static final String NDVI_MEAN_BAND_NAME = "ndvi_mean";
            private static final String NDVI_STD_BAND_NAME = "ndvi_std";
            private static final String NDVI_STATUS_BAND_NAME = "ndvi_status";
            private static final String NDVI_N_YEAR_OBS_BAND_NAME = "ndvi_nYearObs";
            private static final String BA_OCC_BAND_NAME = "ba_occ";
            private static final String BA_N_YEAR_OBS_BAND_NAME = "ba_nYearObs";
            private static final String SNOW_OCC_BAND_NAME = "snow_occ";
            private static final String SNOW_N_YEAR_OBS_BAND_NAME = "snow_nYearObs";

            @Override
            public void preEncode(ProfileWriteContext ctx, Product p) throws IOException {

                final NFileWriteable ncFile = ctx.getNetcdfFileWriteable();
                StringBuilder ancillaryVariables = new StringBuilder();
                for (Band band : p.getBands()) {
                    if (isAncillaryBand(band)) {
                        if (ancillaryVariables.length() > 0) {
                            ancillaryVariables.append(' ');
                        }
                        ancillaryVariables.append(band.getName());
                    }
                }
                final Dimension tileSize = p.getPreferredTileSize();
                for (Band band : p.getBands()) {
                    if (NDVI_MEAN_BAND_NAME.equals(band.getName())) {
                        addNdviMeanVariable(ncFile, band, tileSize, ancillaryVariables.toString());
                    } else if (NDVI_STD_BAND_NAME.equals(band.getName())) {
                        addNdviStdVariable(ncFile, band, tileSize);
                    } else if (NDVI_STATUS_BAND_NAME.equals(band.getName())) {
                        addNdviStatusVariable(ncFile, band, tileSize);
                    } else if (NDVI_N_YEAR_OBS_BAND_NAME.equals(band.getName())) {
                        addNdviNYearObsVariable(ncFile, band, tileSize);
                    } else if (BA_OCC_BAND_NAME.equals(band.getName())) {
                        addBaOccVariable(ncFile, band, tileSize, ancillaryVariables.toString());
                    } else if (BA_N_YEAR_OBS_BAND_NAME.equals(band.getName())) {
                        addBaNYearObsVariable(ncFile, band, tileSize);
                    } else if (SNOW_OCC_BAND_NAME.equals(band.getName())) {
                        addSnowOccVariable(ncFile, band, tileSize, ancillaryVariables.toString());
                    } else if (SNOW_N_YEAR_OBS_BAND_NAME.equals(band.getName())) {
                        addSnowNYearObsVariable(ncFile, band, tileSize);
                    } else {
                        // this branch is passed if an aggregated product is subsetted
                        addGeneralAggregatedVariable(ncFile, band, tileSize);

                    }
                }
            }

            private boolean isAncillaryBand(Band band) {
                String name = band.getName();
                return NDVI_STD_BAND_NAME.equals(name) ||
                       NDVI_STATUS_BAND_NAME.equals(name) ||
                       NDVI_N_YEAR_OBS_BAND_NAME.equals(name) ||
                       BA_N_YEAR_OBS_BAND_NAME.equals(name) ||
                       SNOW_N_YEAR_OBS_BAND_NAME.equals(name);
            }

            private void addNdviMeanVariable(NFileWriteable ncFile, Band band, Dimension tileSize, String ancillaryVariables) throws IOException {
                final DataType ncDataType = DataTypeUtils.getNetcdfDataType(band.getDataType());
                final String variableName = ReaderUtils.getVariableName(band);
                final NVariable variable = ncFile.addVariable(variableName, ncDataType, false, tileSize, ncFile.getDimensions());
                variable.addAttribute("long_name", band.getDescription());
                variable.addAttribute("standard_name", "normalized_difference_vegetation_index");
                variable.addAttribute("valid_min", -10000);
                variable.addAttribute("valid_max", 10000);
                variable.addAttribute("scale_factor", 0.0001f);
                if (ncDataType == DataType.SHORT) {
                    variable.addAttribute(Constants.FILL_VALUE_ATT_NAME, (short) 32767);
                } else {
                    variable.addAttribute(Constants.FILL_VALUE_ATT_NAME, (byte) 255);
                }
                if (ancillaryVariables.length() > 0) {
                    variable.addAttribute("ancillary_variables", ancillaryVariables);
                }
            }

            private void addNdviStdVariable(NFileWriteable ncFile, Band band, Dimension tileSize) throws IOException {
                final DataType ncDataType = DataTypeUtils.getNetcdfDataType(band.getDataType());
                final String variableName = ReaderUtils.getVariableName(band);
                final NVariable variable = ncFile.addVariable(variableName, ncDataType, false, tileSize, ncFile.getDimensions());
                variable.addAttribute("long_name", band.getDescription());
                variable.addAttribute("standard_name", "normalized_difference_vegetation_index standard_error");
                variable.addAttribute("valid_min", 0);
                variable.addAttribute("valid_max", 10000);
                variable.addAttribute("scale_factor", 0.0001f);
                if (ncDataType == DataType.BYTE) {
                    variable.addAttribute(Constants.FILL_VALUE_ATT_NAME, (byte) 32767);
                } else {
                    variable.addAttribute(Constants.FILL_VALUE_ATT_NAME, (short) -1);
                }
            }

            private void addNdviNYearObsVariable(NFileWriteable ncFile, Band band, Dimension tileSize) throws IOException {
                final DataType ncDataType = DataTypeUtils.getNetcdfDataType(band.getDataType());
                final String variableName = ReaderUtils.getVariableName(band);
                final NVariable variable = ncFile.addVariable(variableName, ncDataType, false, tileSize, ncFile.getDimensions());
                variable.addAttribute("long_name", band.getDescription());
                variable.addAttribute("standard_name", "normalized_difference_vegetation_index number_of_observations");
                variable.addAttribute("valid_min", 0);
                variable.addAttribute("valid_max", 30);
                if (ncDataType == DataType.BYTE) {
                    variable.addAttribute(Constants.FILL_VALUE_ATT_NAME, (byte) -1);
                } else {
                    variable.addAttribute(Constants.FILL_VALUE_ATT_NAME, (short) -1);
                }
            }

            private void addNdviStatusVariable(NFileWriteable ncFile, Band band, Dimension tileSize) throws IOException {
                final DataType ncDataType = DataTypeUtils.getNetcdfDataType(band.getDataType());
                final String variableName = ReaderUtils.getVariableName(band);
                final NVariable variable = ncFile.addVariable(variableName, ncDataType, false, tileSize, ncFile.getDimensions());
                final byte[] conditionFlagValues = new byte[]{0, 1, 2, 3, 4, 5};
                final String conditionFlagMeanings = "invalid land water snow cloud filled_ice";
                final Array valids = Array.factory(DataType.BYTE ,new int[]{6},conditionFlagValues);
                variable.addAttribute("long_name", band.getDescription());
                variable.addAttribute("standard_name", "normalized_difference_vegetation_index status_flag");
                variable.addAttribute("flag_values", valids);
                variable.addAttribute("flag_meanings", conditionFlagMeanings);
                variable.addAttribute("valid_min", 1);
                variable.addAttribute("valid_max", 5);
                if (ncDataType == DataType.BYTE) {
                    variable.addAttribute(Constants.FILL_VALUE_ATT_NAME, (byte) -1);
                } else {
                    variable.addAttribute(Constants.FILL_VALUE_ATT_NAME, (short) -1);
                }
            }

            private void addBaOccVariable(NFileWriteable ncFile, Band band, Dimension tileSize, String ancillaryVariables) throws IOException {
                final DataType ncDataType = DataTypeUtils.getNetcdfDataType(band.getDataType());
                final String variableName = ReaderUtils.getVariableName(band);
                final NVariable variable = ncFile.addVariable(variableName, ncDataType, false, tileSize, ncFile.getDimensions());
                variable.addAttribute("long_name", band.getDescription());
                variable.addAttribute("valid_min", 0);
                variable.addAttribute("valid_max", 100);
                if (ncDataType == DataType.SHORT) {
                    variable.addAttribute(Constants.FILL_VALUE_ATT_NAME, (short) -2);
                } else {
                    variable.addAttribute(Constants.FILL_VALUE_ATT_NAME, (byte) -2);
                }
                if (ancillaryVariables.length() > 0) {
                    variable.addAttribute("ancillary_variables", ancillaryVariables);
                }
            }

            private void addBaNYearObsVariable(NFileWriteable ncFile, Band band, Dimension tileSize) throws IOException {
                final DataType ncDataType = DataTypeUtils.getNetcdfDataType(band.getDataType());
                final String variableName = ReaderUtils.getVariableName(band);
                final NVariable variable = ncFile.addVariable(variableName, ncDataType, false, tileSize, ncFile.getDimensions());
                variable.addAttribute("long_name", band.getDescription());
                variable.addAttribute("valid_min", 0);
                variable.addAttribute("valid_max", 30);
                if (ncDataType == DataType.BYTE) {
                    variable.addAttribute(Constants.FILL_VALUE_ATT_NAME, (byte) -1);
                } else {
                    variable.addAttribute(Constants.FILL_VALUE_ATT_NAME, (short) -1);
                }
            }

            private void addSnowOccVariable(NFileWriteable ncFile, Band band, Dimension tileSize, String ancillaryVariables) throws IOException {
                final DataType ncDataType = DataTypeUtils.getNetcdfDataType(band.getDataType());
                final String variableName = ReaderUtils.getVariableName(band);
                final NVariable variable = ncFile.addVariable(variableName, ncDataType, false, tileSize, ncFile.getDimensions());
                variable.addAttribute("long_name", band.getDescription());
                variable.addAttribute("valid_min", 0);
                variable.addAttribute("valid_max", 100);
                if (ncDataType == DataType.SHORT) {
                    variable.addAttribute(Constants.FILL_VALUE_ATT_NAME, (short) -1);
                    variable.addAttribute("_WaterValue", (byte) -2);
                } else {
                    variable.addAttribute(Constants.FILL_VALUE_ATT_NAME, (byte) -1);
                    variable.addAttribute("_WaterValue", (short) -2);
                }
                if (ancillaryVariables.length() > 0) {
                    variable.addAttribute("ancillary_variables", ancillaryVariables);
                }
            }

            private void addSnowNYearObsVariable(NFileWriteable ncFile, Band band, Dimension tileSize) throws IOException {
                final DataType ncDataType = DataTypeUtils.getNetcdfDataType(band.getDataType());
                final String variableName = ReaderUtils.getVariableName(band);
                final NVariable variable = ncFile.addVariable(variableName, ncDataType, false, tileSize, ncFile.getDimensions());
                variable.addAttribute("long_name", band.getDescription());
                variable.addAttribute("valid_min", 0);
                variable.addAttribute("valid_max", 30);
                if (ncDataType == DataType.BYTE) {
                    variable.addAttribute(Constants.FILL_VALUE_ATT_NAME, (byte) -1);
                } else {
                    variable.addAttribute(Constants.FILL_VALUE_ATT_NAME, (short) -1);
                }
            }

            private void addGeneralAggregatedVariable(NFileWriteable ncFile, Band band, Dimension tileSize) throws IOException {
                final DataType ncDataType = DataTypeUtils.getNetcdfDataType(band.getDataType());
                final String variableName = ReaderUtils.getVariableName(band);
                final NVariable variable = ncFile.addVariable(variableName, ncDataType, tileSize, ncFile.getDimensions());
                variable.addAttribute("long_name", band.getDescription());
                variable.addAttribute("standard_name", band.getName());
                if (!band.getName().endsWith("nYearObs_sum")) {
                    variable.addAttribute(Constants.FILL_VALUE_ATT_NAME, Float.NaN);
                }
            }
        };

    }

}
