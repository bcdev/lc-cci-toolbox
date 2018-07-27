package org.esa.cci.lc.io;

import org.esa.snap.core.dataio.ProductWriter;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.FlagCoding;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.dataio.netcdf.*;
import org.esa.snap.dataio.netcdf.metadata.ProfileInitPartWriter;
import org.esa.snap.dataio.netcdf.metadata.ProfilePartWriter;
import org.esa.snap.dataio.netcdf.metadata.profiles.beam.BeamBandPart;
import org.esa.snap.dataio.netcdf.metadata.profiles.beam.BeamInitialisationPart;
import org.esa.snap.dataio.netcdf.metadata.profiles.beam.BeamNetCdf4WriterPlugIn;
import org.esa.snap.dataio.netcdf.metadata.profiles.cf.CfFlagCodingPart;
import org.esa.snap.dataio.netcdf.nc.NFileWriteable;
import org.esa.snap.dataio.netcdf.nc.NVariable;
import org.esa.snap.dataio.netcdf.util.DataTypeUtils;
import org.esa.snap.dataio.netcdf.util.ReaderUtils;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.Variable;

import java.awt.*;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;


public class LcFireNetCDF4WriterPlugin extends BeamNetCdf4WriterPlugIn {

    public static final String FORMAT_NAME = "NetCDF4-LC-FIRE";

    @Override
    public String[] getFormatNames() {
        return new String[]{FORMAT_NAME};
    }

    @Override
    public ProductWriter createWriterInstance() {
        return new FireNetCdfWriter(this);
    }

    @Override
    public ProfilePartWriter createMetadataPartWriter() {
        return new NullProfilePartWriter();
    }
    @Override
    public ProfileInitPartWriter createInitialisationPartWriter() {
        return new LcFireInitialisationPart();
    }

    //writes all variable data which are not bands
    @Override
    public ProfilePartWriter createImageInfoPartWriter() {
        return new N4VariableWriter();
    }




    @Override
    public ProfilePartWriter createDescriptionPartWriter() {
        return new NullProfilePartWriter();
    }

    class LcFireInitialisationPart extends BeamInitialisationPart {

        @Override
        public void writeProductBody(ProfileWriteContext ctx, Product product) throws IOException {
            final NFileWriteable writeable = ctx.getNetcdfFileWriteable();
            NetcdfFileWriter writer = writeable.getWriter();

            String path = product.getFileLocation().getAbsolutePath();
            NetcdfFileWriter onlyReader = NetcdfFileWriter.openExisting(path);

            //add dimensions
            List<ucar.nc2.Dimension> dimensionList = onlyReader.getNetcdfFile().getDimensions();
            for (ucar.nc2.Dimension d : dimensionList) {
                if (!d.getFullName().equals("nv")) {
                    writeable.addDimension(d.getFullName(), d.getLength());
                }
                else{
                    writeable.addDimension("bounds",d.getLength());
                }
            }
            if (!writer.hasDimension(null,"time")) {
                writeable.addDimension("time",1);
            }


            // add global attributes
            writeable.addGlobalAttribute("geospatial_lon_min","0");
            writeable.addGlobalAttribute("geospatial_lon_max","360");
            List<Attribute> globalAttributes = onlyReader.getNetcdfFile().getGlobalAttributes();
            for (Attribute attribute : globalAttributes) {
                if (!attribute.getFullName().equals("geospatial_lon_min") && !attribute.getFullName().equals("geospatial_lon_max"))
                writeable.addGlobalAttribute(attribute.getFullName(),attribute.getStringValue());
            }
            onlyReader.close();
        }
        }


