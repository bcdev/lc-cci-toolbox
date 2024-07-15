package org.esa.cci.lc.io;

import org.esa.snap.binning.PlanetaryGrid;
import org.esa.snap.binning.support.PlateCarreeGrid;
import org.esa.snap.binning.support.RegularGaussianGrid;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.Test;

import java.awt.geom.Rectangle2D;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * @author Marco Peters
 */
public class RegionalPlanetaryGridTest {

    private static final Rectangle2D.Double GEO_REGION = new Rectangle2D.Double(-3.4, 61.7, 20, -40); // height: minus because lat is upside-down
    private static final Rectangle2D.Double CENTRAL_AMERICA = new Rectangle2D.Double(-93, 28, 34, -21);
    private static final Rectangle2D.Double GLOBAL_REGION = new Rectangle2D.Double(-180, 90, 360, -180);

    @Test
    public void testGeneralFunctionality() throws Exception {
        final RegionalPlanetaryGrid regionalGrid = createRegionalPlanetaryGrid(GEO_REGION);

        assertEquals(401, regionalGrid.getNumRows());
        assertEquals(201, regionalGrid.getNumCols(0));
        assertEquals(201, regionalGrid.getNumCols(36));
        assertEquals(201, regionalGrid.getNumCols(289));
        assertEquals(201 * 401, regionalGrid.getNumBins());

        assertEquals(1800 - 34, regionalGrid.getColumnOffset());
        assertEquals(900 - (617 + 1), regionalGrid.getRowOffset());

        assertEquals((900 - (617 + 1)) * 3600 + (1800 - 34), regionalGrid.getFirstBinIndex(0));

        assertEquals(((900 - (454 + 1)) * 3600) + (1800 + 67), regionalGrid.getBinIndex(45.4, 6.7));
    }

    @Test
    public void testWithGlobalRegion() throws Exception {
        final RegionalPlanetaryGrid regionalGrid = createRegionalPlanetaryGrid(GLOBAL_REGION);

        assertEquals(1800, regionalGrid.getNumRows());
        assertEquals(3600, regionalGrid.getNumCols(0));
        assertEquals(3600, regionalGrid.getNumCols(36));
        assertEquals(3600, regionalGrid.getNumCols(289));
        assertEquals(3600 * 1800, regionalGrid.getNumBins());

        assertEquals(0, regionalGrid.getColumnOffset());
        assertEquals(0, regionalGrid.getRowOffset());

        assertEquals(0, regionalGrid.getFirstBinIndex(0));

        assertEquals(0, regionalGrid.getBinIndex(90, -180));
        assertEquals((899 * 3600) + 1800, regionalGrid.getBinIndex(0, 0));
        assertEquals((3600 * 1800) - 1, regionalGrid.getBinIndex(-90, 180));
        assertEquals(((900 - (454 + 1)) * 3600) + (1800 + 67), regionalGrid.getBinIndex(45.4, 6.7));
    }

    @Test
    public void testThatCertainIndexIsnotInGrid_WithCentralAmerica() throws Exception {
        final ReferencedEnvelope region = new ReferencedEnvelope(CENTRAL_AMERICA,    // minus because lat is upside-down
                                                                 DefaultGeographicCRS.WGS84);

        final RegionalPlanetaryGrid grid = new RegionalPlanetaryGrid(new RegularGaussianGrid(1280), region);
        final int binIndex = 1127261;
        assertFalse(grid.isBinIndexInRegionalGrid(binIndex));
    }

    private RegionalPlanetaryGrid createRegionalPlanetaryGrid(Rectangle2D.Double rectangle) {
        final ReferencedEnvelope region = new ReferencedEnvelope(rectangle, DefaultGeographicCRS.WGS84);
        final PlanetaryGrid globalGrid = new PlateCarreeGrid(1800);
        return new RegionalPlanetaryGrid(globalGrid, region);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetRowIndex_withBinIndexLessThanMinBinIndex() throws Exception {
        createRegionalPlanetaryGrid(GEO_REGION).getRowIndex(23);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetRowIndex_withBinIndexGreaterThanMaxBinIndex() throws Exception {
        createRegionalPlanetaryGrid(GEO_REGION).getRowIndex(1015200 + 1);
    }

    @Test
    public void testIsBinIndexInRegionalGrid_OutsideRegionWest() throws Exception {
        final RegionalPlanetaryGrid grid = createRegionalPlanetaryGrid(GEO_REGION);
        final long minBinIndexInRow = grid.getFirstBinIndex(156);
        final long maxBinIndexInRow = minBinIndexInRow + grid.getNumCols(156);

        assertFalse(grid.isBinIndexInRegionalGrid(maxBinIndexInRow + 10));
    }

    @Test
    public void testIsBinIndexInRegionalGrid_OutsideRegionEast() throws Exception {
        final RegionalPlanetaryGrid grid = createRegionalPlanetaryGrid(GEO_REGION);
        final long minBinIndexInRow = grid.getFirstBinIndex(156);

        assertFalse(grid.isBinIndexInRegionalGrid(minBinIndexInRow - 10));
    }
}
