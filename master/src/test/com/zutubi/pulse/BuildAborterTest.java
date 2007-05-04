package com.zutubi.pulse;

import com.mockobjects.dynamic.C;
import com.mockobjects.dynamic.Mock;
import com.zutubi.pulse.core.model.Feature;
import com.zutubi.pulse.model.*;
import com.zutubi.pulse.test.PulseTestCase;

import java.util.ArrayList;
import java.util.Arrays;

/**
 */
public class BuildAborterTest extends PulseTestCase
{
    private MockProjectManager projectManager;
    private MockBuildManager buildManager;
    private BuildAborterStartupTask aborter;

    protected void setUp() throws Exception
    {
        projectManager = new MockProjectManager();
        buildManager = new MockBuildManager();
        aborter = new BuildAborterStartupTask();
        aborter.setProjectManager(projectManager);
        aborter.setBuildManager(buildManager);

        Mock mockUserManager = new Mock(UserManager.class);
        mockUserManager.expectAndReturn("getAllUsers", C.ANY_ARGS, new ArrayList<User>());
        aborter.setUserManager((UserManager) mockUserManager.proxy());
    }

    public void testNoProjects()
    {
        aborter.execute();
    }

    public void testNoBuilds()
    {
        projectManager.create(new Project());
        aborter.execute();
    }

    public void testCompletedBuild()
    {
        Project project = new Project();
        BuildResult result = new BuildResult(new TriggerBuildReason("scm trigger"), project, 1, false);
        result.commence(10);
        result.complete();

        projectManager.create(project);
        buildManager.save(result);

        assertTrue(result.succeeded());
        aborter.execute();
        assertTrue(result.succeeded());
    }

    public void testIncompleteBuild()
    {
        Project project = new Project();
        BuildResult result = new BuildResult(new TriggerBuildReason("scm trigger"), project, 1, false);
        result.commence(10);

        projectManager.create(project);
        buildManager.save(result);

        assertTrue(result.commenced());
        assertFalse(result.completed());
        aborter.execute();
        assertTrue(result.errored());
        assertTrue(result.getFeatures(Feature.Level.ERROR).get(0).getSummary().contains("shut down"));
    }

    public void testCompletePersonalBuild()
    {
        Project project = new Project();
        User user = new User("u", "u");
        BuildResult result = new BuildResult(user, project, 1);
        result.commence(10);
        result.complete();

        buildManager.save(result);

        assertTrue(result.succeeded());

        Mock mockUserManager = new Mock(UserManager.class);
        mockUserManager.expectAndReturn("getAllUsers", C.ANY_ARGS, Arrays.asList(new User[] { user }));
        aborter.setUserManager((UserManager) mockUserManager.proxy());
        aborter.execute();

        assertTrue(result.succeeded());
    }

    public void testIncompletePersonalBuild()
    {
        Project project = new Project();
        User user = new User("u", "u");
        BuildResult result = new BuildResult(user, project, 1);
        result.commence(10);

        buildManager.save(result);

        assertTrue(result.commenced());
        assertFalse(result.completed());

        Mock mockUserManager = new Mock(UserManager.class);
        mockUserManager.expectAndReturn("getAllUsers", C.ANY_ARGS, Arrays.asList(new User[] { user }));
        aborter.setUserManager((UserManager) mockUserManager.proxy());
        aborter.execute();

        assertTrue(result.errored());
        assertTrue(result.getFeatures(Feature.Level.ERROR).get(0).getSummary().contains("shut down"));
    }
}
