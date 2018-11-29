/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.cci.lc.io;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.dataio.netcdf.AbstractNetCdfWriterPlugIn;
import org.esa.snap.dataio.netcdf.DefaultNetCdfWriter;
import org.esa.snap.dataio.netcdf.nc.NVariable;
import org.esa.snap.dataio.netcdf.util.ReaderUtils;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Variable;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

/**
 * cds NetCDF writer configured by an implementation of {@link AbstractNetCdfWriterPlugIn}.
 */
public class CdsNetCdfWriter extends DefaultNetCdfWriter   {
    private HashMap<String, NVariable> variableMap;
    AbstractNetCdfWriterPlugIn plugIn = getWriterPlugIn();




    public CdsNetCdfWriter(AbstractNetCdfWriterPlugIn writerPlugIn) {
        super(writerPlugIn);
        variableMap = new HashMap<>();
    }

    @Override
    public AbstractNetCdfWriterPlugIn getWriterPlugIn() {
        return (AbstractNetCdfWriterPlugIn) super.getWriterPlugIn();
    }

    @Override
    public void writeBandRasterData(Band sourceBand, int sourceOffsetX, int sourceOffsetY, int sourceWidth,
                                    int sourceHeight, ProductData sourceBuffer, ProgressMonitor pm) throws IOException {
        final String variableName = ReaderUtils.getVariableName(sourceBand);


        if (!sourceBand.getProduct().getMetadataRoot().getElement("global_attributes").getAttributeString("parent_path").endsWith(".tif")) {
            if (shallWriteVariable(variableName)  ) {
                writeBandNoShift(sourceBand, sourceOffsetX, sourceOffsetY, sourceWidth, sourceHeight, sourceBuffer, pm, variableName);
            } else if (variableName.contains("burned_area_in_vegetation_class")) {
                writeBurnedAreaNoShift(sourceBand, sourceOffsetX, sourceOffsetY, sourceWidth, sourceHeight, sourceBuffer, pm, variableName);
            }
        }
        else if (sourceBand.getProduct().getMetadataRoot().getElement("global_attributes").getAttributeString("parent_path").endsWith(".tif")) {
            if (shallWriteVariable(variableName)) {
                writeBandNoShift(sourceBand, sourceOffsetX, sourceOffsetY, sourceWidth, sourceHeight, sourceBuffer, pm, variableName);
            }
        }

    }

    private void writeBandNoShift(Band sourceBand, int sourceOffsetX, int sourceOffsetY, int sourceWidth,
                                  int sourceHeight, ProductData sourceBuffer, ProgressMonitor pm,String variableName) throws IOException {
        ProductData scaledBuffer = sourceBuffer;
        synchronized (getWriteable()) {
            Object elems = scaledBuffer.getElems();
            Variable variable = getWriteable().getWriter().findVariable(variableName);
            final int[] shape = new int[]{1, sourceHeight, sourceWidth};
            final int[] origin = new int[]{0, sourceOffsetY, sourceOffsetX};
            Array array = Array.factory(variable.getDataType(), shape, elems);
            try {
                getWriteable().getWriter().write(variable, origin, array);
            }
            catch (InvalidRangeException e) {
            }
        }
    }



    private void writeBandWithShift(Band sourceBand, int sourceOffsetX, int sourceOffsetY, int sourceWidth,
                                    int sourceHeight, ProductData sourceBuffer, ProgressMonitor pm,String variableName) throws IOException
    {
        ProductData scaledBuffer = sourceBuffer;

        synchronized (getWriteable()) {
            Object elems = scaledBuffer.getElems();
            Variable variable = getWriteable().getWriter().findVariable(variableName);
            final int totalWidthHalf = sourceBand.getRasterWidth() / 2;
            final int totalWidth = sourceBand.getRasterWidth();

            if (sourceOffsetX >= totalWidthHalf) {
                final int[] shape = new int[]{1, sourceHeight, sourceWidth};
                final int[] origin = new int[]{0, sourceOffsetY, sourceOffsetX - totalWidthHalf};
                Array array = Array.factory(variable.getDataType(), shape, elems);
                try {
                    getWriteable().getWriter().write(variable, origin, array);
                } catch (InvalidRangeException e) {
                }
            } else if (sourceOffsetX <= totalWidthHalf && (sourceOffsetX + sourceWidth + totalWidthHalf) <= (totalWidth)) {
                sourceOffsetX = sourceOffsetX + totalWidthHalf;
                final int[] shape = new int[]{1, sourceHeight, sourceWidth};
                final int[] origin = new int[]{0, sourceOffsetY, sourceOffsetX};
                Array array = Array.factory(variable.getDataType(), shape, elems);
                try {
                    getWriteable().getWriter().write(variable, origin, array);
                } catch (InvalidRangeException e) {
                }
            } else {
                final int[] shape = new int[]{1, sourceHeight, sourceWidth};
                int[] originX1 = new int[]{0, sourceOffsetY, sourceOffsetX + totalWidthHalf};
                int[] originX2 = new int[]{0, sourceOffsetY, 0};
                int[] shape1 = new int[]{1, sourceHeight, totalWidthHalf - sourceOffsetX};
                int[] shape2 = new int[]{1, sourceHeight, sourceWidth - totalWidthHalf + sourceOffsetX};
                Array array = Array.factory(variable.getDataType(), shape, elems);
                int[] originCut1 = new int[]{0, 0, 0};
                int[] originCut2 = new int[]{0, 0, totalWidth - sourceOffsetX - totalWidthHalf};
                try {
                    Array piece1 = array.sectionNoReduce(originCut1, shape1, new int[]{1, 1, 1});
                    Array piece2 = array.sectionNoReduce(originCut2, shape2, new int[]{1, 1, 1});
                    getWriteable().getWriter().write(variable, originX1, piece1);
                    getWriteable().getWriter().write(variable, originX2, piece2);
                } catch (InvalidRangeException e) {
                }
            }
        }

    }


