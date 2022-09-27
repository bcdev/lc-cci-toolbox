package org.esa.cci.lc.io;

import org.esa.cci.lc.util.CdsVariableWriter;
import org.esa.cci.lc.util.LcHelper;
import org.esa.snap.core.dataio.ProductWriter;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.dataio.netcdf.NullProfilePartWriter;
import org.esa.snap.dataio.netcdf.ProfileWriteContext;
import org.esa.snap.dataio.netcdf.metadata.ProfileInitPartWriter;
import org.esa.snap.dataio.netcdf.metadata.ProfilePartWriter;
import org.esa.snap.dataio.netcdf.metadata.profiles.beam.BeamBandPart;
import org.esa.snap.dataio.netcdf.metadata.profiles.beam.BeamInitialisationPart;
import org.esa.snap.dataio.netcdf.metadata.profiles.beam.BeamNetCdf4WriterPlugIn;
import org.esa.snap.dataio.netcdf.metadata.profiles.cf.CfIndexCodingPart;
import org.esa.snap.dataio.netcdf.nc.NFileWriteable;
import org.esa.snap.dataio.netcdf.nc.NVariable;
import org.esa.snap.dataio.netcdf.util.DataTypeUtils;
import org.esa.snap.dataio.netcdf.util.ReaderUtils;
import ucar.ma2.DataType;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.Variable;

import java.awt.*;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;


public class LcPftAggregateNetCdf4WriterPlugin extends BeamNetCdf4WriterPlugIn {

    public static final String FORMAT_NAME = "NetCDF4-LC-PFT-Aggregate";

    @Override
    public String[] getFormatNames() {
        return new String[]{FORMAT_NAME};
    }

    @Override
    public ProductWriter createWriterInstance() {
        return new CdsNetCdfWriter(this);
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
        return new CdsVariableWriter();
    }

    @Override
    public ProfilePartWriter createDescriptionPartWriter() {
        return new NullProfilePartWriter();
    }

    @Override
    public ProfilePartWriter createGeoCodingPartWriter() {
        return new NullProfilePartWriter();
    }

    @Override
    public ProfilePartWriter createIndexCodingPartWriter() {
        return new CfIndexCodingPart();
    }


    class LcFireInitialisationPart extends BeamInitialisationPart {

        @Override
        public void writeProductBody(ProfileWriteContext ctx, Product product) throws IOException {
            final NFileWriteable writeable = ctx.getNetcdfFileWriteable();
            NetcdfFileWriter onlyReader;
            MetadataElement element = product.getMetadataRoot().getElement("global_attributes");
            NetcdfFileWriter writer = writeable.getWriter();
            String path;
            //case of aggregated PFT
            if (element == null && product.getName().contains("aggregated") && product.getName().contains("PFT") ) {
                path = product.getName().replace("ESACCI-LC-L4-PFT-Map-300m-P1Y-aggregated","ESACCI-LC-L4-PFT-Map-300m-P1Y");
                MetadataElement globalAttributes = new MetadataElement("global_attributes");
                product.getMetadataRoot().addElement(globalAttributes);
                element = product.getMetadataRoot().getElement("global_attributes");
                setAggregatedPFTAttributes(writeable, element, path);
            }
            else {
                path = element.getAttributeString("parent_path");
            }

            onlyReader = NetcdfFileWriter.openExisting(path);
                //add dimensions
            List<ucar.nc2.Dimension> dimensionList = onlyReader.getNetcdfFile().getDimensions();
            for (ucar.nc2.Dimension d : dimensionList) {
                if (d.getFullName().equals("lon")){
                        writeable.addDimension(d.getFullName(), product.getSceneRasterWidth());
                    }
                    else if (d.getFullName().equals("lat")){
                        writeable.addDimension(d.getFullName(), product.getSceneRasterHeight());
                    }
                    else if (!d.getFullName().equals("nv")) {
                        writeable.addDimension(d.getFullName(), d.getLength());
                    }
                }
            onlyReader.close();


            if (!writer.hasDimension(null, "time")) {
                writeable.addDimension("time", 1);
            }
            if (!writer.hasDimension(null, "bounds")) {
                writeable.addDimension("bounds", 2);
            }
            if (!writer.hasDimension(null, "lon")) {
                writeable.addDimension("lon", product.getSceneRasterWidth());
            }
            if (!writer.hasDimension(null, "lat")) {
                writeable.addDimension("lat", product.getSceneRasterHeight());
            }

            // add global attributes
             if (element.getAttributeString("type").equals("PFT_product") || element.getAttributeString("type").equals("ESACCI-LC-L4-PFT-Map-300m-P1Y") ) {
                writePFTGlobalAttribute(writeable, element);
            }

        }
    }


