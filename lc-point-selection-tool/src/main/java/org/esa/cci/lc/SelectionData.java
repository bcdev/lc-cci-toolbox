package org.esa.cci.lc;

import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.gpf.Tile;

import java.awt.Rectangle;
import java.awt.image.Raster;


public class SelectionData {


    public void selectedFRData(Tile sourceTileMap2005,
                               Tile sourceTileMap2010,
                               long[] SelectedPointMap,
                               GeoCoding geoCoding,
                               int maskNumber,
                               String[] maskNames,
                               Raster[] maskData,
                               int step,
                               Rectangle targetRectangle) {

        int targetWidth = targetRectangle.width;
        int targetHeight = targetRectangle.height;

        final int[] Map2005 = sourceTileMap2005.getSamplesInt();
        final int[] Map2010 = sourceTileMap2010.getSamplesInt();

        int[] mask = new int[targetWidth * targetHeight];
        int selectedPixel = Math.round((float) (step / 2. - 1.));

        if (selectedPixel > targetHeight - 1 || selectedPixel > targetWidth - 1) {
            selectedPixel = Math.min(targetHeight - 1, targetWidth - 1);
            System.out.printf("mask selected_pixel width height :   %d  %d  %d   \n", selectedPixel, targetWidth, targetHeight);
        }

        int y = selectedPixel + sourceTileMap2005.getMinY();
        int x = selectedPixel + sourceTileMap2005.getMinX();

        int p = selectedPixel * targetWidth + selectedPixel;

        GeoPos geoPos = geoCoding.getGeoPos(new PixelPos(x, y), null);
        float latitude = geoPos.lat;
        float longitude = geoPos.lon;

        String LcClass = "no_class";
        int LcClassNumber = -1;


        for (int k = 0; k < maskNumber; k++) {
            //System.out.printf(" mask selected_pixel width height :  %d  %d  %d  %d   \n", k, selectedPixel , targetWidth, targetHeight);
            maskData[k].getPixels(targetRectangle.x, targetRectangle.y, targetWidth, targetHeight, mask);

            if (mask[p] != 0) {
                LcClass = maskNames[k];
                LcClassNumber = k;
                SelectedPointMap[p] = (long) Map2005[p] + 1000 * (long) Map2010[p];
                //System.out.printf(" masknumber mask width height :  %d  %d  %d  %d   \n", maskNumber, k , targetWidth, targetHeight);
                //System.out.printf(" Map2005 Map2010 SelectedPointMap:  %d  %d  %d   \n", Map2005[p] , Map2010[p] , SelectedPointMap[p]);
                break;
            }
        }

        WriteLogFiles writtenLogFiles = new WriteLogFiles();
        writtenLogFiles.writingLogFiles(LcClassNumber, LcClass,
                                        latitude,
                                        longitude);

    }

    public void selectedRRData(Tile sourceTileMap2005,
                               Tile sourceTileMap2010,
                               long[] SelectedPointMap,
                               GeoCoding geoCoding,
                               int maskNumber,
                               String[] maskNames,
                               Raster[] maskData,
                               int step,
                               Rectangle targetRectangle) {

        int targetWidth = targetRectangle.width;
        int targetHeight = targetRectangle.height;

        final int[] Map2005 = sourceTileMap2005.getSamplesInt();
        final int[] Map2010 = sourceTileMap2010.getSamplesInt();

        int[] mask = new int[targetWidth * targetHeight];
        int selectedPixel = Math.round((float) (step / 2. - 1.));


        if (selectedPixel > targetHeight - 1 || selectedPixel > targetWidth - 1) {
            selectedPixel = Math.min(targetHeight - 1, targetWidth - 1);
            System.out.printf("mask selected_pixel width height :   %d  %d  %d   \n", selectedPixel, targetWidth, targetHeight);
        }

        int y = selectedPixel + sourceTileMap2005.getMinY();
        int x = selectedPixel + sourceTileMap2005.getMinX();

        int p = selectedPixel * targetWidth + selectedPixel;

        GeoPos geoPos = geoCoding.getGeoPos(new PixelPos(x, y), null);
        float latitude = geoPos.lat;
        float longitude = geoPos.lon;

        String LcClass = "no_class";
        int LcClassNumber = -1;


        for (int k = 0; k < maskNumber; k++) {
            //System.out.printf(" mask selected_pixel width height :  %d  %d  %d  %d   \n", k, selectedPixel , targetWidth, targetHeight);
            maskData[k].getPixels(targetRectangle.x, targetRectangle.y, targetWidth, targetHeight, mask);

            if (mask[p] != 0) {
                LcClass = maskNames[k];
                LcClassNumber = k;
                SelectedPointMap[p] = (long) Map2005[p] + 1000 * (long) Map2010[p];
                //System.out.printf(" masknumber mask width height :  %d  %d  %d  %d   \n", maskNumber, k , targetWidth, targetHeight);
                //System.out.printf(" Map2005 Map2010 SelectedPointMap:  %d  %d  %d   \n", Map2005[p] , Map2010[p] , SelectedPointMap[p]);
                break;
            }
        }

        WriteLogFiles writtenLogFiles = new WriteLogFiles();
        writtenLogFiles.writingLogFiles(LcClassNumber, LcClass,
                                        latitude,
                                        longitude);

    }

}


