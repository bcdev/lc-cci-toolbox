package org.esa.cci.lc;

public interface FlagDetector {

    boolean isLand(int x, int y);

    boolean isInvalid(int x, int y);

    boolean isClearLand(int x, int y);

}


