@echo off

if "%1"=="" GOTO HELP

for /f %%j in ("java.exe") do (
    set JAVA_LOCATION=%%~dp$PATH:j
)
if %JAVA_LOCATION%.==. GOTO JAVA_NO_INSTALLED

set TOOL_HOME="%CD%"

java ^
    -Xmx2G -Dbeam.reader.tileHeight=1024 -Dbeam.reader.tileWidth=1024 ^
    -Dceres.context=lccci ^
    -Dlccci.mainClass=org.esa.beam.framework.gpf.main.GPT ^
    -jar "%TOOL_HOME%\ceres-launcher.jar" ^
    LCCCI.Convert %*

exit /B %ERRORLEVEL%

:HELP
echo Land Cover CCI Conversion Tool (Tiff to NetCDF-4)
echo call: convert.bat ^<classification-tif-file^>|^<condition-tif-file^>
echo
echo For further information see the readme.txt
exit /B 1

:JAVA_NO_INSTALLED
echo Java is not installed. Please install Java JRE (version >= 1.6) first. ^
(http://www.oracle.com/technetwork/java/javase/downloads/index.html)
exit /B 2