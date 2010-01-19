package com.zutubi.pulse.core.scm.svn;

import com.zutubi.pulse.core.PulseExecutionContext;
import com.zutubi.pulse.core.scm.api.*;
import com.zutubi.pulse.core.test.TestUtils;
import com.zutubi.pulse.core.test.api.PulseTestCase;
import com.zutubi.pulse.core.util.process.ProcessControl;
import com.zutubi.util.FileSystemUtils;
import com.zutubi.util.io.IOUtils;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.wc.SVNClientManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import static java.util.Arrays.asList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

public class SubversionExternalsTest extends PulseTestCase
{
    private SubversionClient client;
    private File tempDir;
    private File checkoutDir;
    private Process svnProcess;

    //    jsankey@tiberius-v ~/ext/all
    //    $ svn -v log
    //    ------------------------------------------------------------------------
    //    r8 | (no author) | 2006-11-18 17:07:15 +1100 (Sat, 18 Nov 2006) | 1 line
    //    Changed paths:
    //       M /bundle/trunk/file2
    //       M /ext1/trunk/file2
    //       M /ext2/trunk/file2
    //       M /meta/trunk/file2
    //    
    //    Edit in all
    //    ------------------------------------------------------------------------
    //    r7 | (no author) | 2006-11-18 17:06:20 +1100 (Sat, 18 Nov 2006) | 1 line
    //    Changed paths:
    //       M /meta/trunk/file1
    //
    //    Edit in meta
    //    ------------------------------------------------------------------------
    //    r6 | (no author) | 2006-11-18 17:06:06 +1100 (Sat, 18 Nov 2006) | 1 line
    //    Changed paths:
    //       M /ext1/trunk/file1
    //
    //    Edit in ext1
    //    ------------------------------------------------------------------------
    //    r5 | (no author) | 2006-11-18 17:05:46 +1100 (Sat, 18 Nov 2006) | 1 line
    //    Changed paths:
    //       M /bundle/trunk/file1
    //
    //    Edit in bundle
    //    ------------------------------------------------------------------------
    //    r4 | (no author) | 2006-11-18 17:04:21 +1100 (Sat, 18 Nov 2006) | 1 line
    //    Changed paths:
    //       M /ext2/trunk
    //
    //    Fixed meta external on ext2.
    //    ------------------------------------------------------------------------
    //    r3 | (no author) | 2006-11-18 17:03:24 +1100 (Sat, 18 Nov 2006) | 1 line
    //    Changed paths:
    //       M /ext2/trunk
    //
    //    Added meta external to ext2.
    //    ------------------------------------------------------------------------
    //    r2 | (no author) | 2006-11-18 17:02:19 +1100 (Sat, 18 Nov 2006) | 1 line
    //    Changed paths:
    //       M /bundle/trunk
    //
    //    Added externals to bundle.
    //    ------------------------------------------------------------------------
    //    r1 | (no author) | 2006-11-18 16:57:35 +1100 (Sat, 18 Nov 2006) | 1 line
    //    Changed paths:
    //       A /bundle
    //       A /bundle/trunk
    //       A /bundle/trunk/file1
    //       A /bundle/trunk/file2
    //       A /ext1
    //       A /ext1/trunk
    //       A /ext1/trunk/file1
    //       A /ext1/trunk/file2
    //       A /ext2
    //       A /ext2/trunk
    //       A /ext2/trunk/file1
    //       A /ext2/trunk/file2
    //       A /meta
    //       A /meta/trunk
    //       A /meta/trunk/file1
    //       A /meta/trunk/file2
    //
    //    Initil import
    //    ------------------------------------------------------------------------

