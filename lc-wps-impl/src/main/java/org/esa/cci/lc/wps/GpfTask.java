package org.esa.cci.lc.wps;

import com.bc.wps.utilities.WpsLogger;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.OperatorException;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author hans
 */
public class GpfTask implements Callable<Boolean> {


    private final String jobId;
    private final Map<String, Object> parameters;
    private final Product sourceProduct;
    private final File targetDir;
    private final String hostName;
    private final int portNumber;
    private Logger logger = WpsLogger.getLogger();

    public GpfTask(String jobId, Map<String, Object> parameters, Product sourceProduct, File targetDir, String hostName, int portNumber) {
        this.jobId = jobId;
        this.parameters = parameters;
        this.sourceProduct = sourceProduct;
        this.targetDir = targetDir;
        this.hostName = hostName;
        this.portNumber = portNumber;
    }

    @Override
    public Boolean call() throws Exception {
        ProductionStatus status = GpfProductionService.getProductionStatusMap().get(jobId);
        status.setState(ProductionState.RUNNING);
        status.setProgress(10);
        try {
            logger.log(Level.INFO, "starting subsetting operation...");
            GPF.createProduct("LCCCI.Subset", parameters, sourceProduct);
            logger.log(Level.INFO, "subsetting operation completed...");

            List<String> resultUrls = GpfProductionService.getProductUrls(hostName, portNumber, targetDir);
            status.setState(ProductionState.SUCCESSFUL);
            status.setProgress(100);
            status.setResultUrls(resultUrls);
            GpfProductionService.getProductionStatusMap().put(jobId, status);
            return true;
        } catch (OperatorException exception) {
            status.setState(ProductionState.FAILED);
            status.setMessage("GPF process failed : " + exception.getMessage());
            GpfProductionService.getProductionStatusMap().put(jobId, status);
            logger.log(Level.SEVERE, "GPF process failed...", exception);
            return false;
        } catch (Exception exception) {
            status.setState(ProductionState.FAILED);
            status.setMessage("Processing failed : " + exception.getMessage());
            GpfProductionService.getProductionStatusMap().put(jobId, status);
            logger.log(Level.SEVERE, "Processing failed...", exception);
            return false;
        }

    }
}
