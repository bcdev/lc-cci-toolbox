package org.esa.cci.lc.aggregation;

import org.esa.beam.binning.BinContext;
import org.esa.beam.binning.Observation;
import org.esa.beam.binning.support.ObservationImpl;
import org.esa.beam.binning.support.VectorImpl;

import java.util.HashMap;

public class AggregatorTestUtils {

    public static VectorImpl vec(float... values) {
        return new VectorImpl(values);
    }

    public static Observation obs(float... values) {
        return new ObservationImpl(90.0f, 180.0f, 0.0, values);
    }

    public static BinContext createCtx() {
        return new BinContext() {
            private HashMap map = new HashMap();

            @Override
            public long getIndex() {
                return 0;
            }

            @Override
            public <T> T get(String name) {
                return (T) map.get(name);
            }

            @Override
            public void put(String name, Object value) {
                map.put(name, value);
            }
        };
    }
}