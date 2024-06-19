@echo off

if "%1"=="" GOTO HELP

for /f %%j in ("java.exe") do (
    set JAVA_LOCATION=%%~dp$PATH:j
)
if "%JAVA_LOCATION%".==. GOTO JAVA_NO_INSTALLED

set TOOL_HOME="%CD%"
set PATH=%PATH%;%CD%\..\lib

java -Xmx8G -Dceres.context=snap ^
    -Dsnap.logLevel=INFO -Dsnap.consoleLog=true ^
    -Dsnap.mainClass=org.esa.snap.core.gpf.main.GPT ^
    -Dsnap.binning.sliceHeight=64 ^
    -Dsnap.dataio.reader.tileHeight=2025 ^
    -Dsnap.dataio.reader.tileWidth=2025 ^
    -jar "%TOOL_HOME%\ceres-launcher.jar" ^
    LCCCI.Aggregate.Map -e -c 1024M %*

exit /B %ERRORLEVEL%

:HELP
echo Land Cover CCI Aggregation Tool
echo
echo For further information see the readme.txt
exit /B 1

:JAVA_NO_INSTALLED
echo Java is not installed. Please install Java JRE 64Bit (version ^>= 1.7) first. ^
(http://www.oracle.com/technetwork/java/javase/downloads/index.html)
exit /B 2