package org.esa.cci.lc.util;

import org.esa.beam.binning.PlanetaryGrid;
import org.esa.beam.binning.support.RegularGaussianGrid;
import org.esa.beam.util.StringUtils;
import org.esa.beam.util.io.CsvReader;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Implementation of a regular gaussian grid. It is often used in the climate modelling community.
 * <p/>
 * The grid points of a gaussian grid along each latitude are equally spaced. This means that the distance in degree
 * between two adjacent longitudes is the same.
 * Along the longitudes the grid points are not equally spaced. The distance varies along the meridian.
 * There are two types of the gaussian grid. The regular and the reduced grid.
 * While the regular grid has for each grid row the same number of columns, the number of columns varies in the reduced
 * grid type.
 *
 * @author Marco Peters
 */
public class LcRegularGaussianGrid implements PlanetaryGrid {

    private final GaussianGridConfig config;
    private final int numRows;

    /**
     * Creates a new regular gaussian grid.
     *
     * @param numRows the number of rows of the grid (from pole to pole)
     */
    public LcRegularGaussianGrid(int numRows) {
        this.numRows = numRows;
        try {
            config = GaussianGridConfig.load(numRows / 2);
        } catch (IOException e) {
            throw new IllegalStateException("Could not create gaussian grid: " + e.getMessage(), e);
        }
    }

    @Override
    public long getBinIndex(double lat, double lon) {
        int rowIndex = findClosestInArray(config.getLatitudePoints(), lat);
        int colIndex = findClosestInArray(config.getRegularLongitudePoints(), lon);
        return getFirstBinIndex(rowIndex) + colIndex;
    }

    @Override
    public int getRowIndex(long binIndex) {
        return (int) (binIndex / (config.getRegularColumnCount()));
    }

    @Override
    public long getNumBins() {
        return getNumRows() * config.getRegularColumnCount();
    }

    @Override
    public int getNumRows() {
        return numRows;
    }

    @Override
    public int getNumCols(int rowIndex) {
        validateRowIndex(rowIndex);
        return config.getRegularColumnCount();
    }

    @Override
    public long getFirstBinIndex(int rowIndex) {
        validateRowIndex(rowIndex);
        return rowIndex * getNumCols(rowIndex);
    }

    @Override
    public double getCenterLat(int rowIndex) {
        validateRowIndex(rowIndex);
        return config.getLatitude(rowIndex);
    }

    @Override
    public double[] getCenterLatLon(long bin) {
        int row = getRowIndex(bin);
        int col = getColumnIndex(bin);
        double latitude = getCenterLat(row);
        double longitude = config.getRegularLongitudePoints()[col];

        return new double[]{latitude, longitude};
    }

    private void validateRowIndex(int rowIndex) {
        int maxRowIndex = getNumRows() - 1;
        if (rowIndex > maxRowIndex) {
            String msg = String.format("Invalid row index. Maximum allowed is %d, but was %d.", maxRowIndex, rowIndex);
            throw new IllegalArgumentException(msg);
        }
    }

    static int findClosestInArray(double[] array, double value) {
        double dist = Double.NaN;
        for (int i = 0; i < array.length; i++) {
            double arrayValue = array[i];
            double currentDist = Math.abs(arrayValue - value);
            if (currentDist > dist) {
                return i - 1; // previous in array
            }
            dist = currentDist;
        }
        // if not yet found it is the last one
        return array.length - 1;
    }

    private int getColumnIndex(long bin) {
        int rowIndex = getRowIndex(bin);
        long firstBinIndex = getFirstBinIndex(rowIndex);
        return (int) (bin - firstBinIndex);
    }

    static class GaussianGridConfig {

        private static final int[] ALLOWED_ROW_COUNTS = new int[]{32, 48, 80, 128, 160, 200, 256, 320, 400, 512, 640};

        private int[] reducedColumnCount;
        private int[] reducedFirstBinIndexes;
        private double[] regularLongitudePoints;
        private List<double[]> reducedLongitudePoints;
        private int regularColumnCount;
        private double[] latitudePoints;

        public static GaussianGridConfig load(int rowCount) throws IOException {
            if (Arrays.binarySearch(ALLOWED_ROW_COUNTS, rowCount) < 0) {
                String msg = String.format("Invalid rowCount. Must be one of {%s}, but is %d",
                                           StringUtils.arrayToCsv(ALLOWED_ROW_COUNTS), rowCount);
                throw new IllegalArgumentException(msg);
            }
            int numRecords = rowCount * 2;
            int regularColumnCount = rowCount * 4;
            int[] reducedColumnCount = new int[numRecords];
            double[] latitudePoints = new double[numRecords];
            int[] reducedFirstBinIndexes = new int[numRecords];
            readGridConfig(rowCount, numRecords, reducedColumnCount, latitudePoints, reducedFirstBinIndexes);

            GaussianGridConfig config = new GaussianGridConfig();
            config.regularColumnCount = regularColumnCount;
            config.regularLongitudePoints = computeLongitudePoints(regularColumnCount);
            config.reducedLongitudePoints = new ArrayList<>(numRecords);
            for (int i = 0; i < numRecords; i++) {
                double[] longitudePointsInRow = computeLongitudePoints(reducedColumnCount[i]);
                config.reducedLongitudePoints.add(i, longitudePointsInRow);
            }
            config.reducedColumnCount = reducedColumnCount;
            config.reducedFirstBinIndexes = reducedFirstBinIndexes;
            config.latitudePoints = latitudePoints;


            return config;
        }

        private GaussianGridConfig() {
        }

        public int getRegularColumnCount() {
            return regularColumnCount;
        }

        public double[] getRegularLongitudePoints() {
            return regularLongitudePoints;
        }

        public int getReducedColumnCount(int rowIndex) {
            return reducedColumnCount[rowIndex];
        }

        public double[] getReducedLongitudePoints(int rowIndex) {
            return reducedLongitudePoints.get(rowIndex);
        }

        public int getReducedFirstBinIndex(int rowIndex) {
            return reducedFirstBinIndexes[rowIndex];
        }

        public double getLatitude(int row) {
            return latitudePoints[row];
        }

        public double[] getLatitudePoints() {
            return latitudePoints;
        }

        static double[] computeLongitudePoints(int columnCount) {
            double[] longitudePoints = new double[columnCount];
            for (int i = 0; i < longitudePoints.length; i++) {
                longitudePoints[i] = (i + 0.5) * (360.0 / columnCount) - 180.0;
            }
            return longitudePoints;
        }

        private static void readGridConfig(int rowCount, int numRecords, int[] reducedColumnCount, double[] latitudePoints,
                                           int[] reducedFirstBinIndexes) throws IOException {
            InputStream is = RegularGaussianGrid.class.getResourceAsStream(String.format("N%d.txt", rowCount));
            reducedFirstBinIndexes[0] = 0;
            try (CsvReader csvReader = new CsvReader(new InputStreamReader(is), new char[]{'\t'}, true, "#")) {
                for (int i = 0; i < numRecords; i++) {
                    String[] record = csvReader.readRecord();
                    reducedColumnCount[i] = Integer.parseInt(record[0]);
                    latitudePoints[i] = Double.parseDouble(record[2]);
                    if (i > 0) {
                        reducedFirstBinIndexes[i] = reducedFirstBinIndexes[i - 1] + reducedColumnCount[i - 1];
                    }
                }
            }
        }

    }

}
