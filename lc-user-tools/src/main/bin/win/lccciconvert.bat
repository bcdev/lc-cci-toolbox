@echo off

IF "%1"=="" GOTO HELP

IF "%JAVA_HOME%"=="" GOTO JAVA_NO_SET

set TOOL_HOME="%CD%"

java ^
    -Xmx2048M -Dbeam.reader.tileHeight=1024 -Dbeam.reader.tileWidth=1024 ^
    -Dceres.context=lccci ^
    -Dlccci.mainClass=org.esa.beam.framework.gpf.main.GPT ^
    -jar "%TOOL_HOME%\ceres-launcher.jar" ^
    LCCCI.Convert %*

exit /B %ERRORLEVEL%


:HELP
@echo off
echo Land Cover CCI Tiff to NetCDF 4 converter
echo call: lccciconvert.bat ^<classification-tif-file^>|^<condition-tif-file^>
echo
echo For further information take a look at the readme.txt file.
exit /B 1

:JAVA_NO_SET
echo You need to set the JAVA_HOME environment variable to the location of your Java installation. ^
e.g. set JAVA_HOME="C:\Program Files\Java\jdk1.6.0_41"
exit /B 2