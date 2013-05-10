   LC-CCI User Tools
   v0.5 - 13.05.2013
~~~~~~~~~~~~~~~~~~~~~~~

Summary
~~~~~~~
These set of tools (conversion tool, aggregation tool, subset tool) provides the possibilities
to prepare data for model computation.


Installation
~~~~~~~~~~~~
1) Unzip the zip-file in a directory of your choice.
2) Inside the unzipped directory you can find a folder which is named 'bin'.
   Inside you can find the windows and unix start scripts for the LCCCI tools.


Execution
~~~~~~~~~

All start scripts are available in widows and unix versions.
Use the scripts in the same manner.

Convert map tif file example:
   lccciconvert.sh <path to map tif file>

Convert condition tif file example:
   lccciconvert.sh <path to condition tif file>

Map aggregation example:
   lccciaggregation.sh -PprojectionMethod=<name> -PnumRows=<integer>
                       -PoutputLCCSClasses=<boolean> -PnumMajorityClasses=<integer>
                       -PoutputPFTClasses=<boolean> -PtargetFile=<filePath>
                       -SsourceProduct=<filePath>

Create subset example:
   lcccisubset.sh -P todo



PFT  todo
Tree Broadleaf Evergreen|Tree Broadleaf Deciduous|Tree Needleleaf Evergreen|Tree Needleleaf Deciduous|Shrub Broadleaf Evergreen|Shrub Broadleaf Deciduous|Shrub Needleleaf Evergreen|Shrub Needleleaf Deciduous |Natural Grass|Managed Grass|Bare soil|Water|Snow/Ice|No data





In the tools menu of VISAT a menu item named 'LC CCI Aggregation Tool...' can be found. This entry invokes the user
interface for the aggregation tool. The parameters for defining the output (projection method, pixel size, bounds) are
not considered in this version.

On the command line the tool can be invoked as follows.
1) Change the current directory to <BEAM_INSTALLATION_DIR>\bin
2) The tool can then be called by:
gpt LCCCI.Aggregate -PoutputLCCSClasses=<boolean> -PnumMajorityClasses=<integer>
          -PoutputPFTClasses=<boolean> -PnumRows=<integer>
          -PtargetFile=<filePath> -SsourceProduct=<filePath>

Where
* gpt LCCCI.Aggregate calls the gpt tool with the LC-CCI aggregation tool.
* -PoutputLCCSClasses=<boolean> specifies if the LCCS classes shall be added to the output. This parameter can be
   omitted. The default is true.
* -PnumMajorityClasses=<integer> specifies the number of majority classes in the output. This parameter
  can be omitted, in this case the default (5) is used.
* -PoutputPFTClasses=<boolean> specifies if a conversion to PFT classes shall be performed and the result added to the
  output. This parameter can be omitted.The default is true. In this version a fixed Look-Up-Table (LUT) is used. In
  later versions the LUT will be selectable.
* -PnumRows=<integer> specifies the number of rows of the internally use SEA (Sinusoidal Equal Area) grid. Specifies
  indirectly the resolution of the output. If omitted, the default value (2160) is used. A grid with the default number
  of rows leads to a resolution of ~9.8km/pixel in the target product.
* -PtargetFile=<filePath> specifies the the file where the target will be written. It is written as NetCDF4 file.
  Previous results are overwritten.
* -Ssource=<filePath> defines the path to the source file.


A real example might look like the following:
gpt LCCCI.Aggregate -PoutputLCCSClasses=false -PnumberOfMajorityClasses=3 -PnumRows=216
       -PtargetFile="C:\Data\output\aggregated.nc" -Ssource="C:\Data\LCCCI\lc_classif_lccs_v2.tif"




