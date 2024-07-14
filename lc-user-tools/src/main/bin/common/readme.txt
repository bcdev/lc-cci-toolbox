CCI-LC User Tools
~~~~~~~~~~~~~~~~~
Version: ${pom.version}
Release: ${buildDate}


Summary
~~~~~~~
This set of tools (aggregation, remapping, PFT conversion, and subsetting) prepares data for model computation.


General Note
~~~~~~~~~~~~
The CCI-LC User Tool requires the NetCDF-formatted LC files as input, NOT the GeoTIFF-formatted ones.

The target files are always written in NetCDF-4 (enhanced model) file format.
If the NetCDF-4 Classic file format is needed the standard nccopy tool can be used for conversion.
When the REGULAR_GAUSSIAN_GRID is chosen as target grid and a regional subset which crosses the
prime meridian is also defined the the aggregation or the subsetting process will not work. This affects
the predefined regions WESTERN_EUROPE_AND_MEDITERRANEAN and AFRICA.


Installation
~~~~~~~~~~~~
As a prerequisite the CCI-LC User Tools require an installed Java SE 64Bit JRE version 8
on the system. It can be obtained from the web page at
https://www.azul.com/downloads/#downloads-table-zulu . Please, select a Java 8 JDK.

1) Unzip the lc user tool zip file in a directory of your choice.
2) Inside the unzipped directory you can find a folder named 'bin'.
   There, you can find the windows and unix start scripts for the CCI-LC tools.


Execution
~~~~~~~~~
All provided scripts are available in windows (*.bat) and unix (*.sh) versions.
The scripts need to be invoked from the command line.
Write the command as described as follows.

    Aggregation Tool Usage
    ~~~~~~~~~~~~~~~~~~~~~~

      CCI-LC Condition Products
      ~~~~~~~~~~~~~~~~~~~~~~~~~
        aggregate-cond(.sh/.bat) -PgridName=<name> -PnumRows=<integer> -PtargetDir=<dirPath> <sourceFilePath>

        Parameter Description:
            -PgridName=<name>
                Specifies the target grid of the resulting product. This is a mandatory parameter.
                Valid parameters are: GEOGRAPHIC_LAT_LON  and  REGULAR_GAUSSIAN_GRID.
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

      CCI-LC WB Products
      ~~~~~~~~~~~~~~~~~~~~~~~~~
        aggregate-wb(.sh/.bat) -PoutputWbClasses=<boolean> -PnumMajorityClasses=<integer>
                        -PgridName=<name> -PnumRows=<integer> -PtargetDir=<dirPath> <sourceFilePath>

        Parameter Description:
            -PoutputWbClasses=<boolean>
                Whether or not to add the WB class areas to the output. The default is true
            -PnumMajorityClasses=<integer>
                The number of majority classes generated and added to the output. The default is 2.
            -PgridName=<name>
                Specifies the target grid of the resulting product. This is a mandatory parameter.
                Valid parameters are: GEOGRAPHIC_LAT_LON  and  REGULAR_GAUSSIAN_GRID.
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
        aggregate-map(.sh/.bat) -PgridName=<name> -PnumRows=<integer>
                         -PoutputLCCSClasses=<boolean> -PnumMajorityClasses=<integer>
                         -PoutputPFTClasses=<boolean> -PuserPFTConversionTable=<filePath>
                         -PadditionalUserMap=<filePath> -PoutputUserMapClasses=<boolean>
                         -PadditionalUserMapPFTConversionTable=<filePath>
                         -PoutputAccuracy=<boolean>
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
            -PadditionalUserMap=<filePath>
                A map containing additional classes which can be used to refine the conversion from
                LCCS to PFT classes.
            -PoutputUserMapClasses=<boolean>
                Whether or not to add the classes of the user map to the output.
                This option is only applicable if the additional user map is given too.
            -PadditionalUserMapPFTConversionTable=<filePath>
                The conversion table from LCCS to PFTs considering the additional user map.
                This option is only applicable if the additional user map is given too.
            -PoutputAccuracy=<boolean>
                Specifies the computation of the accuracy shall be performed and the result added to the
                output. This parameter can be omitted. The default is true.
            <sourceFilePath>
                Is the path to the source NetCDF-4 file.


        A real example might look like the following:
        aggregate-map(.sh/.bat) -PgridName=REGULAR_GAUSSIAN_GRID -PnumRows=320 -PoutputLCCSClasses=false -PnumMajorityClasses=3
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
        subset(.sh/.bat) -PpredefinedRegion=<regionName> -PtargetDir=<dirPath> <sourceFilePath>
                or
        subset(.sh/.bat) -Pnorth=<degree> -Peast=<degree> -Psouth=<degree> -Pwest=<degree> -PtargetDir=<dirPath> <sourceFilePath>

        -PpredefinedRegion=<regionName>
            Specifies one of the available predefined regions.
            Valid Values are: NORTH_AMERICA, CENTRAL_AMERICA, SOUTH_AMERICA, WESTERN_EUROPE_AND_MEDITERRANEAN,
                              ASIA, AFRICA, SOUTH_EAST_ASIA, AUSTRALIA_AND_NEW_ZEALAND, GREENLAND
        -Pnorth=<degree>
            Specifies north bound of the regional subset.
        -Peast=<degree>
            Specifies east bound of the regional subset. If the grid of the source product is REGULAR_GAUSSIAN_GRID
            coordinates the values must be between 0 and 360.
        -Psouth=<degree>
            Specifies south bound of the regional subset.
        -Pwest=<degree>
            Specifies west bound of the regional subset. If the grid of the source product is REGULAR_GAUSSIAN_GRID
            coordinates the values must be between 0 and 360.
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

        remap(.sh/.bat) -PuserPFTConversionTable=<filePath>
                        -PadditionalUserMap=<filePath>
                        -PadditionalUserMapPFTConversionTable=<filePath>
                        <sourceFilePath>

            -PuserPFTConversionTable=<filePath>
                Specifies the path to a user defined PFT conversion table. If not given the default
                CCI-LC conversion table will be used. For a description of the file format see further down.
            -PadditionalUserMap=<filePath>
                A map containing additional classes which can be used to refine the conversion from
                LCCS to PFT classes.
            -PadditionalUserMapPFTConversionTable=<filePath>
                The conversion table from LCCS to PFTs considering the additional user map.
                This option is only applicable if the additional user map is given too.
            <sourceFilePath>
                The source file to create a regional subset from.

        This tool splits up the information found in the band "lccs_class" into the PFTs given via look-up
        table files.
        The basis for the look-up table is the csv file provided as userPFTConversionTable.
        If additionally the additionalUserMapPFTConversionTable csv file is specified, it is used to improve the
        conversion to PFTs by using the also the additionalUserMap.

        For an example of the userPFTConversionTable please have a look at the section 'Aggregation Tool Usage'.
        The additionalUserMapPFTConversionTable has a similar structure.
        # Koeppen-Geiger Map
        LCCS_Class|Köppen_Geiger_Class|PFT_1|PFT_2|_PFT3|...|No_data
        10|11|||||||||14|86||||
        10|12|||||||||11|89||||
        10|13|||||||||10|90||||
        10|14|||||||||4|96||||
        10|21|||||||||6|94||||
        10|22|||||||||20|80||||
        10|26|||||||||13|87||||
        20|11|||||||||15|85||||
        20|12|||||||||21|79||||
        20|26|||||||||2|98||||

        The first column is again the LCCS class, the second the class in the additional user map.
        If one LCCS class, user class combination is missing the algorithm falls back to the userPFTConversionTable,
        if given or to the defaults of th CCI-LC conversion table

    300m-PFT Product Aggregation
    ~~~~~~~~~~~~~~~~~~~
        aggregate-pft(.sh/.bat) -PnumRows=<integer> -PtargetDir=<dirPath> <sourceFilePath>

            -PnumRows=<integer>
                        Specifies the number of rows for the specified grid.
                        Default ist 2160 rows. A grid with the default number of rows leads to a resolution of
                        ~9.8km/pixel in the target product.
            -PtargetDir=<path>
                        Specifies the output directory

        Example call:

            aggregate-pft.sh -PnumRows=2160 ESACCI-LC-L4-PFT-Map-300m-P1Y-2007-v2.0.8.nc

        Examples Aggregation Result:
           Input  : ESACCI-LC-L4-PFT-Map-300m-P1Y-2007-v2.0.8.nc
           Output : ESACCI-LC-L4-PFT-Map-300m-P1Y-aggregated-2007-v2.0.8.nc


