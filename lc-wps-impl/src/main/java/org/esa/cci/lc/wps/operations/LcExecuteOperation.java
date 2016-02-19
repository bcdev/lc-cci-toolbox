package org.esa.cci.lc.wps.operations;

import com.bc.wps.api.WpsServiceException;
import com.bc.wps.api.schema.Execute;
import com.bc.wps.api.schema.ExecuteResponse;
import com.bc.wps.api.schema.StatusType;
import com.bc.wps.utilities.WpsLogger;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.util.StringUtils;
import org.esa.cci.lc.subset.PredefinedRegion;
import org.esa.cci.lc.wps.ExecuteRequestExtractor;
import org.esa.cci.lc.wps.exceptions.MissingInputParameterException;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author hans
 */
public class LcExecuteOperation {

    public static final String LC_CCI_INPUT_DIRECTORY = "/lc-cci_input";
    public static final String LC_CCI_OUTPUT_DIRECTORY = "/lc-cci_output";
    private Logger logger = WpsLogger.getLogger();

    public ExecuteResponse doExecute(Execute executeRequest) throws WpsServiceException {
        ExecuteRequestExtractor requestExtractor = new ExecuteRequestExtractor(executeRequest);
        Map<String, String> inputParameters = requestExtractor.getInputParametersMap();

        GPF.getDefaultInstance().getOperatorSpiRegistry().loadOperatorSpis();
        final Product sourceProduct;
        try {
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
            for(PredefinedRegion region : PredefinedRegion.values()){
                if(region.name().equals(predefinedRegionName)){
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
        } catch (IOException exception) {
            logger.log(Level.SEVERE, "Unable to perform Execute operation successfully", exception);
            throw new WpsServiceException("Unable to perform Execute operation successfully", exception);
        }

        ExecuteResponse successfulResponse = new ExecuteResponse();
        StatusType successfulStatus = new StatusType();
        successfulStatus.setProcessSucceeded("Successful");
        successfulResponse.setStatus(successfulStatus);
        return successfulResponse;
    }

}
