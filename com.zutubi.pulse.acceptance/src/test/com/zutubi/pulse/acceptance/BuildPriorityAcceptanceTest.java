/* Copyright 2017 Zutubi Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.zutubi.pulse.acceptance;

import com.zutubi.pulse.acceptance.forms.admin.TriggerBuildForm;
import com.zutubi.pulse.acceptance.pages.browse.ProjectHomePage;
import com.zutubi.pulse.acceptance.utils.BuildRunner;
import com.zutubi.pulse.acceptance.utils.WaitProject;
import com.zutubi.pulse.master.tove.config.agent.AgentConfiguration;
import com.zutubi.pulse.master.tove.config.project.BuildStageConfiguration;
import com.zutubi.util.io.FileSystemUtils;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static com.zutubi.pulse.core.engine.api.ResultState.*;
import static com.zutubi.util.CollectionUtils.asPair;

/**
 * Set of acceptance tests that work on testing builds priorities.
 */
public class BuildPriorityAcceptanceTest extends AcceptanceTestBase
{
    private static final int WAIT_FOR_TIMEOUT = 20000;

    private BuildRunner buildRunner;

    private File tempDir;

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();

        tempDir = FileSystemUtils.createTempDir();

        buildRunner = new BuildRunner(rpcClient.RemoteApi);
        rpcClient.loginAsAdmin();
    }

    @Override
    protected void tearDown() throws Exception
    {
        rpcClient.cancelIncompleteBuilds();
        rpcClient.logout();

        removeDirectory(tempDir);

        super.tearDown();
    }

    // Test that by default, builds run in the order they were triggered.
    public void testDefaultPriorities() throws Exception
    {
        WaitProject projectA = createProject("A");
        WaitProject projectB = createProject("B");
        WaitProject projectC = createProject("C");

        insertProjects(projectA, projectB, projectC);

        buildRunner.triggerBuild(projectA);
        rpcClient.RemoteApi.waitForBuildInProgress(projectA.getName(), 1);
        
        buildRunner.triggerBuild(projectB);
        rpcClient.RemoteApi.waitForBuildInPending(projectB.getName(), 1);
        buildRunner.triggerBuild(projectC);

        assertBuildOrder(projectA, projectB, projectC);
    }

    public void testProjectPriorities() throws Exception
    {
        WaitProject projectA = createProject("A");
        WaitProject projectB = createProject("B");
        projectB.getOptions().setPriority(1);
        WaitProject projectC = createProject("C");
        projectC.getOptions().setPriority(5);

        insertProjects(projectA, projectB, projectC);

        buildRunner.triggerBuild(projectA);
        rpcClient.RemoteApi.waitForBuildInProgress(projectA.getName(), 1);

        buildRunner.triggerBuild(projectB);
        buildRunner.triggerBuild(projectC);

        assertBuildOrder(projectA, projectC, projectB);
    }

    public void testNegativePriorityIsLowerThanDefaultPriority() throws Exception
    {
        WaitProject projectA = createProject("A");
        WaitProject projectB = createProject("B");
        projectB.getOptions().setPriority(-1);
        WaitProject projectC = createProject("C");

        insertProjects(projectA, projectB, projectC);

        buildRunner.triggerBuild(projectA);
        rpcClient.RemoteApi.waitForBuildInProgress(projectA.getName(), 1);

        buildRunner.triggerBuild(projectB);
        buildRunner.triggerBuild(projectC);

        assertBuildOrder(projectA, projectC, projectB);
    }

    public void testStagePriorities() throws Exception
    {
        WaitProject project = projectConfigurations.createWaitAntProject(randomName(), tempDir, false);
        project.addStage("B").setPriority(2);
        project.addStage("C").setPriority(4);
        project.addStage("D").setPriority(-1);
        bindStagesToMaster(project);

        insertProjects(project);

        buildRunner.triggerBuild(project);
        rpcClient.RemoteApi.waitForBuildStageInProgress(project.getName(), "C", 1, WAIT_FOR_TIMEOUT);

        assertEquals(IN_PROGRESS, rpcClient.RemoteApi.getBuildStageStatus(project.getName(), "C", 1));
        assertEquals(PENDING, rpcClient.RemoteApi.getBuildStageStatus(project.getName(), "B", 1));
        assertEquals(PENDING, rpcClient.RemoteApi.getBuildStageStatus(project.getName(), project.getDefaultStage().getName(), 1));
        assertEquals(PENDING, rpcClient.RemoteApi.getBuildStageStatus(project.getName(), "D", 1));

        project.releaseStage("C");
        rpcClient.RemoteApi.waitForBuildStageInProgress(project.getName(), "B", 1, WAIT_FOR_TIMEOUT);

        assertEquals(IN_PROGRESS, rpcClient.RemoteApi.getBuildStageStatus(project.getName(), "B", 1));
        assertEquals(PENDING, rpcClient.RemoteApi.getBuildStageStatus(project.getName(), project.getDefaultStage().getName(), 1));
        assertEquals(PENDING, rpcClient.RemoteApi.getBuildStageStatus(project.getName(), "D", 1));

        project.releaseStage("B");

        rpcClient.RemoteApi.waitForBuildStageInProgress(project.getName(), project.getDefaultStage().getName(), 1, WAIT_FOR_TIMEOUT);
        assertEquals(IN_PROGRESS, rpcClient.RemoteApi.getBuildStageStatus(project.getName(), project.getDefaultStage().getName(), 1));
        assertEquals(PENDING, rpcClient.RemoteApi.getBuildStageStatus(project.getName(), "D", 1));

        project.releaseStage(project.getDefaultStage().getName());

        rpcClient.RemoteApi.waitForBuildStageInProgress(project.getName(), "D", 1, WAIT_FOR_TIMEOUT);
        assertEquals(IN_PROGRESS, rpcClient.RemoteApi.getBuildStageStatus(project.getName(), "D", 1));

        project.releaseStage("D");

        rpcClient.RemoteApi.waitForBuildToComplete(project.getName(), 1);
        rpcClient.RemoteApi.waitForProjectToBeIdle(project.getName());
        assertEquals(SUCCESS, rpcClient.RemoteApi.getBuildStatus(project.getName(), 1));
    }

    public void testPriorityViaRemoteApi() throws Exception
    {
        WaitProject projectA = createProject("A");
        WaitProject projectB = createProject("B");
        WaitProject projectC = createProject("C");

        insertProjects(projectA, projectB, projectC);

        buildRunner.triggerBuild(projectA);
        rpcClient.RemoteApi.waitForBuildInProgress(projectA.getName(), 1);

        buildRunner.triggerBuild(projectB, asPair("priority", (Object) 2));
        buildRunner.triggerBuild(projectC, asPair("priority", (Object) 3));

        assertBuildOrder(projectA, projectC, projectB);
    }

    public void testPriorityViaManualPrompt() throws Exception
    {
        WaitProject projectA = createProject("A");
        WaitProject projectB = createProject("B");
        WaitProject projectC = createProject("C");

        insertProjects(projectA, projectB, projectC);

        buildRunner.triggerBuild(projectA);
        rpcClient.RemoteApi.waitForBuildInProgress(projectA.getName(), 1);

        buildRunner.triggerBuild(projectB);

        getBrowser().loginAsAdmin();

        ProjectHomePage home = getBrowser().openAndWaitFor(ProjectHomePage.class, projectC.getName());
        home.triggerBuild();

        TriggerBuildForm form = getBrowser().createForm(TriggerBuildForm.class);
        form.waitFor();
        form.triggerFormElements(asPair("priority", "5"));

        assertBuildOrder(projectA, projectC, projectB);
    }

    public void testPriorityAcrossDependencies() throws Exception
    {
        WaitProject projectA = createProject("A");
        WaitProject projectB = createProject("B");
        WaitProject projectC = createProject("C");
        
        WaitProject projectD = createProject("D");
        WaitProject projectE = createProject("E");
        projectE.addDependency(projectD);

        insertProjects(projectA, projectB, projectC, projectD, projectE);

        buildRunner.triggerBuild(projectA);
        rpcClient.RemoteApi.waitForBuildInProgress(projectA.getName(), 1);

        buildRunner.triggerBuild(projectB);
        rpcClient.RemoteApi.waitForBuildInPending(projectB.getName(), 1);
        buildRunner.triggerBuild(projectC);
        rpcClient.RemoteApi.waitForBuildInPending(projectC.getName(), 1);
        buildRunner.triggerBuild(projectD, asPair("priority", (Object)10));
        rpcClient.RemoteApi.waitForBuildInPending(projectD.getName(), 1);

        projectA.releaseBuild();
        rpcClient.RemoteApi.waitForBuildToComplete(projectA.getName(), 1);

        rpcClient.RemoteApi.waitForBuildInProgress(projectD.getName(), 1);
        assertEquals(PENDING, rpcClient.RemoteApi.getBuildStatus(projectB.getName(), 1));
        assertEquals(PENDING, rpcClient.RemoteApi.getBuildStatus(projectC.getName(), 1));

        projectD.releaseBuild();
        rpcClient.RemoteApi.waitForBuildToComplete(projectD.getName(), 1);
        rpcClient.RemoteApi.waitForBuildInProgress(projectB.getName(), 1);
        assertEquals(PENDING, rpcClient.RemoteApi.getBuildStatus(projectC.getName(), 1));
        rpcClient.RemoteApi.waitForBuildInPending(projectE.getName(), 1);

        projectB.releaseBuild();
        rpcClient.RemoteApi.waitForBuildToComplete(projectB.getName(), 1);

        rpcClient.RemoteApi.waitForBuildInProgress(projectE.getName(), 1);
        assertEquals(PENDING, rpcClient.RemoteApi.getBuildStatus(projectC.getName(), 1));
        projectE.releaseBuild();
        rpcClient.RemoteApi.waitForBuildToComplete(projectE.getName(), 1);

        rpcClient.RemoteApi.waitForBuildInProgress(projectC.getName(), 1);
        projectC.releaseBuild();
        rpcClient.RemoteApi.waitForBuildToComplete(projectC.getName(), 1);
    }

    private void assertBuildOrder(WaitProject... projects) throws Exception
    {
        List<WaitProject> remaining = new LinkedList<WaitProject>();
        remaining.addAll(Arrays.asList(projects));

        if (remaining.size() > 0)
        {
            do
            {
                WaitProject current = remaining.remove(0);
                rpcClient.RemoteApi.waitForBuildInProgress(current.getName(), 1);
                for (WaitProject project : remaining)
                {
                    rpcClient.RemoteApi.waitForBuildInPending(project.getName(), 1);
                }
                current.releaseBuild();
                rpcClient.RemoteApi.waitForBuildToComplete(current.getName(), 1);
                assertEquals(SUCCESS, rpcClient.RemoteApi.getBuildStatus(current.getName(), 1));
            }
            while (remaining.size() > 0);
        }
    }

    private WaitProject createProject(String suffix) throws Exception
    {
        WaitProject project = projectConfigurations.createWaitAntProject(randomName() + "-" + suffix, new File(tempDir, suffix), false);
        // projects can only be built on the master to ensure that we get expected queing behaviour.
        bindStagesToMaster(project);
        return project;
    }

    private void bindStagesToMaster(WaitProject project) throws Exception
    {
        AgentConfiguration master = CONFIGURATION_HELPER.getMasterAgentReference();
        for (BuildStageConfiguration stage : project.getStages())
        {
            stage.setAgent(master);
        }
    }

    private void insertProjects(WaitProject... projects) throws Exception
    {
        for (WaitProject project: projects)
        {
            CONFIGURATION_HELPER.insertProject(project.getConfig(), false);
        }
    }
}
