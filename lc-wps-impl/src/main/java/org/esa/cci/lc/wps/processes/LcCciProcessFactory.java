package org.esa.cci.lc.wps.processes;

import org.esa.cci.lc.wps.exceptions.ProcessorNotFoundException;

/**
 * @author hans
 */
public class LcCciProcessFactory {

    public static LcCciProcess getProcessor(String processId) throws ProcessorNotFoundException {
        if ("subsetting".equals(processId)) {
            return new LcCciSubsettingProcess();
        } else {
            throw new ProcessorNotFoundException(processId);
        }
    }

}
