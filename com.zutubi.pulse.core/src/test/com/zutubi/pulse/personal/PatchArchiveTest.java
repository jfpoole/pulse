package com.zutubi.pulse.personal;

import com.zutubi.pulse.core.PulseException;
import com.zutubi.pulse.core.util.ZipUtils;
import com.zutubi.pulse.core.util.FileSystemUtils;
import com.zutubi.pulse.core.scm.FileStatus;
import com.zutubi.pulse.core.scm.WorkingCopyStatus;
import com.zutubi.pulse.core.test.PulseTestCase;

import java.io.File;
import java.io.IOException;

/**
 */
public class PatchArchiveTest extends PulseTestCase
{
    private static final String TEST_FILENAME = "test.txt";

    private File tempDir;
    private File baseDir;
    private File extractDir;
    private File archiveFile;
    private File testFile;

    protected void setUp() throws Exception
    {
        super.setUp();
        tempDir = FileSystemUtils.createTempDir(PatchArchiveTest.class.getName(), "");
        baseDir = new File(tempDir, "base");
        baseDir.mkdir();
        extractDir = new File(tempDir, "extract");
        extractDir.mkdir();
        archiveFile = new File(tempDir, "patch.zip");
        testFile = new File(baseDir, TEST_FILENAME);
        FileSystemUtils.createFile(testFile, "test file");
    }


    protected void tearDown() throws Exception
    {
        FileSystemUtils.rmdir(tempDir);
        super.tearDown();
    }

    public void testPropertySerialisation() throws PulseException, IOException
    {
        WorkingCopyStatus wcs = new WorkingCopyStatus(baseDir);
        FileStatus fs = new FileStatus(TEST_FILENAME, FileStatus.State.UNCHANGED, false);
        fs.setProperty(FileStatus.PROPERTY_EOL_STYLE, FileStatus.EOLStyle.CARRIAGE_RETURN.toString());
        wcs.add(fs);
        
        PatchArchive archive = new PatchArchive(wcs, archiveFile, null);
        assertTrue(archiveFile.exists());
        ZipUtils.extractZip(archiveFile, extractDir);

        archive = new PatchArchive(archiveFile);
        fs = archive.getStatus().getFileStatus(TEST_FILENAME);
        assertEquals(FileStatus.EOLStyle.CARRIAGE_RETURN.toString(), fs.getProperty(FileStatus.PROPERTY_EOL_STYLE));
    }
}
