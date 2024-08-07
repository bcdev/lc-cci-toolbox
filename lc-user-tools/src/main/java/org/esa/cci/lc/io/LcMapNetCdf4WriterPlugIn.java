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
import org.esa.snap.core.util.StringUtils;
import org.esa.cci.lc.aggregation.LCCS;
import org.esa.cci.lc.util.LcHelper;
import ucar.ma2.Array;
import ucar.ma2.ArrayByte;
import ucar.ma2.DataType;

import java.awt.Dimension;
import java.io.IOException;

/**
 * @author Martin Boettcher
 */
public class LcMapNetCdf4WriterPlugIn extends BeamNetCdf4WriterPlugIn {

    public static final String FORMAT_NAME = "NetCDF4-LC-Map";

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

            final NFileWriteable writeable = ctx.getNetcdfFileWriteable();

            final LcMapMetadata lcMapMetadata = new LcMapMetadata(product);
            final String spatialResolution = lcMapMetadata.getSpatialResolution();
            final String temporalResolution = lcMapMetadata.getTemporalResolution();
            final String epoch = lcMapMetadata.getEpoch();
            final String version = lcMapMetadata.getVersion();


            final GeoCoding geoCoding = product.getSceneGeoCoding();
            final GeoPos upperLeft = geoCoding.getGeoPos(new PixelPos(0, 0), null);
            final GeoPos lowerRight = geoCoding.getGeoPos(new PixelPos(product.getSceneRasterWidth(), product.getSceneRasterHeight()), null);
            final String latMax = String.valueOf(upperLeft.getLat());
            final String latMin = String.valueOf(lowerRight.getLat());
            final String lonMin = String.valueOf(upperLeft.getLon());
            final String lonMax = String.valueOf(lowerRight.getLon());

            final String spatialResolutionDegrees = "300m".equals(spatialResolution) ? "0.002778" : "0.011112";
            int epochInt = Integer.parseInt(epoch);
            int temporalResolutionInt = Integer.parseInt(temporalResolution);
            final String startYear = String.valueOf(epochInt - temporalResolutionInt / 2);
            final String startTime = startYear + "0101";
            final String endYear = String.valueOf(epochInt + temporalResolutionInt / 2);
            final String endTime = endYear + "1231";
            final String temporalCoverageYears = getTemporalCoverage(startYear, endYear);

            // global attributes
            writeable.addGlobalAttribute("title", "ESA CCI Land Cover Map");
            writeable.addGlobalAttribute("summary",
                                         "This dataset contains the global ESA CCI land cover classification map derived from satellite data of one epoch.");
            writeable.addGlobalAttribute("type", lcMapMetadata.getType());
            writeable.addGlobalAttribute("id", lcMapMetadata.getId());
            final Dimension tileSize = product.getPreferredTileSize();
            LcWriterUtils.addGenericGlobalAttributes(writeable, LcHelper.format(tileSize));

            LcWriterUtils.addSpecificGlobalAttributes("MERIS FR L1B version 5.05, MERIS RR L1B version 8.0, SPOT VGT P",
                                                      "amorgos-4,0, lc-sdr-1.0, lc-sr-1.0, lc-classification-1.0",
                                                      spatialResolutionDegrees, spatialResolution,
                                                      temporalCoverageYears, temporalResolution, "Y",
                                                      startTime, endTime,
                                                      version, latMax, latMin, lonMin, lonMax, writeable, "University catholique de Louvain");

            final String pftTable = lcMapMetadata.getPftTable();
            if (pftTable != null) {
                writeable.addGlobalAttribute("pft_table", pftTable);
            }
            final String pftTableComment = lcMapMetadata.getPftTableComment();
            if (pftTableComment != null) {
                writeable.addGlobalAttribute("pft_table_comment", pftTableComment);
            }


