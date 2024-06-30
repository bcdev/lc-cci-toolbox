#!/bin/bash
# convert.sh /data/lc-map-example/lc_classif_lccs_2010_v2.tif

if [ -z "$1" ]; then
    echo "Land Cover CCI Conversion Tool (Tiff to NetCDF-4)"
    echo ""
    echo "For further information see the readme.txt"
    exit 1
fi

export TOOL_HOME=`( cd $(dirname $0); cd ..; pwd )`
echo "using user tool $TOOL_HOME"
export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:$TOOL_HOME/lib

exec java \
    -cp "$TOOL_HOME/modules/*"
    -Xmx8G \
    -Dsnap.mainClass=org.esa.snap.core.gpf.main.GPT \
    -Dsnap.home="$TOOL_HOME" \
    -Dsnap.logLevel=INFO \
    -Dsnap.consoleLog=true \
    -Dsnap.dataio.reader.tileHeight=2025 \
    -Dsnap.dataio.reader.tileWidth=2025 \
    -Dsnap.gpf.tileComputationObserver=org.esa.snap.core.gpf.monitor.TileComputationEventLogger \
    org.esa.snap.runtime.Launcher
    LCCCI.Convert -e $@