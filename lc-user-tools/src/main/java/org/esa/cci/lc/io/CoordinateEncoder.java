package org.esa.cci.lc.io;

import org.esa.beam.dataio.netcdf.nc.NFileWriteable;

import java.io.IOException;

interface CoordinateEncoder {

    void addCoordVars(NFileWriteable writeable) throws IOException;

    void fillCoordinateVars(NFileWriteable writeable) throws IOException;
}
