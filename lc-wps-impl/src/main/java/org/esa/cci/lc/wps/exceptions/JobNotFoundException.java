package org.esa.cci.lc.wps.exceptions;

/**
 * @author hans
 */
public class JobNotFoundException extends Exception {

    public JobNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public JobNotFoundException(String message) {
        super(message);
    }

}
