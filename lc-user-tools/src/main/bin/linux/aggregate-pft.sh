#!/bin/bash
# aggregation.sh /data/lc-map-example/ESACCI-LC-L4-LCCS-Map-300m-P5Y-2010-v2.nc

if [ -z "$1" ]; then
    echo "Land Cover PFT Aggregation Tool"
    echo ""
    echo "For further information see the readme.txt"
    exit 1
fi

export TOOL_HOME=`( cd $(dirname $0); cd ..; pwd )`
export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:$TOOL_HOME/lib

exec java -Xmx8G -Dceres.context=snap \
    -Dsnap.logLevel=INFO -Dsnap.consoleLog=true \
    -Dsnap.mainClass=org.esa.snap.core.gpf.main.GPT \
    -Dsnap.binning.sliceHeight=64 \
    -jar "$TOOL_HOME/bin/ceres-launcher.jar" \
    LC.Aggregate.Pft -e -c 1024M $@