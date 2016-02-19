package org.esa.cci.lc.wps.operations;

import com.bc.wps.api.WpsServiceException;
import com.bc.wps.api.schema.Execute;
import com.bc.wps.api.schema.ExecuteResponse;
import com.bc.wps.api.schema.StatusType;
import com.bc.wps.utilities.WpsLogger;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.GPF;
import org.esa.cci.lc.subset.PredefinedRegion;
import org.esa.cci.lc.wps.ExecuteRequestExtractor;

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
            parameters.put("targetDir", new File(LcWpsConstants.WPS_ROOT + LC_CCI_OUTPUT_DIRECTORY));
            parameters.put("predefinedRegion", PredefinedRegion.GREENLAND);
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
