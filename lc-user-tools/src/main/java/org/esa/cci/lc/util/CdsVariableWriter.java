package org.esa.cci.lc.util;


import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.dataio.netcdf.ProfileReadContext;
import org.esa.snap.dataio.netcdf.ProfileWriteContext;
import org.esa.snap.dataio.netcdf.metadata.ProfilePartIO;
import org.esa.snap.dataio.netcdf.nc.NFileWriteable;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Additional class used by @CdsNetCdfWriter in order to write down data for the variables which are not bands.
 */
public class CdsVariableWriter extends ProfilePartIO {

    public void decode(ProfileReadContext ctx, Product p)  {
    }
    public void encode(ProfileWriteContext ctx, Product p) throws IOException {
        Array data ;
        Double lonMin;
        Double lonMax;
        final NFileWriteable writeable = ctx.getNetcdfFileWriteable();
        MetadataElement element = p.getMetadataRoot().getElement("global_attributes");
        String path = element.getAttributeString("parent_path");

        Double latMin = p.getMetadataRoot().getElement("global_attributes").getAttributeDouble("geospatial_lat_min");
        Double latMax = p.getMetadataRoot().getElement("global_attributes").getAttributeDouble("geospatial_lat_max");
        if (element.containsAttribute("subsetted") || path.endsWith(".tif")) {
             lonMin = p.getMetadataRoot().getElement("global_attributes").getAttributeDouble("geospatial_lon_min");
             lonMax = p.getMetadataRoot().getElement("global_attributes").getAttributeDouble("geospatial_lon_max");
        }
        else {
            lonMin = 0D ;
            lonMax = 360D;
        }
        lcLatLonCustomBoundsWriter(writeable, p.getSceneRasterHeight(), p.getSceneRasterWidth(), lonMin, lonMax, latMin, latMax);

        timeWriter(writeable, element);

    }

    public static void lcLatLonCustomBoundsWriter(NFileWriteable writeable, int heigth, int width, Double minLon, Double maxLon, Double minLat, Double maxLat) throws IOException {
        Array data;
        int numLatPixels = heigth;
        double divider = numLatPixels / (maxLat - minLat);
        double step = 1 / divider;

        double[] latArray = new double[2 * numLatPixels];
        double[] lat = new double[ numLatPixels];
        int j = 0;
        for (int i = 0; i < numLatPixels; i += 1) {
            latArray[j] = minLat + (i * step);
            latArray[j + 1] = minLat + (i + 1) * step;
            j += 2;
            lat[i]=minLat + ((i+0.5) * step);
        }
        data = Array.factory(DataType.DOUBLE, new int[]{numLatPixels, 2}, latArray);
        try {
            data = data.flip(0);
            data = data.flip(1);
            writeable.getWriter().write("lat_bounds", data);
        } catch (InvalidRangeException e) {
        }
        data = Array.factory(DataType.DOUBLE,new int[]{numLatPixels, 1}, lat);
        data = data.flip(0);
        try {
            writeable.getWriter().write("lat", data);
        }
        catch (InvalidRangeException e) {
        }
        //lon part
        int numLonPixels = width;
        if (minLon>maxLon){maxLon=maxLon+360;}
        divider = numLonPixels / (maxLon -minLon);
        step = 1 / divider;
        double[] lonArray = new double[2 * numLonPixels];
        double[] lon = new double[ numLonPixels];
        j = 0;
        for (int i = 0; i < numLonPixels; i += 1) {
            lonArray[j] = (minLon + (i * step))%360;
            lonArray[j + 1] = (minLon + (i + 1) * step)%360;
            lon[i]=(minLon + ((i+0.5) * step))%360;
            if ((minLon + (i * step))==360){
                lonArray[j]=360;
            }
            if ((minLon + (i + 1) * step)==360){
                lonArray[j + 1]=360;
            }
            if (minLon + ((i+0.5) * step)==360){
                lon[i]=360;
            }
            j += 2;
        }
        data = Array.factory(DataType.DOUBLE, new int[]{numLonPixels, 2}, lonArray);
        try {
            writeable.getWriter().write("lon_bounds", data);
        }
        catch (InvalidRangeException e) {
        }
        data = Array.factory(DataType.DOUBLE,new int[]{numLonPixels, 1}, lon);
        try {
            writeable.getWriter().write("lon", data);
        }
        catch (InvalidRangeException e) {
        }
    }

    public static void timeWriter (NFileWriteable writeable, MetadataElement element) throws IOException {
        Array data;
        String path = element.getAttributeString("parent_path");
        //time and timebounds for lccs
        String YearString = element.getAttributeString("time_coverage_start").substring(0, 4);
        String firstMonth = element.getAttributeString("time_coverage_start").substring(4, 6);
        String lastMonth = element.getAttributeString("time_coverage_end").substring(4, 6);
        String firstDay = element.getAttributeString("time_coverage_start").substring(6, 8);
        String lastDay = element.getAttributeString("time_coverage_end").substring(6, 8);
        try {
            SimpleDateFormat tempFormat = new SimpleDateFormat("dd MM yyyy");
            Date startDay = tempFormat.parse(firstDay+" "+firstMonth+" "+ YearString);
            Date finalDay = tempFormat.parse(lastDay+" "+lastMonth+" "+YearString);
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