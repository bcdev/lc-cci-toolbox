package org.esa.cci.lc.subset;

// Defines a set of predefined regions.
public enum PredefinedRegion {
    NORTH_AMERICA(-180, 85, -50, 19),
    CENTRAL_AMERICA(-93, 28, -59, 7),
    SOUTH_AMERICA(-105, 19, -34, -57),
    WESTERN_EUROPE_AND_MEDITERRANEAN(-26, 83, 53, 25),
    ASIA(53, 83, 180, 0),
    AFRICA(-26, 40, 53, -40),
    SOUTH_EAST_ASIA(90, 29, 163, -12),
    AUSTRALIA_AND_NEW_ZEALAND(95, 0, 180, -53),
    GREENLAND(-74, 84, -11, 59);

    private float north;
    private float east;
    private float south;
    private float west;

    private PredefinedRegion(float west, float north, float east, float south) {
        this.north = north;
        this.east = east;
        this.south = south;
        this.west = west;
    }

    public float getEast() {
        return east;
    }

    public float getNorth() {
        return north;
    }

    public float getSouth() {
        return south;
    }

    public float getWest() {
        return west;
    }
}
