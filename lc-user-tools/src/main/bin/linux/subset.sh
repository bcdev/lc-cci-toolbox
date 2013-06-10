#!/bin/bash
# subset.sh -PpredefinedRegion=ASIA /data/lc-map-example/ESACCI-LC-L4-LCCS-Map-300m-P5Y-2010-v2.nc

if [ -z "$1" ]; then
    echo "Land Cover CCI NetCDF 4 Subsetting Tool"
    echo "call: subset.sh -Pnorth=<degree> -Peast=<degree> -Psouth=<degree> -Pwest=<degree> <netcdf-file>"
    echo "or"
    echo "call: subset.sh -PpredefinedRegion=<EUROPE|ASIA> <netcdf-file>"
    echo ""
    echo "For further information see the readme.txt"
    exit 1
fi

export TOOL_HOME=`( cd $(dirname $0); cd ..; pwd )`

exec java -Xmx2G -Dceres.context=beam \
    -Dbeam.logLevel=INFO -Dbeam.consoleLog=true ^
    -Dbeam.mainClass=org.esa.beam.framework.gpf.main.GPT \
    -jar "$TOOL_HOME/bin/ceres-launcher.jar" \
    LCCCI.Subset $@