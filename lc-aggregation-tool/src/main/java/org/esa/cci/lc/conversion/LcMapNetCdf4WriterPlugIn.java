package org.esa.cci.lc.conversion;

import org.esa.beam.dataio.netcdf.DefaultNetCdfWriter;
import org.esa.beam.dataio.netcdf.NullProfilePartWriter;
import org.esa.beam.dataio.netcdf.ProfileWriteContext;
import org.esa.beam.dataio.netcdf.metadata.ProfileInitPartWriter;
import org.esa.beam.dataio.netcdf.metadata.ProfilePartWriter;
import org.esa.beam.dataio.netcdf.metadata.profiles.beam.BeamBandPart;
import org.esa.beam.dataio.netcdf.metadata.profiles.beam.BeamInitialisationPart;
import org.esa.beam.dataio.netcdf.metadata.profiles.beam.BeamNetCdf4WriterPlugIn;
import org.esa.beam.dataio.netcdf.metadata.profiles.cf.CfGeocodingPart;
import org.esa.beam.dataio.netcdf.nc.NFileWriteable;
import org.esa.beam.dataio.netcdf.nc.NVariable;
import org.esa.beam.dataio.netcdf.util.Constants;
import org.esa.beam.dataio.netcdf.util.DataTypeUtils;
import org.esa.beam.dataio.netcdf.util.ReaderUtils;
import org.esa.beam.framework.dataio.ProductWriter;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.PixelGeoCoding;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.jai.ImageManager;
import ucar.ma2.ArrayByte;
import ucar.ma2.ArrayInt;
import ucar.ma2.ArrayShort;
import ucar.ma2.DataType;

import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.lang.Override;import java.lang.String;import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.UUID;

/**
 * TODO add API doc
 *
 * @author Martin Boettcher
 */
public class LcMapNetCdf4WriterPlugIn extends BeamNetCdf4WriterPlugIn {
    @Override
    public String[] getFormatNames() {
        return new String[]{"NetCDF4-LC-Map"};
    }

    @Override
    public ProductWriter createWriterInstance() {
        return new DefaultNetCdfWriter(this) {
            @Override
            public Object getOutput() {
                final Product sourceProduct = getSourceProduct();
                final String epoch = sourceProduct.getMetadataRoot().getAttributeString("epoch");
                final String version = sourceProduct.getMetadataRoot().getAttributeString("version");
                final String spatialResolution = sourceProduct.getMetadataRoot().getAttributeString("spatialResolution");
                final String temporalResolution = sourceProduct.getMetadataRoot().getAttributeString("temporalResolution");
                String lcOutputFilename =
                        MessageFormat.format("ESACCI-LC-L4-Map-{0}m-P{1}Y-{2}-v{3}.nc",
                                             spatialResolution,
                                             temporalResolution,
                                             epoch,
                                             version);
                String lcOutputPath;
                String outputPath = super.getOutput().toString();
                int pos = outputPath.lastIndexOf(File.separatorChar);
                if (pos >= 0) {
                    lcOutputPath = outputPath.substring(0, pos+1) + lcOutputFilename;
                } else {
                    lcOutputPath = lcOutputFilename;
                }

                return lcOutputPath;
            }
        };
    }

    @Override
    public ProfilePartWriter createMetadataPartWriter() {
        return new NullProfilePartWriter();
        //return super.createMetadataPartWriter();
    }

    @Override
    public ProfileInitPartWriter createInitialisationPartWriter() {
        return new LcSrInitialisationPart();
    }

    class LcSrInitialisationPart extends BeamInitialisationPart {

        private final SimpleDateFormat COMPACT_ISO_FORMAT = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");

        LcSrInitialisationPart() {
            COMPACT_ISO_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
        }

