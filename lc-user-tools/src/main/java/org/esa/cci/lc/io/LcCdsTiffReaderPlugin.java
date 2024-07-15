package org.esa.cci.lc.io;

import org.esa.snap.core.dataio.DecodeQualification;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.util.io.SnapFileFilter;

import java.io.File;
import java.util.Locale;

public class LcCdsTiffReaderPlugin implements ProductReaderPlugIn {
    private static final String[] DEFAULT_FILE_EXTENSIONS = new String[]{".tif", ".tiff", ".TIF", ".TIFF"};
    private static final String FORMAT_NAME_TIFF = "LC_CDS_TIFF";
    private static final Class[] READER_INPUT_TYPES = new Class[]{String.class, File.class};
    private static final String[] FORMAT_NAMES = new String[]{FORMAT_NAME_TIFF};
    private static final String READER_DESCRIPTION = "Land Cover CCI map tiff with flag tiffs in same dir";
    private static final SnapFileFilter FILE_FILTER = new LcCdsTiffReaderPlugin.TiffFileFilter();


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
        if (filename.matches(LcCdsTiffReader.LC_CONDITION_FILENAME_PATTERN) ||  filename.matches(LcCdsTiffReader.LC_ALTERNATIVE_CONDITION_FILENAME_PATTERN)) {
            return DecodeQualification.INTENDED;
        } else {
            return DecodeQualification.UNABLE;
        }
    }

    @Override
    public String[] getFormatNames() {
        return FORMAT_NAMES;
    }

    @Override
    public Class[] getInputTypes() {
        return READER_INPUT_TYPES;
    }

    @Override
    public ProductReader createReaderInstance() {
        return new LcCdsTiffReader(this);
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


    private static class TiffFileFilter extends SnapFileFilter {
        public TiffFileFilter() {
            super();
            setFormatName(FORMAT_NAMES[0]);
            setDescription(READER_DESCRIPTION);
        }
    }
}
