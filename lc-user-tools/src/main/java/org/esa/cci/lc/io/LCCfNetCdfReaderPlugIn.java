/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.cci.lc.io;

import org.esa.snap.dataio.netcdf.ProfileReadContext;
import org.esa.snap.dataio.netcdf.metadata.ProfilePartReader;
import org.esa.snap.dataio.netcdf.metadata.profiles.cf.CfGeocodingPart;
import org.esa.snap.dataio.netcdf.metadata.profiles.cf.CfHdfEosGeoInfoExtractor;
import org.esa.snap.dataio.netcdf.metadata.profiles.cf.CfNetCdfReaderPlugIn;
import org.esa.snap.dataio.netcdf.metadata.profiles.hdfeos.HdfEosGeocodingPart;
import org.esa.snap.dataio.netcdf.util.Constants;
import org.esa.snap.dataio.netcdf.util.DimKey;
import org.esa.snap.dataio.netcdf.util.ReaderUtils;
import org.esa.snap.core.dataio.DecodeQualification;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.CrsGeoCoding;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.GeoCodingFactory;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.util.logging.BeamLogManager;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import ucar.ma2.Array;
import ucar.ma2.Index;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.io.IOException;
import java.util.List;

/**
 * Special NetCDF reader for LC-CCI data if stored with a gaussian grid.
 * It overcomes the problem that the standard reader shifts the coordinates and data if
 * the coordinates go from 0 to 360. But for gaussian grids we exactly want this in LC.
 */
public class LCCfNetCdfReaderPlugIn extends CfNetCdfReaderPlugIn {

    private static final String ATTRIBUTE_NAME_TYPE = "type";

    @Override
    public DecodeQualification getDecodeQualification(NetcdfFile netcdfFile) {
        DecodeQualification decodeQualification = super.getDecodeQualification(netcdfFile);
        if (DecodeQualification.SUITABLE.equals(decodeQualification) && isEsaCciLcFile(netcdfFile)) {
            return DecodeQualification.INTENDED;
        }
        return decodeQualification;
    }

