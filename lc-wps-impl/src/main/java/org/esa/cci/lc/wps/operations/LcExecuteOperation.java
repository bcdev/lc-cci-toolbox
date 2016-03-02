package org.esa.cci.lc.wps.operations;

import com.bc.wps.api.WpsServerContext;
import com.bc.wps.api.WpsServiceException;
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
import java.nio.file.FileSystemException;
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

    public ExecuteResponse doExecute(Execute executeRequest, WpsServerContext serverContext)
                throws WpsServiceException, IOException, DatatypeConfigurationException, OperatorException {

        ResponseFormType responseFormType = executeRequest.getResponseForm();
        ResponseDocumentType responseDocumentType = responseFormType.getResponseDocument();
        boolean isAsynchronous = responseDocumentType.isStatus();
        boolean isLineage = responseDocumentType.isLineage();
        String processId = executeRequest.getIdentifier().getValue();

        ExecuteRequestExtractor requestExtractor = new ExecuteRequestExtractor(executeRequest);
        Map<String, String> inputParameters = requestExtractor.getInputParametersMap();

        GPF.getDefaultInstance().getOperatorSpiRegistry().loadOperatorSpis();

        final Product sourceProduct = getSourceProduct(inputParameters);
        String jobId = GpfProductionService.createJobId();
        File targetDir = getTargetDirectory(jobId);
        HashMap<String, Object> parameters = getSubsettingParameters(inputParameters, targetDir);

        if (isAsynchronous) {
            return processAsynchronous(jobId, parameters, sourceProduct, targetDir, serverContext);
        } else {
            return processSynchronous(jobId, parameters, sourceProduct, targetDir, serverContext);
        }
    }

    private ExecuteResponse processAsynchronous(String jobId, Map<String, Object> parameters,
                                                Product sourceProduct, File targetDir, WpsServerContext serverContext)
                throws DatatypeConfigurationException {
        logger.log(Level.INFO, "starting asynchronous process...");
        ProductionStatus status = new ProductionStatus(jobId, ProductionState.ACCEPTED, 0, "The request has been queued.", null);
        GpfProductionService.getProductionStatusMap().put(jobId, status);
        GpfTask gpfTask = new GpfTask(jobId, parameters, sourceProduct, targetDir, serverContext.getHostAddress(), serverContext.getPort());
        GpfProductionService.getWorker().submit(gpfTask);

        LcExecuteResponse executeResponse = new LcExecuteResponse();
        return executeResponse.getAcceptedResponse(status, serverContext);
    }

    private ExecuteResponse processSynchronous(String jobId, Map<String, Object> parameters, Product sourceProduct, File targetDir, WpsServerContext serverContext)
                throws IOException, DatatypeConfigurationException {

        try {
            logger.log(Level.INFO, "starting synchronous process...");
            GPF.createProduct("LCCCI.Subset", parameters, sourceProduct);

            List<String> resultUrls = GpfProductionService.getProductUrls(serverContext.getHostAddress(), serverContext.getPort(), targetDir);
            ProductionStatus status = new ProductionStatus(jobId,
                                                           ProductionState.SUCCESSFUL,
                                                           100,
                                                           "The request has been processed successfully.",
                                                           resultUrls);
            LcExecuteResponse executeResponse = new LcExecuteResponse();
            return executeResponse.getSuccessfulResponse(status);
        } catch (OperatorException exception) {
            ProductionStatus status = new ProductionStatus(jobId,
                                                           ProductionState.FAILED,
                                                           0,
                                                           "Processing failed : " + exception.getMessage(),
                                                           null);
            LcExecuteResponse executeResponse = new LcExecuteResponse();
            return executeResponse.getFailedResponse(status);
        }
    }

    private HashMap<String, Object> getSubsettingParameters(Map<String, String> inputParameters, File targetDir) throws MissingInputParameterException {
        HashMap<String, Object> parameters = new HashMap<>();
        parameters.put("targetDir", targetDir);
        String predefinedRegionName = inputParameters.get("predefinedRegion");
        PredefinedRegion predefinedRegion = null;
        for (PredefinedRegion region : PredefinedRegion.values()) {
            if (region.name().equals(predefinedRegionName)) {
                predefinedRegion = region;
            }
        }
        if (predefinedRegion != null) {
            parameters.put("predefinedRegion", predefinedRegion);
        } else if (StringUtils.isNotNullAndNotEmpty(inputParameters.get("north")) ||
                   StringUtils.isNotNullAndNotEmpty(inputParameters.get("west")) ||
                   StringUtils.isNotNullAndNotEmpty(inputParameters.get("east")) ||
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

    private File getTargetDirectory(String jobId) throws FileSystemException {
        File targetDir = new File(CATALINA_BASE + PropertiesWrapper.get("wps.application.path")
                                  + "/" + PropertiesWrapper.get("lc.cci.output.directory"),
                                  jobId);
        if (!targetDir.mkdir()) {
            throw new FileSystemException("Unable to create a new directory '" + targetDir.getAbsolutePath() + "'.");
        }
        return targetDir;
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