Output File Naming Convention
"""""""""""""""""""""""""""""

    Split Points:
    ~~~~~~~~~~~~~
        Map Product:        ESACCI-LC-L4-LCCS-Map-{sRes}m-P{tRes}Y-{epoch}-v{versNr}.nc
                                                                  ^
                                                                  |--- Split Position

        Condition Product:  ESACCI-LC-L4-{condition}-Cond-{sRes}m-P{tRes}D-{startY}{MonthDay}-v{versNr}.nc
                                                                          ^
                                                                          |--- Split Position

    Example Map Result:
    ~~~~~~~~~~~~~~~~~~~~
        Aggregation:
            Input  :  ESACCI-LC-L4-LCCS-Map-300m-P5Y-2006-v2.nc

            Output :  ESACCI-LC-L4-LCCS-Map-300m-P5Y-aggregated-0.083333Deg-2006-v2.nc

        Subset:
            Input  :  ESACCI-LC-L4-LCCS-Map-300m-P5Y-aggregated-0.083333Deg-2006-v2.nc

            Output :  ESACCI-LC-L4-LCCS-Map-300m-P5Y-aggregated-0.083333Deg-EUROPE-2006-v2.nc
            Output :  ESACCI-LC-L4-LCCS-Map-300m-P5Y-aggregated-0.083333Deg-ASIA-2006-v2.nc
            Output :  ESACCI-LC-L4-LCCS-Map-300m-P5Y-aggregated-0.083333Deg-USER_REGION-2006-v2.nc

    Example Condition Result:
    ~~~~~~~~~~~~~~~~~~~~~~~~~~
        Subset:
            Input  :  ESACCI-LC-L4-NDVI-Cond-300m-P9Y7D-20010101-v2.nc

            Output :  ESACCI-LC-L4-NDVI-Cond-300m-P9Y7D-EUROPE-20010101-v2.nc
            Output :  ESACCI-LC-L4-NDVI-Cond-300m-P9Y7D-ASIA-20010101-v2.nc
            Output :  ESACCI-LC-L4-NDVI-Cond-300m-P9Y7D-USER_REGION-20010101-v2.nc
