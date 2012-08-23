package org.esa.cci.lc.aggregation;

import com.bc.ceres.binding.PropertyDescriptor;
import com.bc.ceres.binding.PropertySet;
import org.esa.beam.binning.Aggregator;
import org.esa.beam.binning.AggregatorDescriptor;
import org.esa.beam.binning.VariableContext;

/**
 * @author Marco Peters
 */
public class LcAggregatorDescriptor implements AggregatorDescriptor {

    public static final String LC_AGGR = "LC_AGGR";
    // todo: adapt to real number of LC classes
    public static final int NUM_LC_CLASSES = 24;

    @Override
    public String getName() {
        return LC_AGGR;
    }

    @Override
    public PropertyDescriptor[] getParameterDescriptors() {
        return new PropertyDescriptor[]{
                new PropertyDescriptor("numMajorityClasses", Integer.class)
        };
    }

    @Override
    public Aggregator createAggregator(VariableContext varCtx, PropertySet configuration) {

        int numMajorityClasses = (Integer) configuration.getValue("numMajorityClasses");

        String[] spatialFeatureNames = new String[NUM_LC_CLASSES];
        for (int i = 0; i < NUM_LC_CLASSES; i++) {
            spatialFeatureNames[i] = "class_area_" + (i + 1);
        }
        String[] outputFeatureNames = new String[NUM_LC_CLASSES + numMajorityClasses];
        System.arraycopy(spatialFeatureNames, 0, outputFeatureNames, 0, spatialFeatureNames.length);
        for (int i = 0; i < numMajorityClasses; i++) {
            outputFeatureNames[NUM_LC_CLASSES + i] = "majority_class_" + (i + 1);
        }

        return new LcAggregator(spatialFeatureNames, outputFeatureNames);
    }
}
