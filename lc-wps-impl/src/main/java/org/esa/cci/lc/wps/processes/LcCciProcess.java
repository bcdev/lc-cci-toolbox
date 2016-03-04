package org.esa.cci.lc.wps.processes;

import com.bc.wps.api.schema.ExecuteResponse;
import org.esa.cci.lc.wps.ProductionStatus;

/**
 * @author hans
 */
public interface LcCciProcess {

    ProductionStatus processAsynchronous(LcCciProcessBuilder processBuilder);

    ProductionStatus processSynchronous(LcCciProcessBuilder processBuilder);

    ExecuteResponse createLineageAsyncExecuteResponse(ProductionStatus status, LcCciProcessBuilder processBuilder);

    ExecuteResponse createLineageSyncExecuteResponse(ProductionStatus status, LcCciProcessBuilder processBuilder);

}
