package org.esa.cci.lc.aggregation;

import org.esa.snap.binning.*;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Product;

import java.awt.*;
import java.util.Collections;
import java.util.SortedMap;
import java.util.TreeMap;

public class LcPftAggregator extends AbstractAggregator {

    private static final LCCS LCCS_CLASSES = LCCS.getInstance();
    private AreaCalculator areaCalculator;
    private boolean outputLCCSClasses;
    private int numMajorityClasses;
    private final Product additionalUserMap;
    private final boolean outputUserMapClasses;
    private Lccs2PftLut pftLut;


    public LcPftAggregator(boolean outputLCCSClasses, int numMajorityClasses,
                           Product additionalUserMap, boolean outputUserMapClasses,
                           AreaCalculator calculator, Lccs2PftLut pftLut, String[] spatialFeatureNames, String[] outputFeatureNames) {
        super(LcMapAggregatorDescriptor.NAME, spatialFeatureNames, spatialFeatureNames, outputFeatureNames);
        this.outputLCCSClasses = outputLCCSClasses;
        this.numMajorityClasses = numMajorityClasses;
        this.additionalUserMap = additionalUserMap;
        this.outputUserMapClasses = outputUserMapClasses;
        this.pftLut = pftLut;
        this.areaCalculator = calculator;
    }


    @Override
    public void initSpatial(BinContext ctx, WritableVector vector) {
        initVector(vector, Float.NaN);
    }

    @Override
    public void aggregateSpatial(BinContext ctx, Observation observation, WritableVector spatialVector) {
        final double obsLatitude = observation.getLatitude();
        final double obsLongitude = observation.getLongitude();
        final float areaFraction = (float) areaCalculator.calculate(obsLongitude, obsLatitude, ctx.getIndex());

        final int index = LCCS_CLASSES.getClassIndex((short) observation.get(0));
        final float oldValue = spatialVector.get(index);
        if (Float.isNaN(oldValue)) {
            spatialVector.set(index, areaFraction);
        } else {
            spatialVector.set(index, oldValue + areaFraction);
        }

        final int userMapIndex = spatialVector.size() - 1;
        if (Float.isNaN(spatialVector.get(userMapIndex)) && (outputUserMapClasses || additionalUserMap != null)) {
            final float userMapValue = getUserMapValue(obsLatitude, obsLongitude);
            spatialVector.set(userMapIndex, userMapValue);
        }
    }

    @Override
    public void completeSpatial(BinContext ctx, int numSpatialObs, WritableVector spatialVector) {
        // normalizing the data because of aggregating with float data and the float inaccuracy
        float sum = 0;

        for (int i = 0; i < LCCS_CLASSES.getNumClasses(); i++) {
            float v = spatialVector.get(i);
            if (!Float.isNaN(v)) {
                sum += v;
            }
        }
        if (sum != 1.0f) {
            for (int i = 0; i < LCCS_CLASSES.getNumClasses(); i++) {
                spatialVector.set(i, spatialVector.get(i) / sum);
            }
        }
    }

    @Override
    public void initTemporal(BinContext ctx, WritableVector vector) {
        // Nothing to be done here
    }

    @Override
    public void aggregateTemporal(BinContext ctx, Vector spatialVector, int numSpatialObs,
                                  WritableVector temporalVector) {
        // simply copy the data; no temporal aggregation needed
        for (int i = 0; i < spatialVector.size(); i++) {
            temporalVector.set(i, spatialVector.get(i));
        }
    }

    @Override
    public void completeTemporal(BinContext ctx, int numTemporalObs, WritableVector temporalVector) {
        // Nothing to be done here
    }



    @Override
    public void computeOutput(Vector temporalVector, WritableVector outputVector) {
        initVector(outputVector, Float.NaN);
        SortedMap<Float, Integer> sortedMap = new TreeMap<>(Collections.reverseOrder());
        int outputVectorIndex = 0;
        for (short i = 0; i < LCCS_CLASSES.getNumClasses(); i++) {
            float classArea = temporalVector.get(i);
            if (numMajorityClasses > 0 && !Float.isNaN(classArea)) {
                sortedMap.put(classArea, LCCS_CLASSES.getClassValue(i));
            }
            if (outputLCCSClasses) {
                outputVector.set(outputVectorIndex++, classArea);
            }
        }
        Integer userMapValue = Integer.MIN_VALUE;
        if (additionalUserMap != null) {
            final float tempUserMapValue = temporalVector.get(LCCS_CLASSES.getNumClasses());
            if (!Float.isNaN(tempUserMapValue)) {
                userMapValue = (int) Math.floor(tempUserMapValue);
            }
            if (outputUserMapClasses) {
                outputVector.set(outputVectorIndex++, userMapValue);
            }
        }

        if (numMajorityClasses > 0) {
            Integer[] classesSortedByOccurrence = sortedMap.values().toArray(new Integer[sortedMap.size()]);
            for (int i = 0; i < numMajorityClasses; i++) {
                if (i >= classesSortedByOccurrence.length) {
                    outputVector.set(outputVectorIndex++, Float.NaN);
                } else {
                    outputVector.set(outputVectorIndex++, classesSortedByOccurrence[i]);
                }
            }
        }

        if (pftLut != null) {
            for (int i = 0; i < LCCS_CLASSES.getNumClasses(); i++) {
                float classArea = temporalVector.get(i);
                if (!Float.isNaN(classArea)) {
                    final int lccsClass = LCCS_CLASSES.getClassValue(i);
                    float[] classPftFactors;
                    classPftFactors = pftLut.getConversionFactors(lccsClass, userMapValue);
                    for (int j = 0; j < classPftFactors.length; j++) {
                        float factor = classPftFactors[j];
                        if (!Float.isNaN(factor)) {
                            int currentOutputIndex = outputVectorIndex + j;
                            float oldValue = outputVector.get(currentOutputIndex);
                            if (Float.isNaN(oldValue)) {
                                outputVector.set(currentOutputIndex, classArea * factor);
                            } else {
                                outputVector.set(currentOutputIndex, oldValue + (classArea * factor));
                            }
                        }
                    }
                }
            }
        }
    }

    private void initVector(WritableVector outputVector, float initValue) {
        for (int i = 0; i < outputVector.size(); i++) {
            outputVector.set(i, initValue);
        }
    }

    public float getUserMapValue(double obsLatitude, double obsLongitude) {
        final Band firstBand = additionalUserMap.getBandAt(0);
        final PixelPos pixelPos = firstBand.getGeoCoding().getPixelPos(new GeoPos((float) obsLatitude, (float) obsLongitude), null);
        final int pixX = (int) Math.floor(pixelPos.getX());
        final int pixY = (int) Math.floor(pixelPos.getY());
        if (firstBand.getGeophysicalImage().getBounds().contains(pixX, pixY)) {
            return firstBand.getGeophysicalImage().getData(new Rectangle(pixX, pixY, 1, 1)).getSample(pixX, pixY, 0);
        }
        return Float.NaN;
    }

}
