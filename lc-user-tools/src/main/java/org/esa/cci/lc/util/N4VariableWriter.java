package org.esa.cci.lc.util;


import org.esa.snap.core.datamodel.Product;
import org.esa.snap.dataio.netcdf.ProfileReadContext;
import org.esa.snap.dataio.netcdf.ProfileWriteContext;
import org.esa.snap.dataio.netcdf.metadata.ProfilePartIO;
import org.esa.snap.dataio.netcdf.nc.NFileWriteable;
import ucar.ma2.*;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.Variable;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Additional class used by @FireNetCdfWriter in order to write down data for the variables which are not bands.
 */
public class N4VariableWriter extends ProfilePartIO {

    public void decode(ProfileReadContext ctx, Product p)  {
    }
    public void encode(ProfileWriteContext ctx, Product p) throws IOException {
        final NFileWriteable writeable = ctx.getNetcdfFileWriteable();
        String path = p.getFileLocation().getAbsolutePath();
        NetcdfFileWriter onlyReader = NetcdfFileWriter.openExisting(path);
        List<Variable> list = onlyReader.getNetcdfFile().getVariables();
        String[] listBands= p.getBandNames();
        List bandArray = Arrays.asList(listBands);

        for (Variable variable : list) {
            String variableName = variable.getFullName();

            if (!bandArray.contains(variableName) && !variableName.contains("burned_area_in_vegetation_class")) {
                Array data=Array.factory(DataType.FLOAT, new int[]{1}, new float[]{0});
                if (variable.getDimensionsString().contains(" nv")) {
                    {
                        if (variableName.equals("lat_bnds")) {
                            variableName = "latitude_bounds";
                            data = variable.read();
                        }
                        else if (variableName.equals("lon_bnds")) {
                            variableName = "longitude_bounds";
                            data = variable.read();
                            IndexIterator iterator = data.getIndexIterator();
                            for (int i = 0; iterator.hasNext(); i++) {
                                data.setFloat(i, data.getFloat(i) + 180);
                                iterator.next();
                            }
                        } else if (variableName.equals("time_bnds")) {
                            variableName="time_bounds";
                            float begTime = variable.read().getFloat(0);
                            float endTime = variable.read().getFloat(1);
                            data = Array.factory(DataType.FLOAT, new int[]{1,2}, new float[]{begTime,endTime});
                        }
                    }
                } else {
                    data = variable.read();
                    if (variable.getFullName().equals("lon")) {
                        IndexIterator iterator = data.getIndexIterator();
                        for (int i = 0; iterator.hasNext(); i++) {
                            data.setFloat(i, data.getFloat(i) + 180);
                            iterator.next();
                        }
                    }
                }
                try {
                    writeable.getWriter().write(writeable.getWriter().findVariable(variableName), data);
                } catch (InvalidRangeException e) {
                }
            }
        }
        onlyReader.close();
    }

    @Override
    public void preEncode(ProfileWriteContext ctx, Product p) throws IOException{}


}