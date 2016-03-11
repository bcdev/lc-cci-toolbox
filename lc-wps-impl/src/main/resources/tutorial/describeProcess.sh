#!/bin/sh

# This is a script to send DescribeProcess request to LC CCI WPS.
# This script takes 1 parameter : the processor ID. For including all parameters, use 'all' as the processor ID.
# The response is displayed on the screen.
# Usage : describeProcess.sh [process ID]
# Example : describeProcess.sh subsetting
#           describeProcess.sh all

PROCESS_ID=$1

read -p "Enter User Name: " USER

wget -q --user=$USER --ask-password -O- "www.brockmann-consult.de/bc-wps/wps/lc-cci?Service=WPS&Request=DescribeProcess&Version=1.0.0&Identifier=${PROCESS_ID}"
