package com.zutubi.pulse.core.scm.git;

import com.zutubi.pulse.core.scm.RecordingScmFeedbackHandler;
import com.zutubi.pulse.core.scm.api.ScmException;
import com.zutubi.pulse.core.test.api.PulseTestCase;
import com.zutubi.pulse.core.util.ZipUtils;
import com.zutubi.util.FileSystemUtils;
import com.zutubi.util.io.IOUtils;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.ParseException;
import java.util.List;

public class NativeGitTest extends PulseTestCase
{
    private File tmp;
    private NativeGit git;
    private String repository;
    private File repositoryBase;

    protected void setUp() throws Exception
    {
        super.setUp();

        tmp = FileSystemUtils.createTempDir();

        git = new NativeGit();

        URL url = getClass().getResource("NativeGitTest.zip");
        ZipUtils.extractZip(new File(url.toURI()), new File(tmp, "repo"));

        repositoryBase = new File(tmp, "repo");

        repository = "file://" + repositoryBase.getCanonicalPath();
    }

    protected void tearDown() throws Exception
    {
        removeDirectory(tmp);

        super.tearDown();
    }

    public void testSystemPropertyPickedUp()
    {
        final String INVALID_COMMAND = "thereisnosuchcommand";

        String previousValue = System.getProperty(GitConstants.PROPERTY_GIT_COMMAND);
        System.setProperty(GitConstants.PROPERTY_GIT_COMMAND, INVALID_COMMAND);
        try
        {
            git.log();
            fail("Git should not run when a bad command is set");
        }
        catch (GitException e)
        {
            assertTrue("Message '" + e.getMessage() + "' does not contain the invalid command", e.getMessage().contains(INVALID_COMMAND));
        }
        finally
        {
            if (previousValue == null)
            {
                System.clearProperty(GitConstants.PROPERTY_GIT_COMMAND);
            }
            else
            {
                System.setProperty(GitConstants.PROPERTY_GIT_COMMAND, previousValue);
            }
        }
    }

    public void testClone() throws ScmException, IOException
    {
        git.setWorkingDirectory(tmp);
        git.clone(new RecordingScmFeedbackHandler(), repository, "base");

        File cloneBase = new File(tmp, "base");
        assertTrue(new File(cloneBase, ".git").isDirectory());

        // no content is checked out by default.
        assertFalse(new File(cloneBase, "README.txt").isFile());
        assertFalse(new File(cloneBase, "build.xml").isFile());
    }

    public void testCloneStatusMessages() throws ScmException
    {
        RecordingScmFeedbackHandler handler = new RecordingScmFeedbackHandler();
        git.setWorkingDirectory(tmp);
        git.clone(handler,  repository, "base");

        assertThat(handler.getStatusMessages().size(), greaterThan(1));
        assertThat(handler.getStatusMessages(), hasItem(startsWith("Initialized empty Git repository")));
    }

    public void testLog() throws ScmException
    {
        git.setWorkingDirectory(tmp);
        git.clone(null, repository, "base");
        git.setWorkingDirectory(new File(tmp, "base"));

        assertEquals(2, git.log().size());
    }

    public void testLogHead() throws ScmException, ParseException
    {
        git.setWorkingDirectory(tmp);
        git.clone(null, repository, "base");
        git.setWorkingDirectory(new File(tmp, "base"));

        List<GitLogEntry> entries = git.log("HEAD^", "HEAD");
        assertEquals(1, entries.size());
        GitLogEntry entry = entries.get(0);
        assertEquals("78be6b2f12399ea2332a5148440086913cb910fb", entry.getId());
    }

    public void testLogCount() throws ScmException, ParseException
    {
        git.setWorkingDirectory(tmp);
        git.clone(null, repository, "base");
        git.setWorkingDirectory(new File(tmp, "base"));

        List<GitLogEntry> entries = git.log(1);
        assertEquals(1, entries.size());
        GitLogEntry entry = entries.get(0);
        assertEquals("78be6b2f12399ea2332a5148440086913cb910fb", entry.getId());
    }

    public void testBranchOnOriginalRepository() throws ScmException
    {
        git.setWorkingDirectory(repositoryBase);
        List<GitBranchEntry> branches = git.branch();

        assertNotNull(branches);
        assertEquals(2, branches.size());
        assertEquals("branch", branches.get(0).getName());
        assertEquals("master", branches.get(1).getName());
    }

    public void testBranchOnCloneRepository() throws ScmException
    {
        git.setWorkingDirectory(tmp);
        git.clone(null, repository, "base");
        git.setWorkingDirectory(new File(tmp, "base"));

        List<GitBranchEntry> branches = git.branch();

        assertNotNull(branches);
        assertEquals(1, branches.size());
        assertEquals("master", branches.get(0).getName());
    }

    public void testCheckoutBranch() throws ScmException, IOException
    {
        git.setWorkingDirectory(tmp);
        git.clone(null, repository, "base");

        File cloneBase = new File(tmp, "base");
        git.setWorkingDirectory(cloneBase);
        git.checkout(null, "master");

        assertFalse(IOUtils.fileToString(new File(cloneBase, "README.txt")).contains("ON BRANCH"));

        git.checkout(null, "origin/branch", "local");

        assertTrue(IOUtils.fileToString(new File(cloneBase, "README.txt")).contains("ON BRANCH"));
    }

    public void testDiffFeedback() throws GitException
    {
        git.setWorkingDirectory(tmp);
        git.clone(null, repository, "base");

        git.setWorkingDirectory(new File(tmp, "base"));
        RecordingScmFeedbackHandler handler = new RecordingScmFeedbackHandler();
        git.diff(handler, null);
        List<String> messages = handler.getStatusMessages();
        assertEquals(2, messages.size());
        assertEquals("M\tsmiley.txt", messages.get(1));
    }

    public void testPull() throws ScmException, IOException
    {
        git.setWorkingDirectory(tmp);
        git.clone(null, repository, "base");

        git.setWorkingDirectory(new File(tmp, "base"));
        git.pull(null);
    }
}