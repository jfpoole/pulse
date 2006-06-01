package com.zutubi.pulse.util;

import com.zutubi.pulse.test.PulseTestCase;

import java.io.File;
import java.io.IOException;

/**
 */
public class RemoveDirectoryTest extends PulseTestCase
{
    public void testRemoveNonExistant()
    {
        assertTrue(FileSystemUtils.removeDirectory(new File("/this/directory/does/not/exist")));
    }

    public void testRemoveFile() throws IOException
    {
        File tmpDir = createTmpDir();
        File aFile = new File(tmpDir, "aFile");

        try
        {
            FileSystemUtils.createFile(aFile, "some data");
            assertFalse(FileSystemUtils.removeDirectory(aFile));
        }
        finally
        {
            aFile.delete();
            tmpDir.delete();
        }
    }

    public void testRemoveEmpty() throws IOException
    {
        File tmpDir = createTmpDir();
        assertTrue(FileSystemUtils.removeDirectory(tmpDir));
        assertFalse(tmpDir.exists());
    }

    public void testRemoveContents() throws IOException
    {
        File tmpDir = createTmpDir();
        File aFile = new File(tmpDir, "aFile");
        File anotherFile = new File(tmpDir, "anotherFile");

        FileSystemUtils.createFile(aFile, "content");
        FileSystemUtils.createFile(anotherFile, "content");

        assertTrue(FileSystemUtils.removeDirectory(tmpDir));
        assertFalse(tmpDir.exists());
        assertFalse(aFile.exists());
        assertFalse(anotherFile.exists());
    }

    public void testRemoveNested() throws IOException
    {
        File tmpDir = createTmpDir();

        File aFile = new File(tmpDir, "aFile");
        FileSystemUtils.createFile(aFile, "content");

        File nestedDir = new File(tmpDir, "nested");
        assertTrue(nestedDir.mkdirs());

        File nestedFile = new File(nestedDir, "aFile");
        FileSystemUtils.createFile(nestedFile, "data");

        assertTrue(FileSystemUtils.removeDirectory(tmpDir));
        assertFalse(tmpDir.exists());
        assertFalse(nestedDir.exists());
    }

    public void testExternalSymlink() throws IOException, InterruptedException
    {
        File tmpDir = createTmpDir();
        File symlink = new File(tmpDir, "symlink");
        File linkDestination = createTmpDir();

        if (FileSystemUtils.createSymlink(symlink, linkDestination))
        {
            assertTrue(FileSystemUtils.removeDirectory(tmpDir));
            assertTrue(linkDestination.isDirectory());
        }
        else
        {
            FileSystemUtils.removeDirectory(tmpDir);
        }

        assertTrue(FileSystemUtils.removeDirectory(linkDestination));
    }

    private File createTmpDir() throws IOException
    {
        return FileSystemUtils.createTempDirectory(RemoveDirectoryTest.class.getName(), "");
    }
}
