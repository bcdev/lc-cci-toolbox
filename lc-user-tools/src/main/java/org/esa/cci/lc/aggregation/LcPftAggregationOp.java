package org.esa.cci.lc.aggregation;


import org.esa.cci.lc.io.LcBinWriter;
import org.esa.snap.binning.operator.BinningOp;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.geotools.geometry.jts.ReferencedEnvelope;

import java.util.HashMap;

@OperatorMetadata(
        alias = "LCPFT.Aggregate.Map",
        internal = true,
        version = "4.7",
        authors = "Roman Shevchuk",
        copyright = "(c) 2022 by Brockmann Consult",
        description = "Allows to aggregate LC PFT products.",
        autoWriteDisabled = true)
public class LcPftAggregationOp extends AbstractLcAggregationOp {


    boolean outputTargetProduct;


    @Override
    public void initialize() throws OperatorException {
        super.initialize();
        Product source = getSourceProduct();
        final HashMap<String, String> lcProperties = getLcProperties();
        String id = createTypeAndID();
        BinningOp binningOp;
        try {
            binningOp = new BinningOp();
            binningOp.setParameterDefaultValues();
        } catch (Exception e) {
            throw new OperatorException("Could not create binning operator.", e);
        }
        final ReferencedEnvelope regionEnvelope = getRegionEnvelope();
        if (regionEnvelope != null) {
            source = createSubset(source, regionEnvelope);
        }


        binningOp.setSourceProduct(source);
        binningOp.setOutputTargetProduct(outputTargetProduct);
        binningOp.setParameter("outputBinnedData", true);
        binningOp.setBinWriter(new LcBinWriter(lcProperties, regionEnvelope));

    }






    private String createTypeAndID(){

        return "ID";
    }
}
