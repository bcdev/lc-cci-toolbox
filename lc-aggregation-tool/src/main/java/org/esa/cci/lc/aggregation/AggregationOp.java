package org.esa.cci.lc.aggregation;

import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;

/**
 * The LC map and conditions products are delivered in a full spatial resolution version, both as global
 * files and as regional subsets, in a Plate Carree projection. However, climate models may need products
 * associated with a coarser spatial resolution, over specific areas (e.g. for regional climate models)
 * and/or in another projection. This Operator implementation provides this functionality.
 *
 * @author Marco Peters
 */
@OperatorMetadata(
        alias = "Aggregate",
        version = "0.1",
        authors = "Marco Peters",
        copyright = "(c) 2012 by Brockmann Consult",
        description = "Allows to re-project, aggregate and subset LC map and conditions products.")
public class AggregationOp extends Operator {

    @Override
    public void initialize() throws OperatorException {
    }


    /**
     * The Service Provider Interface (SPI) for the operator.
     * It provides operator meta-data and is a factory for new operator instances.
     */
    public static class Spi extends OperatorSpi {

        public Spi() {
            super(AggregationOp.class);
        }
    }

}
