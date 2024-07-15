#!/bin/bash
# aggregation.sh /data/lc-map-example/ESACCI-LC-L4-LCCS-Map-300m-P5Y-2010-v2.nc

if [ -z "$1" ]; then
    echo "Land Cover PFT Aggregation Tool"
    echo ""
    echo "For further information see the readme.txt"
    exit 1
fi

export TOOL_HOME=`( cd $(dirname $0); cd ..; pwd )`
echo "using user tool $TOOL_HOME"
export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:$TOOL_HOME/lib

exec java \
    -cp "$TOOL_HOME/modules/*" \
    -Xmx8G \
    -Dsnap.mainClass=org.esa.snap.core.gpf.main.GPT \
    -Dsnap.home="$TOOL_HOME" \
    -Djava.io.tmpdir=. \
    -Dsnap.logLevel=INFO \
    -Dsnap.consoleLog=true \
    -Dsnap.binning.sliceHeight=1024 \
    -Dsnap.dataio.reader.tileHeight=2025 \
    -Dsnap.dataio.reader.tileWidth=2025 \
    org.esa.snap.runtime.Launcher \
    LC.Aggregate.Pft -e -c 1024M -PoutputTileSize=405:2025 $@
