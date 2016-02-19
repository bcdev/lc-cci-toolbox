package org.esa.cci.lc.wps.operations;

import org.junit.*;

import java.io.File;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * @author hans
 */
public class LcExecuteOperationTest {

    @Test
    public void testGetFiles() throws Exception {

        Path dir = Paths.get("C:\\Personal\\CabLab\\EO data");
        List<File> files = new ArrayList<>();
        PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:*ESACCI-LC-L4-LCCS-Map-300m-P5Y-2005-v1.3.nc");
        DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "ESACCI-LC-L4-LCCS-Map-300m-P5Y-2005-v1.3.nc");
        for (Path entry : stream) {
            if (matcher.matches(entry)) {
                files.add(entry.toFile());
            }
        }
        for (File file : files) {
            System.out.println("file.getAbsolutePath() = " + file.getAbsolutePath());
        }
    }
}