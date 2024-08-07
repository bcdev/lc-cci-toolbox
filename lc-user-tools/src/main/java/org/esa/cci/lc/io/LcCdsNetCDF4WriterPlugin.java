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


public class LcCdsNetCDF4WriterPlugin extends BeamNetCdf4WriterPlugIn {

    public static final String FORMAT_NAME = "NetCDF4-LC-CDS";

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
            //String path = product.getFileLocation().getAbsolutePath();
            String path;
            //case of aggregated PFT
            if (element == null && product.getName().contains("aggregated") && product.getName().contains("PFT") ) {
                path = product.getName().replace("ESACCI-LC-L4-PFT-Map-300m-aggregated","ESACCI-LC-L4-PFT-Map-300m");
                MetadataElement globalAttributes = new MetadataElement("global_attributes");
                product.getMetadataRoot().addElement(globalAttributes);
                element = product.getMetadataRoot().getElement("global_attributes");
                setAggregatedPFTAttributes(writeable, element, path);
                //product.removeBand(product.getBand("num_obs"));
            }
            else {
                path = element.getAttributeString("parent_path");
            }
            if (! path.endsWith(".tif") ) {
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

            }
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
            if ( element.getAttributeString("type").equals("pixel_product") ){
                writePPGlobalAttribute( writeable, element);
            }
            else if (element.getAttributeString("type").equals("ESACCI-LC-L4-LCCS-Map-300m-P1Y")) {
                writeLCGlobalAttribute( writeable,  element);
            }
            else if (element.getAttributeString("type").equals("burned_area")) {
                writeBAGlobalAttribute( writeable,  element);
            }
            else if (element.getAttributeString("type").equals("lccds2")) {
                writeLC2GlobalAttribute( writeable,  element);
            }
            else if (element.getAttributeString("type").equals("pixel_product2") ) {
                writePP2GlobalAttribute(writeable, element);
            }
            else if (element.getAttributeString("type").equals("PFT_product") || element.getAttributeString("type").equals("ESACCI-LC-L4-PFT-Map-300m-P1Y") ) {
                //writePFTGlobalAttribute(writeable, element);
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
                if (! path.endsWith(".tif") ) {
                    NetcdfFileWriter onlyReader = NetcdfFileWriter.openExisting(path);


                    for (Band band : p.getBands()) {
                        if (onlyReader.findVariable(band.getName())!=null ) {
                            if (!band.getName().contains("vegetation_class")) {
                                addBandVariable(ncFile, band, onlyReader, tileSize);
                            } else {
                                addBandClassVariable(ncFile, band, onlyReader, tileSize);
                            }
                        }
                        else if (band.getName().contains("vegetation_class")) {
                            addBandClassVariable(ncFile, band, onlyReader, tileSize);
                        }
                        else {
                            addCustomVariable(ncFile,band.getName(),"time lat lon",DataTypeUtils.getNetcdf4DataType(band.getDataType()),tileSize,element);
                        }
                    }


                    List<Variable> list = onlyReader.getNetcdfFile().getVariables();
                    for (Variable variable : list) {
                        if (!Arrays.asList(p.getBandNames()).contains(variable.getFullName()) && (!variable.getFullName().contains("burned_area_in_vegetation_class"))) {
                            addNotBandVariable(ncFile, variable);
                        }
                    }

                    onlyReader.close();
                }
                else {
                    final Dimension tileSizePixel = new Dimension(2025, 2025);
                    Band[] bands = element.getProduct().getBands();
                    if (!bands[0].getName().equals("JD")) {
                        if ( element.getName().contains("aggregated") ) {
                            for ( Band band : bands) {
                                addCustomVariable(ncFile,band.getName(),"time lat lon", DataType.DOUBLE,tileSizePixel, element);
                            }
                        }
                        else {
                            for (Band band : bands) {
                                addCustomVariable(ncFile, band.getName(), "time lat lon", DataType.BYTE, tileSizePixel, element);
                            }
                        }
                        addCustomVariable(ncFile, "lon", "lon", DataType.DOUBLE, null, element);
                        addCustomVariable(ncFile, "lat", "lat", DataType.DOUBLE, null, element);
                    }
                    else {
                        addCustomVariable(ncFile, "JD", "time lat lon", DataType.SHORT, tileSizePixel, element);
                        addCustomVariable(ncFile, "CL", "time lat lon", DataType.BYTE, tileSizePixel, element);
                        addCustomVariable(ncFile, "LC", "time lat lon", DataType.UBYTE, tileSizePixel, element);
                        addCustomVariable(ncFile, "lon", "lon", DataType.DOUBLE, null, element);
                        addCustomVariable(ncFile, "lat", "lat", DataType.DOUBLE, null, element);
                    }
                }

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
        String[] listPFTVariables = {"WATER","BARE","BUILT","GRASS-MAN","GRASS-NAT","SHRUBS-BD","SHRUBS-BE","SHRUBS-ND","SHRUBS-NE",
               "SNOWICE","TREES-BD","TREES-BE","TREES-ND","TREES-NE","LAND","WATER_OCEAN"};

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
        else if (variableName.equals("JD")){
            nVariable.addAttribute("long_name", "Date of the first detection");
            nVariable.addAttribute("units", "Day of the year");
            nVariable.addAttribute("comment", "Possible values: 0  when the pixel is not burned; 1 to 366 day of the first detection when the pixel is burned; -1 when the pixel is " +
                    "not observed in the month; -2 when pixel is not burnable: water bodies, bare areas, urban areas and permanent snow and ice.");
        }
        else if (variableName.equals("CL")){
            nVariable.addAttribute("long_name", "Confidence Level");
            nVariable.addAttribute("units", "percent");
            nVariable.addAttribute("comment", "Probability of detecting a pixel as burned. Possible values: 0 when the pixel is not observed in the month, or it is not burnable;" +
                    " 1 to 100 probability values when the pixel was observed. The closer to 100, the higher the confidence that the pixel is actually burned.");
        }
        else if (variableName.equals("LC")){
            nVariable.addAttribute("long_name", "Land cover of burned pixels");
            nVariable.addAttribute("units", "Land cover code");
            nVariable.addAttribute("comment", "Land cover of the burned pixel, extracted from the C3S LandCover v2.1.1 . N is the number of the land cover category in the reference map. It is only valid when JD > 0. Pixel value is 0 to N under the following codes: 10 = Cropland, rainfed; 20 = Cropland, irrigated or post-flooding; 30 = Mosaic cropland (>50%) / natural vegetation (tree, shrub, herbaceous cover) (<50%); 40 = Mosaic natural vegetation (tree, shrub, herbaceous cover) (>50%) / cropland (<50%); 50 = Tree cover, broadleaved, evergreen, closed to open (>15%); 60 = Tree cover, broadleaved, deciduous, closed to open (>15%); 70 = Tree cover, needleleaved, evergreen, closed to open (>15%); 80 = Tree cover, needleleaved, deciduous, closed to open (>15%); 90 = Tree cover, mixed leaf type (broadleaved and needleleaved); 100 = Mosaic tree and shrub (>50%) / herbaceous cover (<50%); 110 = Mosaic herbaceous cover (>50%) / tree and shrub (<50%); 120 = Shrubland; 130 = Grassland; 140 = Lichens and mosses; 150 = Sparse vegetation (tree, shrub, herbaceous cover) (<15%); 160 = Tree cover, flooded, fresh or brackish water; 170 = Tree cover, flooded, saline water; 180 = Shrub or herbaceous cover, flooded, fresh/saline/brackish water.");
        }
        else if (Arrays.asList(listPFTVariables).contains(variableName)) {
            setPFTVariableAttributes(nVariable);
        }
    }


