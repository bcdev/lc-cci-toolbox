#!/usr/bin/env bash

###################################################################
####################### README ####################################
# This scripts fixes the LC-CCI Map product.
# * It converts the variable lccs_class from byte to ubyte
# * Corrects the flag_values attribute of lccs_class
# * Removes the _Unsigned and _FillValue attributes from lccs_class
# * It sets a better chunk size for all variables
# * and some other attributes are updated.
###################################################################


set dataDir=/mnt/g/EOData/related/LC-CCI
set oldVersion=2.0.7
set newVersion=2.0.8
# possible values for chunkSize
#tw = [225, 270, 405, 450, 675, 810, 1350, 2025]
#th = [225, 405, 675, 2025]
set chunkSize=2025

################ Converting to UBYTE #######################
# -h switches off the writing of the history attribute
# -O means overwrite output without asking
# -s 'algebra' specifies the operations
# -4 means NetCDF4 output
# -L Lempel-Ziv deflation (lvl=0..9)

# create temp ubyte variable
# inplace changing doesn't work
###ncap2 -h -O -4 -L 6 -s 'lccs_class=lccs_class.ubyte()' %dataDir/ESACCI-LC-L4-LCCS-Map-300m-P1Y-2015-v2.0.7.nc %dataDir/out1.nc
ncap2 -h -O -4 -L 6 -s 'temp_lccs_class=lccs_class.ubyte()' %dataDir/ESACCI-LC-L4-LCCS-Map-300m-P1Y-2015-v%oldVersion.nc %dataDir/out1.nc

# remove old lccs_class variable and change chunking
# -x process all vars except thos specified by -v (inverts meaning of -v)
# -a disables alphabetical order
# specifying a certain order of variables seems not to be possible
ncks -h -O -4 -L 6 -a -x -v lccs_class --cnk_dmn lat,%chunkSize --cnk_dmn lon,%chunkSize %dataDir/out1.nc %dataDir/out2.nc

# rename temp variable to lccs_class
ncrename -h -v temp_lccs_class,lccs_class %dataDir/out2.nc
#change global:TileSize according chunkSize
# -a atttribute description att_nm, var_nm, mode, att_type, att_val
ncatted -h -O -a TileSize,global,o,c,'%chunkSize:%chunkSize' %dataDir/out2.nc

################ Removing _Unsigned and _FillValue #######################
ncatted -h -O -a _Unsigned,lccs_class,d,, -a _FillValue,lccs_class,d,, %dataDir/out2.nc

################ Change flag_values attribute #######################
ncatted -h -O -a flag_values,lccs_class,o,ub,'0,10,11,12,20,30,40,50,60,61,62,70,71,72,80,81,82,90,100,110,120,121,122,130,140,150,151,152,153,160,170,180,190,200,201,202,210,220' %dataDir/out2.nc
ncatted -h -O -a flag_meanings,lccs_class,o,c,'no_data cropland_rainfed cropland_rainfed_herbaceous_cover cropland_rainfed_tree_or_shrub_cover cropland_irrigated mosaic_cropland mosaic_natural_vegetation tree_broadleaved_evergreen_closed_to_open tree_broadleaved_deciduous_closed_to_open tree_broadleaved_deciduous_closed tree_broadleaved_deciduous_open tree_needleleaved_evergreen_closed_to_open tree_needleleaved_evergreen_closed tree_needleleaved_evergreen_open tree_needleleaved_deciduous_closed_to_open tree_needleleaved_deciduous_closed tree_needleleaved_deciduous_open tree_mixed mosaic_tree_and_shrub mosaic_herbaceous shrubland shrubland_evergreen shrubland_deciduous grassland lichens_and_mosses sparse_vegetation sparse_tree sparse_shrub sparse_herbaceous tree_cover_flooded_fresh_or_brakish_water tree_cover_flooded_saline_water shrub_or_herbaceous_cover_flooded urban bare_areas bare_areas_consolidated bare_areas_unconsolidated water snow_and_ice' %dataDir/out2.nc

################ Remove _FillValue attribute #######################
# from processed_flag, current_pixel_state, observation_count and change_count
ncatted -h -O -a _FillValue,processed_flag,d,, -a _FillValue,current_pixel_state,d,, -a _FillValue,observation_count,d,, -a _FillValue,change_count,d,, %dataDir/out2.nc

################ Change global:product_version attribute #######################
ncatted -h -O -a product_version,global,o,c,'%newVersion' %dataDir/out2.nc

################ remove global:history attribute and other attributes added by nco#######################
ncatted -h -O -a history,global,d,, %dataDir/out2.nc

################ copy & rename file #######################
cp %dataDir/LC-CCI/out2.nc %dataDir/ESACCI-LC-L4-LCCS-Map-300m-P1Y-2015-v&newVersion.nc