    private boolean isEsaCciLcFile(NetcdfFile netcdfFile) {
        List<Attribute> globalAttributes = netcdfFile.getGlobalAttributes();
        for (Attribute globalAttribute : globalAttributes) {
            if (globalAttribute.getShortName().equals(ATTRIBUTE_NAME_TYPE)) {
                if (globalAttribute.getStringValue().startsWith("ESACCI-LC-L4")
                        || globalAttribute.getStringValue().startsWith("C3S-LC-L4"))  {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public ProfilePartReader createGeoCodingPartReader() {
        return new LCCfGeocodingPart();
    }

    public static class LCCfGeocodingPart extends CfGeocodingPart {

        @Override
        public void decode(ProfileReadContext ctx, Product p) throws IOException {
            GeoCoding geoCoding = readConventionBasedMapGeoCoding(ctx, p);
            if (geoCoding == null) {
                geoCoding = readPixelGeoCoding(p);
            }
            // If there is still no geocoding, check special case of netcdf file which was converted
            // from hdf file and has 'StructMetadata.n' element.
            // In this case, the HDF 'elements' were put into a single Netcdf String attribute
            // todo: in fact this has been checked only for MODIS09 HDF-EOS product. Try to further generalize
            if (geoCoding == null && hasHdfMetadataOrigin(ctx.getNetcdfFile().getGlobalAttributes())) {
                hdfDecode(ctx, p);
            }
            if (geoCoding != null) {
                p.setSceneGeoCoding(geoCoding);
            }
        }

        private void hdfDecode(ProfileReadContext ctx, Product p) throws IOException {
            final CfHdfEosGeoInfoExtractor cfHdfEosGeoInfoExtractor = new CfHdfEosGeoInfoExtractor(
                    ctx.getNetcdfFile().getGlobalAttributes());
            cfHdfEosGeoInfoExtractor.extractInfo();

            String projection = cfHdfEosGeoInfoExtractor.getProjection();
            double upperLeftLon = cfHdfEosGeoInfoExtractor.getUlLon();
            double upperLeftLat = cfHdfEosGeoInfoExtractor.getUlLat();

            double lowerRightLon = cfHdfEosGeoInfoExtractor.getLrLon();
            double lowerRightLat = cfHdfEosGeoInfoExtractor.getLrLat();

            HdfEosGeocodingPart.attachGeoCoding(p, upperLeftLon, upperLeftLat, lowerRightLon, lowerRightLat, projection, null);
        }

        private boolean hasHdfMetadataOrigin(List<Attribute> netcdfAttributes) {
            for (Attribute att : netcdfAttributes) {
                if (att.getShortName().startsWith("StructMetadata")) {
                    return true;
                }
            }
            return false;
        }

        private static GeoCoding readConventionBasedMapGeoCoding(ProfileReadContext ctx, Product product) {
            final String[] cfConvention_lonLatNames = new String[]{
                    Constants.LON_VAR_NAME,
                    Constants.LAT_VAR_NAME
            };
            final String[] coardsConvention_lonLatNames = new String[]{
                    Constants.LONGITUDE_VAR_NAME,
                    Constants.LATITUDE_VAR_NAME
            };

            Variable[] lonLat;
            List<Variable> variableList = ctx.getNetcdfFile().getVariables();
            lonLat = ReaderUtils.getVariables(variableList, cfConvention_lonLatNames);
            if (lonLat == null) {
                lonLat = ReaderUtils.getVariables(variableList, coardsConvention_lonLatNames);
            }

            if (lonLat != null) {
                final Variable lonVariable = lonLat[0];
                final Variable latVariable = lonLat[1];
                final DimKey rasterDim = ctx.getRasterDigest().getRasterDim();
                if (rasterDim.fitsTo(lonVariable, latVariable)) {
                    try {
                        return createConventionBasedMapGeoCoding(lonVariable, latVariable,
                                                                 product.getSceneRasterWidth(),
                                                                 product.getSceneRasterHeight(), ctx);
                    } catch (Exception e) {
                        BeamLogManager.getSystemLogger().warning("Failed to create NetCDF geo-coding");
                    }
                }
            }
            return null;
        }

        private static GeoCoding createConventionBasedMapGeoCoding(Variable lon,
                                                                   Variable lat,
                                                                   int sceneRasterWidth,
                                                                   int sceneRasterHeight,
                                                                   ProfileReadContext ctx) throws Exception {
            double pixelX;
            double pixelY;
            double easting;
            double northing;
            double pixelSizeX;
            double pixelSizeY;

            boolean yFlipped;
            Array lonData = lon.read();
/////////////////////////////////////////////////////////
            // In LC we don't want this behaviour
            // SPECIAL CASE: check if we have a global geographic lat/lon with lon from 0..360 instead of -180..180
//            if (isGlobalShifted180(lonData)) {
//                // if this is true, subtract 180 from all longitudes and
//                // add a global attribute which will be analyzed when setting up the image(s)
//                final List<Variable> variables = ctx.getNetcdfFile().getVariables();
//                for (Variable next : variables) {
//                    next.getAttributes().add(new Attribute("LONGITUDE_SHIFTED_180", 1));
//                }
//                for (int i = 0; i < lonData.getSize(); i++) {
//                    final Index ii = lonData.getIndex().set(i);
//                    final double theLon = lonData.getDouble(ii) - 180.0;
//                    lonData.setDouble(ii, theLon);
//                }
//            }
/////////////////////////////////////////////////////////

            double sum = 0;
            for (int i = 0; i < lonData.getSize() - 1; i++) {
                double delta = (lonData.getDouble(i + 1) - lonData.getDouble(i) + 360) % 360;
                sum += delta;
            }
            pixelSizeX = sum / (sceneRasterWidth - 1);

            final Index i0 = lonData.getIndex().set(0);
            easting = lonData.getDouble(i0);

            final int latSize = lat.getShape(0);
            final Array latData = lat.read();
            final Index j0 = latData.getIndex().set(0);
            final Index j1 = latData.getIndex().set(latSize - 1);
            pixelSizeY = (latData.getDouble(j1) - latData.getDouble(j0)) / (sceneRasterHeight - 1);

            pixelX = 0.5f;
            pixelY = 0.5f;

            if (pixelSizeY < 0) {
                pixelSizeY = -pixelSizeY;
                yFlipped = false;
                northing = latData.getDouble(latData.getIndex().set(0));
            } else {
                yFlipped = true;
                northing = latData.getDouble(latData.getIndex().set(latSize - 1));
            }

            if (pixelSizeX <= 0 || pixelSizeY <= 0) {
                return null;
            }
            ctx.setProperty(Constants.Y_FLIPPED_PROPERTY_NAME, yFlipped);
            return new CrsGeoCoding(DefaultGeographicCRS.WGS84,
                                    sceneRasterWidth, sceneRasterHeight,
                                    easting, northing,
                                    pixelSizeX, pixelSizeY,
                                    pixelX, pixelY);
        }

        private static GeoCoding readPixelGeoCoding(Product product) throws IOException {
            Band lonBand = product.getBand(Constants.LON_VAR_NAME);
            if (lonBand == null) {
                lonBand = product.getBand(Constants.LONGITUDE_VAR_NAME);
            }
            Band latBand = product.getBand(Constants.LAT_VAR_NAME);
            if (latBand == null) {
                latBand = product.getBand(Constants.LATITUDE_VAR_NAME);
            }
            if (latBand != null && lonBand != null) {
                return GeoCodingFactory.createPixelGeoCoding(latBand, lonBand, latBand.getValidMaskExpression(), 5);
            }
            return null;
        }

    }
}
