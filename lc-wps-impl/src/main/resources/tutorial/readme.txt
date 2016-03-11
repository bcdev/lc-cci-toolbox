This is an instruction on how to send a request to LC CCI WPS.

Pre-requisites:
1. wget

GetCapabilities URL (GET):
[browser] http://www.brockmann-consult.de/bc-wps/wps/lc-cci?Service=WPS&Request=GetCapabilities
[script] getCapabilities.sh
The expected response can be seen in this file lc-cci-wps-GetCapabilities-sample-response.xml

DescribeProcess URL (GET):
[browser] http://www.brockmann-consult.de/bc-wps/wps/lc-cci?Service=WPS&Request=DescribeProcess&Version=1.0.0&Identifier=subsetting
[script] describeProcess.sh subsetting
The expected response can be seen in this file lc-cci-wps-DescribeProcess-sample-response.xml

Execute URL (POST):
This is a POST request, so a client tool is required to send a request with the request XML. One tool that has been used for testing is
a Chrome extension named DHC by Restlet. When that is not possible, a script would be much more practical.
[REST client] www.brockmann-consult.de/bc-wps/wps/lc-cci
[script] execute.sh lc-cci-wps-Execute-request.xml response.xml
The sample request can be seen in this file lc-cci-wps-execute-request.xml.
For that request, the response should look like lc-cci-wps-execute-response.xml.
A GetStatus URL is available in that response to check the status of the process.

GetStatus URL (GET):
[browser] http://www.brockmann-consult.de/bc-wps/wps/lc-cci?Service=WPS&Request=GetStatus&JobId=[the job ID]
[script] getStatus.sh response.xml
Note that the complete URL of GetStatus request is available on the response of the Execute request.
When the process has been successfully completed, the URL(s) to the products is also available at the response.
Enter the URL(s) in the browser to download the product(s).