        @Override
        public void writeProductBody(ProfileWriteContext ctx, Product product) throws IOException {

            final String epoch = product.getMetadataRoot().getAttributeString("epoch");
            final String spatialResolution = product.getMetadataRoot().getAttributeString("spatialResolution");
            final String temporalResolution = product.getMetadataRoot().getAttributeString("temporalResolution");
            final String version = product.getMetadataRoot().getAttributeString("version");
            final String spatialResolutionDegrees = "300".equals(spatialResolution) ? "0.002778" : "0.011112";
            final String startTime = String.valueOf(Integer.parseInt(epoch) - Integer.parseInt(temporalResolution) / 2) + "0101";
            final String endTime = String.valueOf(Integer.parseInt(epoch) + Integer.parseInt(temporalResolution) / 2) + "1231";
            float latMax = 90.0f;
            float latMin = -90.0f;
            float lonMin = -180.0f;
            float lonMax = 180.0f;

            NFileWriteable writeable = ctx.getNetcdfFileWriteable();

            // global attributes
            writeable.addGlobalAttribute("title", "ESA CCI Land Cover Map");
            writeable.addGlobalAttribute("summary", "This dataset contains the global ESA CCI land cover classification map derived from satellite data of one epoch.");
            writeable.addGlobalAttribute("project", "Climate Change Initiative - European Space Agency");
            writeable.addGlobalAttribute("references", "http://www.esa-landcover-cci.org/");
            writeable.addGlobalAttribute("institution", "Universite catholique de Louvain");
            writeable.addGlobalAttribute("contact", "landcover-cci@uclouvain.be");
            writeable.addGlobalAttribute("source", "MERIS FR L1B version 5.05, MERIS RR L1B version 8.0, SPOT VGT P");
            writeable.addGlobalAttribute("history", "amorgos-4,0, lc-sdr-1.0, lc-sr-1.0, lc-classification-1.0");  // versions
            writeable.addGlobalAttribute("comment", "");

            writeable.addGlobalAttribute("Conventions", "CF-1.6");
            writeable.addGlobalAttribute("standard_name_vocabulary", "NetCDF Climate and Forecast (CF) Standard Names version 21");
            writeable.addGlobalAttribute("keywords", "land cover classification,satellite,observation");
            writeable.addGlobalAttribute("keywords_vocabulary", "NASA Global Change Master Directory (GCMD) Science Keywords");
            writeable.addGlobalAttribute("license", "ESA CCI Data Policy: free and open access");
            writeable.addGlobalAttribute("naming_authority", "org.esa-cci");
            writeable.addGlobalAttribute("cdm_data_type", "grid");

            //writeable.addGlobalAttribute("platform", platform);
            //writeable.addGlobalAttribute("sensor", sensor);
            writeable.addGlobalAttribute("type", MessageFormat.format("ESACCI-LC-L4-Map-{0}m-P{1}Y",
                                                       spatialResolution,
                                                       temporalResolution));
            writeable.addGlobalAttribute("id", MessageFormat.format("ESACCI-LC-L4-Map-{0}m-P{1}Y-{2}-v{3}",
                                                       spatialResolution,
                                                       temporalResolution,
                                                       epoch,
                                                       version));
            writeable.addGlobalAttribute("tracking_id", UUID.randomUUID().toString());
            writeable.addGlobalAttribute("product_version", version);
            writeable.addGlobalAttribute("date_created", COMPACT_ISO_FORMAT.format(new Date()));
            writeable.addGlobalAttribute("creator_name", "University catholique de Louvain");
            writeable.addGlobalAttribute("creator_url", "http://www.uclouvain.be/");
            writeable.addGlobalAttribute("creator_email", "landcover-cci@uclouvain.be");

//            writeable.addGlobalAttribute("time_coverage_start", COMPACT_ISO_FORMAT.format(product.getStartTime().getAsDate()));
//            writeable.addGlobalAttribute("time_coverage_end", COMPACT_ISO_FORMAT.format(product.getEndTime().getAsDate()));
            writeable.addGlobalAttribute("time_coverage_start", startTime);
            writeable.addGlobalAttribute("time_coverage_end", endTime);
            writeable.addGlobalAttribute("time_coverage_duration", "P" + temporalResolution + "Y");

            writeable.addGlobalAttribute("geospatial_lat_min", String.valueOf(latMin));
            writeable.addGlobalAttribute("geospatial_lat_max", String.valueOf(latMax));
            writeable.addGlobalAttribute("geospatial_lon_min", String.valueOf(lonMin));
            writeable.addGlobalAttribute("geospatial_lon_max", String.valueOf(lonMax));
            writeable.addGlobalAttribute("spatial_resolution", spatialResolution + "m");
            writeable.addGlobalAttribute("geospatial_lat_units", "degrees_north");
            writeable.addGlobalAttribute("geospatial_lat_resolution", spatialResolutionDegrees);
            writeable.addGlobalAttribute("geospatial_lon_units", "degrees_east");
            writeable.addGlobalAttribute("geospatial_lon_resolution", spatialResolutionDegrees);

            final Dimension tileSize = ImageManager.getPreferredTileSize(product);
            writeable.addGlobalAttribute("TileSize", tileSize.height + ":" + tileSize.width);
            //TODO writeable.addDimension("time", 1);
            writeable.addDimension("lat", product.getSceneRasterHeight());
            writeable.addDimension("lon", product.getSceneRasterWidth());
        }
    }

