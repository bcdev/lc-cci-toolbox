package org.esa.cci.lc.util;

public class LcHelper {

    public static String getTargetFileName(String insertion, String sourceFileName) {
        final String sep = "-";
        final String[] strings = sourceFileName.split(sep);
        final int insertionPos = sourceFileName.startsWith("ESACCI-LC-L4-LCCS-Map-") ? strings.length - 2 : strings.length - 4;
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < strings.length - 1; i++) {
            String string = strings[i];
            if (i == insertionPos) {
                sb.append(insertion).append(sep);
            }
            sb.append(string).append(sep);
        }
        sb.append(strings[strings.length - 1]);
        return sb.toString();
    }
}
