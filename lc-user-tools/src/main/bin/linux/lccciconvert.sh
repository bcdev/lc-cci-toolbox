#!/bin/bash
# lccciconvert.sh /data/lc-map-example/lc_classif_lccs_2010_v2.tif

if [ -z "$1" ]; then
    echo "Land Cover CCI Tiff to NetCDF 4 converter"
    echo "call   : lccciconvert.sh <classification-tif-file>|<condition-tif-file>"
    echo ""
    echo "For further information take a look at the readme.txt file."
    exit 1
fi

export TOOL_HOME=`( cd $(dirname $0); cd ..; pwd )`

exec java \
    -Xmx2048M -Dbeam.reader.tileHeight=1024 -Dbeam.reader.tileWidth=1024 \
    -Dceres.context=beam \
    -Dbeam.mainClass=org.esa.beam.framework.gpf.main.GPT \
    "-Dbeam.home=$TOOL_HOME" \
    "-Dncsa.hdf.hdflib.HDFLibrary.hdflib=$TOOL_HOME/modules/lib-hdf-2.7/lib/libjhdf.so" \
    "-Dncsa.hdf.hdf5lib.H5.hdf5lib=$TOOL_HOME/modules/lib-hdf-2.7/lib/libjhdf5.so" \
    -jar "$TOOL_HOME/bin/ceres-launcher.jar" \
    LCCCI.Convert $@