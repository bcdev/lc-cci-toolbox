package org.esa.cci.lc.wps;

import com.bc.wps.api.WpsRequestContext;
import com.bc.wps.api.WpsServiceException;
import com.bc.wps.api.WpsServiceInstance;
import com.bc.wps.api.schema.*;
import com.bc.wps.utilities.WpsLogger;
import org.esa.cci.lc.wps.operations.LcDescribeProcessOperation;
import org.esa.cci.lc.wps.operations.LcGetCapabilitiesOperation;

import javax.xml.bind.JAXBException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author hans
 */
public class LcWpsProvider implements WpsServiceInstance {

    private Logger logger = WpsLogger.getLogger();

    @Override
    public Capabilities getCapabilities(WpsRequestContext wpsRequestContext) throws WpsServiceException {
        LcGetCapabilitiesOperation getCapabilitiesOperation = new LcGetCapabilitiesOperation();
        try {
            return getCapabilitiesOperation.getCapabilities();
        } catch (JAXBException exception) {
            logger.log(Level.SEVERE, "Unable to perform GetCapabilities operation successfully", exception);
            throw new WpsServiceException("Unable to perform GetCapabilities operation successfully", exception);
        }
    }

    @Override
    public List<ProcessDescriptionType> describeProcess(WpsRequestContext wpsRequestContext, String processId) throws WpsServiceException {
        LcDescribeProcessOperation describeProcessOperation = new LcDescribeProcessOperation();
        return describeProcessOperation.getProcesses(processId);
    }

    @Override
    public ExecuteResponse doExecute(WpsRequestContext wpsRequestContext, Execute execute) throws WpsServiceException {
        return null;
    }

    @Override
    public ExecuteResponse getStatus(WpsRequestContext wpsRequestContext, String s) throws WpsServiceException {
        return null;
    }

    @Override
    public void dispose() {

    }
}
