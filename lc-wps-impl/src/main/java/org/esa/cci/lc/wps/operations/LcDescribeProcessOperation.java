package org.esa.cci.lc.wps.operations;

import com.bc.wps.api.schema.InputDescriptionType;
import com.bc.wps.api.schema.ProcessDescriptionType;
import com.bc.wps.api.schema.ProcessDescriptionType.DataInputs;
import com.bc.wps.api.utils.InputDescriptionTypeBuilder;
import org.esa.cci.lc.subset.PredefinedRegion;

import java.util.ArrayList;
import java.util.List;

import static com.bc.wps.api.utils.WpsTypeConverter.str2CodeType;
import static com.bc.wps.api.utils.WpsTypeConverter.str2LanguageStringType;

/**
 * @author hans
 */
public class LcDescribeProcessOperation {


    public List<ProcessDescriptionType> getProcesses(String processId) {
        // TODO add a better way to input available processes
        List<ProcessDescriptionType> processes = new ArrayList<>();

        ProcessDescriptionType subsettingProcess = new ProcessDescriptionType();
        subsettingProcess.setStoreSupported(true);
        subsettingProcess.setStatusSupported(true);
        subsettingProcess.setProcessVersion("1.0");
        subsettingProcess.setIdentifier(str2CodeType("subsetting"));
        subsettingProcess.setTitle(str2LanguageStringType("Subsetting service"));
        subsettingProcess.setAbstract(str2LanguageStringType("This is a Subsetting Tool for LC-CCI"));

        DataInputs dataInputs = getDataInputs();
        subsettingProcess.setDataInputs(dataInputs);

        processes.add(subsettingProcess);
        return processes;
    }

    private DataInputs getDataInputs() {
        DataInputs dataInputs = new DataInputs();

        // TODO find a better way to get the input parameters
        List<String> allowedRegionNameList = new ArrayList<>();
        allowedRegionNameList.add("North America");
        allowedRegionNameList.add("Central America");
        allowedRegionNameList.add("South America");
        allowedRegionNameList.add("Western Europe and Mediterranean");
        allowedRegionNameList.add("Asia");
        allowedRegionNameList.add("Africa");
        allowedRegionNameList.add("South East Asia");
        allowedRegionNameList.add("Australia and New Zealand");
        allowedRegionNameList.add("Greenland");

        InputDescriptionType regionName = InputDescriptionTypeBuilder
                .create()
                .withIdentifier("regionName")
                .withTitle("Predefined region name")
                .withDataType("string")
                .withAllowedValues(allowedRegionNameList)
                .build();

        dataInputs.getInput().add(regionName);
        return dataInputs;
    }
}
