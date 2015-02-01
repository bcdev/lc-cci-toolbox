package org.esa.cci.lc.io;

import org.esa.beam.binning.PlanetaryGrid;
import org.esa.beam.dataio.netcdf.nc.NFileWriteable;
import org.esa.beam.dataio.netcdf.nc.NVariable;
import ucar.ma2.Array;
import ucar.ma2.DataType;

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
        latVar.addAttribute("units", "degrees_north");
        latVar.addAttribute("long_name", "latitude");
        latVar.addAttribute("standard_name", "latitude");

        lonVar = writeable.addVariable("lon", DataType.FLOAT, null, "lon");
        lonVar.addAttribute("units", "degrees_east");
        lonVar.addAttribute("long_name", "longitude");
        lonVar.addAttribute("standard_name", "longitude");
    }

    @Override
    public void fillCoordinateVars(NFileWriteable writeable) throws IOException {
        int sceneHeight = planetaryGrid.getNumRows();
        int sceneWidth = planetaryGrid.getNumCols(0);

        final float[] lats = getLatValues(sceneHeight);
        latVar.writeFully(Array.factory(lats));

        final float[] lons = getLonValues(sceneWidth);
        lonVar.writeFully(Array.factory(lons));
    }

    protected abstract float[] getLonValues(int sceneWidth);

    protected abstract float[] getLatValues(int sceneHeight);
}
