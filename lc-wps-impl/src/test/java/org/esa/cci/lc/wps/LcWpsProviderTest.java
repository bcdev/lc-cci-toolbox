package org.esa.cci.lc.wps;

import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.GPF;
import org.esa.cci.lc.subset.LcSubsetOp;
import org.esa.cci.lc.subset.PredefinedRegion;
import org.junit.*;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * @author hans
 */
public class LcWpsProviderTest {

    @Ignore // Ignored because it takes too long.
    @Test
    public void testCreateSubset() throws Exception {
        GPF.getDefaultInstance().getOperatorSpiRegistry().loadOperatorSpis();
        final Product sourceProduct;
        sourceProduct = ProductIO.readProduct("C:\\Personal\\CabLab\\EO data\\ESACCI-LC-L4-LCCS-Map-300m-P5Y-2000-v1.3.nc");
        HashMap<String, Object> parameters = new HashMap<>();
        parameters.put("targetDir", new File("."));
        parameters.put("predefinedRegion", PredefinedRegion.GREENLAND);
        GPF.createProduct("LCCCI.Subset", parameters, sourceProduct);
    }

}