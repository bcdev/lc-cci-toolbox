package org.esa.cci.lc.aggregation;

import org.esa.beam.binning.TemporalBin;
import org.esa.beam.binning.operator.BinWriter;
import org.esa.beam.dataio.netcdf.nc.NFileWriteable;
import org.esa.beam.dataio.netcdf.nc.NWritableFactory;
import org.esa.beam.util.io.FileUtils;
import org.esa.beam.util.logging.BeamLogManager;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * @author Marco Peters
 */
class LcBinWriter implements BinWriter {

    private String targetFilePath;
    private Logger logger;

    LcBinWriter() {
        logger = BeamLogManager.getSystemLogger();
    }

    @Override
    public void write(Map<String, String> metadataProperties, List<TemporalBin> temporalBins) throws IOException {
        NFileWriteable writeable = NWritableFactory.create(targetFilePath, "netcdf4");
        try {
            addGlobalAttributes(writeable);
            writeable.create();
        } catch (Throwable e) {
            e.printStackTrace();
        } finally {
            writeable.close();
        }
    }

    private void addGlobalAttributes(NFileWriteable writeable) throws IOException {
        writeable.addGlobalAttribute("title", "ESA CCI Land Cover Map");
        writeable.addGlobalAttribute("summary", "Fill in something meaningful."); // TODO
        writeable.addGlobalAttribute("project", "Climate Change Initiative - European Space Agency");
        writeable.addGlobalAttribute("references", "http://www.esa-landcover-cci.org/");
        writeable.addGlobalAttribute("source", "ESA CCI Land Cover Map product"); // TODO epoch, version, resolution
        writeable.addGlobalAttribute("history", "amorgos-4,0, lc-sdr-1.0, lc-sr-1.0, lc-classification-1.0, aggregation-tool-0.8");  // versions
        writeable.addGlobalAttribute("comment", ""); // TODO

        writeable.addGlobalAttribute("Conventions", "CF-1.6");
        writeable.addGlobalAttribute("standard_name_vocabulary", "NetCDF Climate and Forecast (CF) Standard Names version 21");
        writeable.addGlobalAttribute("keywords", "land cover classification,satellite,observation");     // TODO
        writeable.addGlobalAttribute("keywords_vocabulary", "NASA Global Change Master Directory (GCMD) Science Keywords");
        writeable.addGlobalAttribute("license", "ESA CCI Data Policy: free and open access");
        writeable.addGlobalAttribute("naming_authority", "org.esa-cci");
        writeable.addGlobalAttribute("cdm_data_type", "grid"); // todo
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
}
