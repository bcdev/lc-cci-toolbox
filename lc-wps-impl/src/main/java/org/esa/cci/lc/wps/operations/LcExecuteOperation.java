package org.esa.cci.lc.wps.operations;

import com.bc.wps.api.WpsRequestContext;
import com.bc.wps.api.WpsServerContext;
import com.bc.wps.api.WpsServiceException;
import com.bc.wps.api.schema.DocumentOutputDefinitionType;
import com.bc.wps.api.schema.Execute;
import com.bc.wps.api.schema.ExecuteResponse;
import com.bc.wps.api.schema.ResponseDocumentType;
import com.bc.wps.api.schema.ResponseFormType;
import com.bc.wps.utilities.WpsLogger;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.util.StringUtils;
import org.esa.cci.lc.subset.PredefinedRegion;
import org.esa.cci.lc.wps.ExecuteRequestExtractor;
import org.esa.cci.lc.wps.GpfProductionService;
import org.esa.cci.lc.wps.GpfTask;
import org.esa.cci.lc.wps.LcExecuteResponse;
import org.esa.cci.lc.wps.ProductionState;
import org.esa.cci.lc.wps.ProductionStatus;
import org.esa.cci.lc.wps.exceptions.MissingInputParameterException;
import org.esa.cci.lc.wps.utils.PropertiesWrapper;

import javax.xml.datatype.DatatypeConfigurationException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author hans
 */
public class LcExecuteOperation {

    private static final String CATALINA_BASE = System.getProperty("catalina.base");
    private Logger logger = WpsLogger.getLogger();

    public ExecuteResponse doExecute(Execute executeRequest, WpsRequestContext requestContext)
                throws WpsServiceException, IOException, DatatypeConfigurationException, OperatorException {

        WpsServerContext serverContext = requestContext.getServerContext();
        ResponseFormType responseFormType = executeRequest.getResponseForm();
        ResponseDocumentType responseDocumentType = responseFormType.getResponseDocument();
        boolean isAsynchronous = responseDocumentType.isStatus();
        boolean isLineage = responseDocumentType.isLineage();
        String processId = executeRequest.getIdentifier().getValue();

        ExecuteRequestExtractor requestExtractor = new ExecuteRequestExtractor(executeRequest);
        Map<String, String> inputParameters = requestExtractor.getInputParametersMap();

        GPF.getDefaultInstance().getOperatorSpiRegistry().loadOperatorSpis();

        final Product sourceProduct = getSourceProduct(inputParameters);
        String jobId = GpfProductionService.createJobId(requestContext.getUserName());
        Path targetDirPath = getTargetDirectoryPath(jobId);
        HashMap<String, Object> parameters = getSubsettingParameters(inputParameters, targetDirPath);
        LcExecuteResponse executeResponse = new LcExecuteResponse();

        if (isAsynchronous) {
            ProductionStatus status = processAsynchronous(jobId, parameters, sourceProduct, targetDirPath, serverContext);
            if (isLineage) {
                return createLineageAsyncExecuteResponse(executeRequest, status, serverContext);
            }
            return executeResponse.getAcceptedResponse(status, serverContext);
        } else {
            ProductionStatus status = processSynchronous(jobId, parameters, sourceProduct, targetDirPath, serverContext);
            if (status.getState() != ProductionState.SUCCESSFUL) {
                return executeResponse.getFailedResponse(status);
            }
            if (isLineage) {
                return createLineageSyncExecuteResponse(executeRequest, status);
            }
            return executeResponse.getSuccessfulResponse(status);
        }
    }

    private ProductionStatus processAsynchronous(String jobId, Map<String, Object> parameters,
                                                 Product sourceProduct, Path targetDirPath, WpsServerContext serverContext) {
        logger.log(Level.INFO, "[" + jobId + "] starting asynchronous process...");
        ProductionStatus status = new ProductionStatus(jobId, ProductionState.ACCEPTED, 0, "The request has been queued.", null);
        GpfProductionService.getProductionStatusMap().put(jobId, status);
        GpfTask gpfTask = new GpfTask(jobId,
                                      parameters,
                                      sourceProduct,
                                      targetDirPath.toFile(),
                                      serverContext.getHostAddress(),
                                      serverContext.getPort());
        GpfProductionService.getWorker().submit(gpfTask);
        logger.log(Level.INFO, "[" + jobId + "] job has been queued...");

        return status;
    }