    @Override
    public ProfilePartWriter createBandPartWriter() {
        return new BeamBandPart() {
            @Override
            public void preEncode(ProfileWriteContext ctx, Product p) throws IOException {
                /// opening netcdf file in order to initialize variables
                final NFileWriteable ncFile = ctx.getNetcdfFileWriteable();
                //String path = p.getFileLocation().getAbsolutePath();
                MetadataElement element = p.getMetadataRoot().getElement("global_attributes");
                String path = element.getAttributeString("parent_path");

                final Dimension tileSize = new Dimension(2025, 2025);

                NetcdfFileWriter onlyReader = NetcdfFileWriter.openExisting(path);


                for (Band band : p.getBands()) {
                        if (onlyReader.findVariable(band.getName())!=null ) {
                            if (!band.getName().contains("sigma") || !band.getName().contains("num")) {
                                    addBandVariable(ncFile, band, onlyReader, tileSize);
                                }

                        }
                    }


                    List<Variable> list = onlyReader.getNetcdfFile().getVariables();
                    for (Variable variable : list) {
                        if (!Arrays.asList(p.getBandNames()).contains(variable.getFullName()) && (!variable.getFullName().contains("burned_area_in_vegetation_class")) &&(! variable.getFullName().contains("sigma")) ) {
                            addNotBandVariable(ncFile, variable);
                        }
                    }
                onlyReader.close();



                if ((ctx.getNetcdfFileWriteable().getWriter().findVariable("lat_bounds") == null)) {
                    addCustomVariable(ncFile, "lat_bounds", "lat bounds", DataType.DOUBLE,null,element);
                }
                if ((ctx.getNetcdfFileWriteable().getWriter().findVariable("lon_bounds") == null)) {
                    addCustomVariable(ncFile, "lon_bounds", "lon bounds", DataType.DOUBLE,null,element);
                }
                if ((ctx.getNetcdfFileWriteable().getWriter().findVariable("time_bounds") == null)) {
                    addCustomVariable(ncFile, "time_bounds", "time bounds", DataType.DOUBLE,null,element);
                }
                if ((ctx.getNetcdfFileWriteable().getWriter().findVariable("time") == null)) {
                    addCustomVariable(ncFile, "time", "time", DataType.DOUBLE,null,element);
                }
            }
        };
    }


    public static void addCustomVariable(NFileWriteable ncFile, String variableName, String dimString, DataType dataType,Dimension tileSize, MetadataElement element) throws IOException {
        //needed to initialize variables which didnt exist before.
        String[] listPFTVariables = {"BARE","BUILT","GRASS-MAN","GRASS-NAT","SHRUBS-BD","SHRUBS-BE","SHRUBS-ND","SHRUBS-NE","WATER_INLAND",
                "SNOWICE","TREES-BD","TREES-BE","TREES-ND","TREES-NE","WATER","LAND","WATER_OCEAN"};

        NVariable nVariable = ncFile.addVariable(variableName, dataType, dataType.isUnsigned(), tileSize, dimString);
        if (variableName.equals("time")) {
            nVariable.addAttribute("standard_name", "time");
            nVariable.addAttribute("long_name", "time");
            nVariable.addAttribute("axis", "T");
            nVariable.addAttribute("calendar", "standard");
            nVariable.addAttribute("units", "days since 1970-01-01 00:00:00");
            nVariable.addAttribute("bounds", "time_bounds");
        }
        else if (variableName.equals("lat")){
            nVariable.addAttribute("standard_name", "latitude");
            nVariable.addAttribute("units", "degrees_north");
            nVariable.addAttribute("axis", "Y");
            nVariable.addAttribute("long_name", "latitude");
            nVariable.addAttribute("bounds", "lat_bounds");
            nVariable.addAttribute("valid_min", (int) element.getAttributeDouble("geospatial_lat_min"));
            nVariable.addAttribute("valid_max", (int) element.getAttributeDouble("geospatial_lat_max"));
        }
        else if (variableName.equals("lon")){
            nVariable.addAttribute("standard_name", "longitude");
            nVariable.addAttribute("units", "degrees_east");
            nVariable.addAttribute("axis", "X");
            nVariable.addAttribute("long_name", "longitude");
            nVariable.addAttribute("bounds", "lon_bounds");
            nVariable.addAttribute("valid_min", (int) element.getAttributeDouble("geospatial_lon_min"));
            nVariable.addAttribute("valid_max", (int) element.getAttributeDouble("geospatial_lon_max"));
        }
        else if (Arrays.asList(listPFTVariables).contains(variableName)) {
            setPFTVariableAttributes(nVariable);
        }
    }


