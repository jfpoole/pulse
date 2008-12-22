package com.zutubi.pulse.upgrade;

import com.zutubi.pulse.Version;
import com.zutubi.pulse.bootstrap.ComponentContext;
import com.zutubi.pulse.bootstrap.Data;
import com.zutubi.pulse.bootstrap.MasterConfigurationManager;
import com.zutubi.pulse.test.PulseTestCase;
import com.zutubi.pulse.upgrade.tasks.MockUpgradeTask;
import com.zutubi.pulse.util.FileSystemUtils;
import org.springframework.context.ApplicationContext;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

/**
 * <class-comment/>
 */
public class DefaultUpgradeManagerTest extends PulseTestCase
{
    private DefaultUpgradeManager upgradeManager;
    private File tmpDir;
    private Data tmpData;

    private ApplicationContext context = null;

    public DefaultUpgradeManagerTest(String testName)
    {
        super(testName);
    }

    public void setUp() throws Exception
    {
        super.setUp();

        upgradeManager = new DefaultUpgradeManager();

        upgradeManager.addTask(new MockUpgradeTask(30));
        upgradeManager.addTask(new MockUpgradeTask(40));
        upgradeManager.addTask(new MockUpgradeTask(20));

        ComponentContext.addClassPathContextDefinitions("com/zutubi/pulse/bootstrap/testBootstrapContext.xml");
        context = ComponentContext.getContext();
        MasterConfigurationManager configurationManager = (MasterConfigurationManager) context.getBean("configurationManager");

        tmpDir = FileSystemUtils.createTempDir("DefaultUpgradeManagerTest", getName());
        tmpData = new Data(tmpDir);
        tmpData.init(configurationManager.getSystemPaths());
        tmpData.setBuildNumber(0);
    }

    public void tearDown() throws Exception
    {
        // add tear down code here.
        tmpData = null;
        upgradeManager = null;

        removeDirectory(tmpDir);

        context = null;
        
        ComponentContext.closeAll();

        super.tearDown();
    }

    public void testUpgradeTaskSelection()
    {
        assertTrue(upgradeManager.isUpgradeRequired(2, 35));

        List<UpgradeTask> preview = upgradeManager.determineRequiredUpgradeTasks(2, 35);
        assertNotNull(preview);
        assertEquals(2, preview.size());
        assertEquals(20, preview.get(0).getBuildNumber());
        assertEquals(30, preview.get(1).getBuildNumber());
    }

    public void testInvalidTargetBuildNumberSameAsUpgradeToLatest()
    {
        assertTrue(upgradeManager.isUpgradeRequired(25, Version.INVALID));

        List<UpgradeTask> preview = upgradeManager.determineRequiredUpgradeTasks(25, Version.INVALID);
        assertNotNull(preview);
        assertEquals(2, preview.size());
        assertEquals(30, preview.get(0).getBuildNumber());
        assertEquals(40, preview.get(1).getBuildNumber());
    }

    //@Required(tmpDir)
    public void testHaltOnFailureAbortsRemainingTasks()
    {
        List<UpgradeTask> tasks = new LinkedList<UpgradeTask>();
        tasks.add(new ErrorOnExecuteUpgradeTask(30));
        tasks.add(new MockUpgradeTask(20));
        tasks.add(new MockUpgradeTask(40));
        upgradeManager.setTasks(tasks);

        upgradeManager.prepareUpgrade(tmpData);
        upgradeManager.executeUpgrade();
        assertTrue(upgradeManager.getUpgradeMonitor().isStarted());

        UpgradeProgressMonitor monitor = upgradeManager.getUpgradeMonitor();
        assertTrue(monitor.isError());
        assertEquals(UpgradeTaskProgress.FAILED, monitor.getTaskProgress(tasks.get(0)).getStatus());
        assertEquals(UpgradeTaskProgress.COMPLETE, monitor.getTaskProgress(tasks.get(1)).getStatus());
        assertEquals(UpgradeTaskProgress.ABORTED, monitor.getTaskProgress(tasks.get(2)).getStatus());

        assertEquals(20, tmpData.getBuildNumber());
    }

