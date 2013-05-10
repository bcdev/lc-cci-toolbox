package org.esa.cci.lc.subset;

// Defines a set of predefined regions.
public enum PredefinedRegion {
    EUROPE(75, 40, 35, -25),
    ASIA(80, 180, 0, 40);

    private float north;
    private float east;
    private float south;
    private float west;

    private PredefinedRegion(float north, float east, float south, float west) {
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
