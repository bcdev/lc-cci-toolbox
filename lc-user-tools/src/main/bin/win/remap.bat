@echo off
rem remap.bat /data/lc-map-example/ESACCI-LC-L4-LCCS-Map-300m-P5Y-2010-v2.nc [/data/lc-map-example/Default_LCCS2PFT_LUT_update.txt]

for /f %%j in ("java.exe") do (
    set JAVA_LOCATION=%%~dp$PATH:j
)
if %JAVA_LOCATION%.==. GOTO JAVA_NO_INSTALLED

set TOOL_HOME="%CD%"

if [%1]==[] GOTO HELP
if [%2]==[] (set lut="..\resources\Default_LCCS2PFT_LUT_update.csv") else (set lut=%2)

java ^
    -Xmx250M -Dceres.context=beam ^
    -Dbeam.logLevel=INFO -Dbeam.consoleLog=true ^
    -Dbeam.mainClass=org.esa.cci.lc.conversion.RemapGraphCreator ^
    -jar %TOOL_HOME%\ceres-launcher.jar ^
    %lut%

if %ERRORLEVEL% NEQ 0 exit /B %ERRORLEVEL%

java ^
    -Xmx6G -Dceres.context=beam ^
    -Dbeam.reader.tileHeight=1024 -Dbeam.reader.tileWidth=1024 ^
    -Dbeam.logLevel=INFO -Dbeam.consoleLog=true ^
    -Dbeam.mainClass=org.esa.beam.framework.gpf.main.GPT ^
    -jar %TOOL_HOME%\ceres-launcher.jar ^
    remap_graph.xml -x -f NetCDF4-LC-Map -t %1_updated.nc %1

del remap_graph.xml

exit /B %ERRORLEVEL%


:HELP
echo Land Cover CCI Classes Remapping Tool
echo call: remap.bat ^<map-netcdf-file^> ^[classes_LUT^]
echo.
echo For further information see the readme.txt
exit /B 1

:JAVA_NO_INSTALLED
echo Java is not installed. Please install Java JRE (version >= 1.6) first. ^
(http://www.oracle.com/technetwork/java/javase/downloads/index.html)
exit /B 2