package org.esa.cci.lc.wps.processes;

import com.bc.wps.api.WpsServerContext;
import com.bc.wps.api.schema.Execute;
import org.esa.beam.framework.datamodel.Product;

import java.nio.file.Path;
import java.util.Map;

/**
 * @author hans
 */
public class LcCciProcessBuilder {

    private String jobId;
    private Map<String, Object> parameters;
    private Product sourceProduct;
    private Path targetDirPath;
    private WpsServerContext serverContext;
    private Execute executeRequest;

    public static LcCciProcessBuilder create(){
        return new LcCciProcessBuilder();
    }

    public String getJobId() {
        return jobId;
    }

    public LcCciProcessBuilder withJobId(String jobId) {
        this.jobId = jobId;
        return this;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public LcCciProcessBuilder withParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
        return this;
    }

    public Product getSourceProduct() {
        return sourceProduct;
    }

    public LcCciProcessBuilder withSourceProduct(Product sourceProduct) {
        this.sourceProduct = sourceProduct;
        return this;
    }

    public Path getTargetDirPath() {
        return targetDirPath;
    }

    public LcCciProcessBuilder withTargetDirPath(Path targetDirPath) {
        this.targetDirPath = targetDirPath;
        return this;
    }

    public WpsServerContext getServerContext() {
        return serverContext;
    }

    public LcCciProcessBuilder withServerContext(WpsServerContext serverContext) {
        this.serverContext = serverContext;
        return this;
    }

    public Execute getExecuteRequest() {
        return executeRequest;
    }

    public LcCciProcessBuilder withExecuteRequest(Execute executeRequest) {
        this.executeRequest = executeRequest;
        return this;
    }
}