    private void addNotBandVariable(NFileWriteable ncFile, Variable variable) throws IOException {
        //initializes variables which were present in previous file but are non-bands.
        NVariable nVariable;
        String dimString = variable.getDimensionsString();
        if (variable.getFullName().equals("lat_bnds")) {
            nVariable = ncFile.addVariable("lat_bounds", DataType.DOUBLE, null, dimString);
        } else if (variable.getFullName().equals("lon_bnds")) {
            nVariable = ncFile.addVariable("lon_bounds", DataType.DOUBLE, null, dimString);
        } else if (variable.getFullName().equals("time_bnds")) {
            nVariable = ncFile.addVariable("time_bounds", DataType.DOUBLE, null, dimString);
        } else if (variable.getFullName().equals("lat")) {
            nVariable = ncFile.addVariable("lat", DataType.DOUBLE, null, dimString);
        } else if (variable.getFullName().equals("lon")) {
            nVariable = ncFile.addVariable("lon", DataType.DOUBLE, null, dimString);
        } else if (variable.getFullName().equals("time")) {
            nVariable = ncFile.addVariable("time", DataType.DOUBLE, null, dimString);
        } else {
            nVariable = ncFile.addVariable(variable.getFullName(), variable.getDataType(), variable.getDataType().isUnsigned(), null, dimString);
        }
        List<Attribute> attributeList = variable.getAttributes();
        for (Attribute attribute : attributeList) {
            if (attribute.getFullName().equals("valid_min") && variable.getFullName().equals("lon")) {
                //nVariable.addAttribute("valid_min",  0d);
                nVariable.addAttribute("valid_min",  -180d);
            } else if (attribute.getFullName().equals("valid_max") && variable.getFullName().equals("lon")) {
                //nVariable.addAttribute("valid_max",  360d);
                nVariable.addAttribute("valid_max",  180d);
            } else if (attribute.getFullName().equals("valid_max") && variable.getFullName().equals("lat")) {
                nVariable.addAttribute("valid_max", 90d);
            } else if (attribute.getFullName().equals("valid_min") && variable.getFullName().equals("lat")) {
                nVariable.addAttribute("valid_min",  -90d);
            } else if (attribute.getFullName().equals("bounds")) {
                nVariable.addAttribute(attribute.getFullName(), attribute.getStringValue().replace("bnds", "bounds"));
            } else if (attribute.getFullName().equals("units")) {
                nVariable.addAttribute(attribute.getFullName(), attribute.getStringValue());
            } else if (attribute.getLength() == 1 && !attribute.getFullName().equals("_Unsigned")) {
                if (!(attribute.getNumericValue() == null)) {
                    nVariable.addAttribute(attribute.getFullName(), attribute.getNumericValue());
                } else {
                    nVariable.addAttribute(attribute.getFullName(), attribute.getStringValue());
                }
            }
        }
        if (variable.getFullName().equals("lon")) {
            nVariable.addAttribute("bounds", "lon_bounds");
            nVariable.addAttribute("axis", "X");
        } else if (variable.getFullName().equals("lat")) {
            nVariable.addAttribute("bounds", "lat_bounds");
            nVariable.addAttribute("axis", "Y");
        } else if (variable.getFullName().equals("time")) {
            nVariable.addAttribute("bounds", "time_bounds");
            nVariable.addAttribute("axis", "T");
        }
    }


