#!/bin/bash

# This is a script to send an execute request to LC CCI WPS with a given xml request file
# Usage : execute.sh [execute request XML] [response XML file]
# Example : execute.sh lc-cci-wps-Execute-request.xml response.xml

EXECUTE_XML=$1
RESPONSE_FILE=$2

read -p "Enter User Name: " WPS_USER

wget --user=$WPS_USER --ask-password --header="Content-Type:application/xml" --post-file="${EXECUTE_XML}" -O "${RESPONSE_FILE}" "www.brockmann-consult.de/bc-wps/wps/lc-cci"
