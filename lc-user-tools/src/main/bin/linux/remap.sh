#!/bin/bash
# convert.sh /data/lc-map-example/lc_classif_lccs_2010_v2.tif

if [ -z "$1" ]; then
    echo "Land Cover CCI Conversion Tool (Tiff to NetCDF-4)"
    echo ""
    echo "For further information see the readme.txt"
    exit 1
fi

export TOOL_HOME=`( cd $(dirname $0); cd ..; pwd )`

exec java -Xmx8G -Dceres.context=beam \
    -Dbeam.logLevel=INFO -Dbeam.consoleLog=true \
    -Dbeam.mainClass=org.esa.beam.framework.gpf.main.GPT \
    -jar "$TOOL_HOME/bin/ceres-launcher.jar" \
    LCCCI.Remap -e -c 1024M $@