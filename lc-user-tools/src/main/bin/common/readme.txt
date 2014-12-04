CCI-LC User Tools
~~~~~~~~~~~~~~~~~
Version: ${pom.version}
Release: ${buildDate}


Summary
~~~~~~~
This set of tools (conversion tool, aggregation tool, subset tool) prepares data for model computation.


General Note
~~~~~~~~~~~~
The target files are always written in NetCDF-4 (enhanced model) file format.
If the NetCDF-4 Classic file format is needed the standard nccopy tool can be used for conversion.


Installation
~~~~~~~~~~~~
As a prerequisite the CCI-LC User Tools require an installed Java SE JRE version 7 or higher
on the system. It can be obtained from the web page at
http://www.oracle.com/technetwork/java/javase/downloads/index.html.

1) Unzip the zip-file in a directory of your choice.
2) Inside the unzipped directory you can find a folder which is named 'bin'.
   Inside you can find the windows and unix start scripts for the CCI-LC tools.


Execution
~~~~~~~~~
All start scripts are available in windows and unix versions.
Use the scripts in the same manner.

    Conversion Tool Usage (converts Tiff to NetCDF-4 files)
    ~~~~~~~~~~~~~~~~~~~~~~~~
        convert.sh -PtargetDir=<dirPath> <pathToMapTifFile|pathToConditionTifFile>

        In case of a CCI-LC Map file the corresponding flag files must be in the same directory as the Map file.
        They are automatically detected and added to the output NetCDF-4 file.
        If a condition product shall be converted the AggMean tif file must be provided as source. All the associated
        variables (AggMean, Std, Status and NYearObs) are considered and integrated into the output NetCDF-4 file if
        they reside in the same folder as the source tif file.

        Parameter Description:
            -PtargetDir=<dirPath>
                Specifies the directory where the target will be written. If this parameter is omitted the directory
                of the source file is used. The target is written as NetCDF-4 file.
                If already a file with the same name/path exists, it will be overwritten.
                (see "Output File Naming Convention" )


    Aggregation Tool Usage
    ~~~~~~~~~~~~~~~~~~~~~~

      CCI-LC Condition Products
      ~~~~~~~~~~~~~~~~~~~~~~~~~
        aggregate-cond.sh -PgridName=<name> -PnumRows=<integer> -PtargetDir=<dirPath> <sourceFilePath>

        Parameter Description:
            -PgridName=<name>
                Specifies the target grid of the resulting product. For example a regular gaussian grid.
                Valid Parameters are:  GEOGRAPHIC_LAT_LON  and  REGULAR_GAUSSIAN_GRID
                This is a mandatory parameter.
            -PnumRows=<integer>
                Specifies the number of rows for the specified grid.
                Default ist 2160 rows. A grid with the default number of rows leads to a resolution of
                ~9.8km/pixel in the target product.
                For a REGULAR_GAUSSIAN_GRID only the following values are valid:
                    32, 48, 80, 128, 160, 200, 256, 320, 400, 512, 640
            -PpredefinedRegion=<regionName>
                Specifies one of the available predefined regions. This is an optional value.
                If a predefined region is given it has precedence over the user defined region (north, east, ...)
                Valid Values are: NORTH_AMERICA, CENTRAL_AMERICA, SOUTH_AMERICA, WESTERN_EUROPE_AND_MEDITERRANEAN,
                                  ASIA, AFRICA, SOUTH_EAST_ASIA, AUSTRALIA_AND_NEW_ZEALAND, GREENLAND
            -Pnorth=<degree>
                Specifies north bound of the regional subset. This is an optional value
            -Peast=<degree>
                Specifies east bound of the regional subset. This is an optional value
            -Psouth=<degree>
                Specifies south bound of the regional subset. This is an optional value
            -Pwest=<degree>
                Specifies west bound of the regional subset. This is an optional value
            -PtargetDir=<dirPath>
                Specifies the directory where the target will be written. If this parameter is omitted the directory
                of the source file is used. It is written as NetCDF-4 file.
                If already a file with the same name/path exists, it will be overwritten.
                (see "Output File Naming Convention" )
            <sourceFilePath>
                Is the path to the source NetCDF-4 file.

      CCI-LC Map Products
      ~~~~~~~~~~~~~~~~~~~
        aggregate-map.sh -PgridName=<name> -PnumRows=<integer>
                         -PoutputLCCSClasses=<boolean> -PnumMajorityClasses=<integer>
                         -PoutputPFTClasses=<boolean> -PuserPFTConversionTable=<filePath>
                         -PtargetDir=<dirPath> <sourceFilePath>

        Parameter Description:
            For a description of the common aggregation parameters please have a look into the above section
            for the CCI-LC Condition Products. In addition for the aggregation of the CCI-LC Map Products the
            following parameters exist:

            -PoutputLCCSClasses=<boolean>
                Specifies whether the LCCS classes shall be added to the output. This parameter can be
                omitted. The default is true.
            -PnumMajorityClasses=<integer>
                Specifies the number of majority classes in the output. This parameter can be
                omitted, in this case the default (5) is used. A value of 1 will produce an output with
                just the majority class.
            -PoutputPFTClasses=<boolean>
                Specifies if a conversion to PFT classes shall be performed and the result added to the
                output. This parameter can be omitted. The default is true.
            -PuserPFTConversionTable=<filePath>
                Specifies the path to a user defined PFT conversion table. If not given the default
                CCI-LC conversion table will be used. For a description of the file format see further down.
            -PoutputAccuracy=<boolean>
                Specifies the computation of the accuracy shall be performed and the result added to the
                output. This parameter can be omitted. The default is true.

        A real example might look like the following:
        aggregation-map.sh -PgridName=REGULAR_GAUSSIAN_GRID -PnumRows=320 -PoutputLCCSClasses=false -PnumMajorityClasses=3
                           -PpredefinedRegion=AUSTRALIA_AND_NEW_ZEALAND
                           -PtargetDir="/data/CCI-LC/output/" "/data/CCI-LC/ESACCI-LC-L4-LCCS-Map-300m-P5Y-2010-v2.nc"

        The PFT (Plant Functional Type) conversion table
        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
            The table, also known as Cross Walking Table, describes the conversion of the LCCS classes to PFTs.
            The file can start with an optional comment. If the comment is used the first line must start with '#' in
            order to indicate the comment. Multiple lines are not supported. The comment ('pft_table_comment') is
            included as an attribute into the NetCDF output file.
            The actual PFT table starts with a table header. Each column of the header defines one PFT except the first.
            The first column is for the LCCS class indices.
            The subsequent data rows, one for each LCCS class, define the conversion from corresponding class to the
            PFTs. Each cell specifies the percentage of the PFT, floating point values can be used. Zero percentage
            can be omitted. Columns are separated with the pipe ('|') symbol and the column header names are used
            as band names.

            Example:
            # An optional comment describing the conversion table
            LCCS Class|Tree Broadleaf Evergreen|...|Managed Grass|Bare soil|Water|Snow/Ice|No data
            0||...|||||100
            10||...|100||||
            11||...|100||||
            12||...|50||||
            20||...|100||||
            30|5|...|60||||
            40|5|...|25|40|||
            ...
            220||...||||100|



    Subset Tool Usage
    ~~~~~~~~~~~~~~~~~~
        subset.sh -PpredefinedRegion=<regionName> -PtargetDir=<dirPath> <sourceFilePath>
                or
        subset.sh -Pnorth=<degree> -Peast=<degree> -Psouth=<degree> -Pwest=<degree> -PtargetDir=<dirPath> <sourceFilePath>

        -PpredefinedRegion=<regionName>
            Specifies one of the available predefined regions.
            Valid Values are: NORTH_AMERICA, CENTRAL_AMERICA, SOUTH_AMERICA, WESTERN_EUROPE_AND_MEDITERRANEAN,
                              ASIA, AFRICA, SOUTH_EAST_ASIA, AUSTRALIA_AND_NEW_ZEALAND, GREENLAND
        -Pnorth=<degree>
            Specifies north bound of the regional subset.
        -Peast=<degree>
            Specifies east bound of the regional subset.
        -Psouth=<degree>
            Specifies south bound of the regional subset.
        -Pwest=<degree>
            Specifies west bound of the regional subset.
        -PtargetDir=<dirPath>
            Specifies the directory where the target will be written. It is written as NetCDF-4 file.
            If already a file with the same name/path exists, it will be overwritten.
            (see "Output File Naming Convention" )
        <sourceFilePath>
            The source file to create a regional subset from.

        In order to create a regional subset of a map, condition or aggregated product the subset
        tool can be used. As parameter either one of the predefined regions can be selected or the
        outer bounds of the desired region can be specified. The target file is written into
        the directory of the source file.


    Classes Remapping Tool Usage
    ~~~~~~~~~~~~~~~~~~~~~~~~
        remap.sh <map-netcdf-file> [classes_LUT]

        This tool splits up the information found in the band "lccs_class" into the classes given via a CSV file.

        Either such a file is provided as second parameter, or a default classification will be used.
        The input CSV needs to adhere to the following format:
            <Source Name>|<target band name 1>|<target band name 2>| ...
            120|20|30| ...
            150|10|| ...

        Such a file would be interpreted as follows: It applies to each pixel that if the source band has the value 120,
        the target band 1 is assigned the value 20 and the target band 2 the value 30. If the source band has the value
        150, the target band 1 is assigned the value 10 and the target band 2 the no-data-value.
        Note that the separator character is expected to be '|'.

        Parameter Description:
            <map-netcdf-file>
                Specifies the input file.

            [classes_LUT] (optional)
                Points to the LUT file that will be used for remapping.


