LC-CCI Aggregation Tool
   v0.1 - 10.09.2012
~~~~~~~~~~~~~~~~~~~~~~~

Installation
~~~~~~~~~~~~
1) Install the current snapshot version of BEAM. It can be downloaded from the BEAM web site at
(http://www.brockmann-consult.de/beam-snapshots/beam-installers/).
2) Copy the lc-aggregation-tool-0.1.jar file into the modules folder of the BEAM installation directory.
3) For running the aggregation tool one of the batch files needs to be changed. In the <BEAM_INSTALLATION_DIR>\bin
   folder you can find the gpt.bat file. Copy this file to your desktop and open it in a text editor.
   Change the line
        -Xmx1024M
    to
        -Xmx3500M
   This allows the tool to consume more memory. Instead of 1GB it can then use 3.5GB. If you can not specify such high
   values (because your computer doesn't have enough memory installed) it can happen that the process takes very long or
   brakes after a long time of computation.
   After changing and saving the file copy it back to the original folder and replace the old batch file.

Execution
~~~~~~~~~
The capabilities of the aggregation tool are limited in this version. Currently it can only be invoked from the command
line. However, in the tools menu of VISAT a menu item named 'LC CCI Aggregation Tool...' can be found. This entry in
the menu invokes the user interface for the aggregation tool. At the current stage it is only a demonstrator and will
not give proper results. Actually it will exit with an exception when the run button is hit. Even the tool is not
runnable from the GUI, it gives a good impression the supported options and how it can be used when it is finished.

On the command line the tool can be invoked as follows.
1) Change the current directory to <BEAM_INSTALLATION_DIR>\bin
2) The tool can then be called by:
gpt LCCCI.Aggregate -PnumberOfMajorityClasses=<numMajorityClasses> -PnumRows=<numRows> -SsourceProduct=<sourceFile>

Where
* gpt LCCCI.Aggregate calls the gpt tool with the LC-CCI aggregation tool.
* -PnumberOfMajorityClasses=<numMajorityClasses> specifies the number of majority classes in the output. This parameter
  can be omitted, in this case the default (5) is used.
* -PnumRows=<numRows> specifies the number of rows of the internally use SEA (Sinusoidal Equal Area) grid. Specifies
  indirectly the resolution of the output. If omitted, the default value (2160) is used. A grid with the default number
  of rows leads to a resolution of ~9.8km/pixel in the target product.
* -SsourceProduct=<sourceFile> defines the path to the source file.

A complete example might look like the following:
gpt LCCCI.Aggregate -PnumberOfMajorityClasses=3 -PnumRows=216 -SsourceProduct="C:\Data\LCCCI\lc_classif_lccs_v2.tif"

The result of the aggregation is written to the current directory (<BEAM_INSTALLATION_DIR>\bin). It is written as
NetCDF4 file, which is named target.nc. Previous results are overwritten.




