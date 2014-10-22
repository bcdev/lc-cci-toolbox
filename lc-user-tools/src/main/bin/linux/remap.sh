#!/bin/bash
# remap.sh /data/lc-map-example/ESACCI-LC-L4-LCCS-Map-300m-P5Y-2010-v2.nc [/data/lc-map-example/Default_LCCS2PFT_LUT_update.txt]

set -e

if [ -z "$1" ]; then
    echo "Land Cover CCI Classes Remapping Tool"
    echo "call: remap.sh <map-netcdf-file> [classes_LUT]"
    echo ""
    echo "For further information see the readme.txt"
    exit 1
fi

if [ -z "$2" ]; then
    lut=../resources/Default_LCCS2PFT_LUT_update.csv
else
    lut=$2
fi

export TOOL_HOME=`( cd $(dirname $0); cd ..; pwd )`

java -Xmx250M -Dceres.context=beam \
    -Dbeam.logLevel=INFO -Dbeam.consoleLog=true \
    -Dbeam.mainClass=org.esa.cci.lc.conversion.RemapGraphCreator \
    -jar "$TOOL_HOME/bin/ceres-launcher.jar" \
    ${lut} $1_updated.nc

java -Xmx16G -Dceres.context=beam \
    -Dbeam.reader.tileHeight=1024 -Dbeam.reader.tileWidth=1024 \
    -Dbeam.logLevel=INFO -Dbeam.consoleLog=true \
    -Dbeam.mainClass=org.esa.beam.framework.gpf.main.GPT \
    -jar "$TOOL_HOME/bin/ceres-launcher.jar" \
    remap_graph.xml $1

rm remap_graph.xml