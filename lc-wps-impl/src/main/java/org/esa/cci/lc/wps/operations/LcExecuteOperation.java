package org.esa.cci.lc.wps.operations;

import com.bc.wps.api.WpsServerContext;
import com.bc.wps.api.WpsServiceException;
import com.bc.wps.api.schema.CodeType;
import com.bc.wps.api.schema.Execute;
import com.bc.wps.api.schema.ExecuteResponse;
import com.bc.wps.api.schema.OutputDataType;
import com.bc.wps.api.schema.OutputDefinitionsType;
import com.bc.wps.api.schema.OutputReferenceType;
import com.bc.wps.api.schema.StatusType;
import com.bc.wps.utilities.WpsLogger;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.util.StringUtils;
import org.esa.cci.lc.subset.PredefinedRegion;
import org.esa.cci.lc.wps.ExecuteRequestExtractor;
import org.esa.cci.lc.wps.exceptions.MissingInputParameterException;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * @author hans
 */
public class LcExecuteOperation {

    public static final String LC_CCI_INPUT_DIRECTORY = "/lc-cci_input";
    public static final String LC_CCI_OUTPUT_DIRECTORY = "/lc-cci_output";
    private static final String APP_NAME = "/bc-wps";
    private Logger logger = WpsLogger.getLogger();

    public ExecuteResponse doExecute(Execute executeRequest, WpsServerContext serverContext)
                throws WpsServiceException, IOException, DatatypeConfigurationException {
        ExecuteRequestExtractor requestExtractor = new ExecuteRequestExtractor(executeRequest);
        Map<String, String> inputParameters = requestExtractor.getInputParametersMap();

        GPF.getDefaultInstance().getOperatorSpiRegistry().loadOperatorSpis();
        final Product sourceProduct;
        Path dir = Paths.get(LcWpsConstants.WPS_ROOT + LC_CCI_INPUT_DIRECTORY);
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
        HashMap<String, Object> parameters = new HashMap<>();
        File targetDir = new File(LcWpsConstants.WPS_ROOT + LC_CCI_OUTPUT_DIRECTORY, new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()));
        if (!targetDir.mkdir()) {
            throw new FileSystemException("Unable to create a new directory '" + targetDir.getAbsolutePath() + "'.");
        }
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
        GPF.createProduct("LCCCI.Subset", parameters, sourceProduct);

        ExecuteResponse successfulResponse = new ExecuteResponse();
        StatusType statusType = new StatusType();
        XMLGregorianCalendar currentTime = getXmlGregorianCalendar();
        statusType.setCreationTime(currentTime);
        statusType.setProcessSucceeded("The request has been processed successfully.");
        successfulResponse.setStatus(statusType);

        List<String> resultUrls = new ArrayList<>();
        String[] resultProductNames = targetDir.list();
        for (String filename : resultProductNames) {
            String productUrl = "http://"
                                + serverContext.getHostAddress()
                                + ":" + serverContext.getPort()
                                + APP_NAME
                                + LC_CCI_OUTPUT_DIRECTORY
                                + "/" + targetDir.getName()
                                + "/" + filename;
            resultUrls.add(productUrl);
        }
        ExecuteResponse.ProcessOutputs productUrl = getProcessOutputs(resultUrls);
        successfulResponse.setProcessOutputs(productUrl);
        OutputDefinitionsType outputDefinitionsType = new OutputDefinitionsType();
        successfulResponse.setOutputDefinitions(outputDefinitionsType);
        return successfulResponse;
    }

    private ExecuteResponse.ProcessOutputs getProcessOutputs(List<String> resultUrls) {
        ExecuteResponse.ProcessOutputs productUrl = new ExecuteResponse.ProcessOutputs();

        for (String productionResultUrl : resultUrls) {
            OutputDataType url = new OutputDataType();
            CodeType outputId = new CodeType();
            outputId.setValue("productionResults");
            url.setIdentifier(outputId);
            OutputReferenceType urlLink = new OutputReferenceType();
            urlLink.setHref(productionResultUrl);
            urlLink.setMimeType("binary");
            url.setReference(urlLink);

            productUrl.getOutput().add(url);
        }
        return productUrl;
    }

    private XMLGregorianCalendar getXmlGregorianCalendar() throws DatatypeConfigurationException {
        GregorianCalendar gregorianCalendar = new GregorianCalendar();
        return DatatypeFactory.newInstance().newXMLGregorianCalendar(gregorianCalendar);
    }

}
