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
import ucar.ma2.ArrayByte;
import ucar.ma2.DataType;

import java.awt.Dimension;
import java.io.IOException;

/**
 * @author Martin Boettcher
 */
public class LcWbNetCdf4WriterPlugIn extends BeamNetCdf4WriterPlugIn {

    public static final String FORMAT_NAME = "NetCDF4-LC-WB";

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
        return new LcWbInitialisationPart();
    }

    @Override
    public ProfilePartWriter createDescriptionPartWriter() {
        return new NullProfilePartWriter();
    }

    class LcWbInitialisationPart extends BeamInitialisationPart {

        @Override
        public void writeProductBody(ProfileWriteContext ctx, Product product) throws IOException {

            final NFileWriteable writeable = ctx.getNetcdfFileWriteable();

            final LcWbMetadata lcWbMetadata = new LcWbMetadata(product);
            final String spatialResolution = lcWbMetadata.getSpatialResolution();
            final String temporalResolution = lcWbMetadata.getTemporalResolution();
            final String epoch = lcWbMetadata.getEpoch();
            final String version = lcWbMetadata.getVersion();


            final GeoCoding geoCoding = product.getSceneGeoCoding();
            final GeoPos upperLeft = geoCoding.getGeoPos(new PixelPos(0, 0), null);
            final GeoPos lowerRight = geoCoding.getGeoPos(new PixelPos(product.getSceneRasterWidth(), product.getSceneRasterHeight()), null);
            final String latMax = String.valueOf(upperLeft.getLat());
            final String latMin = String.valueOf(lowerRight.getLat());
            final String lonMin = String.valueOf(upperLeft.getLon());
            final String lonMax = String.valueOf(lowerRight.getLon());

            final String spatialResolutionDegrees = "1000m".equals(spatialResolution) ? "0.011112" :
                    "300m".equals(spatialResolution) ? "0.002778" :
                    "150m".equals(spatialResolution) ? "0.001389" : ("180*"+spatialResolution+"/19440km");
            int epochInt = Integer.parseInt(epoch);
            int temporalResolutionInt = Integer.parseInt(temporalResolution);
            final String startYear = String.valueOf(epochInt);
            final String startTime = startYear + "0101";
            final String endYear = String.valueOf(epochInt + temporalResolutionInt - 1);
            final String endTime = endYear + "1231";
            final String temporalCoverageYears = getTemporalCoverage(startYear, endYear);

            // global attributes
            writeable.addGlobalAttribute("title", "ESA CCI Land Cover Water Bodies Map");
            writeable.addGlobalAttribute("summary",
                                         "This dataset contains the global ESA CCI land cover water bodies map derived from satellite data.");
            writeable.addGlobalAttribute("type", lcWbMetadata.getType());
            writeable.addGlobalAttribute("id", lcWbMetadata.getId());
            final Dimension tileSize = product.getPreferredTileSize();
            LcWriterUtils.addGenericGlobalAttributes(writeable, String.format("%d:%d", tileSize.width, tileSize.height));
            LcWriterUtils.addSpecificGlobalAttributes("ASAR",
                                                      "lc-wb-classification-1.0",
                                                      spatialResolutionDegrees, spatialResolution,
                                                      temporalCoverageYears, temporalResolution, "Y",
                                                      startTime, endTime,
                                                      version, latMax, latMin, lonMin, lonMax, writeable,
                                                      "ESA 2015 - UCLouvain and Gamma-RS");

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
            private static final String WB_CLASS_BAND_NAME = "wb_class";
            private static final String WS_OBSERVATION_COUNT_BAND_NAME = "ws_observation_count";
            private static final String GM_OBSERVATION_COUNT_BAND_NAME = "gm_observation_count";

            @Override
            public void preEncode(ProfileWriteContext ctx, Product p) throws IOException {

                final NFileWriteable ncFile = ctx.getNetcdfFileWriteable();
                String ancillaryVariableString = getAncillaryVariableString(p);
                for (Band band : p.getBands()) {
                    final Dimension tileSize = p.getPreferredTileSize();
                    if (WB_CLASS_BAND_NAME.equals(band.getName())) {
                        addWbClassVariable(ncFile, band, tileSize, ancillaryVariableString);
                    } else if (WS_OBSERVATION_COUNT_BAND_NAME.equals(band.getName()) ||
                               GM_OBSERVATION_COUNT_BAND_NAME.equals(band.getName())) {
                        addObservationCountVariable(ncFile, band, tileSize);
                    }else {
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
                return WS_OBSERVATION_COUNT_BAND_NAME.equals(bandName) ||
                       GM_OBSERVATION_COUNT_BAND_NAME.equals(bandName);
            }

            private void addWbClassVariable(NFileWriteable ncFile, Band band, Dimension tileSize, String ancillaryVariables) throws IOException {
                final DataType ncDataType = DataTypeUtils.getNetcdfDataType(band.getDataType());
                final String variableName = ReaderUtils.getVariableName(band);
                //nccopy does not support reading ubyte variables, therefore preliminarily commented out
                final NVariable variable = ncFile.addVariable(variableName, ncDataType, false, tileSize, ncFile.getDimensions());
                byte[] wbClassFlagValues = new byte[] {0, 1, 2 };
                final ArrayByte.D1 valids = new ArrayByte.D1(wbClassFlagValues.length,variable.getDataType().isUnsigned());
                for (int i = 0; i < wbClassFlagValues.length; ++i) {
                    valids.set(i, wbClassFlagValues[i]);
                }
                variable.addAttribute("long_name", band.getDescription());
                variable.addAttribute("standard_name", "land_cover_lccs");
                variable.addAttribute("flag_values", valids);
                variable.addAttribute("flag_meanings", "terrestrial water");
                variable.addAttribute("valid_min", 0);
                variable.addAttribute("valid_max", 2);
                variable.addAttribute("_Unsigned", "true");
                variable.addAttribute(Constants.FILL_VALUE_ATT_NAME, (byte) 0);
                if (ancillaryVariables.length() > 0) {
                    variable.addAttribute("ancillary_variables", ancillaryVariables);
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
