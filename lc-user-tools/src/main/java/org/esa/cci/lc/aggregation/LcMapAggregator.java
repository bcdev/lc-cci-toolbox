package org.esa.cci.lc.aggregation;

import org.esa.beam.binning.AbstractAggregator;
import org.esa.beam.binning.BinContext;
import org.esa.beam.binning.Observation;
import org.esa.beam.binning.Vector;
import org.esa.beam.binning.WritableVector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * This class implements the aggregation of LC-CCI Map products. The aggregation includes the computation
 * of the fractional area covered by the different LCCS classes and the conversion of the areas into PFTs.
 *
 * @author Marco Peters
 */
@SuppressWarnings("FieldCanBeLocal")
class LcMapAggregator extends AbstractAggregator {

    private static final LCCS LCCS_CLASSES = LCCS.getInstance();
    private FractionalAreaCalculator areaCalculator;
    private boolean outputLCCSClasses;
    private int numMajorityClasses;
    private PftLut pftLut;

    public LcMapAggregator(boolean outputLCCSClasses, int numMajorityClasses,
                           FractionalAreaCalculator calculator, PftLut pftLut) {
        this(createSpatialFeatureNames(), outputLCCSClasses, numMajorityClasses, calculator, pftLut);
    }

    private LcMapAggregator(String[] spatialFeatureNames, boolean outputLCCSClasses, int numMajorityClasses,
                            FractionalAreaCalculator calculator, PftLut pftLut) {
        super(LcMapAggregatorDescriptor.NAME, spatialFeatureNames, spatialFeatureNames,
              createOutputFeatureNames(outputLCCSClasses, numMajorityClasses, pftLut, spatialFeatureNames));
        this.outputLCCSClasses = outputLCCSClasses;
        this.numMajorityClasses = numMajorityClasses;
        this.pftLut = pftLut;
        this.areaCalculator = calculator;
    }

    int getNumPFTs() {
        return pftLut.getPFTNames().length;
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
    }

    @Override
    public void completeSpatial(BinContext ctx, int numSpatialObs, WritableVector spatialVector) {
        // Nothing to be done here
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
        SortedMap<Float, Integer> sortedMap = new TreeMap<Float, Integer>(Collections.reverseOrder());
        int outputVectorIndex = 0;
        for (short i = 0; i < temporalVector.size(); i++) {
            float classArea = temporalVector.get(i);
            if (!Float.isNaN(classArea)) {
                sortedMap.put(classArea, LCCS_CLASSES.getClassValue(i));
            }
            if (outputLCCSClasses) {
                outputVector.set(outputVectorIndex++, classArea);
            }
        }

        Integer[] classesSortedByOccurrence = sortedMap.values().toArray(new Integer[sortedMap.size()]);
        for (int i = 0; i < numMajorityClasses; i++) {
            if (i >= classesSortedByOccurrence.length) {
                outputVector.set(outputVectorIndex++, Float.NaN);
            } else {
                outputVector.set(outputVectorIndex++, classesSortedByOccurrence[i]);
            }
        }

        if (pftLut != null) {
            float[][] conversionFactors = pftLut.getConversionFactors();
            for (int i = 0; i < temporalVector.size(); i++) {
                float classArea = temporalVector.get(i);
                if (!Float.isNaN(classArea)) {
                    float[] classPftFactors = conversionFactors[i];
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

    private static String[] createSpatialFeatureNames() {
        String[] spatialFeatureNames = new String[LCCS_CLASSES.getNumClasses()];
        short[] classValues = LCCS_CLASSES.getClassValues();
        for (int i = 0; i < spatialFeatureNames.length; i++) {
            spatialFeatureNames[i] = "class_area_" + classValues[i];
        }
        return spatialFeatureNames;
    }

    private static String[] createOutputFeatureNames(boolean outputLCCSClasses, int numMajorityClasses, PftLut pftLut,
                                                     String[] spatialFeatureNames) {
        List<String> outputFeatureNames = new ArrayList<String>();
        if (outputLCCSClasses) {
            outputFeatureNames.addAll(Arrays.asList(spatialFeatureNames));
        }
        for (int i = 0; i < numMajorityClasses; i++) {
            outputFeatureNames.add("majority_class_" + (i + 1));
        }
        if (pftLut != null) {
            outputFeatureNames.addAll(Arrays.asList(pftLut.getPFTNames()));
        }
        return outputFeatureNames.toArray(new String[outputFeatureNames.size()]);
    }

}
