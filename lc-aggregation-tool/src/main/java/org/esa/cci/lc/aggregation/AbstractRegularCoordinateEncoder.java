package org.esa.cci.lc.aggregation;

import org.esa.beam.binning.PlanetaryGrid;
import org.esa.beam.dataio.netcdf.nc.NFileWriteable;
import org.esa.beam.dataio.netcdf.nc.NVariable;
import ucar.ma2.Array;

import java.io.IOException;

public abstract class AbstractRegularCoordinateEncoder implements CoordinateEncoder {

    protected final PlanetaryGrid planetaryGrid;
    protected NVariable latVar;
    protected NVariable lonVar;

    public AbstractRegularCoordinateEncoder(PlanetaryGrid planetaryGrid) {
        this.planetaryGrid = planetaryGrid;
    }

    @Override
    public abstract void addCoordVars(NFileWriteable writeable) throws IOException;

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