    public void testHaltOnExceptionAbortsRemainingTasks()
    {
        List<UpgradeTask> tasks = new LinkedList<UpgradeTask>();
        tasks.add(new ExceptionOnExecuteUpgradeTask(30));
        tasks.add(new MockUpgradeTask(20));
        tasks.add(new MockUpgradeTask(40));
        upgradeManager.setTasks(tasks);

        upgradeManager.prepareUpgrade(tmpData);
        upgradeManager.executeUpgrade();
        assertTrue(upgradeManager.getUpgradeMonitor().isStarted());

        UpgradeProgressMonitor monitor = upgradeManager.getUpgradeMonitor();
        assertTrue(monitor.isError());
        assertEquals(UpgradeTaskProgress.FAILED, monitor.getTaskProgress(tasks.get(0)).getStatus());
        assertEquals(UpgradeTaskProgress.COMPLETE, monitor.getTaskProgress(tasks.get(1)).getStatus());
        assertEquals(UpgradeTaskProgress.ABORTED, monitor.getTaskProgress(tasks.get(2)).getStatus());

        assertEquals(20, tmpData.getBuildNumber());
    }

    public void testNoHaltOnFailureProcessesAllTasks()
    {
        List<UpgradeTask> tasks = new LinkedList<UpgradeTask>();
        tasks.add(new ErrorOnExecuteUpgradeTask(30, false));
        tasks.add(new MockUpgradeTask(20));
        tasks.add(new MockUpgradeTask(40));
        upgradeManager.setTasks(tasks);

        upgradeManager.prepareUpgrade(tmpData);
        upgradeManager.executeUpgrade();
        assertTrue(upgradeManager.getUpgradeMonitor().isStarted());

        UpgradeProgressMonitor monitor = upgradeManager.getUpgradeMonitor();
        assertTrue(monitor.isError());
        assertEquals(UpgradeTaskProgress.FAILED, monitor.getTaskProgress(tasks.get(0)).getStatus());
        assertEquals(UpgradeTaskProgress.COMPLETE, monitor.getTaskProgress(tasks.get(1)).getStatus());
        assertEquals(UpgradeTaskProgress.COMPLETE, monitor.getTaskProgress(tasks.get(2)).getStatus());

        assertEquals(40, tmpData.getBuildNumber());
    }

    public void testSuccessfulUpgradeOfVersionDetails()
    {
        List<UpgradeTask> tasks = new LinkedList<UpgradeTask>();
        tasks.add(new MockUpgradeTask(20));
        upgradeManager.setTasks(tasks);

        upgradeManager.prepareUpgrade(tmpData);
        upgradeManager.executeUpgrade();
        assertTrue(upgradeManager.getUpgradeMonitor().isStarted());

        Version targetVersion = tmpData.getVersion();
/*
        // all of these things are specified for accept.master :|
        assertEquals("@BUILD_NUMBER@", targetVersion.getBuildNumber());
        assertEquals("@VERSION@", targetVersion.getVersionNumber());
        assertEquals("@RELEASE_DATE@", targetVersion.getReleaseDate());
*/
    }

    public void testUpgradeMultipleExecuteUpgradeRequestsIgnored()
    {
        MockUpgradeTask upgradeTask = new MockUpgradeTask(20);
        List<UpgradeTask> tasks = new LinkedList<UpgradeTask>();
        tasks.add(upgradeTask);

        upgradeManager.setTasks(tasks);

        upgradeManager.prepareUpgrade(tmpData);
        assertEquals(0, upgradeTask.getExecutionCount());
        upgradeManager.executeUpgrade();
        assertTrue(upgradeManager.getUpgradeMonitor().isStarted());
        assertEquals(1, upgradeTask.getExecutionCount());
        upgradeManager.executeUpgrade();
        assertEquals(1, upgradeTask.getExecutionCount());
    }

    private class ErrorOnExecuteUpgradeTask extends MockUpgradeTask
    {
        public ErrorOnExecuteUpgradeTask(int version)
        {
            super(version, true);
        }

        public ErrorOnExecuteUpgradeTask(int version, boolean haltOnFailure)
        {
            super(version, haltOnFailure);
        }

        public void execute(UpgradeContext context) throws UpgradeException
        {
            super.execute(context);
            errors.add("ErrorOnExecute");
        }
    }

    private class ExceptionOnExecuteUpgradeTask extends MockUpgradeTask
    {
        public ExceptionOnExecuteUpgradeTask(int version)
        {
            super(version, true);
        }

        public void execute(UpgradeContext context) throws UpgradeException
        {
            super.execute(context);
            throw new UpgradeException("ExceptionOnExecute");
        }
    }
}