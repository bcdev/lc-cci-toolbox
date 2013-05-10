#!/bin/bash
# convert.sh /data/lc-map-example/lc_classif_lccs_2010_v2.tif

if [ -z "$1" ]; then
    echo "Land Cover CCI Conversion Tool (Tiff to NetCDF-4)"
    echo "call: convert.sh <classification-tif-file>|<condition-tif-file>"
    echo ""
    echo "For further information see the readme.txt"
    exit 1
fi

export TOOL_HOME=`( cd $(dirname $0); cd ..; pwd )`

exec java \
    -Xmx2G -Dbeam.reader.tileHeight=1024 -Dbeam.reader.tileWidth=1024 \
    -Dceres.context=lccci \
    -Dlccci.mainClass=org.esa.beam.framework.gpf.main.GPT \
    -jar "$TOOL_HOME/bin/ceres-launcher.jar" \
    LCCCI.Convert $@