package org.esa.cci.lc;

class DefaultFlagDetector implements FlagDetector {

    @Override
    public boolean isLand(int x, int y) {
        return false;
    }

    @Override
    public boolean isInvalid(int x, int y) {
        return false;
    }

}

