package com.zutubi.pulse.util;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 */
public class SystemUtils
{
    public static String osName()
    {
        return System.getProperty("os.name");
    }

    public static boolean isLinux()
    {
        return osName().equals("Linux");
    }

    public static boolean isWindows()
    {
        return osName().toLowerCase().contains("win");
    }

    public static File findInPath(String name)
    {
        return findInPath(name, null);
    }

    /**
     * Attempts to find an executable with the given name in the given
     * extra paths or directories in the system PATH.  For most systems,
     * this equates to finding a file of the given name in one of the
     * extra paths or a directory in the PATH.  On windows, files are
     * expected to have one of the extensions in PATHEXT.
     *
     * @param name the name of the executable to look for
     * @param extraPaths a set of extra paths to check, in order, BEFORE
     *                   checking the system PATH
     * @return the file in the path, or null if not found
     */
    public static File findInPath(String name, Collection<String> extraPaths)
    {
        List<String> allPaths = new LinkedList<String>();
        if(extraPaths != null)
        {
            allPaths.addAll(extraPaths);
        }

        String path = System.getenv("PATH");
        if (path != null)
        {
            String[] paths = path.split(File.pathSeparator);
            allPaths.addAll(Arrays.asList(paths));
        }

        if (isWindows())
        {
            return findInWindowsPaths(allPaths, name);
        }
        else
        {
            return findInPaths(allPaths, name);
        }
    }

    private static File findInPaths(List<String> paths, String name)
    {
        for (String dir : paths)
        {
            File test = new File(dir, name);
            if (test.isFile())
            {
                return test;
            }
        }

        return null;
    }

    private static File findInWindowsPaths(List<String> paths, String name)
    {
        // Force uppercase for name and extensions to do case insensitive
        // comparisons
        name = name.toUpperCase();

        // Use PATHEXT for executable extensions where is is defined,
        // otherwise use a sensible default list.
        String[] extensions;
        String pathext = System.getenv("PATHEXT");
        if(pathext == null)
        {
            extensions = new String[] { ".COM", ".EXE", ".BAT", ".CMD", ".VBS", ".VBE", ".JS", ".JSE", ".WSF", ".WSH" };
        }
        else
        {
            extensions = pathext.split(";");
            for(int i = 0; i < extensions.length; i++)
            {
                extensions[i] = extensions[i].toUpperCase();
            }
        }

        for (String p: paths)
        {
            File dir = new File(p);
            if(dir.isDirectory())
            {
                String[] list = dir.list();
                for(String filename: list)
                {
                    File candidate = new File(dir, filename);
                    if(candidate.isFile() && filenameMatches(name, filename, extensions))
                    {
                        return candidate;
                    }
                }
            }
        }

        return null;
    }

    private static boolean filenameMatches(String name, String filename, String[] extensions)
    {
        filename = filename.toUpperCase();

        for(String extension: extensions)
        {
            if(filename.equals(name + extension))
            {
                return true;
            }
        }

        return false;
    }

    public static String getLineSeparator()
    {
        if(isWindows())
        {
            return "\r\n";
        }
        else
        {
            return "\n";
        }
    }
}