    @Override
    public ProfilePartWriter createBandPartWriter() {
        return new BeamBandPart() {
            @Override
            public void preEncode(ProfileWriteContext ctx, Product p) throws IOException {
                /// opening netcdf file in order to initialize variables
                final NFileWriteable ncFile = ctx.getNetcdfFileWriteable();
                String path = p.getFileLocation().getAbsolutePath();
                NetcdfFileWriter onlyReader = NetcdfFileWriter.openExisting(path);
                final Dimension tileSize = p.getPreferredTileSize();

                for (Band band : p.getBands()) {
                    if (!band.getName().contains("vegetation_class")) {
                        addBandVariable(ncFile, band, onlyReader);
                    } else {
                        addBandClassVariable(ncFile, band, onlyReader);
                    }
                }

                List<Variable> list = onlyReader.getNetcdfFile().getVariables();
                for (Variable variable : list) {
                    if (!Arrays.asList(p.getBandNames()).contains(variable.getFullName()) && (!variable.getFullName().contains("burned"))) {
                        addNotBandVariable(ncFile, variable);
                    }
                }

                ///flagcoding
                if (! (p.getBand("lccs_class")==null)) {
                    FlagCoding lcFlagCoding = readFlagCoding(onlyReader.findVariable("lccs_class"), "defaultcoding");
                    p.getBand("lccs_class").setSampleCoding(lcFlagCoding);
                }
                if (! (p.getBand("processed_flag")==null)) {
                    FlagCoding processedFlagCoding = readFlagCoding(onlyReader.findVariable("processed_flag"), "defaultcoding");
                    p.getBand("processed_flag").setSampleCoding(processedFlagCoding);
                }
                if (! (p.getBand("current_pixel_state")==null)) {
                    FlagCoding currentPixelStateCoding = readFlagCoding(onlyReader.findVariable("current_pixel_state"), "defaultcoding");
                    p.getBand("current_pixel_state").setSampleCoding(currentPixelStateCoding);
                }
                onlyReader.close();
            }

            private void addNotBandVariable(NFileWriteable ncFile, Variable variable) throws IOException {
                NVariable nVariable;
                String dimString = variable.getDimensionsString();
                if (dimString.contains("nv")) {
                    dimString = dimString.replace("nv", "bounds");
                }

                if (variable.getFullName().equals("lat_bnds")) {
                    nVariable = ncFile.addVariable("latitude_bounds", variable.getDataType(), variable.getDataType().isUnsigned(), null, dimString);
                } else if (variable.getFullName().equals("lon_bnds")) {
                    nVariable = ncFile.addVariable("longitude_bounds", variable.getDataType(), variable.getDataType().isUnsigned(), null, dimString);
                } else if (variable.getFullName().equals("time_bnds")) {
                    nVariable = ncFile.addVariable("time_bounds", variable.getDataType(), variable.getDataType().isUnsigned(), null, dimString);
                } else {
                    nVariable = ncFile.addVariable(variable.getFullName(), variable.getDataType(), variable.getDataType().isUnsigned(), null, dimString);
                }
                List<Attribute> attributeList = variable.getAttributes();
                for (Attribute attribute : attributeList) {
                    if (attribute.getFullName().equals("valid_min") && variable.getFullName().equals("lon")) {
                        nVariable.addAttribute("valid_min", 0);
                    } else if (attribute.getFullName().equals("valid_max") && variable.getFullName().equals("lon")) {
                        nVariable.addAttribute("valid_max", 360);
                    } else if (attribute.getLength() == 1 && !attribute.getFullName().equals("_Unsigned")) {
                        if (!(attribute.getNumericValue() == null)) {
                            nVariable.addAttribute(attribute.getFullName(), attribute.getNumericValue());
                        } else {
                            nVariable.addAttribute(attribute.getFullName(), attribute.getStringValue());
                        }
                    }
                }
            }


            private void addBandVariable(NFileWriteable ncFile, Band band, NetcdfFileWriter onlyReader) throws IOException {
                NVariable addedVariable;
                final String variableName = ReaderUtils.getVariableName(band);
                String dimString = "lat lon time";
                Dimension tilesize = band.getProduct().getPreferredTileSize();

                Variable oldVariable = onlyReader.findVariable(variableName);
                if (oldVariable.getFullName().equals("lccs_class") || oldVariable.getFullName().equals("observation_count") || oldVariable.getFullName().equals("change_count")) {
                    DataType dataType = oldVariable.getDataType().withSignedness(DataType.Signedness.UNSIGNED);
                    addedVariable = ncFile.addVariable(variableName, dataType, true, tilesize, dimString);
                } else {
                    addedVariable = ncFile.addVariable(variableName, oldVariable.getDataType(), oldVariable.getDataType().isUnsigned(), tilesize, dimString);
                }
                addedVariable.addAttribute("long_name", band.getDescription());
                addedVariable.addAttribute("standard_name", band.getName());
                if (band.getScalingOffset() != 0.0) {
                    addedVariable.addAttribute("add_offset", band.getScalingOffset());
                }
                if (band.getScalingFactor() != 1.0) {
                    addedVariable.addAttribute("scale_factor", band.getScalingFactor());
                }
                List<Attribute> attributeList = oldVariable.getAttributes();
                for (Attribute attribute : attributeList) {
                    if (attribute.getLength() == 1 && !attribute.getFullName().equals("flag_meanings") && !attribute.getFullName().equals("_Unsigned")) {
                        if (!(attribute.getNumericValue() == null)) {
                            addedVariable.addAttribute(attribute.getFullName(), attribute.getNumericValue());
                        } else {
                            addedVariable.addAttribute(attribute.getFullName(), attribute.getStringValue());
                        }
                    }
                }
            }
        };
    }

