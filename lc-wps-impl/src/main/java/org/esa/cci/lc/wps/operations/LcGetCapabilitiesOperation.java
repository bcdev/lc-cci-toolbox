package org.esa.cci.lc.wps.operations;

import com.bc.wps.api.WpsRequestContext;
import com.bc.wps.api.schema.*;
import com.bc.wps.api.utils.CapabilitiesBuilder;
import com.bc.wps.api.utils.WpsTypeConverter;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.util.List;

import static org.esa.cci.lc.wps.operations.LcWpsConstants.*;

/**
 * @author hans
 */
public class LcGetCapabilitiesOperation {

    public Capabilities getCapabilities() throws JAXBException {

        return CapabilitiesBuilder.create()
                .withOperationsMetadata(getOperationsMetadata())
                .withServiceIdentification(getServiceIdentification())
                .withServiceProvider(getServiceProvider())
                .withProcessOfferings(getProcessOfferings())
                .withLanguages(getLanguages())
                .build();
    }

    protected OperationsMetadata getOperationsMetadata() {
        OperationsMetadata operationsMetadata = new OperationsMetadata();

        Operation getCapabilitiesOperation = new Operation();
        getCapabilitiesOperation.setName("GetCapabilities");
        DCP getCapabilitiesDcp = getGetDcp(WPS_GET_REQUEST_URL);
        getCapabilitiesOperation.getDCP().add(getCapabilitiesDcp);

        Operation describeProcessOperation = new Operation();
        describeProcessOperation.setName("DescribeProcess");
        DCP describeProcessDcp = getGetDcp(WPS_GET_REQUEST_URL);
        describeProcessOperation.getDCP().add(describeProcessDcp);

        Operation executeOperation = new Operation();
        executeOperation.setName("Execute");
        DCP executeDcp = getPostDcp(WPS_POST_REQUEST_URL);
        executeOperation.getDCP().add(executeDcp);

        Operation getStatusOperation = new Operation();
        getStatusOperation.setName("GetStatus");
        DCP getStatusDcp = getGetDcp(WPS_GET_REQUEST_URL);
        getStatusOperation.getDCP().add(getStatusDcp);

        operationsMetadata.getOperation().add(getCapabilitiesOperation);
        operationsMetadata.getOperation().add(describeProcessOperation);
        operationsMetadata.getOperation().add(executeOperation);
        operationsMetadata.getOperation().add(getStatusOperation);

        return operationsMetadata;
    }

    private DCP getPostDcp(String serviceUrl) {
        DCP executeDcp = new DCP();
        HTTP executeHttp = new HTTP();
        RequestMethodType executeRequestMethod = new RequestMethodType();
        executeRequestMethod.setHref(serviceUrl);
        executeHttp.setPost(executeRequestMethod);
        executeDcp.setHTTP(executeHttp);
        return executeDcp;
    }

    private DCP getGetDcp(String serviceUrl) {
        DCP describeProcessDcp = new DCP();
        HTTP describeProcessHttp = new HTTP();
        RequestMethodType describeProcessRequestMethod = new RequestMethodType();
        describeProcessRequestMethod.setHref(serviceUrl);
        describeProcessHttp.setGet(describeProcessRequestMethod);
        describeProcessDcp.setHTTP(describeProcessHttp);
        return describeProcessDcp;
    }

    protected ServiceProvider getServiceProvider() {
        ServiceProvider serviceProvider = new ServiceProvider();
        serviceProvider.setProviderName(COMPANY_NAME);

        OnlineResourceType siteUrl = new OnlineResourceType();
        siteUrl.setHref(COMPANY_WEBSITE);
        serviceProvider.setProviderSite(siteUrl);

        ResponsiblePartySubsetType contact = new ResponsiblePartySubsetType();
        contact.setIndividualName(PROJECT_MANAGER_NAME);
        contact.setPositionName(PROJECT_MANAGER_POSITION_NAME);

        ContactType contactInfo = new ContactType();

        TelephoneType phones = new TelephoneType();
        phones.getVoice().add(COMPANY_PHONE_NUMBER);
        phones.getFacsimile().add(COMPANY_FAX_NUMBER);
        contactInfo.setPhone(phones);

        AddressType address = new AddressType();
        address.getDeliveryPoint().add(COMPANY_ADDRESS);
        address.setCity(COMPANY_CITY);
        address.setAdministrativeArea(COMPANY_ADMINISTRATIVE_AREA);
        address.setPostalCode(COMPANY_POST_CODE);
        address.setCountry(COMPANY_COUNTRY);
        address.getElectronicMailAddress().add(COMPANY_EMAIL_ADDRESS);
        contactInfo.setAddress(address);

        contactInfo.setOnlineResource(siteUrl);
        contactInfo.setHoursOfService(COMPANY_SERVICE_HOURS);
        contactInfo.setContactInstructions(COMPANY_CONTACT_INSTRUCTION);

        contact.setContactInfo(contactInfo);

        CodeType role = new CodeType();
        role.setValue("PointOfContact");
        contact.setRole(role);
        serviceProvider.setServiceContact(contact);

        return serviceProvider;
    }

    protected ProcessOfferings getProcessOfferings() {
        ProcessOfferings processOfferings = new ProcessOfferings();
        ProcessBriefType singleProcessor = new ProcessBriefType();

        singleProcessor.setIdentifier(WpsTypeConverter.str2CodeType("subsetting"));
        singleProcessor.setTitle(WpsTypeConverter.str2LanguageStringType("Subsetting service"));
        singleProcessor.setAbstract(WpsTypeConverter.str2LanguageStringType("This is a Subsetting Tool for LC-CCI"));

        processOfferings.getProcess().add(singleProcessor);
        return processOfferings;
    }

    protected ServiceIdentification getServiceIdentification() {
        ServiceIdentification serviceIdentification = new ServiceIdentification();
        LanguageStringType title = new LanguageStringType();
        title.setValue(WPS_SERVICE_ID);
        serviceIdentification.setTitle(title);

        LanguageStringType abstractText = new LanguageStringType();
        abstractText.setValue(WPS_SERVICE_ABSTRACT);
        serviceIdentification.setAbstract(abstractText);

        CodeType serviceType = new CodeType();
        serviceType.setValue(WPS_SERVICE_TYPE);
        serviceIdentification.setServiceType(serviceType);

        serviceIdentification.getServiceTypeVersion().add(0, WPS_VERSION);
        return serviceIdentification;
    }

    protected Languages getLanguages() {
        Languages languages = new Languages();

        Languages.Default defaultLanguage = new Languages.Default();
        defaultLanguage.setLanguage(WPS_DEFAULT_LANG);
        languages.setDefault(defaultLanguage);

        LanguagesType languageType = new LanguagesType();
        languageType.getLanguage().add(0, WPS_SUPPORTED_LANG);
        languages.setSupported(languageType);

        return languages;
    }
}
