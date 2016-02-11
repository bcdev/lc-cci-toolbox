package org.esa.cci.lc.wps;

import com.bc.wps.api.WpsRequestContext;
import com.bc.wps.api.WpsServiceException;
import com.bc.wps.api.WpsServiceInstance;
import com.bc.wps.api.schema.Capabilities;
import com.bc.wps.api.schema.Execute;
import com.bc.wps.api.schema.ExecuteResponse;
import com.bc.wps.api.schema.ProcessDescriptionType;

import java.util.List;

/**
 * @author hans
 */
public class LcWpsProvider implements WpsServiceInstance {

    @Override
    public Capabilities getCapabilities(WpsRequestContext wpsRequestContext) throws WpsServiceException {
        return null;
    }

    @Override
    public List<ProcessDescriptionType> describeProcess(WpsRequestContext wpsRequestContext, String s) throws WpsServiceException {
        return null;
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
