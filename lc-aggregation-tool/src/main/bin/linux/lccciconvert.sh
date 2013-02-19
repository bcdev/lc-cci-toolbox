#!/bin/sh
# lccciconvert.sh /data/lc-map-example/lc_classif_lccs_2010_v2.tif

if [ -z $1 ]; then
    echo "Land Cover CCI Tiff to NetCDF 4 converter
    echo "call   : lccciconvert.sh <classification-tif-file>"
    echo "example: lccciconvert.sh /data/lc-map-product/lc_classif_lccs_2010_v2.tif"
    echo "pattern: lc_classif_lccs_<epoch>_<version>.tif"
    echo "pattern: lc_flag<n>_<epoch>_<version>.tif  (for n in 1..5, optional files)"
    echo "output : <map-netcdf-file>"
    echo "example: ESACCI-LC-L4-Map-300m-P5Y-2010-v2.nc"
    exit 1
fi

export BEAM4_HOME=`dirname $0`/..
target=`dirname $1`/dummy

exec java \
    -Xmx2048M -Dbeam.reader.tileHeight=1024 -Dbeam.reader.tileWidth=1024 \
    -Dceres.context=beam \
    -Dbeam.mainClass=org.esa.beam.framework.gpf.main.GPT \
    "-Dbeam.home=$BEAM4_HOME" \
    "-Dncsa.hdf.hdflib.HDFLibrary.hdflib=$BEAM4_HOME/modules/lib-hdf-2.7/lib/libjhdf.so" \
    "-Dncsa.hdf.hdf5lib.H5.hdf5lib=$BEAM4_HOME/modules/lib-hdf-2.7/lib/libjhdf5.so" \
    -jar "$BEAM4_HOME/bin/ceres-launcher.jar" \
    Write -PclearCacheAfterRowWrite=true -f NetCDF4-LC-Map -t $target $@
