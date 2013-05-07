@echo off

if "%1"=="" GOTO HELP

for /f %%j in ("java.exe") do (
    set JAVA_LOCATION=%%~dp$PATH:j
)
if %JAVA_LOCATION%.==. GOTO JAVA_NO_INSTALLED

set TOOL_HOME="%CD%"

java -Xmx4G -Dceres.context=lccci ^
    -Dlccci.mainClass=org.esa.beam.framework.gpf.main.GPT ^
    -jar "%TOOL_HOME%\ceres-launcher.jar" ^
    LCCCI.Aggregate %*

exit /B %ERRORLEVEL%

:HELP
@echo off
echo Land Cover CCI Aggregation Tool
echo call   : lccciaggregation.bat ^<map-netcdf-file^>
echo example: lccciaggregation.bat /data/ESACCI-LC-L4-LCCS-Map-300m-P5Y-2010-v2.nc
echo output : ^<aggregated-netcdf-file^>
exit /B 1

:JAVA_NO_INSTALLED
echo Java is not installed. Please install Java JRE (version >= 1.6) first. (http://www.oracle.com/technetwork/java/javase/downloads/index.html)
exit /B 2