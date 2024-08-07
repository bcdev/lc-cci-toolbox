package org.esa.cci.lc.io;

import com.bc.ceres.core.CoreException;
import com.bc.ceres.core.runtime.internal.ModuleImpl;
import com.bc.ceres.core.runtime.internal.ModuleReader;
import org.esa.snap.dataio.netcdf.nc.NFileWriteable;
import org.esa.cci.lc.util.LcHelper;

import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * @author Marco Peters
 */
public class LcWriterUtils {

    public static final String ATTRIBUTE_NAME_REGION_IDENTIFIER = "regionIdentifier";

    final static SimpleDateFormat COMPACT_ISO_FORMAT = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");

    static {
        LcWriterUtils.COMPACT_ISO_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    public static String getModuleVersion() throws IOException {
        String lcUserToolsVersion;
        ModuleReader moduleReader = new ModuleReader(Logger.getAnonymousLogger());
        URL moduleLocation = LcWriterUtils.class.getProtectionDomain().getCodeSource().getLocation();
        try {
            ModuleImpl module = moduleReader.readFromLocation(moduleLocation);
            lcUserToolsVersion = module.getVersion().toString();
        } catch (CoreException e) {
            throw new IOException("Could not read version from module.xml");
        }
        return lcUserToolsVersion;
    }

    static void addGenericGlobalAttributes(NFileWriteable writeable, String tileSize) throws IOException {
        writeable.addGlobalAttribute("project", "Climate Change Initiative - European Space Agency");
        writeable.addGlobalAttribute("references", "http://www.esa-landcover-cci.org/");
        writeable.addGlobalAttribute("institution", "Universite catholique de Louvain");
        writeable.addGlobalAttribute("contact", "landcover-cci@uclouvain.be");
        writeable.addGlobalAttribute("comment", "");

        writeable.addGlobalAttribute("Conventions", "CF-1.6");
        writeable.addGlobalAttribute("standard_name_vocabulary", "NetCDF Climate and Forecast (CF) Standard Names version 21");
        writeable.addGlobalAttribute("keywords", "land cover classification,satellite,observation");
        writeable.addGlobalAttribute("keywords_vocabulary", "NASA Global Change Master Directory (GCMD) Science Keywords");
        writeable.addGlobalAttribute("license", "ESA CCI Data Policy: free and open access");
        writeable.addGlobalAttribute("naming_authority", "org.esa-cci");
        writeable.addGlobalAttribute("cdm_data_type", "grid");
        writeable.addGlobalAttribute(LcHelper.PROP_NAME_TILE_SIZE, tileSize);

    }

    static void addSpecificGlobalAttributes(String source, String history,
                                            String spatialResolutionDegrees, String spatialResolution,
                                            String temporalCoverageYears, String temporalResolution, String unit,
                                            String startTime, String endTime, String version,
                                            String latMax, String latMin, String lonMin, String lonMax, NFileWriteable writeable,
                                            String creator) throws IOException {
        writeable.addGlobalAttribute("tracking_id", UUID.randomUUID().toString());
        writeable.addGlobalAttribute("product_version", version);
        writeable.addGlobalAttribute("date_created", COMPACT_ISO_FORMAT.format(new Date()));
        writeable.addGlobalAttribute("creator_name", creator);
        writeable.addGlobalAttribute("creator_url", "http://www.uclouvain.be/");
        writeable.addGlobalAttribute("creator_email", "landcover-cci@uclouvain.be");
        writeable.addGlobalAttribute("source", source);
        String lcUserToolsVersion = getModuleVersion();
        writeable.addGlobalAttribute("history", history + ",lc-user-tools-" + lcUserToolsVersion);  // versions
//            writeable.addGlobalAttribute("time_coverage_start", COMPACT_ISO_FORMAT.format(product.getStartTime().getAsDate()));
//            writeable.addGlobalAttribute("time_coverage_end", COMPACT_ISO_FORMAT.format(product.getEndTime().getAsDate()));
        writeable.addGlobalAttribute("time_coverage_start", startTime);
        writeable.addGlobalAttribute("time_coverage_end", endTime);
        writeable.addGlobalAttribute("time_coverage_duration", "P" + temporalCoverageYears + "Y");
        writeable.addGlobalAttribute("time_coverage_resolution", "P" + temporalResolution + unit);

        writeable.addGlobalAttribute("geospatial_lat_min", latMin);
        writeable.addGlobalAttribute("geospatial_lat_max", latMax);
        writeable.addGlobalAttribute("geospatial_lon_min", lonMin);
        writeable.addGlobalAttribute("geospatial_lon_max", lonMax);
        writeable.addGlobalAttribute("spatial_resolution", spatialResolution);
        writeable.addGlobalAttribute("geospatial_lat_units", "degrees_north");
        writeable.addGlobalAttribute("geospatial_lat_resolution", spatialResolutionDegrees);
        writeable.addGlobalAttribute("geospatial_lon_units", "degrees_east");
        writeable.addGlobalAttribute("geospatial_lon_resolution", spatialResolutionDegrees);
    }
}
