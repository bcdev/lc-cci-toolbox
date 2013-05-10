#!/bin/bash
# lccciconvert.sh /data/lc-map-example/lc_classif_lccs_2010_v2.tif

if [ -z "$1" ]; then
    echo "Land Cover CCI Conversion Tool (Tiff to NetCDF-4)"
    echo "call   : lccciconvert.sh <classification-tif-file>|<condition-tif-file>"
    echo ""
    echo "For further information see the readme.txt"
    exit 1
fi

export TOOL_HOME=`( cd $(dirname $0); cd ..; pwd )`

exec java \
    -Xmx2048M -Dbeam.reader.tileHeight=1024 -Dbeam.reader.tileWidth=1024 \
    -Dceres.context=beam \
    -Dbeam.mainClass=org.esa.beam.framework.gpf.main.GPT \
    "-Dbeam.home=$TOOL_HOME" \
    -jar "$TOOL_HOME/bin/ceres-launcher.jar" \
    LCCCI.Convert $@