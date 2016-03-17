package org.esa.cci.lc.wps.operations;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import com.bc.wps.api.schema.Capabilities;
import com.bc.wps.api.schema.Languages;
import com.bc.wps.api.schema.OperationsMetadata;
import com.bc.wps.api.schema.ProcessOfferings;
import com.bc.wps.api.schema.ServiceIdentification;
import com.bc.wps.api.schema.ServiceProvider;
import com.bc.wps.utilities.PropertiesWrapper;
import org.junit.*;

/**
 * @author hans
 */
public class LcGetCapabilitiesOperationTest {

    private LcGetCapabilitiesOperation getCapabilitiesOperation;

    @Before
    public void setUp() throws Exception {
        getCapabilitiesOperation = new LcGetCapabilitiesOperation();

        PropertiesWrapper.loadConfigFile("lc-cci-wps-test.properties");
    }

    @Test
    public void canGetCapabilities() throws Exception {
        Capabilities capabilities = getCapabilitiesOperation.getCapabilities();

        assertThat(capabilities.getOperationsMetadata().getOperation().size(), equalTo(4));
        assertThat(capabilities.getServiceProvider().getProviderName(), equalTo("Brockmann-Consult"));
        assertThat(capabilities.getProcessOfferings().getProcess().size(), equalTo(1));
        assertThat(capabilities.getServiceIdentification().getTitle().getValue(), equalTo("LC CCI WPS server"));
        assertThat(capabilities.getLanguages().getDefault().getLanguage(), equalTo("EN"));
    }

    @Test
    public void canGetOperationsMetadata() throws Exception {
        OperationsMetadata operationsMetadata = getCapabilitiesOperation.getOperationsMetadata();

        assertThat(operationsMetadata.getOperation().size(), equalTo(4));
        assertThat(operationsMetadata.getOperation().get(0).getName(), equalTo("GetCapabilities"));
        assertThat(operationsMetadata.getOperation().get(0).getDCP().size(), equalTo(1));
        assertThat(operationsMetadata.getOperation().get(0).getDCP().get(0).getHTTP().getGet().getHref(),
                   equalTo("http://www.brockmann-consult.de/bc-wps/lc-cci?"));

        assertThat(operationsMetadata.getOperation().get(1).getName(), equalTo("DescribeProcess"));
        assertThat(operationsMetadata.getOperation().get(1).getDCP().size(), equalTo(1));
        assertThat(operationsMetadata.getOperation().get(1).getDCP().get(0).getHTTP().getGet().getHref(),
                   equalTo("http://www.brockmann-consult.de/bc-wps/lc-cci?"));

        assertThat(operationsMetadata.getOperation().get(2).getName(), equalTo("Execute"));
        assertThat(operationsMetadata.getOperation().get(2).getDCP().size(), equalTo(1));
        assertThat(operationsMetadata.getOperation().get(2).getDCP().get(0).getHTTP().getPost().getHref(),
                   equalTo("http://www.brockmann-consult.de/bc-wps/lc-cci"));

        assertThat(operationsMetadata.getOperation().get(3).getName(), equalTo("GetStatus"));
        assertThat(operationsMetadata.getOperation().get(3).getDCP().size(), equalTo(1));
        assertThat(operationsMetadata.getOperation().get(3).getDCP().get(0).getHTTP().getGet().getHref(),
                   equalTo("http://www.brockmann-consult.de/bc-wps/lc-cci?"));
    }

    @Test
    public void canGetServiceProvider() throws Exception {
        ServiceProvider serviceProvider = getCapabilitiesOperation.getServiceProvider();

        assertThat(serviceProvider.getProviderName(), equalTo("Brockmann-Consult"));
        assertThat(serviceProvider.getProviderSite().getHref(), equalTo("http://www.brockmann-consult.de"));

        assertThat(serviceProvider.getServiceContact().getRole().getValue(), equalTo("PointOfContact"));
        assertThat(serviceProvider.getServiceContact().getIndividualName(), equalTo("Dr. Martin Boettcher"));
        assertThat(serviceProvider.getServiceContact().getPositionName(), equalTo("Project Manager"));

        assertThat(serviceProvider.getServiceContact().getContactInfo().getPhone().getVoice().size(), equalTo(1));
        assertThat(serviceProvider.getServiceContact().getContactInfo().getPhone().getVoice().get(0), equalTo("+49 4152 889300"));
        assertThat(serviceProvider.getServiceContact().getContactInfo().getPhone().getFacsimile().size(), equalTo(1));
        assertThat(serviceProvider.getServiceContact().getContactInfo().getPhone().getFacsimile().get(0), equalTo("+49 4152 889333"));

        assertThat(serviceProvider.getServiceContact().getContactInfo().getAddress().getDeliveryPoint().get(0), equalTo("Max-Planck-Str. 2"));
        assertThat(serviceProvider.getServiceContact().getContactInfo().getAddress().getCity(), equalTo("Geesthacht"));
        assertThat(serviceProvider.getServiceContact().getContactInfo().getAddress().getAdministrativeArea(), equalTo("SH"));
        assertThat(serviceProvider.getServiceContact().getContactInfo().getAddress().getPostalCode(), equalTo("21502"));
        assertThat(serviceProvider.getServiceContact().getContactInfo().getAddress().getCountry(), equalTo("Germany"));
        assertThat(serviceProvider.getServiceContact().getContactInfo().getAddress().getElectronicMailAddress().size(), equalTo(1));
        assertThat(serviceProvider.getServiceContact().getContactInfo().getAddress().getElectronicMailAddress().get(0), equalTo("info@brockmann-consult.de"));

        assertThat(serviceProvider.getServiceContact().getContactInfo().getHoursOfService(), equalTo("24x7"));
        assertThat(serviceProvider.getServiceContact().getContactInfo().getContactInstructions(), equalTo("Don't hesitate to call"));
    }

    @Test
    public void canGetProcessOfferings() throws Exception {
        ProcessOfferings processOfferings = getCapabilitiesOperation.getProcessOfferings();

        assertThat(processOfferings.getProcess().size(), equalTo(1));
        assertThat(processOfferings.getProcess().get(0).getIdentifier().getValue(), equalTo("subsetting"));
        assertThat(processOfferings.getProcess().get(0).getTitle().getValue(), equalTo("Subsetting service"));
        assertThat(processOfferings.getProcess().get(0).getAbstract().getValue(), equalTo("This is a Subsetting Tool for LC-CCI"));
    }

    @Test
    public void canGetServiceIdentification() throws Exception {
        ServiceIdentification serviceIdentification = getCapabilitiesOperation.getServiceIdentification();

        assertThat(serviceIdentification.getTitle().getValue(), equalTo("LC CCI WPS server"));
        assertThat(serviceIdentification.getAbstract().getValue(), equalTo("Web Processing Service for LC CCI User Toolbox"));
        assertThat(serviceIdentification.getServiceType().getValue(), equalTo("WPS"));
        assertThat(serviceIdentification.getServiceTypeVersion().size(), equalTo(1));
        assertThat(serviceIdentification.getServiceTypeVersion().get(0), equalTo("1.0.0"));
    }

    @Test
    public void canGetLanguages() throws Exception {
        Languages languages = getCapabilitiesOperation.getLanguages();

        assertThat(languages.getDefault().getLanguage(), equalTo("EN"));
        assertThat(languages.getSupported().getLanguage().size(), equalTo(1));
        assertThat(languages.getSupported().getLanguage().get(0), equalTo("EN"));
    }

}