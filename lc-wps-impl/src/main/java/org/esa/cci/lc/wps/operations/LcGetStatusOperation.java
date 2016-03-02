package org.esa.cci.lc.wps.operations;

import com.bc.wps.api.schema.ExecuteResponse;
import org.esa.cci.lc.wps.GpfProductionService;
import org.esa.cci.lc.wps.LcExecuteResponse;
import org.esa.cci.lc.wps.ProductionState;
import org.esa.cci.lc.wps.ProductionStatus;
import org.esa.cci.lc.wps.exceptions.JobNotFoundException;

import javax.xml.datatype.DatatypeConfigurationException;

/**
 * @author hans
 */
public class LcGetStatusOperation {

    public ExecuteResponse getStatus(String jobId) throws JobNotFoundException, DatatypeConfigurationException {
        ProductionStatus status = GpfProductionService.getProductionStatusMap().get(jobId);
        if (status != null) {
            if (status.getState() == ProductionState.SUCCESSFUL) {
                return getExecuteSuccessfulResponse(status);
            } else if (status.getState() == ProductionState.FAILED) {
                return getExecuteFailedResponse(status);
            } else {
                return getExecuteInProgressResponse(status);
            }
        } else {
            throw new JobNotFoundException("Unable to retrieve the job with jobId '" + jobId + "'.");
        }
    }

    private ExecuteResponse getExecuteInProgressResponse(ProductionStatus status) throws DatatypeConfigurationException {
        LcExecuteResponse executeResponse = new LcExecuteResponse();
        return executeResponse.getStartedResponse(status);
    }

    private ExecuteResponse getExecuteFailedResponse(ProductionStatus status) throws DatatypeConfigurationException {
        LcExecuteResponse executeResponse = new LcExecuteResponse();
        return executeResponse.getFailedResponse(status);
    }

    private ExecuteResponse getExecuteSuccessfulResponse(ProductionStatus status) throws DatatypeConfigurationException {
        LcExecuteResponse executeResponse = new LcExecuteResponse();
        return executeResponse.getSuccessfulResponse(status);
    }


}
