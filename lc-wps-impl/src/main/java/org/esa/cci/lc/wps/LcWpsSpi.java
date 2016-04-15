package org.esa.cci.lc.wps;

import com.bc.wps.api.WpsServerContext;
import com.bc.wps.api.WpsServiceInstance;
import com.bc.wps.api.WpsServiceProvider;
import com.bc.wps.api.exceptions.WpsRuntimeException;
import com.bc.wps.utilities.PropertiesWrapper;

import java.io.IOException;

/**
 * @author hans
 */
public class LcWpsSpi implements WpsServiceProvider {

    @Override
    public String getId() {
        return "lc-cci";
    }

    @Override
    public String getName() {
        return "LC CCI WPS Server";
    }

    @Override
    public String getDescription() {
        return "This is a LC CCI User Tools WPS implementation";
    }

    @Override
    public WpsServiceInstance createServiceInstance(WpsServerContext wpsServerContext) {
        String propertiesFileName = "lc-cci-wps.properties";
        try {
            PropertiesWrapper.loadConfigFile(propertiesFileName);
        } catch (IOException exception) {
            throw new WpsRuntimeException("Unable to load " + propertiesFileName + " file", exception);
        }
        return new LcWpsProvider();
    }
}