Output File Naming Convention
"""""""""""""""""""""""""""""

    Conversion Tool Output:
    ~~~~~~~~~~~~~~~~~~~~~~~
        Map Product:        ESACCI-LC-L4-LCCS-Map-{sRes}m-P{tRes}Y-{epoch}-v{versNr}.nc

        Condition Product:  ESACCI-LC-L4-{condition}-Cond-{sRes}m-P{tRes}D-{startY}{MonthDay}-v{versNr}.nc



    Split Points:
    ~~~~~~~~~~~~~
        Map Product:        ESACCI-LC-L4-LCCS-Map-{sRes}m-P{tRes}Y-{epoch}-v{versNr}.nc
                                                                  ^
                                                                  |--- Split Position

        Condition Product:  ESACCI-LC-L4-{condition}-Cond-{sRes}m-P{tRes}D-{startY}{MonthDay}-v{versNr}.nc
                                                                          ^
                                                                          |--- Split Position

    Examples Map Result:
    ~~~~~~~~~~~~~~~~~~~~
        Aggregation:
            Input  :  ESACCI-LC-L4-LCCS-Map-300m-P5Y-2006-v2.nc

            Output :  ESACCI-LC-L4-LCCS-Map-300m-P5Y-aggregated-0.083333Deg-2006-v2.nc

        Subset:
            Input  :  ESACCI-LC-L4-LCCS-Map-300m-P5Y-aggregated-0.083333Deg-2006-v2.nc

            Output :  ESACCI-LC-L4-LCCS-Map-300m-P5Y-aggregated-0.083333Deg-EUROPE-2006-v2.nc
            Output :  ESACCI-LC-L4-LCCS-Map-300m-P5Y-aggregated-0.083333Deg-ASIA-2006-v2.nc
            Output :  ESACCI-LC-L4-LCCS-Map-300m-P5Y-aggregated-0.083333Deg-USER_REGION-2006-v2.nc



    Examples Condition Result:
    ~~~~~~~~~~~~~~~~~~~~~~~~~~
        Subset:
            Input  :  ESACCI-LC-L4-NDVI-Cond-300m-P9Y7D-20010101-v2.nc

            Output :  ESACCI-LC-L4-NDVI-Cond-300m-P9Y7D-EUROPE-20010101-v2.nc
            Output :  ESACCI-LC-L4-NDVI-Cond-300m-P9Y7D-ASIA-20010101-v2.nc
            Output :  ESACCI-LC-L4-NDVI-Cond-300m-P9Y7D-USER_REGION-20010101-v2.nc