    protected void setUp() throws Exception
    {
        super.setUp();
        tempDir = FileSystemUtils.createTempDir(getName(), "");
        tempDir = tempDir.getCanonicalFile();

        checkoutDir = new File(tempDir, "checkout");
        assertTrue(checkoutDir.mkdir());

        // Create empty repo
        File repoDir = new File(tempDir, "repo");
        assertTrue(repoDir.mkdir());
        svnProcess = Runtime.getRuntime().exec(new String[] { "svnadmin", "create", repoDir.getAbsolutePath() });
        svnProcess.waitFor();

        // Allow anonymous writes
        File conf = new File(repoDir, FileSystemUtils.composeFilename("conf", "svnserve.conf"));
        FileSystemUtils.createFile(conf, "[general]\nanon-access = write\nauth-access = write\n");

        // Restore from dump
        String tag = getRepoTag();
        unzipInput(tag, tempDir);

        File dump = new File(tempDir, "SubversionExternalsTest." + tag);
        svnProcess = Runtime.getRuntime().exec(new String[] { "svnadmin", "load", "-q", repoDir.getAbsolutePath() });
        FileInputStream is = new FileInputStream(dump);
        IOUtils.joinStreams(is, svnProcess.getOutputStream());
        svnProcess.getOutputStream().close();
        is.close();
        svnProcess.waitFor();

        // Start svn server
        svnProcess = Runtime.getRuntime().exec(new String[] { "svnserve", "--foreground", "-dr", "."}, null, repoDir);
        TestUtils.waitForServer(3690);

        client = new SubversionClient(getRepoUrl(), false);
    }

    private String getRepoTag()
    {
        return isNested() ? "nestedrepo" : "repo";
    }

    private String getRepoUrl()
    {
        return isNested() ? "svn://localhost/Project1" : "svn://localhost/bundle/trunk";
    }

    private boolean isNested()
    {
        return getName().contains("Nested");
    }

    protected void tearDown() throws Exception
    {
        IOUtils.close(client);
        ProcessControl.destroyProcess(svnProcess);
        svnProcess.waitFor();
        Thread.sleep(1000);
        
        removeDirectory(tempDir);
        super.tearDown();
    }

    public void testGetExternals() throws Exception
    {
        client.addExternalPath(".");
        List<SubversionClient.ExternalDefinition> externals = client.getExternals(createRevision(8));
        assertEquals(2, externals.size());
        assertExternal(externals.get(0), "pull1", "svn://localhost/ext1/trunk");
        assertExternal(externals.get(1), "pull2", "svn://localhost/ext2/trunk");
    }

    public void testGetExternalsNoPath() throws Exception
    {
        List<SubversionClient.ExternalDefinition> externals = client.getExternals(createRevision(8));
        assertEquals(0, externals.size());
    }

    public void testGetChangesOnExternal() throws Exception
    {
        client.addExternalPath(".");
        List<Changelist> changes = client.getChanges(null, createRevision(5), createRevision(6));
        assertEquals(1, changes.size());
        assertChange(changes.get(0), "6", "/ext1/trunk/file1");
    }

    public void testGetChangesOnExternalAndBundle() throws Exception
    {
        client.addExternalPath(".");
        List<Changelist> changes = client.getChanges(null, createRevision(4), createRevision(6));
        assertEquals(2, changes.size());
        assertChange(changes.get(0), "5", "/bundle/trunk/file1");
        assertChange(changes.get(1), "6", "/ext1/trunk/file1");
    }

    public void testGetChangesOnMetaExternals() throws Exception
    {
        client.addExternalPath(".");
        List<Changelist> changes = client.getChanges(null, createRevision(6), createRevision(7));
        assertEquals(0, changes.size());
    }

    public void testGetChangesOnAll() throws Exception
    {
        client.addExternalPath(".");
        List<Changelist> changes = client.getChanges(null, createRevision(7), createRevision(8));
        assertEquals(1, changes.size());
        assertChange(changes.get(0), "8", "/bundle/trunk/file2", "/ext1/trunk/file2", "/ext2/trunk/file2", "/meta/trunk/file2");
    }

    public void testCheckoutRevision() throws Exception
    {
        doCheckout(5);

        assertFile("file1", "edited bundle file1\n");
        assertFile("pull1/file1", "");
        assertFile("pull1/file2", "");
        assertFile("pull2/file2", "");
    }

    public void testCheckoutLastRevision() throws Exception
    {
        doCheckout(8);

        assertFile("file2", "edit in all\n");
        assertFile("pull1/file2", "edit in all\n");
        assertFile("pull2/file2", "edit in all\n");
    }

    public void testUpdate() throws Exception
    {
        doCheckout(2);
        PulseExecutionContext context = new PulseExecutionContext();
        context.setWorkingDir(checkoutDir);
        client.update(context, new Revision("5"), null);

        assertFile("file1", "edited bundle file1\n");
        assertFile("pull1/file1", "");
        assertFile("pull1/file2", "");
        assertFile("pull2/file2", "");
    }

