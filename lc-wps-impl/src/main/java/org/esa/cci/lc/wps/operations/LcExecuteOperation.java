package org.esa.cci.lc.wps.operations;

import com.bc.wps.api.WpsServiceException;
import com.bc.wps.api.schema.Execute;
import com.bc.wps.api.schema.ExecuteResponse;
import com.bc.wps.api.schema.InputType;
import com.bc.wps.api.schema.ProcessDescriptionType;
import com.bc.wps.api.schema.StatusType;
import com.bc.wps.utilities.WpsLogger;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.GPF;
import org.esa.cci.lc.subset.PredefinedRegion;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author hans
 */
public class LcExecuteOperation {
    private Logger logger = WpsLogger.getLogger();

    public ExecuteResponse doExecute(Execute executeRequest) throws WpsServiceException {
        for(InputType inputType : executeRequest.getDataInputs().getInput()){
            System.out.println("inputType.getIdentifier() = " + inputType.getIdentifier().getValue());
            System.out.println("inputType.getData().getLiteralData().getValue() = " + inputType.getData().getLiteralData().getValue());
        }

        GPF.getDefaultInstance().getOperatorSpiRegistry().loadOperatorSpis();
        final Product sourceProduct;
        try {
            sourceProduct = ProductIO.readProduct("C:\\Personal\\CabLab\\EO data\\ESACCI-LC-L4-LCCS-Map-300m-P5Y-2000-v1.3.nc");
            HashMap<String, Object> parameters = new HashMap<>();
            parameters.put("targetDir", new File("."));
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
