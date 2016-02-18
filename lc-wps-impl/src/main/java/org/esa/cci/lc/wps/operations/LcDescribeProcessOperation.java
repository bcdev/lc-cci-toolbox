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
import org.esa.cci.lc.subset.LcSubsetOp;
import org.esa.cci.lc.subset.PredefinedRegion;

import java.util.ArrayList;
import java.util.List;

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

    private LcSubsetOp createLcSubsetOp() {
        return (LcSubsetOp) new LcSubsetOp.Spi().createOperator();
    }

    private DataInputs getDataInputs() {
        DataInputs dataInputs = new DataInputs();

//        LcSubsetOp lcSubsetOp = createLcSubsetOp();

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

        InputDescriptionType sourceProduct = getSourceProductDataInputType();
        dataInputs.getInput().add(sourceProduct);

        return dataInputs;
    }

    private InputDescriptionType getSourceProductDataInputType() {
        InputDescriptionType sourceImage = new InputDescriptionType();
        sourceImage.setIdentifier(WpsTypeConverter.str2CodeType("sourceProduct"));
        sourceImage.setTitle(WpsTypeConverter.str2LanguageStringType("LC CCI map or conditions product"));
        sourceImage.setAbstract(WpsTypeConverter.str2LanguageStringType("The source product to create a regional subset from"));

        SupportedComplexDataInputType supportedSourceProductDataInputType = new SupportedComplexDataInputType();
        ComplexDataCombinationType sourceProductDataInputType = new ComplexDataCombinationType();
        ComplexDataDescriptionType netCdfType = new ComplexDataDescriptionType();
        netCdfType.setMimeType("image/netCDF");
        sourceProductDataInputType.setFormat(netCdfType);
        ComplexDataCombinationsType sourceProductDataInputTypes = new ComplexDataCombinationsType();
        sourceProductDataInputTypes.getFormat().add(netCdfType);
        supportedSourceProductDataInputType.setDefault(sourceProductDataInputType);
        supportedSourceProductDataInputType.setSupported(sourceProductDataInputTypes);
        sourceImage.setComplexData(supportedSourceProductDataInputType);
        return sourceImage;
    }
}
