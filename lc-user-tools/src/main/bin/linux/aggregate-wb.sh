#!/bin/bash
# aggregation.sh /data/lc-map-example/ESACCI-LC-L4-WB-Map-300m-P6Y-2005-v3.0.nc

if [ -z "$1" ]; then
    echo "Land Cover CCI Aggregation Tool"
    echo ""
    echo "For further information see the readme.txt"
    exit 1
fi

export TOOL_HOME=`( cd $(dirname $0); cd ..; pwd )`
export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:$TOOL_HOME/lib

exec java -Xmx4G -Dceres.context=snap \
    -Dsnap.logLevel=INFO -Dsnap.consoleLog=true \
    -Dsnap.mainClass=org.esa.snap.core.gpf.main.GPT \
    -Dsnap.binning.sliceHeight=2025 \
    -Dsnap.binning.sliceWidth=2025 \
    -jar "$TOOL_HOME/bin/ceres-launcher.jar" \
    LCCCI.Aggregate.WB -e -c 1024M $@