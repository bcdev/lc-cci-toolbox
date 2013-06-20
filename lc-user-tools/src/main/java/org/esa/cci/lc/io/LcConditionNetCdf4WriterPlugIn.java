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
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.jai.ImageManager;
import ucar.ma2.ArrayByte;
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

            final LcCondMetadata metadata = new LcCondMetadata(product);
            final String condition = metadata.getCondition();
            final String startYear = metadata.getStartYear();
            final String endYear = metadata.getEndYear();
            final String startDate = metadata.getStartDate();
            final String spatialResolution = metadata.getSpatialResolution();
            final String temporalResolution = metadata.getTemporalResolution();
            final String version = metadata.getVersion();

            final String startTime = startYear + startDate;
            final String endTime = endYear + startDate;
            final GeoCoding geoCoding = product.getGeoCoding();
            final GeoPos upperLeft = geoCoding.getGeoPos(new PixelPos(0.0f, 0.0f), null);
            final GeoPos lowerRight = geoCoding.getGeoPos(new PixelPos(product.getSceneRasterWidth(), product.getSceneRasterHeight()), null);
            final String latMax = String.valueOf(upperLeft.getLat());
            final String latMin = String.valueOf(lowerRight.getLat());
            final String lonMin = String.valueOf(upperLeft.getLon());
            final String lonMax = String.valueOf(lowerRight.getLon());

            NFileWriteable writeable = ctx.getNetcdfFileWriteable();

            // global attributes
            writeable.addGlobalAttribute("title", "ESA CCI Land Cover Condition " + condition);
            writeable.addGlobalAttribute("summary",
                                         "This dataset contains the global ESA CCI land cover condition derived from satellite data of a range of years.");
            LcWriterUtils.addGenericGlobalAttributes(writeable);

            //writeable.addGlobalAttribute("platform", platform);
            //writeable.addGlobalAttribute("sensor", sensor);
            MetadataElement metadataRoot = product.getMetadataRoot();
            if (metadataRoot.containsAttribute("regionIdentifier")) {
                String regionIdentifier = metadataRoot.getAttributeString("regionIdentifier");
                writeable.addGlobalAttribute("type", String.format("ESACCI-LC-L4-%s-Cond-%sm-P%sD-%s",
                                                                   condition,
                                                                   spatialResolution,
                                                                   temporalResolution,
                                                                   regionIdentifier));
                writeable.addGlobalAttribute("id", String.format("ESACCI-LC-L4-%s-Cond-%sm-P%sD-%s-%s-%s-%s-v%s",
                                                                 condition,
                                                                 spatialResolution,
                                                                 temporalResolution,
                                                                 regionIdentifier,
                                                                 startYear,
                                                                 endYear,
                                                                 startDate,
                                                                 version));
            } else {
                writeable.addGlobalAttribute("type", String.format("ESACCI-LC-L4-%s-Cond-%sm-P%sD",
                                                                   condition,
                                                                   spatialResolution,
                                                                   temporalResolution));
                writeable.addGlobalAttribute("id", String.format("ESACCI-LC-L4-%s-Cond-%sm-P%sD-%s-%s-%s-v%s",
                                                                 condition,
                                                                 spatialResolution,
                                                                 temporalResolution,
                                                                 startYear,
                                                                 endYear,
                                                                 startDate,
                                                                 version));

            }
            final String spatialResolutionDegrees = "500".equals(spatialResolution) ? "0.005556" : "0.011112";
            final String temporalCoverageYears;
            try {
                temporalCoverageYears = String.valueOf(Integer.parseInt(endYear) - Integer.parseInt(startYear) + 1);
            } catch (NumberFormatException ex) {
                throw new RuntimeException("cannot parse " + startYear + " and " + endYear + " as year numbers", ex);
            }
            LcWriterUtils.addSpecificGlobalAttribute(spatialResolutionDegrees, spatialResolution,
                                                     temporalCoverageYears, temporalResolution,
                                                     startTime, endTime,
                                                     version, latMax, latMin, lonMin, lonMax, writeable);

            final Dimension tileSize = ImageManager.getPreferredTileSize(product);
            writeable.addGlobalAttribute("TileSize", tileSize.height + ":" + tileSize.width);

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
                Dimension tileSize = ImageManager.getPreferredTileSize(p);
                StringBuilder ancillaryVariables = new StringBuilder();
                for (Band band : p.getBands()) {
                    if ("ndvi_std".equals(band.getName()) ||
                        "ndvi_nYearObs".equals(band.getName()) ||
                        "ba_nYearObs".equals(band.getName()) ||
                        "snow_nYearObs".equals(band.getName()) ||
                        "ndvi_status".equals(band.getName())) {
                        if (ancillaryVariables.length() > 0) {
                            ancillaryVariables.append(' ');
                        }
                        ancillaryVariables.append(band.getName());
                    }
                }
                for (Band band : p.getBands()) {
                    if ("ndvi_mean".equals(band.getName())) {
                        addNdviMeanVariable(ncFile, band, tileSize, ancillaryVariables.toString());
                    } else if ("ndvi_std".equals(band.getName())) {
                        addNdviStdVariable(ncFile, band, tileSize);
                    } else if ("ndvi_nYearObs".equals(band.getName())) {
                        addNdviNYearObsVariable(ncFile, band, tileSize);
                    } else if ("ndvi_status".equals(band.getName())) {
                        addNdviStatusVariable(ncFile, band, tileSize);
                    } else if ("ba_occ".equals(band.getName())) {
                        addBaOccVariable(ncFile, band, tileSize, ancillaryVariables.toString());
                    } else if ("ba_nYearObs".equals(band.getName())) {
                        addBaNYearObsVariable(ncFile, band, tileSize);
                    } else if ("snow_occ".equals(band.getName())) {
                        addSnowOccVariable(ncFile, band, tileSize, ancillaryVariables.toString());
                    } else if ("snow_nYearObs".equals(band.getName())) {
                        addSnowNYearObsVariable(ncFile, band, tileSize);
                    }
                }
            }

            private void addNdviMeanVariable(NFileWriteable ncFile, Band band, Dimension tileSize, String ancillaryVariables) throws IOException {
                final DataType ncDataType = DataTypeUtils.getNetcdfDataType(band.getDataType());
                final String variableName = ReaderUtils.getVariableName(band);
                final NVariable variable = ncFile.addVariable(variableName, ncDataType, false, tileSize, ncFile.getDimensions());
                variable.addAttribute("long_name", band.getDescription());
                variable.addAttribute("standard_name", "normalized_difference_vegetation_index");
                variable.addAttribute("valid_min", -100);
                variable.addAttribute("valid_max", 100);
                variable.addAttribute("scale_factor", 0.01f);
                if (ncDataType == DataType.SHORT) {
                    variable.addAttribute(Constants.FILL_VALUE_ATT_NAME, (short) 255);
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
                variable.addAttribute("valid_max", 100);
                variable.addAttribute("scale_factor", 0.01f);
                if (ncDataType == DataType.BYTE) {
                    variable.addAttribute(Constants.FILL_VALUE_ATT_NAME, (byte) -1);
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
                final byte[] CONDITION_FLAG_VALUES = new byte[]{0, 1, 2, 3, 4, 5, 6};
                final String CONDITION_FLAG_MEANINGS = "no_data land water snow cloud cloud_shadow invalid";
                final ArrayByte.D1 valids = new ArrayByte.D1(CONDITION_FLAG_VALUES.length);
                for (int i = 0; i < CONDITION_FLAG_VALUES.length; ++i) {
                    valids.set(i, CONDITION_FLAG_VALUES[i]);
                }
                variable.addAttribute("long_name", band.getDescription());
                variable.addAttribute("standard_name", "normalized_difference_vegetation_index status_flag");
                variable.addAttribute("flag_values", valids);
                variable.addAttribute("flag_meanings", CONDITION_FLAG_MEANINGS);
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
                    variable.addAttribute(Constants.FILL_VALUE_ATT_NAME, (short) -1);
                } else {
                    variable.addAttribute(Constants.FILL_VALUE_ATT_NAME, (byte) -1);
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
                } else {
                    variable.addAttribute(Constants.FILL_VALUE_ATT_NAME, (byte) -1);
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
        };

    }

}
