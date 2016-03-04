package org.esa.cci.lc.wps.processes;

import com.bc.wps.api.schema.DocumentOutputDefinitionType;
import com.bc.wps.api.schema.ExecuteResponse;
import com.bc.wps.utilities.WpsLogger;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.cci.lc.wps.GpfProductionService;
import org.esa.cci.lc.wps.GpfTask;
import org.esa.cci.lc.wps.LcExecuteResponse;
import org.esa.cci.lc.wps.ProductionState;
import org.esa.cci.lc.wps.ProductionStatus;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author hans
 */
public class LcCciSubsettingProcess implements LcCciProcess {

    private Logger logger = WpsLogger.getLogger();

    @Override
    public ProductionStatus processAsynchronous(LcCciProcessBuilder processBuilder) {
        logger.log(Level.INFO, "[" + processBuilder.getJobId() + "] starting asynchronous process...");
        ProductionStatus status = new ProductionStatus(processBuilder.getJobId(), ProductionState.ACCEPTED, 0, "The request has been queued.", null);
        GpfProductionService.getProductionStatusMap().put(processBuilder.getJobId(), status);
        GpfTask gpfTask = new GpfTask(processBuilder.getJobId(),
                                      processBuilder.getParameters(),
                                      processBuilder.getSourceProduct(),
                                      processBuilder.getTargetDirPath().toFile(),
                                      processBuilder.getServerContext().getHostAddress(),
                                      processBuilder.getServerContext().getPort());
        GpfProductionService.getWorker().submit(gpfTask);
        logger.log(Level.INFO, "[" + processBuilder.getJobId() + "] job has been queued...");
        return status;
    }

    @Override
    public ProductionStatus processSynchronous(LcCciProcessBuilder processBuilder) {
        try {
            logger.log(Level.INFO, "[" + processBuilder.getJobId() + "] starting synchronous process...");
            GPF.createProduct("LCCCI.Subset", processBuilder.getParameters(), processBuilder.getSourceProduct());

            logger.log(Level.INFO, "[" + processBuilder.getJobId() + "] constructing result URLs...");
            List<String> resultUrls = GpfProductionService.getProductUrls(processBuilder.getServerContext().getHostAddress(),
                                                                          processBuilder.getServerContext().getPort(),
                                                                          processBuilder.getTargetDirPath().toFile());
            logger.log(Level.INFO, "[" + processBuilder.getJobId() + "] job has been completed, creating successful response...");
            return new ProductionStatus(processBuilder.getJobId(),
                                        ProductionState.SUCCESSFUL,
                                        100,
                                        "The request has been processed successfully.",
                                        resultUrls);
        } catch (OperatorException exception) {
            return new ProductionStatus(processBuilder.getJobId(),
                                        ProductionState.FAILED,
                                        0,
                                        "Processing failed : " + exception.getMessage(),
                                        null);
        }
    }

    @Override
    public ExecuteResponse createLineageAsyncExecuteResponse(ProductionStatus status, LcCciProcessBuilder processBuilder) {
        LcExecuteResponse executeAcceptedResponse = new LcExecuteResponse();
        List<DocumentOutputDefinitionType> outputType = processBuilder.getExecuteRequest().getResponseForm().getResponseDocument().getOutput();
        return executeAcceptedResponse.getAcceptedWithLineageResponse(status, processBuilder.getExecuteRequest().getDataInputs(),
                                                                      outputType, processBuilder.getServerContext());
    }

    @Override
    public ExecuteResponse createLineageSyncExecuteResponse(ProductionStatus status, LcCciProcessBuilder processBuilder) {
        LcExecuteResponse executeSuccessfulResponse = new LcExecuteResponse();
        List<DocumentOutputDefinitionType> outputType = processBuilder.getExecuteRequest().getResponseForm().getResponseDocument().getOutput();
        return executeSuccessfulResponse.getSuccessfulWithLineageResponse(status, processBuilder.getExecuteRequest().getDataInputs(), outputType);
    }
}