    private void addNotBandVariable(NFileWriteable ncFile, Variable variable) throws IOException {
        //initializes variables which were present in previous file but are non-bands.
        NVariable nVariable;
        String dimString = variable.getDimensionsString();
        if (dimString.contains("nv")) {
            dimString = dimString.replace("nv", "bounds");
        }
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
            if (oldVariable.getFullName().equals("lccs_class")) {
                addedVariable.addAttribute("standard_name", "land_cover_lccs");
                addedVariable.addAttribute("flag_colors", "#ffff64 #ffff64 #ffff00 #aaf0f0 #dcf064 #c8c864 #006400 #00a000 #00a000 #aac800 #003c00 #003c00 #005000 #285000 #285000 #286400 #788200 #8ca000 #be9600 #966400 #966400 #966400 #ffb432 #ffdcd2 #ffebaf #ffc864 #ffd278 #ffebaf #00785a #009678 #00dc82 #c31400 #fff5d7 #dcdcdc #fff5d7 #0046c8 #ffffff");
            }
            else if (oldVariable.getFullName().equals("standard_error")) {
                addedVariable.addAttribute("standard_name", "burned_area standard_error");
            }
        }
        else {
            addedVariable = ncFile.addVariable(variableName, oldVariable.getDataType(), oldVariable.getDataType().isUnsigned(), tileSize, dimString);
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


    private void addBandClassVariable(NFileWriteable ncFile, Band band, NetcdfFileWriter onlyReader, Dimension tileSize) throws IOException {
        String dimString = "time vegetation_class lat lon ";
        final DataType ncDataType = DataTypeUtils.getNetcdfDataType(band.getDataType());
        final String variableName = "burned_area_in_vegetation_class";
        if (ncFile.findVariable(variableName) == null) {
            NVariable addedVariable = ncFile.addVariable(variableName, ncDataType, ncDataType.isUnsigned(), tileSize, dimString);
            Variable variable = onlyReader.findVariable(variableName);
            List<Attribute> attributeList = variable.getAttributes();
            for (Attribute attribute : attributeList) {
                if (attribute.getFullName().equals("units")) {
                    addedVariable.addAttribute(attribute.getFullName(), attribute.getStringValue());
                } else if (attribute.getLength() == 1 && !attribute.getFullName().equals("flag_meanings") && !attribute.getFullName().equals("_Unsigned")) {
                    if (!(attribute.getNumericValue() == null)) {
                        addedVariable.addAttribute(attribute.getFullName(), attribute.getNumericValue());
                    } else {
                        addedVariable.addAttribute(attribute.getFullName(), attribute.getStringValue());
                    }
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

    private void writeLCGlobalAttribute(NFileWriteable writeable, MetadataElement element) throws IOException {
        final Dimension tileSize = new Dimension(2025, 2025);
        String history = element.getAttributeString("history");

        addGlobalAttribute(writeable, element, "id", null);
        addGlobalAttribute(writeable, element, "title", "Land Cover Map of ESA CCI brokered by CDS");
        addGlobalAttribute(writeable, element, "summary", "This dataset characterizes the land cover of a particular year (see time_coverage). The land cover was derived from the analysis of satellite data time series of the full period.");
        addGlobalAttribute(writeable, element, "type", null);
        addGlobalAttribute(writeable, element, "project", null);
        addGlobalAttribute(writeable, element, "references", null);
        addGlobalAttribute(writeable, element, "institution", "UCLouvain");
        addGlobalAttribute(writeable, element, "contact", "https://www.ecmwf.int/en/about/contact-us/get-support");
        addGlobalAttribute(writeable, element, "comment", null);
        addGlobalAttribute(writeable, element, "Conventions", null);
        addGlobalAttribute(writeable, element, "standard_name_vocabulary", null);
        addGlobalAttribute(writeable, element, "keywords", null);
        addGlobalAttribute(writeable, element, "keywords_vocabulary", null);
        addGlobalAttribute(writeable, element, "license", null);
        addGlobalAttribute(writeable, element, "naming_authority", null);
        addGlobalAttribute(writeable, element, "cdm_data_type", null);
        addGlobalAttribute(writeable, element, "TileSize", LcHelper.format(tileSize));
        addGlobalAttribute(writeable, element, "tracking_id", UUID.randomUUID().toString());
        addGlobalAttribute(writeable, element, "product_version", null);
        addGlobalAttribute(writeable, element, "creation_date", LcWriterUtils.COMPACT_ISO_FORMAT.format(new Date()));
        addGlobalAttribute(writeable, element, "creator_name", "UCLouvain");
        addGlobalAttribute(writeable, element, "creator_url", "http://www.uclouvain.be/");
        addGlobalAttribute(writeable, element, "creator_email", null);
        addGlobalAttribute(writeable, element, "source", null);
        addGlobalAttribute(writeable, element, "history", history + ",lc-user-tools-" + LcWriterUtils.getModuleVersion());
        addGlobalAttribute(writeable, element, "time_coverage_start", null);
        addGlobalAttribute(writeable, element, "time_coverage_end", null);
        addGlobalAttribute(writeable, element, "time_coverage_duration", null);
        addGlobalAttribute(writeable, element, "time_coverage_resolution", null);
        addGlobalAttribute(writeable, element, "geospatial_lat_min", null);
        addGlobalAttribute(writeable, element, "geospatial_lat_max", null);
        if(element.containsAttribute("subsetted")) {
            addGlobalAttribute(writeable, element, "geospatial_lon_min", null);
            addGlobalAttribute(writeable, element, "geospatial_lon_max", null);
        }
        else{
            //addGlobalAttribute(writeable, element, "geospatial_lon_min", "0");
            addGlobalAttribute(writeable, element, "geospatial_lon_min", "-180");
            //addGlobalAttribute(writeable, element, "geospatial_lon_max", "360");
            addGlobalAttribute(writeable, element, "geospatial_lon_max", "180");
        }
        addGlobalAttribute(writeable, element, "spatial_resolution", null);
        addGlobalAttribute(writeable, element, "geospatial_lat_units", null);
        addGlobalAttribute(writeable, element, "geospatial_lat_resolution", null);
        addGlobalAttribute(writeable, element, "geospatial_lon_units", null);
        addGlobalAttribute(writeable, element, "geospatial_lon_resolution", null);
    }


    private void writeLC2GlobalAttribute(NFileWriteable writeable, MetadataElement element) throws IOException {
        final Dimension tileSize = new Dimension(2025, 2025);
        String history = element.getAttributeString("history");

        addGlobalAttribute(writeable, element, "id", null);
        addGlobalAttribute(writeable, element, "title", "Land Cover Map of "+element.getAttributeString("time_coverage_start").substring(0,4));
        addGlobalAttribute(writeable, element, "summary", "This dataset characterizes the land cover of a particular year (see time_coverage). The land cover was derived from the analysis of satellite data time series of the full period.");
        addGlobalAttribute(writeable, element, "type", "C3S-LC-L4-LCCS-Map-300m-P1Y");
        addGlobalAttribute(writeable, element, "project", "EC C3S Land cover");
        addGlobalAttribute(writeable, element, "references", "https://cds.climate.copernicus.eu/");
        addGlobalAttribute(writeable, element, "institution", "UCLouvain");
        addGlobalAttribute(writeable, element, "contact", "copernicus-support@ecmwf.int");
        addGlobalAttribute(writeable, element, "comment", "");
        addGlobalAttribute(writeable, element, "Conventions", "CF-1.6");
        addGlobalAttribute(writeable, element, "standard_name_vocabulary", "NetCDF Climate and Forecast (CF) Standard Names version 21");
        addGlobalAttribute(writeable, element, "keywords", "land cover classification,satellite,observation");
        addGlobalAttribute(writeable, element, "keywords_vocabulary", "NASA Global Change Master Directory (GCMD) Science Keywords");
        addGlobalAttribute(writeable, element, "license", "EC C3S Land cover Data Policy");
        addGlobalAttribute(writeable, element, "naming_authority", "");
        addGlobalAttribute(writeable, element, "cdm_data_type", "grid");
        addGlobalAttribute(writeable, element, "TileSize", LcHelper.format(tileSize));
        addGlobalAttribute(writeable, element, "tracking_id", UUID.randomUUID().toString());
        addGlobalAttribute(writeable, element, "product_version", "2.1.1");
        addGlobalAttribute(writeable, element, "creation_date", LcWriterUtils.COMPACT_ISO_FORMAT.format(new Date()));
        addGlobalAttribute(writeable, element, "creator_name", "UCLouvain");
        addGlobalAttribute(writeable, element, "creator_url", "http://www.uclouvain.be/");
        addGlobalAttribute(writeable, element, "creator_email", "landcover-cci@uclouvain.be");
        addGlobalAttribute(writeable, element, "source", "PROBA-V");
        addGlobalAttribute(writeable, element, "history", "lc-sr-1.0, lc-classification-1.0,lc-user-tools-3.14"+ ",lc-user-tools-" + LcWriterUtils.getModuleVersion());
        addGlobalAttribute(writeable, element, "time_coverage_start", null);
        addGlobalAttribute(writeable, element, "time_coverage_end", null);
        addGlobalAttribute(writeable, element, "time_coverage_duration", null);
        addGlobalAttribute(writeable, element, "time_coverage_resolution", null);
        addGlobalAttribute(writeable, element, "geospatial_lat_min", null);
        addGlobalAttribute(writeable, element, "geospatial_lat_max", null);
        if(element.containsAttribute("subsetted")) {
            addGlobalAttribute(writeable, element, "geospatial_lon_min", null);
            addGlobalAttribute(writeable, element, "geospatial_lon_max", null);
        }
        else{
            addGlobalAttribute(writeable, element, "geospatial_lon_min", "null");
            addGlobalAttribute(writeable, element, "geospatial_lon_max", "null");
        }
        addGlobalAttribute(writeable, element, "spatial_resolution", null);
        addGlobalAttribute(writeable, element, "geospatial_lat_units", null);
        addGlobalAttribute(writeable, element, "geospatial_lat_resolution", null);
        addGlobalAttribute(writeable, element, "geospatial_lon_units", null);
        addGlobalAttribute(writeable, element, "geospatial_lon_resolution", null);
    }





    private void writeBAGlobalAttribute(NFileWriteable writeable, MetadataElement element) throws IOException {
        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        addGlobalAttribute(writeable, element, "title", null);
        addGlobalAttribute(writeable, element, "institution", null);
        addGlobalAttribute(writeable, element, "source", null);
        addGlobalAttribute(writeable, element, "history", "Created on 2017-12-19 06:42:41; modified with lc-user-tools-"+ LcWriterUtils.getModuleVersion()+" on "+dateFormat.format(new Date()));
        addGlobalAttribute(writeable, element, "references", null);
        addGlobalAttribute(writeable, element, "tracking_id", UUID.randomUUID().toString());
        addGlobalAttribute(writeable, element, "Conventions", null);
        addGlobalAttribute(writeable, element, "product_version", null);
        addGlobalAttribute(writeable, element, "summary", null);
        addGlobalAttribute(writeable, element, "keywords", null);
        addGlobalAttribute(writeable, element, "id", null);
        addGlobalAttribute(writeable, element, "naming_authority", null);
        //addGlobalAttribute(writeable, element, "doi", null);
        addGlobalAttribute(writeable, element, "keywords_vocabulary", null);
        addGlobalAttribute(writeable, element, "cdm_data_type", null);
        addGlobalAttribute(writeable, element, "comment", null);
        addGlobalAttribute(writeable, element, "creation_date", LcWriterUtils.COMPACT_ISO_FORMAT.format(new Date()));
        addGlobalAttribute(writeable, element, "creator_name", null);
        addGlobalAttribute(writeable, element, "creator_url", null);
        addGlobalAttribute(writeable, element, "creator_email", null);
        addGlobalAttribute(writeable, element, "contact", null);
        addGlobalAttribute(writeable, element, "project", null);
        addGlobalAttribute(writeable, element, "geospatial_lat_min", null);
        addGlobalAttribute(writeable, element, "geospatial_lat_max", null);
        //addGlobalAttribute(writeable, element, "geospatial_lon_min", "0");
        addGlobalAttribute(writeable, element, "geospatial_lon_min", null);
        //addGlobalAttribute(writeable, element, "geospatial_lon_max", "360");
        addGlobalAttribute(writeable, element, "geospatial_lon_max", null);
        addGlobalAttribute(writeable, element, "time_coverage_start", null);
        addGlobalAttribute(writeable, element, "time_coverage_end", null);
        addGlobalAttribute(writeable, element, "time_coverage_duration", null);
        addGlobalAttribute(writeable, element, "time_coverage_resolution", null);
        addGlobalAttribute(writeable, element, "standard_name_vocabulary", null);
        addGlobalAttribute(writeable, element, "license", null);
        addGlobalAttribute(writeable, element, "platform", null);
        addGlobalAttribute(writeable, element, "sensor", null);
        addGlobalAttribute(writeable, element, "spatial_resolution", null);
        addGlobalAttribute(writeable, element, "geospatial_lon_units", null);
        addGlobalAttribute(writeable, element, "geospatial_lat_units", null);
        addGlobalAttribute(writeable, element, "geospatial_lon_resolution", null);
        addGlobalAttribute(writeable, element, "geospatial_lat_resolution", null);
    }

    private void writePPGlobalAttribute(NFileWriteable writeable, MetadataElement element) throws IOException {
        String timeYear = element.getProduct().getFileLocation().getName().substring(0,4);
        String timeMonth = element.getProduct().getFileLocation().getName().substring(4,6);
        Calendar calendar = Calendar.getInstance();
        calendar.set(Integer.parseInt(timeYear),Integer.parseInt(timeMonth)-1,1);
        String lastDay  = Integer.toString(calendar.getActualMaximum( Calendar.DAY_OF_MONTH));
        String startObservation = timeYear+timeMonth+"01T000000Z";
        String endObservation = timeYear+timeMonth+lastDay+"T235959Z";
        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));


        addGlobalAttribute(writeable, element, "title", "Fire_cci Pixel MODIS Burned Area product");
        addGlobalAttribute(writeable, element, "institution", "University of Alcala");
        addGlobalAttribute(writeable, element, "source", "MODIS MOD09GQ Collection 6, MODIS MOD09GA Collection 6, MODIS MCD14ML Collection 6, ESA CCI Land Cover dataset v1.6.1");
        addGlobalAttribute(writeable, element, "history", "Created on 2017-12-19 06:42:41; modified with lc-user-tools-"+ LcWriterUtils.getModuleVersion()+" on "+dateFormat.format(new Date()));
        addGlobalAttribute(writeable, element, "references", "See www.esa-fire-cci.org");
        addGlobalAttribute(writeable, element, "tracking_id", UUID.randomUUID().toString());
        addGlobalAttribute(writeable, element, "conventions","CF-1.6");
        String oldVersion = element.getProduct().getFileLocation().getName().substring(42,46);
        //addGlobalAttribute(writeable, element, "product_version","v5.0cds");
        addGlobalAttribute(writeable, element, "product_version",oldVersion+"cds");
        //addGlobalAttribute(writeable, element, "summary","The pixel product is a raster dataset consisting of three layers that together describe the attributes of the BA product. These layers are 1) Date of the first detection; 2) Confidence Level; 3) Land cover of burned pixels");
        addGlobalAttribute(writeable, element, "summary","The pixel product is a raster dataset consisting of three layers that together describe the attributes of the BA product." +
                "It uses the following naming convention: ${Indicative Date}-ESACCI-L3S_FIRE-BA-${Indicative sensor}[-${Additional Segregator}]-fv${xx.x}.nc. ${Indicative Date} is the identifying date for this data set. Format is YYYYMMDD, where YYYY is the four digit year, MM is the two digit month from 01 to 12 and DD is the two digit day of the month from 01 to 31. For monthly products the date is set to 01. ${Indicative sensor} is MODIS. ${Additional Segregator} is the AREA_${TILE_CODE} being the tile code described in the Product User Guide. ${File Version} is the File version number in the form n{1,}[.n{1,}cds] (That is 1 or more digits followed by optional . and another 1 or more digits, and the cds code to identify this product. An example is: 20050301-ESACCI-L3S_FIRE-BA-MODIS-AREA_5-fv5.0cds.nc");
        addGlobalAttribute(writeable, element, "keywords", "Burned Area, Fire Disturbance, Climate Change, ESA, GCOS");
        addGlobalAttribute(writeable, element, "id", null);
        addGlobalAttribute(writeable, element, "naming_authority", "org.esa-fire-cci");
        addGlobalAttribute(writeable, element, "keywords_vocabulary", "none");
        addGlobalAttribute(writeable, element, "cdm_data_type", "Grid");
        addGlobalAttribute(writeable, element, "comment", "These data were produced as part of the ESA Fire_cci programme");
        addGlobalAttribute(writeable, element, "creation_date", LcWriterUtils.COMPACT_ISO_FORMAT.format(new Date()));
        //addGlobalAttribute(writeable, element, "date_created", LcWriterUtils.COMPACT_ISO_FORMAT.format(new Date()));
        addGlobalAttribute(writeable, element, "creator_name", "University of Alcala");
        addGlobalAttribute(writeable, element, "creator_url", "www.esa-fire-cci.org");
        addGlobalAttribute(writeable, element, "creator_email", "emilio.chuvieco@uah.es");
        addGlobalAttribute(writeable, element, "contact", "http://copernicus-support.ecmwf.int");
        addGlobalAttribute(writeable, element, "project", "Climate Change Initiative - European Space Agency");
        addGlobalAttribute(writeable, element, "geospatial_lat_min", null);
        addGlobalAttribute(writeable, element, "geospatial_lat_max", null);
        addGlobalAttribute(writeable, element, "geospatial_lon_min", null);
        addGlobalAttribute(writeable, element, "geospatial_lon_max", null);
        addGlobalAttribute(writeable, element, "time_coverage_start", startObservation);
        addGlobalAttribute(writeable, element, "time_coverage_end", endObservation);
        addGlobalAttribute(writeable, element, "time_coverage_duration", "P1M");
        addGlobalAttribute(writeable, element, "time_coverage_resolution", "P1M");
        addGlobalAttribute(writeable, element, "standard_name_vocabulary", "NetCDF Climate and Forecast (CF) Metadata Convention");
        addGlobalAttribute(writeable, element, "license", "ESA CCI Data Policy: free and open access");
        addGlobalAttribute(writeable, element, "platform", "Terra");
        addGlobalAttribute(writeable, element, "sensor", "MODIS");
        addGlobalAttribute(writeable, element, "spatial_resolution", "0.0022457331");
        addGlobalAttribute(writeable, element, "geospatial_lon_units", "degrees_east");
        addGlobalAttribute(writeable, element, "geospatial_lat_units", "degrees_north");
        addGlobalAttribute(writeable, element, "geospatial_lon_resolution", "0.0022457331");
        addGlobalAttribute(writeable, element, "geospatial_lat_resolution", "0.0022457331");
    }

    private void writePP2GlobalAttribute(NFileWriteable writeable, MetadataElement element) throws IOException {
        String timeYear = element.getProduct().getFileLocation().getName().substring(0,4);
        String timeMonth = element.getProduct().getFileLocation().getName().substring(4,6);
        Calendar calendar = Calendar.getInstance();
        calendar.set(Integer.parseInt(timeYear),Integer.parseInt(timeMonth)-1,1);
        String lastDay  = Integer.toString(calendar.getActualMaximum( Calendar.DAY_OF_MONTH));
        String startObservation = timeYear+timeMonth+"01T000000Z";
        String endObservation = timeYear+timeMonth+lastDay+"T235959Z";
        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        addGlobalAttribute(writeable, element, "title", "ECMWF C3S Pixel OLCI Burned Area product");
        addGlobalAttribute(writeable, element, "institution", "University of Alcala");
        addGlobalAttribute(writeable, element, "source", "Sentinel-3 A+B OLCI FR, MODIS MCD14ML Collection 6, C3S Land Cover dataset v2.1.1");
        addGlobalAttribute(writeable, element, "history", "Created on 2019-09-30 08:30:00; modified with lc-user-tools-"+ LcWriterUtils.getModuleVersion()+" on "+dateFormat.format(new Date()));
        addGlobalAttribute(writeable, element, "references", "https://climate.copernicus.eu/");
        addGlobalAttribute(writeable, element, "tracking_id", UUID.randomUUID().toString());
        addGlobalAttribute(writeable, element, "conventions","CF-1.7");
        addGlobalAttribute(writeable, element, "product_version","v1.0");
        addGlobalAttribute(writeable, element, "summary","The pixel product is a raster dataset consisting of three layers that together describe the attributes of the BA product. " +
                "It uses the following naming convention: ${Indicative Date}-C3S-L3S_FIRE-BA-${Indicative sensor}[-${Additional Segregator}]-fv${xx.x}.nc. ${Indicative Date} is the identifying date for this data set. Format is YYYYMMDD, where YYYY is the four-digit year, MM is the two-digit month from 01 to 12 and DD is the two-digit day of the month from 01 to 31. For monthly products the date is set to 01. ${Indicative sensor} is OLCI. ${Additional Segregator} is the AREA_${TILE_CODE} being the tile code described in the Product User Guide. ${File Version} is the File version number in the form n{1,}[.n{1,}] (That is 1 or more digits followed by optional . and another 1 or more digits). An example is: 20180101-C3S-L3S_FIRE-BA-OLCI-AREA_1-fv1.0.nc");
        addGlobalAttribute(writeable, element, "keywords", "Burned Area, Fire Disturbance, Climate Change, ESA, C3S, GCOS");
        addGlobalAttribute(writeable, element, "id", null);
        addGlobalAttribute(writeable, element, "naming_authority", "org.esa-cci");
        addGlobalAttribute(writeable, element, "keywords_vocabulary", "NASA Global Change Master Directory (GCMD) Science keywords");
        addGlobalAttribute(writeable, element, "cdm_data_type", "Pixel");
        addGlobalAttribute(writeable, element, "comment", "These data were produced as part of the Copernicus Climate Change Service programme.");
        addGlobalAttribute(writeable, element, "creation_date", LcWriterUtils.COMPACT_ISO_FORMAT.format(new Date()));
        //addGlobalAttribute(writeable, element, "date_created", LcWriterUtils.COMPACT_ISO_FORMAT.format(new Date()));
        addGlobalAttribute(writeable, element, "creator_name", "University of Alcala");
        addGlobalAttribute(writeable, element, "creator_url", "http://www.uah.es");
        addGlobalAttribute(writeable, element, "creator_email", "emilio.chuvieco@uah.es");
        addGlobalAttribute(writeable, element, "contact", "http://copernicus-support.ecmwf.int");
        addGlobalAttribute(writeable, element, "project", "EC C3S Fire Burned Area");
        writeable.addGlobalAttribute("geospatial_lat_min", (int) element.getAttributeDouble("geospatial_lat_min"));
        writeable.addGlobalAttribute("geospatial_lat_max", (int) element.getAttributeDouble("geospatial_lat_max"));
        writeable.addGlobalAttribute("geospatial_lon_min", (int) element.getAttributeDouble("geospatial_lon_min"));
        writeable.addGlobalAttribute("geospatial_lon_max", (int) element.getAttributeDouble("geospatial_lon_max"));
        addGlobalAttribute(writeable, element, "time_coverage_start", startObservation);
        addGlobalAttribute(writeable, element, "time_coverage_end", endObservation);
        addGlobalAttribute(writeable, element, "time_coverage_duration", "P1M");
        addGlobalAttribute(writeable, element, "time_coverage_resolution", "P1M");
        addGlobalAttribute(writeable, element, "standard_name_vocabulary", "NetCDF Climate and Forecast (CF) Metadata Convention");
        addGlobalAttribute(writeable, element, "license", "EC C3S FIRE BURNED AREA Data Policy");
        addGlobalAttribute(writeable, element, "platform", "Sentinel-3");
        addGlobalAttribute(writeable, element, "sensor", "OLCI");
        addGlobalAttribute(writeable, element, "spatial_resolution", "0.00277778");
        addGlobalAttribute(writeable, element, "geospatial_lon_units", "degrees_east");
        addGlobalAttribute(writeable, element, "geospatial_lat_units", "degrees_north");
        addGlobalAttribute(writeable, element, "geospatial_lon_resolution", "0.00277778");
        addGlobalAttribute(writeable, element, "geospatial_lat_resolution", "0.00277778");

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
