#!/bin/bash
# lccciaggregation.sh /data/ESACCI-LC-L4-LCCS-Map-300m-P5Y-2010-v2.nc

if [ -z "$1" ]; then
    echo "Land Cover CCI Aggregation Tool"
    echo "call   : lccciaggregation.sh <map-netcdf-file>"
    echo "example: lccciaggregation.sh /data/ESACCI-LC-L4-LCCS-Map-300m-P5Y-2010-v2.nc"
    echo "output : <aggregated-netcdf-file>"
    exit 1
fi

export TOOL_HOME=`( cd $(dirname $0); cd ..; pwd )`

exec java -Xmx4G -Dceres.context=lccci \
    -Dlccci.mainClass=org.esa.beam.framework.gpf.main.GPT \
    -jar "$TOOL_HOME/bin/ceres-launcher.jar" \
    LCCCI.Aggregate $@