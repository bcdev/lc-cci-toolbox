#!/bin/bash
# lcccisubset.sh -PpredefinedRegion=ASIA /data/lc-map-example/ESACCI-LC-L4-LCCS-Map-300m-P5Y-2010-v2.nc

if [ -z "$1" ]; then
    echo "Land Cover CCI NetCDF 4 Subsetting Tool"
    echo "call   : lcccisubset.sh -Pnorth=<degree> -Peast=<degree> -Psouth=<degree> -Pwest=<degree> <netcdf-file>"
    echo "or"
    echo "call   : lcccisubset.sh -PpredefinedRegion=<EUROPE|ASIA> <netcdf-file>"
    echo ""
    echo "For further information see the readme.txt"
    exit 1
fi

export TOOL_HOME=`( cd $(dirname $0); cd ..; pwd )`

exec java -Xmx2048M -Dceres.context=beam \
    -Dbeam.mainClass=org.esa.beam.framework.gpf.main.GPT \
    "-Dbeam.home=$TOOL_HOME" \
    -jar "$TOOL_HOME/bin/ceres-launcher.jar" \
    LCCCI.Subset $@