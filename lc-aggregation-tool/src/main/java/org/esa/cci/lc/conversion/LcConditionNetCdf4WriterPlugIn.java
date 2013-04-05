package org.esa.cci.lc.conversion;

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
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.jai.ImageManager;
import ucar.ma2.ArrayByte;
import ucar.ma2.DataType;

import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.UUID;

/**
 * TODO add API doc
 *
 * @author Martin Boettcher
 */
public class LcConditionNetCdf4WriterPlugIn extends BeamNetCdf4WriterPlugIn {
    @Override
    public String[] getFormatNames() {
        return new String[]{"NetCDF4-LC-Condition"};
    }

    @Override
    public ProductWriter createWriterInstance() {
        return new DefaultNetCdfWriter(this) {
            @Override
            public Object getOutput() {
                final Product sourceProduct = getSourceProduct();
                final String condition = sourceProduct.getMetadataRoot().getAttributeString("condition");
                final String spatialResolution = sourceProduct.getMetadataRoot().getAttributeString("spatialResolution");
                final String temporalResolution = sourceProduct.getMetadataRoot().getAttributeString("temporalResolution");
                final String startYear = sourceProduct.getMetadataRoot().getAttributeString("startYear");
                final String endYear = sourceProduct.getMetadataRoot().getAttributeString("endYear");
                final String weekNumber = sourceProduct.getMetadataRoot().getAttributeString("weekNumber");
                final String version = sourceProduct.getMetadataRoot().getAttributeString("version");
                String lcOutputFilename =
                        MessageFormat.format("ESACCI-LC-L4-Cond-{0}-{1}m-P{2}D-{3}-{4}-{5}-v{6}.nc",
                                             condition,
                                             spatialResolution,
                                             temporalResolution,
                                             startYear,
                                             endYear,
                                             weekNumber,
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

            final String condition = product.getMetadataRoot().getAttributeString("condition");
            final String spatialResolution = product.getMetadataRoot().getAttributeString("spatialResolution");
            final String temporalResolution = product.getMetadataRoot().getAttributeString("temporalResolution");
            final String startYear = product.getMetadataRoot().getAttributeString("startYear");
            final String endYear = product.getMetadataRoot().getAttributeString("endYear");
            final String version = product.getMetadataRoot().getAttributeString("version");
            final String spatialResolutionDegrees = "500".equals(spatialResolution) ? "0.005556" : "0.011112";
            final String startTime = startYear + "0101";
            final String endTime = endYear + "1231";
            float latMax = 90.0f;
            float latMin = -90.0f;
            float lonMin = -180.0f;
            float lonMax = 180.0f;

            NFileWriteable writeable = ctx.getNetcdfFileWriteable();

            // global attributes
            writeable.addGlobalAttribute("title", "ESA CCI Land Cover Condition " + condition);
            writeable.addGlobalAttribute("summary", "This dataset contains the global ESA CCI land cover condition derived from satellite data of a range of years.");
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
            writeable.addGlobalAttribute("type", MessageFormat.format("ESACCI-LC-L4-Cond-{0}-{1}m-P{2}D",
                                                       condition,
                                                       spatialResolution,
                                                       temporalResolution));
            writeable.addGlobalAttribute("id", MessageFormat.format("ESACCI-LC-L4-Cond-{0}-{1}m-P{2}D-{3}-{4}-v{5}",
                                                       condition,
                                                       spatialResolution,
                                                       temporalResolution,
                                                       startYear,
                                                       endYear,
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
            writeable.addGlobalAttribute("time_coverage_duration", "P" + temporalResolution + "D");

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
        return new BeamBandPart() {
            @Override
            public void preEncode(ProfileWriteContext ctx, Product p) throws IOException {

                final NFileWriteable ncFile = ctx.getNetcdfFileWriteable();
                Dimension tileSize = ImageManager.getPreferredTileSize(p);
                for (Band band : p.getBands()) {
                    if ("ndvi_mean".equals(band.getName())) {
                        addNdviMeanVariable(ncFile, band, tileSize);
                    } else if ("ndvi_std".equals(band.getName())) {
                        addNdviStdVariable(ncFile, band, tileSize);
                    } else if ("ndvi_nYearObs".equals(band.getName())) {
                        addNdviNYearObsVariable(ncFile, band, tileSize);
                    } else if ("ndvi_status".equals(band.getName())) {
                        addNdviStatusVariable(ncFile, band, tileSize);
                    }
                }
            }

            private void addNdviMeanVariable(NFileWriteable ncFile, Band band, Dimension tileSize) throws IOException {
                final DataType ncDataType = DataTypeUtils.getNetcdfDataType(band.getDataType());
                final String variableName = ReaderUtils.getVariableName(band);
                final NVariable variable = ncFile.addVariable(variableName, ncDataType, false, tileSize, ncFile.getDimensions());
                variable.addAttribute("long_name", band.getDescription());
                variable.addAttribute("standard_name", "normalised_difference_vegetation_index");
                variable.addAttribute("valid_min", -100);
                variable.addAttribute("valid_max", 100);
                variable.addAttribute("scale_factor", 0.01f);
                variable.addAttribute(Constants.FILL_VALUE_ATT_NAME, (short)255);
            }

            private void addNdviStdVariable(NFileWriteable ncFile, Band band, Dimension tileSize) throws IOException {
                final DataType ncDataType = DataTypeUtils.getNetcdfDataType(band.getDataType());
                final String variableName = ReaderUtils.getVariableName(band);
                final NVariable variable = ncFile.addVariable(variableName, ncDataType, false, tileSize, ncFile.getDimensions());
                variable.addAttribute("long_name", band.getDescription());
                variable.addAttribute("standard_name", "normalised_difference_vegetation_index standard_error");
                variable.addAttribute("valid_min", 0);
                variable.addAttribute("valid_max", 100);
                variable.addAttribute("scale_factor", 0.01f);
                variable.addAttribute(Constants.FILL_VALUE_ATT_NAME, (byte)-1);
            }

            private void addNdviNYearObsVariable(NFileWriteable ncFile, Band band, Dimension tileSize) throws IOException {
                final DataType ncDataType = DataTypeUtils.getNetcdfDataType(band.getDataType());
                final String variableName = ReaderUtils.getVariableName(band);
                final NVariable variable = ncFile.addVariable(variableName, ncDataType, false, tileSize, ncFile.getDimensions());
                variable.addAttribute("long_name", band.getDescription());
                variable.addAttribute("standard_name", "normalised_difference_vegetation_index number_of_observations");
                variable.addAttribute("valid_min", 0);
                variable.addAttribute("valid_max", 30);
                variable.addAttribute(Constants.FILL_VALUE_ATT_NAME, (byte)-1);
            }

            private void addNdviStatusVariable(NFileWriteable ncFile, Band band, Dimension tileSize) throws IOException {
                final DataType ncDataType = DataTypeUtils.getNetcdfDataType(band.getDataType());
                final String variableName = ReaderUtils.getVariableName(band);
                final NVariable variable = ncFile.addVariable(variableName, ncDataType, false, tileSize, ncFile.getDimensions());
                final byte[] CONDITION_FLAG_VALUES = new byte[] { 0, 1, 2, 3, 4, 5, 6 };
                final String CONDITION_FLAG_MEANINGS = "no_data land water snow cloud cloud_shadow invalid";
                final ArrayByte.D1 valids = new ArrayByte.D1(CONDITION_FLAG_VALUES.length);
                for (int i=0; i<CONDITION_FLAG_VALUES.length; ++i) {
                    valids.set(i, CONDITION_FLAG_VALUES[i]);
                }
                variable.addAttribute("long_name", band.getDescription());
                variable.addAttribute("standard_name", "normalised_difference_vegetation_index status_flag");
                variable.addAttribute("flag_values", valids);
                variable.addAttribute("flag_meanings", CONDITION_FLAG_MEANINGS);
                variable.addAttribute("valid_min", 1);
                variable.addAttribute("valid_max", 5);
                variable.addAttribute(Constants.FILL_VALUE_ATT_NAME, (byte)-1);
            }
        };

    }

}
