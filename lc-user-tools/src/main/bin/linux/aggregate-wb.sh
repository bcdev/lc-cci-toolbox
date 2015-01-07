#!/bin/bash
# aggregation.sh /data/lc-map-example/ESACCI-LC-L4-WB-Map-300m-P6Y-2005-v3.0.nc

if [ -z "$1" ]; then
    echo "Land Cover CCI Aggregation Tool"
    echo "call: aggregation.sh <wb-netcdf-file>"
    echo ""
    echo "For further information see the readme.txt"
    exit 1
fi

export TOOL_HOME=`( cd $(dirname $0); cd ..; pwd )`

exec java -Xmx4G -Dceres.context=beam \
    -Dbeam.logLevel=INFO -Dbeam.consoleLog=true \
    -Dbeam.mainClass=org.esa.beam.framework.gpf.main.GPT \
    -Dbeam.binning.sliceHeight=64 \
    -jar "$TOOL_HOME/bin/ceres-launcher.jar" \
    LCCCI.Aggregate.WB -e -c 1024M $@