    private void addBandVariable(NFileWriteable ncFile, Band band, NetcdfFileWriter onlyReader, Dimension tileSize) throws IOException {
        NVariable addedVariable;
        final String variableName = ReaderUtils.getVariableName(band);
        String dimString = "time lat lon";

        Variable oldVariable = onlyReader.findVariable(variableName);

        if (oldVariable.getFullName().equals("lccs_class") || oldVariable.getFullName().equals("observation_count") ||
                oldVariable.getFullName().equals("change_count")) {
            DataType dataType = oldVariable.getDataType().withSignedness(DataType.Signedness.UNSIGNED);
            addedVariable = ncFile.addVariable(variableName, dataType, true, tileSize, dimString);


        }
        else {
            //addedVariable = ncFile.addVariable(variableName, oldVariable.getDataType(), oldVariable.getDataType().isUnsigned(), tileSize, dimString);
            addedVariable = ncFile.addVariable(variableName, DataType.FLOAT, oldVariable.getDataType().isUnsigned(), tileSize, dimString);
        }
        addedVariable.addAttribute("long_name", band.getDescription());
        if (band.getScalingOffset() != 0.0) {
            addedVariable.addAttribute("add_offset", band.getScalingOffset());
        }
        if (band.getScalingFactor() != 1.0) {
            addedVariable.addAttribute("scale_factor", band.getScalingFactor());
        }
        List<Attribute> attributeList = oldVariable.getAttributes();
        for (Attribute attribute : attributeList) {
            if (attribute.getFullName().equals("units")) {
                addedVariable.addAttribute(attribute.getFullName(), attribute.getStringValue());
            }
            else if (attribute.getLength() == 1 && !attribute.getFullName().equals("flag_meanings") && !attribute.getFullName().equals("_Unsigned")) {
                if (!(attribute.getNumericValue() == null)) {
                    addedVariable.addAttribute(attribute.getFullName(), attribute.getNumericValue());
                } else {
                    addedVariable.addAttribute(attribute.getFullName(), attribute.getStringValue().replace("\'", ""));
                }
            }
        }
    }




    private static void setPFTVariableAttributes(NVariable nVariable) throws IOException{
        String variableName = nVariable.getName();
        if (variableName.equals("BARE")){
            nVariable.addAttribute("long_name","Bare");
            nVariable.addAttribute("description", "Percentage cover of bare soil in the 300 m pixel.");
        }
        if (variableName.equals("BUILT")){
            nVariable.addAttribute("long_name","Built");
            nVariable.addAttribute("description","Percentage cover of built (artificial impervious area, e.g., buildings) in the 300 m pixel.");
        }
        if (variableName.equals("SNOWICE")){
            nVariable.addAttribute("long_name","Permanent snow and ice");
            nVariable.addAttribute("description","Percentage cover permanent snow and ice in the 300 m pixel.");
        }
        if (variableName.equals("WATER")){
            nVariable.addAttribute("long_name","Water");
            nVariable.addAttribute("description","Percentage cover of surface water (ocean and permanent inland water bodies) in the 300 m pixel.");
        }
        if (variableName.equals("WATER_INLAND")){
            nVariable.addAttribute("long_name","Inland Water");
            nVariable.addAttribute("description","Percentage cover of permanent inland water bodies in the 300 m pixel. Excludes ocean (i.e., ocean pixels are set to 0% cover in this file).");
        }
        if (variableName.equals("GRASS-MAN")){
            nVariable.addAttribute("long_name","Managed grasses");
            nVariable.addAttribute("description","Percentage cover of managed grasses (i.e., herbaceous crops) in the 300 m pixel.");
        }
        if (variableName.equals("GRASS-NAT")){
            nVariable.addAttribute("long_name","Natural grasses");
            nVariable.addAttribute("description","Percentage cover of natural grasses in the 300 m pixel.");
        }
        if (variableName.equals("TREES-BD")){
            nVariable.addAttribute("long_name","Broadleaved deciduous trees");
            nVariable.addAttribute("description","Percentage cover of broadleaved deciduous trees in the 300 m pixel.");
        }
        if (variableName.equals("TREES-BE")){
            nVariable.addAttribute("long_name","Broadleaved evergreen trees");
            nVariable.addAttribute("description","Percentage cover of broadleaved evergreen trees in the 300 m pixel.");
        }
        if (variableName.equals("TREES-ND")){
            nVariable.addAttribute("long_name","Needleleaved deciduous trees");
            nVariable.addAttribute("description","Percentage cover of needleleaved deciduous trees in the 300 m pixel.");
        }
        if (variableName.equals("TREES-NE")){
            nVariable.addAttribute("long_name","Needleleaved evergreen trees");
            nVariable.addAttribute("description","Percentage cover of needleleaved evergreen trees in the 300 m pixel.");
        }
        if (variableName.equals("SHRUBS-BD")){
            nVariable.addAttribute("long_name","Broadleaved deciduous shrubs");
            nVariable.addAttribute("description","Percentage cover of broadleaved deciduous shrubs in the 300 m pixel.");
        }
        if (variableName.equals("SHRUBS-BE")){
            nVariable.addAttribute("long_name","Broadleaved evergreen shrubs");
            nVariable.addAttribute("description","Percentage cover of broadleaved evergreen shrubs in the 300 m pixel.");
        }
        if (variableName.equals("SHRUBS-ND")){
            nVariable.addAttribute("long_name","Needleleaved deciduous shrubs");
            nVariable.addAttribute("description","Percentage cover of needleleaved deciduous shrubs in the 300 m pixel.");
        }
        if (variableName.equals("SHRUBS-NE")){
            nVariable.addAttribute("long_name","Needleleaved evergreen shrubs");
            nVariable.addAttribute("description","Percentage cover of needleleaved evergreen shrubs in the 300 m pixel.");
        }
        if (variableName.equals("LAND")){
            nVariable.addAttribute("long_name","Land");
            nVariable.addAttribute("description","Percentage cover of land(no ocean and permanent inland water bodies) in the 300 m pixel.");
        }
        if (variableName.equals("WATER_OCEAN")){
            nVariable.addAttribute("long_name","Oceanic water");
            nVariable.addAttribute("description","Percentage cover of ocean in the 300 m pixel. Excludes inland water (i.e., inland water pixels are set to 0% cover in this file).");
        }
    }