    public void testGetLatestRevisionLatestInExternal() throws ScmException, IOException, SVNException
    {
        doCheckout(8);

        File f1 = new File(checkoutDir, "pull1/file1");
        FileSystemUtils.createFile(f1, "edit in external");
        SVNClientManager clientManager = SVNClientManager.newInstance();
        clientManager.getCommitClient().doCommit(new File[] { f1 }, true, "edit ext", null, null, false, false, SVNDepth.EMPTY);
        
        assertEquals("9", client.getLatestRevision(null).getRevisionString());
    }

    // ========================================================================
    // NESTED REPO - this also uses the new "relative" externals syntax
    // ========================================================================
    // $ svn log -v svn://localhost
    // ------------------------------------------------------------------------
    // r7 | jsankey | 2010-01-18 17:19:48 +0000 (Mon, 18 Jan 2010) | 1 line
    // Changed paths:
    //    M /Project5/Project5.txt
    //
    // Make a change to project 5.
    // ------------------------------------------------------------------------
    // r6 | jsankey | 2010-01-18 17:19:27 +0000 (Mon, 18 Jan 2010) | 1 line
    // Changed paths:
    //    M /Project2/Project2.txt
    //
    // ------------------------------------------------------------------------
    // r5 | jsankey | 2010-01-18 16:38:33 +0000 (Mon, 18 Jan 2010) | 1 line
    // Changed paths:
    //    M /Project1
    //    M /Project1/Directory
    //
    // Relative externals take their arguments in reverse order.
    // ------------------------------------------------------------------------
    // r4 | jsankey | 2010-01-18 16:38:24 +0000 (Mon, 18 Jan 2010) | 1 line
    // Changed paths:
    //    M /Project3
    //    M /Project3/Directory
    //
    // Relative externals take their arguments in reverse order.
    // ------------------------------------------------------------------------
    // r3 | jsankey | 2010-01-18 16:33:23 +0000 (Mon, 18 Jan 2010) | 1 line
    // Changed paths:
    //    M /Project1
    //    M /Project1/Directory
    //    M /Project3
    //    M /Project3/Directory
    //
    // Setup externals.
    // ------------------------------------------------------------------------
    // r2 | jsankey | 2010-01-18 16:30:49 +0000 (Mon, 18 Jan 2010) | 1 line
    // Changed paths:
    //    D /Project1/Directory/Project3
    //    D /Project1/Project2
    //    D /Project3/Directory/Project5
    //    D /Project3/Project4
    //
    // Delete directories that I put in the way.
    // ------------------------------------------------------------------------
    // r1 | jsankey | 2010-01-18 16:23:12 +0000 (Mon, 18 Jan 2010) | 1 line
    // Changed paths:
    //    A /Project1
    //    A /Project1/Directory
    //    A /Project1/Directory/Project3
    //    A /Project1/Project1.txt
    //    A /Project1/Project2
    //    A /Project2
    //    A /Project2/Project2.txt
    //    A /Project3
    //    A /Project3/Directory
    //    A /Project3/Directory/Project5
    //    A /Project3/Project3.txt
    //    A /Project3/Project4
    //    A /Project4
    //    A /Project4/Project4.txt
    //    A /Project5
    //    A /Project5/Project5.txt
    //
    // Import project structure.
    // ------------------------------------------------------------------------

    public void testNestedGetAllExternals() throws ScmException, SVNException
    {
        client.setMonitorAllExternals(true);
        assertEquals(asList(
                new SubversionClient.ExternalDefinition("Project2", "svn://localhost/Project2"),
                new SubversionClient.ExternalDefinition("Directory/Project3", "svn://localhost/Project3"),
                new SubversionClient.ExternalDefinition("Directory/Project3/Project4", "svn://localhost/Project4"),
                new SubversionClient.ExternalDefinition("Directory/Project3/Directory/Project5", "svn://localhost/Project5")
        ), client.getExternals(null));
    }

    public void testNestedNoExternalsMonitored() throws ScmException
    {
        assertEquals("5", client.getLatestRevision(null).getRevisionString());
    }

