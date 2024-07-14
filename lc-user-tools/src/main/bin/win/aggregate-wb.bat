@echo off

if "%1"=="" GOTO HELP

for /f %%j in ("java.exe") do (
    set JAVA_LOCATION=%%~dp$PATH:j
)
if "%JAVA_LOCATION%".==. GOTO JAVA_NO_INSTALLED

SET mypath=%~dp0
SET TOOL_HOME=%mypath:~0,-5%
echo "using user tool %TOOL_HOME%"
set PATH=%PATH%;%TOOL_HOME%\lib

java ^
    -cp "%TOOL_HOME%\modules\*" ^
    -Xmx8G ^
    -Dsnap.mainClass=org.esa.snap.core.gpf.main.GPT ^
    -Dsnap.home="$TOOL_HOME" ^
    -Djava.io.tmpdir=. ^
    -Dsnap.logLevel=INFO ^
    -Dsnap.consoleLog=true ^
    -Dsnap.binning.sliceHeight=1024 ^
    -Dsnap.dataio.reader.tileHeight=2025 ^
    -Dsnap.dataio.reader.tileWidth=2025 ^
    org.esa.snap.runtime.Launcher ^
    LCCCI.Aggregate.WB -e -c 1024M -PoutputTileSize=405:2025 %*

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
