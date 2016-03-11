#!/bin/sh

# This is a script to send GetCapabilities request to LC CCI WPS. The response is displayed on the screen.
# Usage : getCapabilities.sh
# Example : getCapabilities.sh

read -p "Enter User Name: " USER

wget -q --user=$USER --ask-password -O- "www.brockmann-consult.de/bc-wps/wps/lc-cci?Service=WPS&Request=GetCapabilities"
