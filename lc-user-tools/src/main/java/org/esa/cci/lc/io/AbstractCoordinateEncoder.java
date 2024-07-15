package org.esa.cci.lc.io;

import org.esa.snap.binning.PlanetaryGrid;
import org.esa.snap.dataio.netcdf.nc.NFileWriteable;
import org.esa.snap.dataio.netcdf.nc.NVariable;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.nc2.Attribute;

import java.io.IOException;

/**
 * @author Marco Peters
 */
abstract class AbstractCoordinateEncoder implements CoordinateEncoder {
    protected final PlanetaryGrid planetaryGrid;
    protected NVariable latVar;
    protected NVariable lonVar;

    public AbstractCoordinateEncoder(PlanetaryGrid planetaryGrid) {
        this.planetaryGrid = planetaryGrid;
    }

    @Override
    public void addCoordVars(NFileWriteable writeable) throws IOException {
        latVar = writeable.addVariable("lat", DataType.FLOAT, null, "lat");
        Attribute attributeLatUnits = latVar.addAttribute("units", "degrees_north");
        Attribute attributeLatLongName = latVar.addAttribute("long_name", "latitude");
        Attribute attributeLatStandardName = latVar.addAttribute("standard_name", "latitude");

        lonVar = writeable.addVariable("lon", DataType.FLOAT, null, "lon");
        Attribute attributeLonUnits = lonVar.addAttribute("units", "degrees_east");
        Attribute attributeLonLongName = lonVar.addAttribute("long_name", "longitude");
        Attribute attributeLonStandardName = lonVar.addAttribute("standard_name", "longitude");
    }

    @Override
    public void fillCoordinateVars(NFileWriteable writeable) throws IOException {
        int sceneHeight = planetaryGrid.getNumRows();
        int sceneWidth = planetaryGrid.getNumCols(0);

        final float[] lats = getLatValues(sceneHeight);
        latVar.writeFully(Array.factory(DataType.FLOAT,new int[]{sceneHeight},lats));

        final float[] lons = getLonValues(sceneWidth);
        lonVar.writeFully(Array.factory(DataType.FLOAT,new int[]{sceneWidth},lons));
    }

    protected abstract float[] getLonValues(int sceneWidth);

    protected abstract float[] getLatValues(int sceneHeight);
}
