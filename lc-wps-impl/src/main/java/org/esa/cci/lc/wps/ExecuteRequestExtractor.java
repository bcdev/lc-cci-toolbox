package org.esa.cci.lc.wps;

import com.bc.wps.api.schema.Execute;
import com.bc.wps.api.schema.InputType;

import java.util.HashMap;
import java.util.Map;

/**
 * @author hans
 */
public class ExecuteRequestExtractor {

    private final Map<String, String> inputParametersMap;

    public ExecuteRequestExtractor(Execute executeRequest) {
        this.inputParametersMap = new HashMap<>();
        extractRequest(executeRequest);
    }

    private void extractRequest(Execute executeRequest) {
        for (InputType inputType : executeRequest.getDataInputs().getInput()) {
            String inputParameterName = inputType.getIdentifier().getValue();
            String inputParameterValue = inputType.getData().getLiteralData().getValue();
            inputParametersMap.put(inputParameterName, inputParameterValue);
        }
    }

    public Map<String, String> getInputParametersMap() {
        return inputParametersMap;
    }
}
