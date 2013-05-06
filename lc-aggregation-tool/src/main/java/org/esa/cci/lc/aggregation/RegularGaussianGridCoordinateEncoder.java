package org.esa.cci.lc.aggregation;

import org.esa.beam.binning.PlanetaryGrid;
import org.esa.beam.dataio.netcdf.nc.NFileWriteable;
import ucar.ma2.DataType;

import java.io.IOException;

class RegularGaussianGridCoordinateEncoder extends AbstractRegularCoordinateEncoder  {

    public RegularGaussianGridCoordinateEncoder(PlanetaryGrid planetaryGrid) {
        super(planetaryGrid);
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
}
