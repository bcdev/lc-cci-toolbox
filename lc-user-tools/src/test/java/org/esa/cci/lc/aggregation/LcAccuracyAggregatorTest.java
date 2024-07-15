package org.esa.cci.lc.aggregation;

import org.esa.snap.binning.SpatialBin;
import org.esa.snap.binning.TemporalBin;
import org.esa.snap.binning.support.ObservationImpl;
import org.esa.snap.binning.support.VariableContextImpl;
import org.esa.snap.binning.support.VectorImpl;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.core.Is.*;
import static org.junit.Assert.*;

public class LcAccuracyAggregatorTest {

    private LcAccuracyAggregator aggregator;
    private SpatialBin spatialBin;
    private float[] elements;
    private int latitude;
    private int longitude;
    private int mjd;

    @Before
    public void setUp() throws Exception {
        final VariableContextImpl varCtx = new VariableContextImpl();
        varCtx.defineVariable("name");

        aggregator = new LcAccuracyAggregator(varCtx, new String[]{"name"}, new String[]{"confidence"}, 8);
        spatialBin = new SpatialBin();
        elements = new float[1];
        latitude = 0;
        longitude = 2;
        mjd = 3;
    }

    @Test
    public void testFeatureNames() {
        String[] spatialFeatureNames = aggregator.getSpatialFeatureNames();
        assertEquals(1, spatialFeatureNames.length);
        assertEquals("confidence", spatialFeatureNames[0]);

        String[] temporalFeatureNames = aggregator.getTemporalFeatureNames();
        assertEquals(1, temporalFeatureNames.length);
        assertEquals("confidence", temporalFeatureNames[0]);

        String[] outputFeatureNames = aggregator.getOutputFeatureNames();
        assertEquals(1, outputFeatureNames.length);
        assertEquals("confidence", outputFeatureNames[0]);
    }

    @Test
    public void testThatZeroObservationsResolvesTo_NaN() {

        // execution
        aggregator.initSpatial(spatialBin, new VectorImpl(elements));
        /*  aggregator.aggregateSpatial(...);  */ //  zero number of spatial aggregations
        aggregator.completeSpatial(spatialBin, 0, new VectorImpl(elements));

        //validation
        assertThat(elements[0], is(Float.NaN));
    }

    @Test
    public void testThatOneObservationsResolvesTo_theObservationItself() {
        // preparation
        final float theObservation = 12.5f;

        // execution
        aggregator.initSpatial(spatialBin, new VectorImpl(elements));
        aggregator.aggregateSpatial(spatialBin, new ObservationImpl(latitude, longitude, mjd, theObservation), new VectorImpl(elements));
        aggregator.completeSpatial(spatialBin, 0, new VectorImpl(elements));

        //validation
        assertThat(elements[0], is(theObservation));
    }

    @Test
    public void testThatEvenNumberOfObservationsResolvesTo_TheMeanOfTheTwoCenterValuesOfSortedValues() {
        // preparation
        final float[] observations = {
                3.1f,
                2.2f,
                2.6f,
                1.2f
        };

        // execution
        aggregator.initSpatial(spatialBin, new VectorImpl(elements));
        for (float observation : observations) {
            aggregator.aggregateSpatial(spatialBin, new ObservationImpl(latitude, longitude, mjd, observation), new VectorImpl(elements));
        }
        aggregator.completeSpatial(spatialBin, 0, new VectorImpl(elements));

        //validation
        assertThat(elements[0], is(2.4f));
    }

    @Test
    public void testThatOddNumberOfObservationsResolvesTo_TheCenterValueOfSortedValues() {
        // preparation
        final float[] observations = {
                3.1f,
                2.2f,
                4.2f,
                2.6f,
                1.2f
        };

        // execution
        aggregator.initSpatial(spatialBin, new VectorImpl(elements));
        for (float observation : observations) {
            aggregator.aggregateSpatial(spatialBin, new ObservationImpl(latitude, longitude, mjd, observation), new VectorImpl(elements));
        }
        aggregator.completeSpatial(spatialBin, 0, new VectorImpl(elements));

        //validation
        assertThat(elements[0], is(2.6f));
    }

    @Test
    public void testThatTemporalAggregationAndComputeOutputDoesNotChangeTheSpatialResult() {
        // preparation
        final float theObservation = 12.5f;

        // execution
        aggregator.initSpatial(spatialBin, new VectorImpl(elements));
        aggregator.aggregateSpatial(spatialBin, new ObservationImpl(latitude, longitude, mjd, theObservation), new VectorImpl(elements));
        final VectorImpl spatialVector = new VectorImpl(elements);
        aggregator.completeSpatial(spatialBin, 0, spatialVector);

        final VectorImpl temporalVector = new VectorImpl(new float[1]);
        final TemporalBin temporalBin = new TemporalBin();
        aggregator.initTemporal(temporalBin, temporalVector);
        aggregator.aggregateTemporal(temporalBin, spatialVector, 127836, temporalVector);
        aggregator.aggregateTemporal(temporalBin, spatialVector, 127836, temporalVector);
        aggregator.aggregateTemporal(temporalBin, spatialVector, 127836, temporalVector);
        aggregator.aggregateTemporal(temporalBin, spatialVector, 127836, temporalVector);
        aggregator.completeTemporal(temporalBin, 23178, temporalVector);

        final float[] output = new float[1];
        aggregator.computeOutput(temporalVector, new VectorImpl(output));

        //validation
        assertThat(output[0], is(theObservation));
    }
}