    public static void addGlobalAttribute(NFileWriteable writeable, MetadataElement element, String name, String value) throws IOException {
        if (element!=null) {
            if (element.containsAttribute(name) && value != null) {
                writeable.addGlobalAttribute(name, value);
                SystemUtils.LOG.warning("You are going to rewrite global attribute " + name + " original value with the " + value + " value");
                element.setAttributeString(name,value);
            } else if (element.containsAttribute(name) && value == null) {
                value = element.getAttributeString(name);
                writeable.addGlobalAttribute(name, value);
                element.setAttributeString(name,value);
            } else if (!element.containsAttribute(name) && value != null) {
                writeable.addGlobalAttribute(name, value);
                element.setAttributeString(name,value);
            } else {
                SystemUtils.LOG.warning("Global attribute " + name + " does not exist in the original product. Nothing is written");
            }
        }
        else {
            writeable.addGlobalAttribute(name, value);
        }
    }



    private void writePFTGlobalAttribute(NFileWriteable writeable, MetadataElement element) throws IOException {
        final Dimension tileSize = new Dimension(2025, 2025);
        String timeYear = element.getAttributeString("id").substring(30,34);
        //timeYear = "2010";
        String startObservation = timeYear+"0101";
        String endObservation = timeYear+"1231";

        addGlobalAttribute(writeable, element, "title", "ESA CCI Land Cover Project: Maps of Plant Functional Type Fractional Cover");
        addGlobalAttribute(writeable, element, "summary", "This dataset contains the global plant functional type fractional " +
                "cover maps of the ESA Medium Resolution CCI Land Cover project");
        addGlobalAttribute(writeable, element,"type","ESACCI-LC-L4-PFT-Map-300m-P1Y");
        addGlobalAttribute(writeable,element,"id",null);
        addGlobalAttribute(writeable,element,"project","Medium Resolution Land Cover - Climate Change Initiative - European Space Agency");
        addGlobalAttribute(writeable,element,"references","https://maps.elie.ucl.ac.be/CCI/viewer/download.php, https://climate.esa.int/fr/projects/land-cover/about/, https://catalogue.ceda.ac.uk/uuid/26a0f46c95ee4c29b5c650b129aab788");
        addGlobalAttribute(writeable,element,"citation","Harper et al., submitted. A 29-year time series of annual 300-metre resolution plant functional type maps for climate models ." +
                " Kandice L. Harper, Céline Lamarche, Andrew Hartley, Philippe Peylin, Catherine Ottlé, Vladislav Bastrikov," +
                " Rodrigo San Martín, Sylvia I. Bohnenstengel, Grit Kirches, Martin Boettcher, Roman Shevchuk, Carsten Brockmann, Pierre Defourny. Dataset doi: doi = \"10.5285/26a0f46c95ee4c29b5c650b129aab788.");
        addGlobalAttribute(writeable,element,"input_data","CCI medium-resolution land cover time series (Defourny et al., submitted)." +
                " Surface water product (Pekel et al., 2016). " +
                "Tree canopy cover product (Hansen et al., 2013). " +
                " Tree canopy height product (Potapov et al., 2021).  Built-up product Global Human Settlement Layer (Pesaresi et al., 2013)." +
                "  Köppen-Geiger climate zone product (Beck et al. 2018).  Landform product (Sayre et al., 2014). " +
                " IMAGE world regions product (Stehfest et al., 2014). CCI medium-resolution water body product v4.1. (Lamarche et al., 2017)." +
                " Detailed references can be found in the paper indicated in NC_GLOBAL");
        addGlobalAttribute(writeable,element,"institution","Universite catholique de Louvain, UCLouvain-Geomatics (Belgium)");
        addGlobalAttribute(writeable,element,"contact","contact@esa-landcover-cci.org");
        addGlobalAttribute(writeable,element,"Conventions","CF-1.6");
        addGlobalAttribute(writeable,element,"standard_name_vocabulary","CF-1.6");
        addGlobalAttribute(writeable,element,"keywords","land cover classification, satellite, observation");
        addGlobalAttribute(writeable,element,"keywords_vocabulary","NASA Global Change Master Directory (GCMD) Science Keywords");
        addGlobalAttribute(writeable,element,"license","CC BY 4.0 - https://creativecommons.org/licenses/by/4.0/ ");
        addGlobalAttribute(writeable,element,"naming_authority","org.esa-cci");
        addGlobalAttribute(writeable,element,"doi","10.5285/26a0f46c95ee4c29b5c650b129aab788");
        addGlobalAttribute(writeable,element,"cdm_data_type","grid");
        addGlobalAttribute(writeable, element, "tracking_id", UUID.randomUUID().toString());
        addGlobalAttribute(writeable, element, "date_created", LcWriterUtils.COMPACT_ISO_FORMAT.format(new Date()));
        addGlobalAttribute(writeable, element, "creator_name", "Universite catholique de Louvain, UCLouvain-Geomatics (Belgium)");
        addGlobalAttribute(writeable, element, "creator_url", "http://www.uclouvain.be");
        addGlobalAttribute(writeable, element, "creator_email", "contact@esa-landcover-cci.org");
        addGlobalAttribute(writeable, element, "source", "ESA CCI land cover maps, auxiliary derived satellite data products");
        addGlobalAttribute(writeable, element, "history", "lc-classification-1.0,pft-classification-1.0,lc-user-tools-"+ LcWriterUtils.getModuleVersion());
        addGlobalAttribute(writeable, element, "time_coverage_start", startObservation);
        addGlobalAttribute(writeable, element, "time_coverage_end", endObservation);
        addGlobalAttribute(writeable, element, "time_coverage_duration", "P1Y");
        addGlobalAttribute(writeable, element, "time_coverage_resolution", "P1Y");
        addGlobalAttribute(writeable, element, "geospatial_lat_min", null);
        addGlobalAttribute(writeable, element, "geospatial_lat_max", null);
        if (element.containsAttribute("subsetted")) {
            addGlobalAttribute(writeable, element, "geospatial_lon_min", null);
            addGlobalAttribute(writeable, element, "geospatial_lon_max", null);
        }
        else {
            addGlobalAttribute(writeable, element, "geospatial_lon_min", "-180");
            addGlobalAttribute(writeable, element, "geospatial_lon_max", "180");
        }
        addGlobalAttribute(writeable, element,"spatial_resolution","300m");
        addGlobalAttribute(writeable, element,"geospatial_lat_units","degrees_north");
        addGlobalAttribute(writeable, element,"geospatial_lat_resolution","002777777777778");
        addGlobalAttribute(writeable, element,"geospatial_lon_units","degrees_east");
        addGlobalAttribute(writeable, element,"geospatial_lon_resolution","002777777777778");
        addGlobalAttribute(writeable, element, "TileSize", LcHelper.format(tileSize));
        addGlobalAttribute(writeable, element, "product_version", "2.0.8");

    }

    private void setAggregatedPFTAttributes(NFileWriteable writeable, MetadataElement element, String path) throws IOException{
        addGlobalAttribute(writeable,element,"type","PFT_product" );
        addGlobalAttribute(writeable,element,"parent_path",path );
        addGlobalAttribute(writeable,element,"id",path );
        writePFTGlobalAttribute(writeable,element);
    }
}
