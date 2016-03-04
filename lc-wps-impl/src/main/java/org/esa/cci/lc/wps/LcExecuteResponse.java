package org.esa.cci.lc.wps;

import com.bc.wps.api.WpsRuntimeException;
import com.bc.wps.api.WpsServerContext;
import com.bc.wps.api.schema.CodeType;
import com.bc.wps.api.schema.DataInputsType;
import com.bc.wps.api.schema.DocumentOutputDefinitionType;
import com.bc.wps.api.schema.ExceptionReport;
import com.bc.wps.api.schema.ExceptionType;
import com.bc.wps.api.schema.ExecuteResponse;
import com.bc.wps.api.schema.OutputDataType;
import com.bc.wps.api.schema.OutputDefinitionsType;
import com.bc.wps.api.schema.OutputReferenceType;
import com.bc.wps.api.schema.ProcessFailedType;
import com.bc.wps.api.schema.ProcessStartedType;
import com.bc.wps.api.schema.StatusType;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.GregorianCalendar;
import java.util.List;

/**
 * @author hans
 */
public class LcExecuteResponse {

    private ExecuteResponse executeResponse;

    public LcExecuteResponse() {
        this.executeResponse = new ExecuteResponse();
        this.executeResponse.setService("WPS");
        this.executeResponse.setVersion("1.0.0");
        this.executeResponse.setLang("en");
    }

    public ExecuteResponse getAcceptedResponse(ProductionStatus status, WpsServerContext context) {
        StatusType statusType = new StatusType();
        XMLGregorianCalendar currentTime = getXmlGregorianCalendar();
        statusType.setCreationTime(currentTime);
        statusType.setProcessAccepted("The request has been accepted. The status of the process can be found in the URL.");
        executeResponse.setStatus(statusType);

        String getStatusUrl = getStatusUrl(status.getJobId(), context);
        executeResponse.setStatusLocation(getStatusUrl);

        return executeResponse;
    }

    public ExecuteResponse getAcceptedWithLineageResponse(ProductionStatus status,
                                                          DataInputsType dataInputs,
                                                          List<DocumentOutputDefinitionType> rawDataOutput,
                                                          WpsServerContext context) {
        StatusType statusType = new StatusType();
        XMLGregorianCalendar currentTime = getXmlGregorianCalendar();
        statusType.setCreationTime(currentTime);
        statusType.setProcessAccepted("The request has been accepted. The status of the process can be found in the URL.");
        executeResponse.setStatus(statusType);
        String getStatusUrl = getStatusUrl(status.getJobId(), context);
        executeResponse.setStatusLocation(getStatusUrl);
        executeResponse.setDataInputs(dataInputs);
        OutputDefinitionsType outputDefinitionsType = new OutputDefinitionsType();
        outputDefinitionsType.getOutput().addAll(rawDataOutput);
        executeResponse.setOutputDefinitions(outputDefinitionsType);

        return executeResponse;
    }

    public ExecuteResponse getSuccessfulResponse(ProductionStatus status) {
        StatusType statusType = new StatusType();
        XMLGregorianCalendar currentTime = getXmlGregorianCalendar();
        statusType.setCreationTime(currentTime);
        statusType.setProcessSucceeded("The request has been processed successfully.");
        executeResponse.setStatus(statusType);

        ExecuteResponse.ProcessOutputs productUrl = getProcessOutputs(status.getResultUrls());
        executeResponse.setProcessOutputs(productUrl);

        return executeResponse;
    }

    public ExecuteResponse getSuccessfulWithLineageResponse(ProductionStatus status,
                                                            DataInputsType dataInputs,
                                                            List<DocumentOutputDefinitionType> outputType) {
        StatusType statusType = new StatusType();
        XMLGregorianCalendar currentTime = getXmlGregorianCalendar();
        statusType.setCreationTime(currentTime);
        statusType.setProcessSucceeded("The request has been processed successfully.");
        executeResponse.setStatus(statusType);

        ExecuteResponse.ProcessOutputs productUrl = getProcessOutputs(status.getResultUrls());
        executeResponse.setProcessOutputs(productUrl);
        executeResponse.setDataInputs(dataInputs);
        OutputDefinitionsType outputDefinitionsType = new OutputDefinitionsType();
        outputDefinitionsType.getOutput().addAll(outputType);
        executeResponse.setOutputDefinitions(outputDefinitionsType);

        return executeResponse;
    }

    public ExecuteResponse getFailedResponse(ProductionStatus status) {
        StatusType statusType = new StatusType();
        XMLGregorianCalendar currentTime = getXmlGregorianCalendar();
        statusType.setCreationTime(currentTime);

        ProcessFailedType processFailedType = new ProcessFailedType();
        ExceptionReport exceptionReport = new ExceptionReport();
        exceptionReport.setVersion("1");
        ExceptionType exceptionType = new ExceptionType();
        exceptionType.getExceptionText().add(status.getMessage());
        exceptionType.setExceptionCode("NoApplicableCode");
        exceptionReport.getException().add(exceptionType);
        processFailedType.setExceptionReport(exceptionReport);
        statusType.setProcessFailed(processFailedType);
        executeResponse.setStatus(statusType);

        return executeResponse;
    }

    public ExecuteResponse getStartedResponse(ProductionStatus status) {
        StatusType statusType = new StatusType();
        XMLGregorianCalendar currentTime = getXmlGregorianCalendar();
        statusType.setCreationTime(currentTime);

        ProcessStartedType processStartedType = new ProcessStartedType();
        processStartedType.setValue(status.getState().toString());
        processStartedType.setPercentCompleted(Math.round(status.getProgress()));
        statusType.setProcessStarted(processStartedType);
        executeResponse.setStatus(statusType);

        return executeResponse;
    }

    private ExecuteResponse.ProcessOutputs getProcessOutputs(List<String> resultUrls) {
        ExecuteResponse.ProcessOutputs productUrl = new ExecuteResponse.ProcessOutputs();

        for (String productionResultUrl : resultUrls) {
            OutputDataType url = new OutputDataType();
            CodeType outputId = new CodeType();
            outputId.setValue("productionResults");
            url.setIdentifier(outputId);
            OutputReferenceType urlLink = new OutputReferenceType();
            urlLink.setHref(productionResultUrl);
            urlLink.setMimeType("binary");
            url.setReference(urlLink);

            productUrl.getOutput().add(url);
        }
        return productUrl;
    }

    private XMLGregorianCalendar getXmlGregorianCalendar() {
        GregorianCalendar gregorianCalendar = new GregorianCalendar();
        try {
            return DatatypeFactory.newInstance().newXMLGregorianCalendar(gregorianCalendar);
        } catch (DatatypeConfigurationException exception) {
            throw new WpsRuntimeException("Unable to create a date in WPS response XML", exception);
        }
    }

    private String getStatusUrl(String jobId, WpsServerContext context) {
        return context.getRequestUrl() + "?Service=WPS&Request=GetStatus&JobId=" + jobId;
    }

}