    @Override
    public ProfilePartWriter createBandPartWriter() {
//        return super.createBandPartWriter();    //To change body of overridden methods use File | Settings | File Templates.
        // TODO pre-encode überschreiben, variablen-attribute setzen
        return new BeamBandPart() {
            @Override
            public void preEncode(ProfileWriteContext ctx, Product p) throws IOException {
//                super.preEncode(ctx, p);    //To change body of overridden methods use File | Settings | File Templates.

                final NFileWriteable ncFile = ctx.getNetcdfFileWriteable();
                java.awt.Dimension tileSize = ImageManager.getPreferredTileSize(p);
                for (Band band : p.getBands()) {
                    if ("lccs_class".equals(band.getName())) {
                        addLccsClassVariable(ncFile, band, tileSize);
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

            private void addLccsClassVariable(NFileWriteable ncFile, Band band, Dimension tileSize) throws IOException {
                final DataType ncDataType = DataTypeUtils.getNetcdfDataType(band.getDataType());
                final String variableName = ReaderUtils.getVariableName(band);
                final NVariable variable = ncFile.addVariable(variableName, ncDataType, true, tileSize, ncFile.getDimensions());
                final short[] LCCS_CLASS_FLAG_VALUES = new short[] { 0, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100, 110, 120, 130, 140, 150, 160, 170, 180, 190, 200, 210, 220, 230, 240 };
                final String LCCS_CLASS_FLAG_MEANINGS = "no_data cropland_rainfed cropland_irrigated mosaic_cropland mosaic_natural_vegetation tree_broadleaved_evergreen_closed_to_open tree_broadleaved_deciduous_closed tree_broadleaved_deciduous_open tree_needleleaved_evergreen_closed tree_needleleaved_evergreen_open tree_needleleaved_deciduous_closed tree_needleleaved_deciduous_open tree_mixed mosaic_tree_and_shrub mosaic_herbaceous shrubland grassland sparse_vegetation tree_cover_flooded_fresh_or_brakish_water tree_cover_flooded_saline_water shrub_or_herbaceous_cover_flooded urban bare_areas water snow_and_ice";
                final ArrayShort.D1 valids = new ArrayShort.D1(LCCS_CLASS_FLAG_VALUES.length);
                for (int i=0; i<LCCS_CLASS_FLAG_VALUES.length; ++i) {
                    valids.set(i, LCCS_CLASS_FLAG_VALUES[i]);
                }
                variable.addAttribute("long_name", band.getDescription());
                variable.addAttribute("standard_name", "land_cover_lccs");
                variable.addAttribute("flag_values", valids);
                variable.addAttribute("flag_meanings", LCCS_CLASS_FLAG_MEANINGS);
                variable.addAttribute("valid_min", 1);
                variable.addAttribute("valid_max", 240);
                variable.addAttribute("_Unsigned", "true");
                variable.addAttribute(Constants.FILL_VALUE_ATT_NAME, 0);
            }

            private void addProcessedFlagVariable(NFileWriteable ncFile, Band band, Dimension tileSize) throws IOException {
                final DataType ncDataType = DataTypeUtils.getNetcdfDataType(band.getDataType());
                final String variableName = ReaderUtils.getVariableName(band);
                final NVariable variable = ncFile.addVariable(variableName, ncDataType, tileSize, ncFile.getDimensions());
                final byte[] FLAG_VALUES = new byte[] { 0, 1 };
                final String FLAG_MEANINGS = "not_processed processed";
                final ArrayByte.D1 valids = new ArrayByte.D1(FLAG_VALUES.length);
                for (int i=0; i<FLAG_VALUES.length; ++i) {
                    valids.set(i, FLAG_VALUES[i]);
                }
                variable.addAttribute("long_name", band.getDescription());
                variable.addAttribute("standard_name", "land_cover_lccs status_flag");
                variable.addAttribute("flag_values", valids);
                variable.addAttribute("flag_meanings", FLAG_MEANINGS);
                variable.addAttribute("valid_min", 0);
                variable.addAttribute("valid_max", 1);
                variable.addAttribute(Constants.FILL_VALUE_ATT_NAME, -1);
            }

            private void addCurrentPixelStateVariable(NFileWriteable ncFile, Band band, Dimension tileSize) throws IOException {
                final DataType ncDataType = DataTypeUtils.getNetcdfDataType(band.getDataType());
                final String variableName = ReaderUtils.getVariableName(band);
                final NVariable variable = ncFile.addVariable(variableName, ncDataType, tileSize, ncFile.getDimensions());
                final byte[] FLAG_VALUES = new byte[] { 0, 1, 2, 3, 4, 5 };
                final String FLAG_MEANINGS = "invalid clear_land clear_water clear_snow_ice cloud cloud_shadow";
                final ArrayByte.D1 valids = new ArrayByte.D1(FLAG_VALUES.length);
                for (int i=0; i<FLAG_VALUES.length; ++i) {
                    valids.set(i, FLAG_VALUES[i]);
                }
                variable.addAttribute("long_name", band.getDescription());
                variable.addAttribute("standard_name", "land_cover_lccs status_flag");
                variable.addAttribute("flag_values", valids);
                variable.addAttribute("flag_meanings", FLAG_MEANINGS);
                variable.addAttribute("valid_min", 0);
                variable.addAttribute("valid_max", 5);
                variable.addAttribute(Constants.FILL_VALUE_ATT_NAME, -1);
            }

            private void addObservationCountVariable(NFileWriteable ncFile, Band band, Dimension tileSize) throws IOException {
                final DataType ncDataType = DataType.SHORT; //DataTypeUtils.getNetcdfDataType(band.getDataType());
                final String variableName = ReaderUtils.getVariableName(band);
                final NVariable variable = ncFile.addVariable(variableName, ncDataType, tileSize, ncFile.getDimensions());
                variable.addAttribute("long_name", band.getDescription());
                variable.addAttribute("standard_name", "land_cover_lccs number_of_observations");
                variable.addAttribute("valid_min", 0);
                variable.addAttribute("valid_max", 32767);
                variable.addAttribute(Constants.FILL_VALUE_ATT_NAME, -1);
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
                variable.addAttribute(Constants.FILL_VALUE_ATT_NAME, -1);
            }

            private void addOverallConfidenceLevelVariable(NFileWriteable ncFile, Band band, Dimension tileSize) throws IOException {
                final DataType ncDataType = DataTypeUtils.getNetcdfDataType(band.getDataType());
                final String variableName = ReaderUtils.getVariableName(band);
                final NVariable variable = ncFile.addVariable(variableName, ncDataType, tileSize, ncFile.getDimensions());
                final byte[] FLAG_VALUES = new byte[] { 0, 1, 2 };
                final String FLAG_MEANINGS = "doubtful reasonable certain";
                final ArrayByte.D1 valids = new ArrayByte.D1(FLAG_VALUES.length);
                for (int i=0; i<FLAG_VALUES.length; ++i) {
                    valids.set(i, FLAG_VALUES[i]);
                }
                variable.addAttribute("long_name", band.getDescription());
                variable.addAttribute("standard_name", "land_cover_lccs status_flag");
                variable.addAttribute("flag_values", valids);
                variable.addAttribute("flag_meanings", FLAG_MEANINGS);
                variable.addAttribute("valid_min", 0);
                variable.addAttribute("valid_max", 1);
                variable.addAttribute(Constants.FILL_VALUE_ATT_NAME, -1);
            }
        };
    }

}
