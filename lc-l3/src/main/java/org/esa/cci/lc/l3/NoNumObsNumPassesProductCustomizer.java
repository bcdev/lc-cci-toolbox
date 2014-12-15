/*
 * Copyright (C) 2014 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.cci.lc.l3;

import org.esa.beam.binning.ProductCustomizer;
import org.esa.beam.binning.ProductCustomizerConfig;
import org.esa.beam.binning.ProductCustomizerDescriptor;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;

/**
 * Removes num_obs and num_passes bands, depending on configuration.
 */
public class NoNumObsNumPassesProductCustomizer extends ProductCustomizer {

    @Override
    public void customizeProduct(Product product) {
        Band numObsBand = product.getBand("num_obs");
        if (numObsBand != null) {
            product.removeBand(numObsBand);
        }
        Band numPassesBand = product.getBand("num_passes");
        if (numPassesBand != null) {
            product.removeBand(numPassesBand);
        }
    }

    public static class Config extends ProductCustomizerConfig {}

    public static class Descriptor implements ProductCustomizerDescriptor {

        @Override
        public ProductCustomizer createProductCustomizer(ProductCustomizerConfig config) {
            return new NoNumObsNumPassesProductCustomizer();
        }

        @Override
        public String getName() {
            return "NoNumObsNumPasses";
        }

        @Override
        public ProductCustomizerConfig createConfig() {
            return new Config();
        }
    }
}
