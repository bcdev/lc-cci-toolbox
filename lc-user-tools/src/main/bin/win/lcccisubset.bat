@echo off

IF "%1"=="" GOTO HELP

IF "%JAVA_HOME%"=="" GOTO JAVA_NO_SET

set TOOL_HOME="%CD%"

java -Xmx2G -Dceres.context=lccci ^
    -Dlccci.mainClass=org.esa.beam.framework.gpf.main.GPT ^
    -jar "%TOOL_HOME%\ceres-launcher.jar" ^
    LCCCI.Subset %*

exit /B %ERRORLEVEL%


:HELP
@echo off
echo Land Cover CCI NetCDF 4 Subsetting Tool
echo call: lcccisubset.bat -Pnorth=^<degree^> -Peast=^<degree^> -Psouth=^<degree^> -Pwest=^<degree^> ^<netcdf-file^>
echo or
echo call: lcccisubset.bat -PpredefinedRegion=^<EUROPE|ASIA^> ^<netcdf-file^>
echo
echo For further information see the readme.txt
exit /B 1

:JAVA_NO_SET
echo You need to set the JAVA_HOME environment variable to the location of your Java installation. ^
e.g. set JAVA_HOME="C:\Program Files\Java\jdk1.6.0_41"
exit /B 2