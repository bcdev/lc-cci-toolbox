package org.esa.cci.lc.util;


import org.esa.snap.core.datamodel.Product;
import org.esa.snap.dataio.netcdf.ProfileReadContext;
import org.esa.snap.dataio.netcdf.ProfileWriteContext;
import org.esa.snap.dataio.netcdf.metadata.ProfilePartIO;
import org.esa.snap.dataio.netcdf.nc.NFileWriteable;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.IndexIterator;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.Variable;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Additional class used by @CdsNetCdfWriter in order to write down data for the variables which are not bands.
 */
public class CdsVariableWriter extends ProfilePartIO {

    public void decode(ProfileReadContext ctx, Product p)  {
    }
    public void encode(ProfileWriteContext ctx, Product p) throws IOException {
        Array data ;
        final NFileWriteable writeable = ctx.getNetcdfFileWriteable();
        String path = p.getFileLocation().getAbsolutePath();

        NetcdfFileWriter onlyReader = NetcdfFileWriter.openExisting(path);
        List<Variable> list = onlyReader.getNetcdfFile().getVariables();
        String[] listBands = p.getBandNames();
        List bandArray = Arrays.asList(listBands);
        for (Variable variable : list) {
            String variableName = variable.getFullName();
            if (!bandArray.contains(variableName) && !variableName.contains("burned_area_in_vegetation_class")) {
                data = variable.read();
                if (variableName.equals("lat_bnds")) {
                    variableName = "lat_bounds";
                }
                else  if (variableName.equals("lon_bnds")) {
                    variableName = "lon_bounds";
                    IndexIterator iterator = data.getIndexIterator();
                    for (int i = 0; iterator.hasNext(); i++) {
                        data.setDouble(i, data.getDouble(i) + 180);
                        iterator.next();
                    }
                }
                else if (variableName.equals("time_bnds")) {
                    variableName = "time_bounds";
                    double begTime = variable.read().getDouble(0);
                    double endTime = variable.read().getDouble(1);
                    data = Array.factory(DataType.DOUBLE, new int[]{1, 2}, new double[]{begTime, endTime});
                }
                else if (variable.getFullName().equals("lon")) {
                    IndexIterator iterator = data.getIndexIterator();
                    for (int i = 0; iterator.hasNext(); i++) {
                        data.setDouble(i, data.getDouble(i) + 180);
                        iterator.next();
                    }
                }

                try {
                    Object temp = data.get1DJavaArray(ctx.getNetcdfFileWriteable().findVariable(variableName).getDataType());
                    data = Array.makeFromJavaArray(temp);
                    data = data.reshape(variable.getShape());
                    writeable.getWriter().write(writeable.getWriter().findVariable(variableName), data);
                } catch (InvalidRangeException e) {
                }
            }
        }
        onlyReader.close();
        if (p.getMetadataRoot().getElement("global_attributes").getAttributeString("type").equals("ESACCI-LC-L4-LCCS-Map-300m-P1Y")) {
            lcLatLonBoundsWriter(ctx,p);
            timeWriter(ctx,p);
        }
    }



    private void lcLatLonBoundsWriter(ProfileWriteContext ctx, Product p) throws IOException {
        Array data;
        final NFileWriteable writeable = ctx.getNetcdfFileWriteable();
        int numLatPixels = p.getSceneRasterHeight(); // 64800

        double divider = numLatPixels / 180; //360; //number of pixels in 1 degree
        double step = 1 / divider;
        int minLat = -90;

        //lat part
        double[] latArray = new double[2 * numLatPixels];
        int j = 0;
        for (int i = 0; i < numLatPixels; i += 1) {
            latArray[j] = minLat + (i * step);
            latArray[j + 1] = minLat + (i + 1) * step;
            j += 2;
        }
        data = Array.factory(DataType.DOUBLE, new int[]{numLatPixels, 2}, latArray);
        try {
            data = data.flip(0);
            data = data.flip(1);
            writeable.getWriter().write("lat_bounds", data);
        } catch (InvalidRangeException e) {
        }
        //lon part
        int numLonPixels = p.getSceneRasterWidth();//129600;
        divider = numLonPixels / 360; //360; //number of pixels in 1 degree
        step = 1 / divider;
        int minLon = 0;
        double[] lonArray = new double[2 * numLonPixels];

        j = 0;
        for (int i = 0; i < numLonPixels; i += 1) {
            lonArray[j] = minLon + (i * step);
            lonArray[j + 1] = minLon + (i + 1) * step;
            j += 2;
        }
        data = Array.factory(DataType.DOUBLE, new int[]{numLonPixels, 2}, lonArray);
        try {
            writeable.getWriter().write("lon_bounds", data);
        }
        catch (InvalidRangeException e) {
        }
    }
    private void timeWriter (ProfileWriteContext ctx, Product p) throws IOException {
        Array data;
        String path = p.getFileLocation().getAbsolutePath();
        final NFileWriteable writeable = ctx.getNetcdfFileWriteable();
        //time and timebounds for lccs
        String YearString = p.getMetadataRoot().getElement("global_attributes").getAttributeString("time_coverage_start").substring(0, 4);
        try {
            SimpleDateFormat tempFormat = new SimpleDateFormat("dd MM yyyy");
            Date startDay = tempFormat.parse("01 01 " + YearString);
            Date finalDay = tempFormat.parse("31 12 " + YearString);
            Date startCalendar = tempFormat.parse("01 01 1970");

            long startYear = (startDay.getTime() - startCalendar.getTime());
            double begTime = TimeUnit.DAYS.convert(startYear, TimeUnit.MILLISECONDS);
            long endYear = (finalDay.getTime() - startCalendar.getTime());
            double endTime = TimeUnit.DAYS.convert(endYear, TimeUnit.MILLISECONDS);
            data = Array.factory(DataType.DOUBLE, new int[]{1, 2}, new double[]{begTime, endTime});

            try {
                writeable.getWriter().write("time_bounds", data);
            } catch (InvalidRangeException e) {
            }
            data = Array.factory(DataType.DOUBLE, new int[]{1}, new double[]{begTime});

            try {
                writeable.getWriter().write("time", data);
            } catch (InvalidRangeException e) {
            }
        } catch (ParseException e) {
        }
    }



    @Override
    public void preEncode(ProfileWriteContext ctx, Product p) throws IOException{}


}