package org.esa.cci.lc.wps.operations;

import static com.bc.wps.api.utils.WpsTypeConverter.str2CodeType;
import static com.bc.wps.api.utils.WpsTypeConverter.str2LanguageStringType;

import com.bc.wps.api.schema.ComplexDataCombinationType;
import com.bc.wps.api.schema.ComplexDataCombinationsType;
import com.bc.wps.api.schema.ComplexDataDescriptionType;
import com.bc.wps.api.schema.InputDescriptionType;
import com.bc.wps.api.schema.ProcessDescriptionType;
import com.bc.wps.api.schema.ProcessDescriptionType.DataInputs;
import com.bc.wps.api.schema.SupportedComplexDataInputType;
import com.bc.wps.api.utils.InputDescriptionTypeBuilder;
import com.bc.wps.api.utils.WpsTypeConverter;
import com.bc.wps.utilities.WpsLogger;
import org.esa.cci.lc.subset.PredefinedRegion;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * @author hans
 */
public class LcDescribeProcessOperation {

    public static final String INPUT_PRODUCT_NAME_PATTERN = "ESACCI-LC-L4-LCCS-Map-300m-P5Y-{2000,2005,2010}-v1.[34].nc";
    public static final String LC_CCI_INPUT_DIRECTORY = "/lc-cci_input";

    public List<ProcessDescriptionType> getProcesses(String processId) throws IOException {
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

    private DataInputs getDataInputs() throws IOException {
        DataInputs dataInputs = new DataInputs();

        List<String> allowedRegionNameList = new ArrayList<>();
        for (PredefinedRegion regionName : PredefinedRegion.values()) {
            allowedRegionNameList.add(regionName.name());
        }

        InputDescriptionType regionName = InputDescriptionTypeBuilder
                    .create()
                    .withIdentifier("regionName")
                    .withTitle("Predefined region name")
                    .withAbstract("Specifies one of the available predefined regions.")
                    .withDataType("string")
                    .withAllowedValues(allowedRegionNameList)
                    .build();
        dataInputs.getInput().add(regionName);

        InputDescriptionType north = InputDescriptionTypeBuilder
                    .create()
                    .withIdentifier("north")
                    .withTitle("The northern latitude")
                    .withAbstract("Specifies north bound of the regional subset.")
                    .withDataType("float")
                    .build();
        dataInputs.getInput().add(north);

        InputDescriptionType south = InputDescriptionTypeBuilder
                    .create()
                    .withIdentifier("south")
                    .withTitle("The southern latitude")
                    .withAbstract("Specifies south bound of the regional subset.")
                    .withDataType("float")
                    .build();
        dataInputs.getInput().add(south);

        InputDescriptionType east = InputDescriptionTypeBuilder
                    .create()
                    .withIdentifier("east")
                    .withTitle("The eastern longitude")
                    .withAbstract("Specifies east bound of the regional subset. If the grid of the source product is " +
                                  "REGULAR_GAUSSIAN_GRID coordinates the values must be between 0 and 360.")
                    .withDataType("float")
                    .build();
        dataInputs.getInput().add(east);

        InputDescriptionType west = InputDescriptionTypeBuilder
                    .create()
                    .withIdentifier("west")
                    .withTitle("The western longitude")
                    .withAbstract("Specifies west bound of the regional subset. If the grid of the source product is " +
                                  "REGULAR_GAUSSIAN_GRID coordinates the values must be between 0 and 360.")
                    .withDataType("float")
                    .build();
        dataInputs.getInput().add(west);

        List<String> inputSourceProductList = new ArrayList<>();
        Path dir = Paths.get(LcWpsConstants.WPS_ROOT + LC_CCI_INPUT_DIRECTORY);
        List<File> files = new ArrayList<>();
        DirectoryStream<Path> stream = Files.newDirectoryStream(dir, INPUT_PRODUCT_NAME_PATTERN);
        for (Path entry : stream) {
            files.add(entry.toFile());
        }
        for (File file : files) {
            inputSourceProductList.add(file.getName());
        }
        InputDescriptionType sourceProduct = InputDescriptionTypeBuilder
                    .create()
                    .withIdentifier("sourceProduct")
                    .withTitle("LC CCI map or conditions product")
                    .withAbstract("The source product to create a regional subset from")
                    .withDataType("string")
                    .withAllowedValues(inputSourceProductList)
                    .build();
        dataInputs.getInput().add(sourceProduct);

        return dataInputs;
    }
}
