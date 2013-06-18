package org.esa.cci.lc.io;

import org.esa.beam.binning.PlanetaryGrid;
import org.esa.beam.dataio.netcdf.nc.NFileWriteable;
import org.esa.beam.dataio.netcdf.nc.NVariable;
import ucar.ma2.Array;
import ucar.ma2.DataType;

import java.io.IOException;

class RegularCoordinateEncoder implements CoordinateEncoder {

    protected final PlanetaryGrid planetaryGrid;
    protected NVariable latVar;
    protected NVariable lonVar;

    public RegularCoordinateEncoder(PlanetaryGrid planetaryGrid) {
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

        final float[] lats = new float[sceneHeight];
        for (int i = 0; i < sceneHeight; i++) {
            lats[i] = (float) planetaryGrid.getCenterLat(i);
        }
        latVar.writeFully(Array.factory(lats));

        int sceneWidth = planetaryGrid.getNumCols(0);
        final float[] lons = new float[sceneWidth];
        for (int i = 0; i < sceneWidth; i++) {
            lons[i] = (float) planetaryGrid.getCenterLatLon(i)[1];
        }
        lonVar.writeFully(Array.factory(lons));
    }
}
