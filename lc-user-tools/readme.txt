                               LC-CCI User Tools
                               v0.5 - 13.05.2013
                            ~~~~~~~~~~~~~~~~~~~~~~~

Summary
~~~~~~~
These set of tools (conversion tool, aggregation tool, subset tool) provides the possibilities
to prepare data for model computation.


General Note
~~~~~~~~~~~~
The target files are always written in NetCDF-4 (enhanced model) file format.
If the NetCDF-4 Classic file format is needed the standard nccopy tool can be used for conversion.


Installation
~~~~~~~~~~~~
1) Unzip the zip-file in a directory of your choice.
2) Inside the unzipped directory you can find a folder which is named 'bin'.
   Inside you can find the windows and unix start scripts for the LCCCI tools.


Execution
~~~~~~~~~
All start scripts are available in widows and unix versions.
Use the scripts in the same manner.

    Conversion Tool Usage (converts Tiff to NetCDF-4 files)
    ~~~~~~~~~~~~~~~~~~~~~~~~
        convert.sh <pathToMapTifFile|pathToConditionTifFile>

        In case of a LCCCI Map file the corresponding flag files (must be in the same directory as the Map file)
        are automatically detected and added to the output NetCDF-4 file.
        If a condition product shall be converted the AggMean tif file must be provided as source. All the associated
        variables (AggMean, Std, Status and NYearObs) are considered and integrated into the output NetCDF-4 file if
        they reside in the same folder as the source tif file.

    Aggregation Tool Usage
    ~~~~~~~~~~~~~~~~~~~~~~
        aggregation.sh -PgridName=<name> -PnumRows=<integer>
                       -PoutputLCCSClasses=<boolean> -PnumMajorityClasses=<integer>
                       -PoutputPFTClasses=<boolean> -PuserPFTConversionTable=<filePath>
                       -PtargetFile=<filePath> <sourceFilePath>

        Parameter Description:
            -PgridName=<name>
                Specifies the target grid of the resulting product. For example a regular gaussian grid.
                Valid Parameters are:  GEOGRAPHIC_LAT_LON  and  REGULAR_GAUSSIAN_GRID
                This is a mandatory parameter.
            -PnumRows=<integer>
                Specifies the number of rows for the specified grid.
                Default ist 2160 rows. A grid with the default number of rows leads to a resolution of
                ~9.8km/pixel in the target product.
                For a REGULAR_GAUSSIAN_GRID onyl the following values are valid:
                    32, 48, 80, 128, 160, 200, 256, 320, 400, 512, 640
            -PoutputLCCSClasses=<boolean>
                Specifies if the LCCS classes shall be added to the output. This parameter can be
                omitted. The default is true.
            -PnumMajorityClasses=<integer>
                Specifies the number of majority classes in the output. This parameter can be
                omitted, in this case the default (5) is used.
            -PoutputPFTClasses=<boolean>
                Specifies if a conversion to PFT classes shall be performed and the result added to the
                output. This parameter can be omitted. The default is true.
            -PuserPFTConversionTable=<filePath>
                Specifies the path to a user defined PFT conversion table. If not given the default
                LCCCI conversion table will be used. For a description of the file format see further down.
            -PtargetFile=<filePath>
                Specifies the file where the target will be written. It is written as NetCDF-4 file.
                If already a file with the same name/path exists, it will be overwritten.
            <sourceFilePath>
                Is the path to the source NetCDF-4 file.

        A real example might look like the following:
        aggregate.sh -PgridName=REGULAR_GAUSSIAN_GRID -PnumRows=320 -PoutputLCCSClasses=false -PnumberOfMajorityClasses=3
                     -PtargetFile="/data/LCCCI/output/aggregated.nc" "/data/LCCCI/ESACCI-LC-L4-LCCS-Map-300m-P5Y-2010-v2.nc"

        The PFT (Plant Functional Type) conversion table
        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
            The file starts with a table header. Each column of the header defines one PFT.
            The subsequent rows, one for each LCCS class, define the conversion from corresponding class to the PFTs.
            Columns are separated with the pipe ('|') symbol and the column header names are used as band names.

    Subset Tool Usage
    ~~~~~~~~~~~~~~~~~~
        subset.sh -PpredefinedRegion=<EUROPE|ASIA> <sourceFilePath>
                or
        subset.sh -Pnorth=<degree> -Peast=<degree> -Psouth=<degree> -Pwest=<degree> <sourceFilePath>

        -PpredefinedRegion=<EUROPE|ASIA>
            Specifies one of the available predefined regions.
            Valid Values are: EUROPE, ASIA
        -Pnorth=<degree>
            Specifies north bound of the regional subset.
        -Peast=<degree>
            Specifies east bound of the regional subset.
        -Psouth=<degree>
            Specifies south bound of the regional subset.
        -Pwest=<degree>
            Specifies west bound of the regional subset.
        <sourceFilePath>
            The source file to create a regional subset from.

        In order to create a regional subset of a map, condition or aggregated product the subset
        tool can be used. As parameter either one of the predefined regions can be selected or the
        outer bounds of the desired region can be specified. The target file is written into
        the directory of the source file.