    private void writeBurnedAreaWithShift(Band sourceBand, int sourceOffsetX, int sourceOffsetY, int sourceWidth,
                                          int sourceHeight, ProductData sourceBuffer, ProgressMonitor pm,String variableName) throws IOException
    {
        int vegetationClass = Integer.parseInt(variableName.replace("burned_area_in_vegetation_class_vegetation_class",""));
        ProductData scaledBuffer = sourceBuffer;

        synchronized (getWriteable()) {
            Object elems = scaledBuffer.getElems();
            Variable variable = getWriteable().getWriter().findVariable("burned_area_in_vegetation_class");

            final int totalWidthHalf = sourceBand.getRasterWidth() / 2;
            final int totalWidth = sourceBand.getRasterWidth();

            if (sourceOffsetX >= totalWidthHalf) {
                final int[] shape = new int[]{1, 1, sourceHeight, sourceWidth};
                final int[] origin = new int[]{0, vegetationClass - 1, sourceOffsetY, sourceOffsetX - totalWidthHalf};
                Array array = Array.factory(variable.getDataType(), shape, elems);
                try {
                    getWriteable().getWriter().write(variable, origin, array);
                } catch (InvalidRangeException e) {
                }
            } else if (sourceOffsetX <= totalWidthHalf && (sourceOffsetX + sourceWidth + totalWidthHalf) <= (totalWidth)) {
                sourceOffsetX = sourceOffsetX + totalWidthHalf;
                final int[] shape = new int[]{1, 1, sourceHeight, sourceWidth};
                final int[] origin = new int[]{0, vegetationClass - 1, sourceOffsetY, sourceOffsetX};
                Array array = Array.factory(variable.getDataType(), shape, elems);
                try {
                    getWriteable().getWriter().write(variable, origin, array);
                } catch (InvalidRangeException e) {
                }

            } else {
                final int[] shape = new int[]{1, 1, sourceHeight, sourceWidth};
                int[] originX1 = new int[]{0, vegetationClass - 1, sourceOffsetY, sourceOffsetX + totalWidthHalf};
                int[] originX2 = new int[]{0, vegetationClass - 1, sourceOffsetY, 0};
                int[] shape1 = new int[]{1, 1, sourceHeight, totalWidthHalf - sourceOffsetX};
                int[] shape2 = new int[]{1, 1, sourceHeight, sourceWidth - totalWidthHalf + sourceOffsetX};
                Array array = Array.factory(variable.getDataType(), shape, elems);
                int[] originCut1 = new int[]{0, 0, 0, 0};
                int[] originCut2 = new int[]{0, 0, 0, totalWidth - sourceOffsetX - totalWidthHalf};
                try {
                    Array piece1 = array.sectionNoReduce(originCut1, shape1, new int[]{1, 1, 1, 1});
                    Array piece2 = array.sectionNoReduce(originCut2, shape2, new int[]{1, 1, 1, 1});
                    getWriteable().getWriter().write(variable, originX1, piece1);
                    getWriteable().getWriter().write(variable, originX2, piece2);
                } catch (InvalidRangeException e) {
                }
            }
        }
    }

    private void writeBurnedAreaNoShift(Band sourceBand, int sourceOffsetX, int sourceOffsetY, int sourceWidth,
                                          int sourceHeight, ProductData sourceBuffer, ProgressMonitor pm,String variableName) throws IOException {
        int vegetationClass = Integer.parseInt(variableName.replace("burned_area_in_vegetation_class_vegetation_class",""));
        ProductData scaledBuffer = sourceBuffer;

        synchronized (getWriteable()) {
            Object elems = scaledBuffer.getElems();
            Variable variable = getWriteable().getWriter().findVariable("burned_area_in_vegetation_class");


            final int[] shape = new int[]{1, 1, sourceHeight, sourceWidth};
            final int[] origin = new int[]{0, vegetationClass - 1, sourceOffsetY, sourceOffsetX};
            Array array = Array.factory(variable.getDataType(), shape, elems);
            try {
                getWriteable().getWriter().write(variable, origin, array);
            } catch (InvalidRangeException e) {
            }
        }
    }

    private boolean shallWriteVariable(String variableName) {
        if (getWriteable() == null) {
            throw new IllegalStateException("NetCdf writer not properly initialised. Consider calling writeProductNodes() before writing data.");
        }
        return getWriteable().getWriter().findVariable(variableName)!=null;
    }

    private File getOutputFile() {
        return new File(getOutputString());
    }

    private String getOutputString() {
        return String.valueOf(getOutput());
    }

}