    private void addBandClassVariable(NFileWriteable ncFile, Band band, NetcdfFileWriter onlyReader)throws IOException{
        String dimString = "lat lon time vegetation_class ";
        final DataType ncDataType = DataTypeUtils.getNetcdfDataType(band.getDataType());
        final String variableName = "burned_area_in_vegetation_class";

        if (ncFile.findVariable(variableName)==null) {
            Dimension tilesize = band.getProduct().getPreferredTileSize();
            NVariable addedVariable = ncFile.addVariable(variableName,ncDataType,ncDataType.isUnsigned(),tilesize,dimString);
            Variable variable = onlyReader.findVariable(variableName);
            List<Attribute> attributeList = variable.getAttributes();
            for (Attribute attribute : attributeList) {
                if (attribute.getLength()==1 &&   !attribute.getFullName().equals("flag_meanings") && !attribute.getFullName().equals("_Unsigned") ) {
                    if (!(attribute.getNumericValue() == null)) {
                        addedVariable.addAttribute(attribute.getFullName(), attribute.getNumericValue());
                    } else {
                        addedVariable.addAttribute(attribute.getFullName(), attribute.getStringValue());
                    }
                }
            }
        }
    }

    private static FlagCoding readFlagCoding(Variable variable, String codingName) {
        final Attribute flagMasks = variable.findAttribute("flag_values");
        final int[] maskValues;
        if (flagMasks != null) {
            final Array flagMasksArray = flagMasks.getValues();
            maskValues = new int[flagMasks.getLength()];
            for (int i = 0; i < maskValues.length; i++) {
                maskValues[i] = flagMasksArray.getInt(i);
            }
        } else {
            maskValues = null;
        }
        final Attribute flagMeanings = variable.findAttribute("flag_meanings");
        final String[] flagNames;
        if (flagMeanings != null) {
            flagNames = flagMeanings.getStringValue().split(" ");
        } else {
            flagNames = null;
        }
        return createFlagCoding(codingName, maskValues, flagNames);
    }


    private static FlagCoding createFlagCoding(String codingName, int[] maskValues, String[] flagNames) {
        if (maskValues != null && flagNames != null && maskValues.length == flagNames.length) {
            final FlagCoding coding = new FlagCoding(codingName);
            for (int i = 0; i < maskValues.length; i++) {
                final String sampleName = replaceNonWordCharacters(flagNames[i]);
                final int sampleValue = maskValues[i];
                coding.addSample(sampleName, sampleValue, "");
            }
            if (coding.getNumAttributes() > 0) {
                return coding;
            }
        }
        return null;
    }

    static String replaceNonWordCharacters(String flagName) {
        return flagName.replaceAll("\\W+", "_");
    }

    @Override
    public ProfilePartWriter createFlagCodingPartWriter() {
        return new CfFlagCodingPart();
    }
}

