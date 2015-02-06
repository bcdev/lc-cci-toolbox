package org.esa.cci.lc.io;

import org.esa.beam.binning.PlanetaryGrid;
import org.geotools.geometry.jts.ReferencedEnvelope;

/**
 * @author Marco Peters
 */
public class RegionalPlanetaryGrid implements PlanetaryGrid {

    private final PlanetaryGrid globalGrid;
    private final ReferencedEnvelope region;

    private long binIndexOffset;
    private long binIndexMax;
    private int rowOffset;
    private int columnOffset;
    private int numRows;
    private long numBins;
    private int numCols;

    public RegionalPlanetaryGrid(PlanetaryGrid globalGrid, ReferencedEnvelope region) {
        this.globalGrid = globalGrid;
        this.region = new ReferencedEnvelope(region);
        initBinIndexValues(globalGrid);
    }

    public PlanetaryGrid getGlobalGrid() {
        return globalGrid;
    }

    public int getRowOffset() {
        return rowOffset;
    }

    public int getColumnOffset() {
        return columnOffset;
    }

    @Override
    public long getBinIndex(double lat, double lon) {
        if (!contains(lat, lon)) {
            final String msg = String.format("Location (%f, %f) not contained in this grid", lat, lon);
            throw new IllegalArgumentException(msg);
        }
        return globalGrid.getBinIndex(lat, lon);
    }

    @Override
    public int getRowIndex(long bin) {
        if (!isBinIndexInRegionalGrid(bin)) {
            final String msg = String.format("Bin index (%d) not contained in this grid", bin);
            throw new IllegalArgumentException(msg);
        }
        return globalGrid.getRowIndex(bin) - rowOffset;
    }

    @Override
    public long getNumBins() {
        return numBins;
    }

    @Override
    public int getNumRows() {
        return numRows;
    }

    @Override
    public int getNumCols(int row) {
        return numCols;
    }

    @Override
    public long getFirstBinIndex(int row) {
        return globalGrid.getFirstBinIndex(rowOffset + row) + columnOffset;
    }

    @Override
    public double getCenterLat(int row) {
        return globalGrid.getCenterLat(rowOffset + row);
    }

    @Override
    public double[] getCenterLatLon(long bin) {
        if (!isBinIndexInRegionalGrid(bin)) {
            return new double[]{Double.NaN, Double.NaN};
        }
        return globalGrid.getCenterLatLon(bin);
    }

    public boolean isBinIndexInRegionalGrid(long bin) {
        if (bin < binIndexOffset || bin > binIndexMax) {
            return false;
        }
        final int numColsParent = globalGrid.getNumCols(0); // OK to use one value for all rows, cause we assume rectangular grids for now
        for (int i = 0; i < getNumRows(); i++) {
            final long startBinIndex = getFirstBinIndex(i);
            final long endBinIndex = startBinIndex + numCols - 1;
            final long endParentRowBinIndex = globalGrid.getFirstBinIndex(i + rowOffset) + numColsParent - 1;
            if (bin >= startBinIndex && bin <= endBinIndex) {
                return true;
            } else if (bin < startBinIndex || (bin > endBinIndex && bin <= endParentRowBinIndex)) {
                return false;
            }
        }
        return false;
    }

    private void initBinIndexValues(PlanetaryGrid globalGrid) {
        binIndexOffset = globalGrid.getBinIndex(getLatMaximum(), getLonMinimum());
        binIndexMax = globalGrid.getBinIndex(getLatMinimum(), getLonMaximum());
        final long urIndex = globalGrid.getBinIndex(getLatMaximum(), getLonMaximum());
        numCols = (int) (urIndex - binIndexOffset) + 1;
        rowOffset = globalGrid.getRowIndex(binIndexOffset);
        columnOffset = (int) (binIndexOffset - globalGrid.getFirstBinIndex(rowOffset));
        int maxRowIndex = globalGrid.getRowIndex(binIndexMax);
        this.numRows = (maxRowIndex + 1) - rowOffset;
        numBins = numCols * numRows;
    }

    private boolean contains(double lat, double lon) {
        return lat <= getLatMaximum() && lat >= getLatMinimum() &&
                lon <= getLonMaximum() && lon >= getLonMinimum();
    }

    private double getLonMaximum() {
        return region.getMaximum(0);
    }

    private double getLatMinimum() {
        return region.getMinimum(1);
    }

    private double getLonMinimum() {
        return region.getMinimum(0);
    }

    private double getLatMaximum() {
        return region.getMaximum(1);
    }

}
