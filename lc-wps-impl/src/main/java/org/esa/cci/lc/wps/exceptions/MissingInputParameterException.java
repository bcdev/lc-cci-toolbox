package org.esa.cci.lc.wps.exceptions;

import java.io.IOException;

/**
 * @author hans
 */
public class MissingInputParameterException extends IOException {

    public MissingInputParameterException(String message) {
        super(message);
    }

    public MissingInputParameterException(String message, Throwable cause) {
        super(message, cause);
    }

}
