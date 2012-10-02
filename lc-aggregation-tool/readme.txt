LC-CCI Aggregation Tool
   v0.2 - 02.10.2012
~~~~~~~~~~~~~~~~~~~~~~~

Installation
~~~~~~~~~~~~
1) Install the current snapshot version of BEAM. It can be downloaded from the BEAM web site at
(http://www.brockmann-consult.de/beam-snapshots/beam-installers/).
2) Copy the lc-aggregation-tool-0.2.jar file into the modules folder of the BEAM installation directory.
3) For running the aggregation tool one of the batch files needs to be changed. In the <BEAM_INSTALLATION_DIR>\bin
   folder you can find the gpt.bat file. Copy this file to your desktop and open it in a text editor.
   Change the line
        -Xmx1024M
    to
        -Xmx5000M
   This allows the tool to consume more memory. Instead of 1GB, it can then use 5GB. If you can not specify such high
   values (because your computer doesn't have enough memory installed) it can happen that the process takes very long or
   brakes after a long time of computation. Therefore specify the highest possible value.
   After changing and saving the file, copy it back to the original folder and replace the old batch file.

Execution
~~~~~~~~~
In the tools menu of VISAT a menu item named 'LC CCI Aggregation Tool...' can be found. This entry in the menu invokes
the user interface for the aggregation tool. The parameters for defining the output (projection method, pixel size,
bounds) are not considered in this version.

On the command line the tool can be invoked as follows.
1) Change the current directory to <BEAM_INSTALLATION_DIR>\bin
2) The tool can then be called by:
gpt LCCCI.Aggregate -PoutputLCCSClasses=<boolean> -PnumberOfMajorityClasses=<integer>
          -PoutputPFTClasses=<boolean> -PnumRows=<integer>
          -PtargetFile=<filePath> -SsourceProduct=<filePath>

Where
* gpt LCCCI.Aggregate calls the gpt tool with the LC-CCI aggregation tool.
* -PoutputLCCSClasses=<boolean> specifies if the LCCS classes shall be added to the output. This parameter can be
   omitted.The default is true.
* -PnumberOfMajorityClasses=<numMajorityClasses> specifies the number of majority classes in the output. This parameter
  can be omitted, in this case the default (5) is used.
* -PoutputPFTClasses=<boolean> specifies if a conversion to PFT classes shall be performed and the result added to the
  output. This parameter can be omitted.The default is true. In this version a fixed Look-Up-Table (LUT) is used. In
  later versions the LUT will be selectable.
* -PnumRows=<numRows> specifies the number of rows of the internally use SEA (Sinusoidal Equal Area) grid. Specifies
  indirectly the resolution of the output. If omitted, the default value (2160) is used. A grid with the default number
  of rows leads to a resolution of ~9.8km/pixel in the target product.
* -PtargetFile=<filePath> specifies the the file where the target will be written. It is written as NetCDF4 file.
  Previous results are overwritten.
* -SsourceProduct=<filePath> defines the path to the source file.


A real example might look like the following:
gpt LCCCI.Aggregate -PoutputLCCSClasses=false -PnumberOfMajorityClasses=3 -PnumRows=216
       -PtargetFile="C:\Data\output\aggregated.nc" -SsourceProduct="C:\Data\LCCCI\lc_classif_lccs_v2.tif"




