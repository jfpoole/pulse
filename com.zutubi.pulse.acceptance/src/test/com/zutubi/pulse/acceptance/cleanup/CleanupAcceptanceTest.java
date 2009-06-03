package com.zutubi.pulse.acceptance.cleanup;

import com.zutubi.pulse.acceptance.SeleniumTestBase;
import com.zutubi.pulse.acceptance.AcceptanceTestUtils;
import com.zutubi.pulse.master.cleanup.config.CleanupWhat;
import com.zutubi.pulse.master.model.ProjectManager;
import com.zutubi.util.Condition;
import static com.zutubi.util.Constants.SECOND;

import java.util.Vector;
import java.io.File;

/**
 * The set of acceptance tests for the projects cleanup configuration.
 */
public class CleanupAcceptanceTest extends SeleniumTestBase
{
    private static final long CLEANUP_TIMEOUT = SECOND * 10;
    private static final long BUILD_TIMEOUT = SECOND * 9;

    private CleanupTestUtils utils;

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();

        loginAsAdmin();

        xmlRpcHelper.loginAsAdmin();

        utils = new CleanupTestUtils(xmlRpcHelper, selenium, urls);
    }

    @Override
    protected void tearDown() throws Exception
    {
        xmlRpcHelper.logout();

        logout();

        super.tearDown();
    }

    public void testCleanupWorkingDirectories() throws Exception
    {
        final String projectName = random;
        xmlRpcHelper.insertSimpleProject(projectName, ProjectManager.GLOBAL_PROJECT_NAME, false);

        utils.setRetainWorkingCopy(projectName, true);
        utils.addCleanupRule(projectName, "working_directory", CleanupWhat.WORKING_COPY_SNAPSHOT);
        utils.deleteCleanupRule(projectName, "default");

        xmlRpcHelper.runBuild(projectName, BUILD_TIMEOUT);
        waitForCleanupToRunAsynchronously();

        assertTrue(utils.hasBuildWorkingCopy(projectName, 1));
        assertTrue(utils.isBuildPresentViaUI(projectName, 1));

        xmlRpcHelper.runBuild(projectName, BUILD_TIMEOUT);
        waitForCleanupToRunAsynchronously(new InvertedCondition()
        {
            public boolean notSatisfied() throws Exception
            {
                return utils.hasBuildWorkingCopy(projectName, 1);
            }
        });

        assertTrue(utils.isBuildPresentViaUI(projectName, 2));
        assertTrue(utils.hasBuildWorkingCopy(projectName, 2));

        assertFalse(utils.hasBuildWorkingCopy(projectName, 1));

        // verify that the UI is as expected - the working copy tab exists and displays the
        // appropriate messages.

        assertFalse(utils.isWorkingCopyPresentViaUI(projectName, 1));
        assertTrue(utils.isWorkingCopyPresentViaUI(projectName, 2));
    }

    public void testCleanupBuildArtifacts() throws Exception
    {
        final String projectName = random;
        xmlRpcHelper.insertSimpleProject(projectName, ProjectManager.GLOBAL_PROJECT_NAME, false);

        utils.setRetainWorkingCopy(projectName, true);
        utils.addCleanupRule(projectName, "build_artifacts", CleanupWhat.BUILD_ARTIFACTS);
        utils.deleteCleanupRule(projectName, "default");

        xmlRpcHelper.runBuild(projectName, BUILD_TIMEOUT);
        waitForCleanupToRunAsynchronously();

        assertTrue(utils.hasBuildDirectory(projectName, 1));
        assertTrue(utils.hasBuildWorkingCopy(projectName, 1));

        xmlRpcHelper.runBuild(projectName, BUILD_TIMEOUT);
        waitForCleanupToRunAsynchronously(new InvertedCondition()
        {
            public boolean notSatisfied() throws Exception
            {
                return utils.hasBuildOutputDirectory(projectName, 1);
            }
        }, new InvertedCondition()
        {
            public boolean notSatisfied() throws Exception
            {
                return utils.hasBuildFeaturesDirectory(projectName, 1);
            }
        });

        assertTrue(utils.hasBuildDirectory(projectName, 2));

        assertTrue(utils.hasBuildDirectory(projectName, 1));
        assertTrue(utils.hasBuildWorkingCopy(projectName, 1));
        assertFalse(utils.hasBuildOutputDirectory(projectName, 1));
        assertFalse(utils.hasBuildFeaturesDirectory(projectName, 1));

        assertTrue(utils.isBuildPulseFilePresentViaUI(projectName, 1));
        assertTrue(utils.isBuildLogsPresentViaUI(projectName, 1));
        assertFalse(utils.isBuildArtifactsPresentViaUI(projectName, 1));

        // the remote api returns artifacts based on what is in the database.
        Vector artifactsInBuild = xmlRpcHelper.getArtifactsInBuild(projectName, 1);
        assertEquals(3, artifactsInBuild.size());
    }

    public void testCleanupAll() throws Exception
    {
        final String projectName = random;
        xmlRpcHelper.insertSimpleProject(projectName, ProjectManager.GLOBAL_PROJECT_NAME, false);

        utils.setRetainWorkingCopy(projectName, true);
        utils.addCleanupRule(projectName, "everything");
        utils.deleteCleanupRule(projectName, "default");

        xmlRpcHelper.runBuild(projectName, BUILD_TIMEOUT);
        waitForCleanupToRunAsynchronously();

        assertTrue(utils.hasBuild(projectName, 1));
        assertTrue(utils.isBuildPresentViaUI(projectName, 1));

        xmlRpcHelper.runBuild(projectName, BUILD_TIMEOUT);
        waitForCleanupToRunAsynchronously(new InvertedCondition()
        {
            public boolean notSatisfied() throws Exception
            {
                return utils.hasBuild(projectName, 1);
            }
        });

        assertTrue(utils.hasBuild(projectName, 2));
        assertTrue(utils.isBuildPresentViaUI(projectName, 2));

        assertFalse(utils.hasBuild(projectName, 1));
        assertFalse(utils.isBuildPresentViaUI(projectName, 1));

        // Unknown build '1' for project 'testCleanupAll-8KHqy3jjGJ'
        try
        {
            xmlRpcHelper.getArtifactsInBuild(projectName, 1);
        }
        catch(Exception e)
        {
            assertTrue(e.getMessage().contains("Unknown build '1' for project '"+projectName+"'"));
        }
    }

    private void waitForCleanupToRunAsynchronously(Condition... conditions)
    {
        if (conditions.length > 0)
        {
            int i = 0;
            for (Condition c : conditions)
            {
                i++;
                AcceptanceTestUtils.waitForCondition(c, CLEANUP_TIMEOUT, "condition("+i+") to be satisfied.");
            }
        }
        else
        {
            try
            {
                Thread.sleep(CLEANUP_TIMEOUT);
            }
            catch (InterruptedException e)
            {
                // noop.
            }
        }
    }

    private abstract class InvertedCondition implements Condition
    {
        public boolean satisfied()
        {
            try
            {
                return !notSatisfied();
            }
            catch(Exception e)
            {
                throw new RuntimeException(e);
            }
        }

        public abstract boolean notSatisfied() throws Exception;
    }
}