    public void testNestedMonitorRelativeExternal() throws ScmException
    {
        // Should pick up external to Project2, changed more recently than Project1.
        client.addExternalPath(".");
        assertEquals("6", client.getLatestRevision(null).getRevisionString());
    }

    public void testNestedMonitorAll() throws ScmException
    {
        // The latest project to change - Project5 - is only referenced
        // indirectly via Project3.
        client.setMonitorAllExternals(true);
        assertEquals("7", client.getLatestRevision(null).getRevisionString());
    }

    public void testNestedCheckoutMonitorAll() throws ScmException, IOException
    {
        client.setMonitorAllExternals(true);
        doCheckout(7);
        assertFile("Project1.txt", "I am Project 1.\n");
        assertFile("Project2/Project2.txt", "I am Project 2.  And I am changed.\n");
        assertFile("Directory/Project3/Directory/Project5/Project5.txt", "I am Project 5.  I am also changed.\n");
    }

    public void testNestedCheckoutPreviousRevisionMonitorAll() throws ScmException, IOException
    {
        client.setMonitorAllExternals(true);
        doCheckout(5);
        assertFile("Project1.txt", "I am Project 1.\n");
        assertFile("Project2/Project2.txt", "I am Project 2.\n");
        assertFile("Directory/Project3/Directory/Project5/Project5.txt", "I am Project 5.\n");
    }

    public void testNestedCheckoutThenUpdateMonitorAll() throws ScmException, IOException
    {
        client.setMonitorAllExternals(true);
        doCheckout(1);

        PulseExecutionContext context = new PulseExecutionContext();
        context.setWorkingDir(checkoutDir);
        client.update(context, new Revision(5), null);

        assertFile("Project1.txt", "I am Project 1.\n");
        assertFile("Project2/Project2.txt", "I am Project 2.\n");
        assertFile("Directory/Project3/Directory/Project5/Project5.txt", "I am Project 5.\n");

        client.update(context, new Revision(7), null);

        assertFile("Project1.txt", "I am Project 1.\n");
        assertFile("Project2/Project2.txt", "I am Project 2.  And I am changed.\n");
        assertFile("Directory/Project3/Directory/Project5/Project5.txt", "I am Project 5.  I am also changed.\n");
    }

    private void doCheckout(int rev) throws ScmException
    {
        PulseExecutionContext context = new PulseExecutionContext();
        context.setWorkingDir(checkoutDir);
        client.addExternalPath(".");
        client.checkout(context, new Revision(Integer.toString(rev)), new ScmFeedbackHandler()
        {
            public void status(String message)
            {
                System.out.println(message);
            }

            public void checkCancelled() throws ScmCancelledException
            {
            }
        });
    }

    private void assertChange(Changelist changelist, String revision, String... paths)
    {
        assertEquals(revision, changelist.getRevision().getRevisionString());
        List<FileChange> changes = new LinkedList<FileChange>(changelist.getChanges());
        Collections.sort(changes, new Comparator<FileChange>()
        {
            public int compare(FileChange o1, FileChange o2)
            {
                return o1.getPath().compareTo(o2.getPath());
            }
        });
        
        for(int i = 0; i < paths.length; i++)
        {
            FileChange change = changes.get(i);
            assertEquals(paths[i], change.getPath());
        }
    }

    private void assertExternal(SubversionClient.ExternalDefinition external, String path, String url)
    {
        assertEquals(path, external.path);
        assertEquals(url, external.url.toDecodedString());
    }

    private void assertFile(String path, String content) throws IOException
    {
        File f = new File(checkoutDir, path);
        assertEquals(content, IOUtils.fileToString(f));
    }

    public static void main(String[] argv) throws ScmException
    {
        SubversionClient server = null;
        try
        {
            server = new SubversionClient("http://svn.nuxeo.org/nuxeo/bundles/ECM-trunk", false);
            server.addExternalPath(".");
            List<Changelist> changelists = server.getChanges(null, createRevision(6600), createRevision(6603));
            for(Changelist list: changelists)
            {
                System.out.println(list.getRevision().getRevisionString() + ": " + list.getComment());
                for(FileChange change: list.getChanges())
                {
                    System.out.println("    " + change.toString());
                }
            }
        }
        finally
        {
            IOUtils.close(server);
        }
    }

    private static Revision createRevision(long rev)
    {
        return new Revision(Long.toString(rev));
    }
}
