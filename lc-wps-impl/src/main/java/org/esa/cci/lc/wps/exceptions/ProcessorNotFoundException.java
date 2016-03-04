package org.esa.cci.lc.wps.exceptions;

/**
 * @author hans
 */
public class ProcessorNotFoundException extends Exception {

    public ProcessorNotFoundException(String processorId, Throwable cause) {
        super("Unable to find process '" + processorId + "'.", cause);
    }

    public ProcessorNotFoundException(String processorId) {
        super("Unable to find process '" + processorId + "'.");
    }

}