            writeable.addDimension("lat", product.getSceneRasterHeight());
            writeable.addDimension("lon", product.getSceneRasterWidth());
        }

        private String getTemporalCoverage(String startYear, String endYear) {
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
            private static final String LCCS_CLASS_BAND_NAME = "lccs_class";
            private static final String PROCESSED_FLAG_BAND_NAME = "processed_flag";
            private static final String CURRENT_PIXEL_STATE_BAND_NAME = "current_pixel_state";
            private static final String OBSERVATION_COUNT_BAND_NAME = "observation_count";
            private static final String ALGORITHMIC_CONFIDENCE_LEVEL_BAND_NAME = "algorithmic_confidence_level";
            private static final String CHANGE_COUNT_BAND_NAME = "change_count";
            private static final String OVERALL_CONFIDENCE_LEVEL_BAND_NAME = "overall_confidence_level";
            private static final String LABEL_CONFIDENCE_LEVEL_BAND_NAME = "label_confidence_level";
            private static final String LABEL_SOURCE_BAND_NAME = "label_source";

            @Override
            public void preEncode(ProfileWriteContext ctx, Product p) throws IOException {

                final NFileWriteable ncFile = ctx.getNetcdfFileWriteable();
                String ancillaryVariableString = getAncillaryVariableString(p);
                final Dimension tileSize = p.getPreferredTileSize();
                for (Band band : p.getBands()) {
                    if (LCCS_CLASS_BAND_NAME.equals(band.getName())) {
                        addLccsClassVariable(ncFile, band, tileSize, ancillaryVariableString);
                    } else if (PROCESSED_FLAG_BAND_NAME.equals(band.getName())) {
                        addProcessedFlagVariable(ncFile, band, tileSize);
                    } else if (CURRENT_PIXEL_STATE_BAND_NAME.equals(band.getName())) {
                        addCurrentPixelStateVariable(ncFile, band, tileSize);
                    } else if (OBSERVATION_COUNT_BAND_NAME.equals(band.getName())) {
                        addObservationCountVariable(ncFile, band, tileSize);
                    } else if (ALGORITHMIC_CONFIDENCE_LEVEL_BAND_NAME.equals(band.getName())) {
                        addAlgorithmicConfidenceLevelVariable(ncFile, band, tileSize);
                    } else if (CHANGE_COUNT_BAND_NAME.equals(band.getName())) {
                        addChangeCountVariable(ncFile, band, tileSize);
                    } else if (OVERALL_CONFIDENCE_LEVEL_BAND_NAME.equals(band.getName())) {
                        addOverallConfidenceLevelVariable(ncFile, band, tileSize);
                    } else if (LABEL_CONFIDENCE_LEVEL_BAND_NAME.equals(band.getName())) {
                        addLabelConfidenceLevelVariable(ncFile, band, tileSize);
                    } else if (LABEL_SOURCE_BAND_NAME.equals(band.getName())) {
                        addLabelSourceVariable(ncFile, band, tileSize);
                    } else {
                        // this branch is passed if an aggregated product is subsetted
                        addGeneralVariable(ncFile, band, tileSize);
                    }
                }
            }

            private String getAncillaryVariableString(Product p) {
                StringBuilder ancillaryVariables = new StringBuilder();
                for (Band band : p.getBands()) {
                    String bandName = band.getName();
                    if (isAncillaryVariable(bandName)) {
                        if (ancillaryVariables.length() > 0) {
                            ancillaryVariables.append(' ');
                        }
                        ancillaryVariables.append(bandName);
                    }
                }
                return ancillaryVariables.toString();
            }

            private boolean isAncillaryVariable(String bandName) {
                return PROCESSED_FLAG_BAND_NAME.equals(bandName) ||
                       CURRENT_PIXEL_STATE_BAND_NAME.equals(bandName) ||
                       OBSERVATION_COUNT_BAND_NAME.equals(bandName) ||
                       ALGORITHMIC_CONFIDENCE_LEVEL_BAND_NAME.equals(bandName) ||
                       CHANGE_COUNT_BAND_NAME.equals(bandName) ||
                       OVERALL_CONFIDENCE_LEVEL_BAND_NAME.equals(bandName) ||
                       LABEL_CONFIDENCE_LEVEL_BAND_NAME.equals(bandName) ||
                       LABEL_SOURCE_BAND_NAME.equals(bandName);
            }

            private void addLccsClassVariable(NFileWriteable ncFile, Band band, Dimension tileSize, String ancillaryVariables) throws IOException {
                final DataType ncDataType = DataTypeUtils.getNetcdfDataType(band.getDataType());
                final String variableName = ReaderUtils.getVariableName(band);
                //nccopy does not support reading ubyte variables, therefore preliminarily commented out
                final NVariable variable = ncFile.addVariable(variableName, ncDataType, tileSize, ncFile.getDimensions());
                int[] lccsClassFlagValues = LCCS.getInstance().getClassValues();
                final ArrayByte.D1 valids = new ArrayByte.D1(lccsClassFlagValues.length,variable.getDataType().isUnsigned());
                for (int i = 0; i < lccsClassFlagValues.length; ++i) {
                    valids.set(i, (byte) lccsClassFlagValues[i]);
                }
                variable.addAttribute("long_name", band.getDescription());
                variable.addAttribute("standard_name", "land_cover_lccs");
                variable.addAttribute("flag_values", valids);
                variable.addAttribute("flag_meanings", StringUtils.arrayToString(LCCS.getInstance().getFlagMeanings(), " "));
                variable.addAttribute("valid_min", 1);
                variable.addAttribute("valid_max", 220);
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
                final byte[] flagValues = new byte[]{0, 1};
                final String flagMeanings = "not_processed processed";
                final Array valids = Array.factory(DataType.BYTE,new int[]{2},flagValues);
                variable.addAttribute("long_name", band.getDescription());
                variable.addAttribute("standard_name", "land_cover_lccs status_flag");
                variable.addAttribute("flag_values", valids);
                variable.addAttribute("flag_meanings", flagMeanings);
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
                final byte[] flagValues = new byte[]{0, 1, 2, 3, 4, 5};
                final String flagMeanings = "invalid clear_land clear_water clear_snow_ice cloud cloud_shadow";
                final Array valids = Array.factory(DataType.BYTE,new int[]{6},flagValues);
                variable.addAttribute("long_name", band.getDescription());
                variable.addAttribute("standard_name", "land_cover_lccs status_flag");
                variable.addAttribute("flag_values", valids);
                variable.addAttribute("flag_meanings", flagMeanings);
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
                variable.addAttribute("standard_name", "land_cover_lccs algorithmic_confidence");
                variable.addAttribute("valid_min", 0);
                variable.addAttribute("valid_max", 100);
                variable.addAttribute("scale_factor", 0.01f);
                if (ncDataType == DataType.SHORT) {
                    variable.addAttribute(Constants.FILL_VALUE_ATT_NAME, (short) -1);
                } else {
                    variable.addAttribute(Constants.FILL_VALUE_ATT_NAME, (byte) -1);
                }
            }

            private void addChangeCountVariable(NFileWriteable ncFile, Band band, Dimension tileSize) throws IOException {
                final DataType ncDataType = DataTypeUtils.getNetcdfDataType(band.getDataType());
                final String variableName = ReaderUtils.getVariableName(band);
                final NVariable variable = ncFile.addVariable(variableName, ncDataType, tileSize, ncFile.getDimensions());
                variable.addAttribute("long_name", band.getDescription());
                variable.addAttribute("valid_min", 0);
                variable.addAttribute("valid_max", 100);
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
                final byte[] flagValues = new byte[]{0, 1, 2};
                final String flagMeanings = "doubtful reasonable certain";
                final Array valids = Array.factory(DataType.BYTE, new int[]{3},flagValues);
                variable.addAttribute("long_name", band.getDescription());
                variable.addAttribute("standard_name", "land_cover_lccs overall_confidence");
                variable.addAttribute("flag_values", valids);
                variable.addAttribute("flag_meanings", flagMeanings);
                variable.addAttribute("valid_min", 0);
                variable.addAttribute("valid_max", 1);
                if (ncDataType == DataType.SHORT) {
                    variable.addAttribute(Constants.FILL_VALUE_ATT_NAME, (short) -1);
                } else {
                    variable.addAttribute(Constants.FILL_VALUE_ATT_NAME, (byte) -1);
                }
            }

            private void addLabelConfidenceLevelVariable(NFileWriteable ncFile, Band band, Dimension tileSize) throws IOException {
                final DataType ncDataType = DataTypeUtils.getNetcdfDataType(band.getDataType());
                final String variableName = ReaderUtils.getVariableName(band);
                final NVariable variable = ncFile.addVariable(variableName, ncDataType, tileSize, ncFile.getDimensions());
                variable.addAttribute("long_name", band.getDescription());
                variable.addAttribute("standard_name", "land_cover_lccs confidence");
                variable.addAttribute("valid_min", 0);
                variable.addAttribute("valid_max", 100);
                variable.addAttribute("scale_factor", 0.01f);
                if (ncDataType == DataType.SHORT) {
                    variable.addAttribute(Constants.FILL_VALUE_ATT_NAME, (short) -1);
                } else {
                    variable.addAttribute(Constants.FILL_VALUE_ATT_NAME, (byte) -1);
                }
            }

            private void addLabelSourceVariable(NFileWriteable ncFile, Band band, Dimension tileSize) throws IOException {
                final DataType ncDataType = DataTypeUtils.getNetcdfDataType(band.getDataType());
                final String variableName = ReaderUtils.getVariableName(band);
                final NVariable variable = ncFile.addVariable(variableName, ncDataType, tileSize, ncFile.getDimensions());
                final byte[] flagValues = new byte[]{0, 1, 2, 3};
                final String flagMeanings = "invalid original_label first_alternative_label second_alternative_label";
                final Array valids = Array.factory(DataType.BYTE,new int[]{4},flagValues);
                variable.addAttribute("long_name", band.getDescription());
                variable.addAttribute("standard_name", "land_cover_lccs status_flag");
                variable.addAttribute("flag_values", valids);
                variable.addAttribute("flag_meanings", flagMeanings);
                variable.addAttribute("valid_min", 1);
                variable.addAttribute("valid_max", 3);
                if (ncDataType == DataType.SHORT) {
                    variable.addAttribute(Constants.FILL_VALUE_ATT_NAME, (short) 0);
                } else {
                    variable.addAttribute(Constants.FILL_VALUE_ATT_NAME, (byte) 0);
                }
            }

            private void addGeneralVariable(NFileWriteable ncFile, Band band, Dimension tileSize) throws IOException {
                final DataType ncDataType = DataTypeUtils.getNetcdfDataType(band.getDataType());
                final String variableName = ReaderUtils.getVariableName(band);
                final NVariable variable = ncFile.addVariable(variableName, ncDataType, tileSize, ncFile.getDimensions());
                variable.addAttribute("long_name", band.getDescription());
                variable.addAttribute("standard_name", band.getName());
                if (band.getScalingOffset() != 0.0) {
                    variable.addAttribute("add_offset", band.getScalingOffset());
                }
                if (band.getScalingFactor() != 1.0) {
                    variable.addAttribute("scale_factor", band.getScalingFactor());
                }
                variable.addAttribute(Constants.FILL_VALUE_ATT_NAME, Float.NaN);
            }

        };
    }

}
