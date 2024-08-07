/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.cci.lc.io;

import org.esa.snap.core.dataio.DecodeQualification;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.util.io.SnapFileFilter;
import org.esa.snap.core.util.io.FileUtils;

import java.io.File;
import java.util.Locale;

/**
 * Plugin class for the {@link LcMapTiffReader} reader.
 *
 * @author Olaf Danne
 */
public class LcMapTiffReaderPlugin implements ProductReaderPlugIn {

    private static final Class[] READER_INPUT_TYPES = new Class[]{String.class, File.class};
    private static final String FORMAT_NAME_TIFF = "LC_MAP_TIFF";
    private static final String[] FORMAT_NAMES = new String[]{FORMAT_NAME_TIFF};
    private static final String[] DEFAULT_FILE_EXTENSIONS = new String[]{".tif", ".tiff", ".TIF", ".TIFF"};
    private static final String READER_DESCRIPTION = "Land Cover CCI map tiff with flag tiffs in same dir";
    private static final SnapFileFilter FILE_FILTER = new TiffFileFilter();

    @Override
    public DecodeQualification getDecodeQualification(Object input) {
        String filename;
        if (input instanceof String) {
            filename = ((String) input).substring(((String) input).lastIndexOf(File.separatorChar) + 1);
        } else if (input instanceof File) {
            filename = ((File) input).getPath().substring(((File) input).getPath().lastIndexOf(File.separatorChar) + 1);
        } else {
            return DecodeQualification.UNABLE;
        }
        if (filename.matches(LcMapTiffReader.LC_MAP_FILENAME_PATTERN) || filename.matches(LcMapTiffReader.LC_ALTERNATIVE_FILENAME_PATTERN)) {
            return DecodeQualification.INTENDED;
        } else {
            return DecodeQualification.UNABLE;
        }
    }


    @Override
    public Class[] getInputTypes() {
        return READER_INPUT_TYPES;
    }

    @Override
    public ProductReader createReaderInstance() {
        return new LcMapTiffReader(this);
    }

    @Override
    public String[] getFormatNames() {
        return FORMAT_NAMES;
    }

    @Override
    public String[] getDefaultFileExtensions() {
        return DEFAULT_FILE_EXTENSIONS;
    }

    @Override
    public String getDescription(Locale locale) {
        return READER_DESCRIPTION;
    }

    @Override
    public SnapFileFilter getProductFileFilter() {
        return FILE_FILTER;
    }

    static boolean isTifFile(File file) {
        if (!(file == null || FileUtils.getExtension(file) == null)) {
            if ((FileUtils.getExtension(file).equalsIgnoreCase(".tif") || FileUtils.getExtension(file).equalsIgnoreCase(".tiff"))) {
                return true;
            }
        }
        return false;
    }

    private static class TiffFileFilter extends SnapFileFilter {

        public TiffFileFilter() {
            super();
            setFormatName(FORMAT_NAMES[0]);
            setDescription(READER_DESCRIPTION);
        }

        @Override
        public boolean accept(final File file) {
            return file.isDirectory() || isTifFile(file);
        }

        @Override
        public FileSelectionMode getFileSelectionMode() {
            return FileSelectionMode.FILES_ONLY;
        }
    }
}