    private ProductionStatus processSynchronous(String jobId, Map<String, Object> parameters,
                                                Product sourceProduct, Path targetDirPath,
                                                WpsServerContext serverContext) {
        try {
            logger.log(Level.INFO, "[" + jobId + "] starting synchronous process...");
            GPF.createProduct("LCCCI.Subset", parameters, sourceProduct);

            logger.log(Level.INFO, "[" + jobId + "] constructing result URLs...");
            List<String> resultUrls = GpfProductionService.getProductUrls(serverContext.getHostAddress(),
                                                                          serverContext.getPort(),
                                                                          targetDirPath.toFile());
            logger.log(Level.INFO, "[" + jobId + "] job has been completed, creating successful response...");
            return new ProductionStatus(jobId,
                                        ProductionState.SUCCESSFUL,
                                        100,
                                        "The request has been processed successfully.",
                                        resultUrls);
        } catch (OperatorException exception) {
            return new ProductionStatus(jobId,
                                        ProductionState.FAILED,
                                        0,
                                        "Processing failed : " + exception.getMessage(),
                                        null);
        }
    }

    protected ExecuteResponse createLineageAsyncExecuteResponse(Execute executeRequest, ProductionStatus status,
                                                                WpsServerContext serverContext)
                throws DatatypeConfigurationException {
        LcExecuteResponse executeAcceptedResponse = new LcExecuteResponse();
        List<DocumentOutputDefinitionType> outputType = executeRequest.getResponseForm().getResponseDocument().getOutput();
        return executeAcceptedResponse.getAcceptedWithLineageResponse(status, executeRequest.getDataInputs(),
                                                                      outputType, serverContext);
    }

    protected ExecuteResponse createLineageSyncExecuteResponse(Execute executeRequest, ProductionStatus status)
                throws DatatypeConfigurationException {
        LcExecuteResponse executeSuccessfulResponse = new LcExecuteResponse();
        List<DocumentOutputDefinitionType> outputType = executeRequest.getResponseForm().getResponseDocument().getOutput();
        return executeSuccessfulResponse.getSuccessfulWithLineageResponse(status, executeRequest.getDataInputs(), outputType);
    }

    private HashMap<String, Object> getSubsettingParameters(Map<String, String> inputParameters, Path targetDirPath)
                throws MissingInputParameterException {
        HashMap<String, Object> parameters = new HashMap<>();
        parameters.put("targetDir", targetDirPath.toString());
        String predefinedRegionName = inputParameters.get("predefinedRegion");
        PredefinedRegion predefinedRegion = null;
        for (PredefinedRegion region : PredefinedRegion.values()) {
            if (region.name().equals(predefinedRegionName)) {
                predefinedRegion = region;
            }
        }
        if (predefinedRegion != null) {
            parameters.put("predefinedRegion", predefinedRegion);
        } else if (StringUtils.isNotNullAndNotEmpty(inputParameters.get("north")) &&
                   StringUtils.isNotNullAndNotEmpty(inputParameters.get("west")) &&
                   StringUtils.isNotNullAndNotEmpty(inputParameters.get("east")) &&
                   StringUtils.isNotNullAndNotEmpty(inputParameters.get("south"))) {
            parameters.put("north", inputParameters.get("north"));
            parameters.put("west", inputParameters.get("west"));
            parameters.put("east", inputParameters.get("east"));
            parameters.put("south", inputParameters.get("south"));
        } else {
            throw new MissingInputParameterException("The region is not properly defined in the request.");
        }
        return parameters;
    }

    private Path getTargetDirectoryPath(String jobId) throws IOException {
        Path targetDirectoryPath = Paths.get(CATALINA_BASE + PropertiesWrapper.get("wps.application.path")
                                             + "/" + PropertiesWrapper.get("lc.cci.output.directory"),
                                             jobId);
        Files.createDirectories(targetDirectoryPath);
        return targetDirectoryPath;
    }

    private Product getSourceProduct(Map<String, String> inputParameters) throws IOException {
        final Product sourceProduct;
        Path dir = Paths.get(CATALINA_BASE + PropertiesWrapper.get("wps.application.path"), PropertiesWrapper.get("lc.cci.input.directory"));
        List<File> files = new ArrayList<>();
        DirectoryStream<Path> stream = Files.newDirectoryStream(dir, inputParameters.get("sourceProduct"));
        for (Path entry : stream) {
            files.add(entry.toFile());
        }

        String sourceProductPath;
        if (files.size() != 0) {
            sourceProductPath = files.get(0).getAbsolutePath();
        } else {
            throw new FileNotFoundException("The source product '" + inputParameters.get("sourceProduct") + "' cannot be found");
        }

        sourceProduct = ProductIO.readProduct(sourceProductPath);
        return sourceProduct;
    }
}
