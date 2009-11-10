package com.zutubi.pulse.acceptance.dependencies;

import com.zutubi.pulse.acceptance.BaseXmlRpcAcceptanceTest;
import static com.zutubi.pulse.core.dependency.ivy.IvyStatus.*;
import com.zutubi.pulse.core.engine.api.ResultState;
import com.zutubi.pulse.master.tove.config.project.triggers.DependentBuildTriggerConfiguration;
import com.zutubi.pulse.master.tove.config.project.DependencyConfiguration;
import com.zutubi.util.CollectionUtils;
import com.zutubi.util.SystemUtils;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

import java.io.IOException;

public class DependenciesAcceptanceTest extends BaseXmlRpcAcceptanceTest
{
    private Repository repository;
    private String randomName;
    private BuildRunner buildRunner;
    private ProjectConfigurations projects;
    private ConfigurationHelper configurationHelper;

    protected void setUp() throws Exception
    {
        super.setUp();

        loginAsAdmin();

        repository = new Repository();
        repository.clean();

        randomName = randomName();

        buildRunner = new BuildRunner(xmlRpcHelper);
        configurationHelper = new ConfigurationHelper();
        configurationHelper.setXmlRpcHelper(xmlRpcHelper);
        configurationHelper.init();

        projects = new ProjectConfigurations(configurationHelper);
    }

    @Override
    protected void tearDown() throws Exception
    {
        logout();

        super.tearDown();
    }

    private void insertProject(ProjectConfigurationHelper project) throws Exception
    {
        configurationHelper.insertProject(project.getConfig());
    }

    private void updateProject(ProjectConfigurationHelper project) throws Exception
    {
        configurationHelper.updateProject(project.getConfig());
    }

    public void testPublish_NoArtifacts() throws Exception
    {
        // configure project.
        DepAntProject project = projects.createDepAntProject(randomName);
        insertProject(project);

        int buildNumber = buildRunner.triggerSuccessfulBuild(project.getConfig());

        assertIvyInRepository(project, buildNumber);
    }

    public void testPublish_SingleArtifact() throws Exception
    {
        DepAntProject project = projects.createDepAntProject(randomName);
        project.addArtifacts("build/artifact.jar");
        project.addFilesToCreate("build/artifact.jar");
        insertProject(project);

        int buildNumber = buildRunner.triggerSuccessfulBuild(project.getConfig());

        // ensure that we have the expected artifact in the repository.
        assertIvyInRepository(project, buildNumber);
        assertArtifactInRepository(project, "default", buildNumber, "artifact", "jar");
    }

    public void testPublish_MultipleArtifacts() throws Exception
    {
        DepAntProject project = projects.createDepAntProject(randomName);
        project.addArtifacts("build/artifact.jar", "build/another-artifact.jar");
        project.addFilesToCreate("build/artifact.jar", "build/another-artifact.jar");
        insertProject(project);

        int buildNumber = buildRunner.triggerSuccessfulBuild(project.getConfig());

        // ensure that we have the expected artifact in the repository.
        assertIvyInRepository(project, buildNumber);
        assertArtifactInRepository(project, "default", buildNumber, "artifact", "jar");
        assertArtifactInRepository(project, "default", buildNumber, "another-artifact", "jar");
    }

    public void testPublish_MultipleStages() throws Exception
    {
        DepAntProject project = projects.createDepAntProject(randomName);
        project.addArtifacts("build/artifact.jar");
        project.addStage("stage");
        project.addFilesToCreate("build/artifact.jar");
        insertProject(project);

        int buildNumber = buildRunner.triggerSuccessfulBuild(project.getConfig());

        assertIvyInRepository(project, buildNumber);
        assertArtifactInRepository(project, "default", buildNumber, "artifact", "jar");
        assertArtifactInRepository(project, "stage", buildNumber, "artifact", "jar");
    }

    public void testPublishFails_MissingArtifacts() throws Exception
    {
        DepAntProject project = projects.createDepAntProject(randomName);
        project.addArtifacts("build/artifact.jar");
        project.addFilesToCreate("incorrect/path/artifact.jar");
        insertProject(project);

        int buildNumber = buildRunner.triggerCompleteBuild(project.getConfig());
        assertEquals(ResultState.ERROR, buildRunner.getBuildStatus(project.getConfig(), buildNumber));

        // ensure that we have the expected artifact in the repository.
        assertIvyNotInRepository(project, buildNumber);
        assertArtifactNotInRepository(project, "default", buildNumber, "artifact", "jar");
    }

    public void testPublish_StatusConfiguration() throws Exception
    {
        DepAntProject project = projects.createDepAntProject(randomName);
        project.getConfig().getDependencies().setStatus(STATUS_RELEASE);
        project.addArtifacts("build/artifact.jar");
        project.addFilesToCreate("build/artifact.jar");
        insertProject(project);

        int buildNumber = buildRunner.triggerSuccessfulBuild(project.getConfig());

        // ensure that we have the expected artifact in the repository.
        assertIvyInRepository(project, buildNumber);
        assertIvyStatus(STATUS_RELEASE, project, buildNumber);
    }

    public void testPublish_DefaultStatus() throws Exception
    {
        DepAntProject project = projects.createDepAntProject(randomName);
        project.addArtifacts("build/artifact.jar");
        project.addFilesToCreate("build/artifact.jar");
        insertProject(project);

        int buildNumber = buildRunner.triggerSuccessfulBuild(project.getConfig());

        assertIvyInRepository(project, buildNumber);
        assertIvyStatus(STATUS_INTEGRATION, project, buildNumber);
    }

    public void testStatusValidation() throws Exception
    {
        try
        {
            DepAntProject project = projects.createDepAntProject(randomName);
            project.getConfig().getDependencies().setStatus("invalid");
            project.addArtifacts("build/artifact.jar");
            insertProject(project);
        }
        catch (Exception e)
        {
            assertThat(e.getMessage(), containsString("status is invalid"));
        }
    }

    public void testRemoteTriggerWithCustomStatus() throws Exception
    {
        DepAntProject project = projects.createDepAntProject(randomName);
        project.addArtifacts("build/artifact.jar");
        project.addFilesToCreate("build/artifact.jar");
        insertProject(project);

        int buildNumber = buildRunner.triggerSuccessfulBuild(project.getConfig(), CollectionUtils.asPair("status", (Object)STATUS_MILESTONE));

        // ensure that we have the expected artifact in the repository.
        assertIvyInRepository(project, buildNumber);
        assertIvyStatus(STATUS_MILESTONE, project, buildNumber);
    }

    public void testRetrieve_SingleArtifact() throws Exception
    {
        DepAntProject projectA = projects.createDepAntProject(randomName + "A");
        projectA.addArtifacts("build/artifact.jar");
        projectA.addFilesToCreate("build/artifact.jar");
        insertProject(projectA);

        int buildNumber = buildRunner.triggerSuccessfulBuild(projectA.getConfig());

        assertIvyInRepository(projectA, buildNumber);

        DepAntProject projectB = projects.createDepAntProject(randomName + "B");
        projectB.addDependency(projectA.getConfig());
        projectB.addExpectedFiles("lib/artifact.jar");
        insertProject(projectB);

        buildNumber = buildRunner.triggerSuccessfulBuild(projectB.getConfig());

        assertIvyInRepository(projectB, buildNumber);
    }

    public void testRetrieve_MultipleArtifacts() throws Exception
    {
        DepAntProject projectA = projects.createDepAntProject(randomName + "A");
        projectA.addArtifacts("build/artifact.jar", "build/another-artifact.jar");
        projectA.addFilesToCreate("build/artifact.jar", "build/another-artifact.jar");
        insertProject(projectA);

        buildRunner.triggerSuccessfulBuild(projectA.getConfig());

        DepAntProject projectB = projects.createDepAntProject(randomName + "B");
        projectB.addDependency(projectA.getConfig());
        projectB.addExpectedFiles("lib/artifact.jar", "lib/another-artifact.jar");
        insertProject(projectB);

        int buildNumber = buildRunner.triggerSuccessfulBuild(projectB.getConfig());

        assertIvyInRepository(projectB, buildNumber);
    }

    public void testRetrieve_SpecificStage() throws Exception
    {
        // need different recipies that produce different artifacts.
        DepAntProject project = projects.createDepAntProject(randomName);
        project.addRecipe("recipeA").addArtifacts("build/artifactA.jar");
        project.addRecipe("recipeB").addArtifacts("build/artifactB.jar");
        project.addStage("A").setRecipe("recipeA");
        project.addStage("B").setRecipe("recipeB");
        project.addFilesToCreate("build/artifactA.jar", "build/artifactB.jar");
        insertProject(project);

        int buildNumber = buildRunner.triggerSuccessfulBuild(project.getConfig());

        assertIvyInRepository(project, buildNumber);
        assertArtifactInRepository(project, "A", buildNumber, "artifactA", "jar");
        assertArtifactNotInRepository(project, "B", buildNumber, "artifactA", "jar");

        assertArtifactInRepository(project, "B", buildNumber, "artifactB", "jar");
        assertArtifactNotInRepository(project, "A", buildNumber, "artifactB", "jar");
    }

    public void testRetrieve_SpeicificRevision() throws Exception
    {
        DepAntProject projectA = projects.createDepAntProject(randomName + "A");
        projectA.addArtifacts("build/default-artifact.jar");
        projectA.addFilesToCreate("build/default-artifact.jar");
        insertProject(projectA);

        // build twice and then depend on the first.
        int buildNumber = buildRunner.triggerSuccessfulBuild(projectA.getConfig());
        buildRunner.triggerSuccessfulBuild(projectA.getConfig());

        DepAntProject projectB = projects.createDepAntProject(randomName + "B");
        projectB.getConfig().getDependencies().setRetrievalPattern("lib/[artifact]-[revision].[ext]");

        DependencyConfiguration dependencyConfig = projectB.addDependency(projectA.getConfig());
        dependencyConfig.setRevision(DependencyConfiguration.REVISION_CUSTOM);
        dependencyConfig.setTransitive(true);
        dependencyConfig.setCustomRevision(String.valueOf(buildNumber));

        projectB.addExpectedFiles("lib/default-artifact-" + buildNumber + ".jar");
        insertProject(projectB);

        buildRunner.triggerSuccessfulBuild(projectB.getConfig());
    }

    public void testRetrieve_MultipleProjects() throws Exception
    {
        DepAntProject projectA = projects.createDepAntProject(randomName + "A");
        projectA.addArtifacts("build/projectA-artifact.jar");
        projectA.addFilesToCreate("build/projectA-artifact.jar");
        insertProject(projectA);
        buildRunner.triggerSuccessfulBuild(projectA.getConfig());

        DepAntProject projectB = projects.createDepAntProject(randomName + "B");
        projectB.addArtifacts("build/projectB-artifact.jar");
        projectB.addFilesToCreate("build/projectB-artifact.jar");
        insertProject(projectB);
        buildRunner.triggerSuccessfulBuild(projectB.getConfig());

        DepAntProject projectC = projects.createDepAntProject(randomName + "C");
        projectC.addDependency(projectA.getConfig());
        projectC.addDependency(projectB.getConfig());
        projectC.addExpectedFiles("lib/projectA-artifact.jar", "lib/projectB-artifact.jar");
        insertProject(projectC);

        int buildNumber = buildRunner.triggerSuccessfulBuild(projectC.getConfig());
        assertIvyInRepository(projectC, buildNumber);
    }

    public void testRetrieve_TransitiveDependencies() throws Exception
    {
        DepAntProject projectA = projects.createDepAntProject(randomName + "A");
        projectA.addArtifacts("build/projectA-artifact.jar");
        projectA.addFilesToCreate("build/projectA-artifact.jar");
        insertProject(projectA);
        buildRunner.triggerSuccessfulBuild(projectA.getConfig());

        DepAntProject projectB = projects.createDepAntProject(randomName + "B");
        projectB.addArtifacts("build/projectB-artifact.jar");
        projectB.addDependency(projectA.getConfig()).setTransitive(true);
        projectB.addFilesToCreate("build/projectB-artifact.jar");
        projectB.addExpectedFiles("lib/projectA-artifact.jar");
        insertProject(projectB);
        buildRunner.triggerSuccessfulBuild(projectB.getConfig());

        DepAntProject projectC = projects.createDepAntProject(randomName + "C");
        projectC.addDependency(projectB.getConfig());
        projectC.addExpectedFiles("lib/projectA-artifact.jar", "lib/projectB-artifact.jar");
        insertProject(projectC);

        int buildNumber = buildRunner.triggerSuccessfulBuild(projectC.getConfig());
        assertIvyInRepository(projectC, buildNumber);
    }

    public void testRetrieve_TransitiveDependenciesDisabled() throws Exception
    {
        DepAntProject projectA = projects.createDepAntProject(randomName + "A");
        projectA.addArtifacts("build/projectA-artifact.jar");
        projectA.addFilesToCreate("build/projectA-artifact.jar");
        insertProject(projectA);
        buildRunner.triggerSuccessfulBuild(projectA.getConfig());

        DepAntProject projectB = projects.createDepAntProject(randomName + "B");
        projectB.addArtifacts("build/projectB-artifact.jar");
        projectB.addDependency(projectA.getConfig());
        projectB.addFilesToCreate("build/projectB-artifact.jar");
        projectB.addExpectedFiles("lib/projectA-artifact.jar");
        insertProject(projectB);

        buildRunner.triggerSuccessfulBuild(projectB.getConfig());

        DepAntProject projectC = projects.createDepAntProject(randomName + "C");
        projectC.addDependency(projectB.getConfig()).setTransitive(false);
        projectC.addExpectedFiles("lib/projectB-artifact.jar");
        projectC.addNotExpectedFiles("lib/projectA-artifact.jar");
        insertProject(projectC);

        int buildNumber = buildRunner.triggerSuccessfulBuild(projectC.getConfig());
        assertIvyInRepository(projectC, buildNumber);
    }

    public void testRetrieveFails_MissingDependencies() throws Exception
    {
        DepAntProject projectA = projects.createDepAntProject(randomName + "A");
        projectA.addArtifacts("build/artifact.jar");
        insertProject(projectA);

        // do not build projectA simulating dependency not available.

        DepAntProject projectB = projects.createDepAntProject(randomName + "B");
        projectB.addDependency(projectA.getConfig());
        projectB.addExpectedFiles("lib/artifact.jar");
        insertProject(projectB);

        int buildNumber = buildRunner.triggerCompleteBuild(projectB.getConfig());
        assertEquals(ResultState.FAILURE, getBuildStatus(projectB.getConfig().getName(), buildNumber));

        // ensure that we have the expected artifact in the repository.
        assertIvyNotInRepository(projectB, buildNumber);
    }

    public void testDependentBuild_TriggeredOnSuccess() throws Exception
    {
        DepAntProject projectA = projects.createDepAntProject(randomName + "A");
        projectA.addArtifacts("build/artifact.jar");
        projectA.addFilesToCreate("build/artifact.jar");
        insertProject(projectA);

        DepAntProject projectB = projects.createDepAntProject(randomName + "B");
        projectB.addDependency(projectA.getConfig());
        insertProject(projectB);

        buildRunner.triggerSuccessfulBuild(projectA.getConfig());

        xmlRpcHelper.waitForBuildToComplete(projectB.getConfig().getName(), 1);
    }

    public void testDependentBuild_PropagateStatus() throws Exception
    {
        DepAntProject projectA = projects.createDepAntProject(randomName + "A");
        projectA.addArtifacts("build/artifact.jar");
        projectA.getConfig().getDependencies().setStatus(STATUS_RELEASE);
        projectA.addFilesToCreate("build/artifact.jar");
        insertProject(projectA);

        DepAntProject projectB = projects.createDepAntProject(randomName + "B");
        projectB.addDependency(projectA.getConfig());
        projectB.getConfig().getDependencies().setStatus(STATUS_INTEGRATION);
        
        DependentBuildTriggerConfiguration trigger = projectB.getTrigger("dependency trigger");
        trigger.setPropagateStatus(true);

        insertProject(projectB);

        buildRunner.triggerSuccessfulBuild(projectA.getConfig());

        xmlRpcHelper.waitForBuildToComplete(projectB.getConfig().getName(), 1);

        assertIvyStatus(STATUS_RELEASE, projectB, 1);
        assertIvyRevision("1", projectB, "1");
    }

    public void testDependentBuild_PropagateVersion() throws Exception
    {
        DepAntProject projectA = projects.createDepAntProject(randomName + "A");
        projectA.addArtifacts("build/artifact.jar");
        projectA.addFilesToCreate("build/artifact.jar");
        projectA.getConfig().getDependencies().setVersion("FIXED");
        insertProject(projectA);

        DepAntProject projectB = projects.createDepAntProject(randomName + "B");
        projectB.addDependency(projectA.getConfig());

        DependentBuildTriggerConfiguration trigger = projectB.getTrigger("dependency trigger");
        trigger.setPropagateVersion(true);
        insertProject(projectB);

        buildRunner.triggerSuccessfulBuild(projectA.getConfig());

        xmlRpcHelper.waitForBuildToComplete(projectB.getConfig().getName(), 1);

        assertIvyInRepository(projectB, "FIXED");
        assertIvyRevision("FIXED", projectB, "FIXED");
    }

    public void testRepositoryFormat_OrgSpecified() throws Exception
    {
        DepAntProject project = projects.createDepAntProject(randomName);
        project.getConfig().setOrganisation("org");
        project.addArtifacts("build/artifact.jar");
        project.addFilesToCreate("build/artifact.jar");
        insertProject(project);

        int buildNumber = buildRunner.triggerSuccessfulBuild(project.getConfig());

        assertIvyInRepository(project, buildNumber);
    }

    public void testArtifactPattern() throws Exception
    {
        DepAntProject project = projects.createDepAntProject(randomName);
        project.addArtifacts("build/artifact-12345.jar").get(0).setArtifactPattern("(.+)-[0-9]+\\.(.+)");
        project.addFilesToCreate("build/artifact-12345.jar");
        insertProject(project);

        int buildNumber = buildRunner.triggerSuccessfulBuild(project.getConfig());

        assertArtifactInRepository(project, "default", buildNumber, "artifact", "jar");
    }

    public void testUnusualCharactersInArtifactName() throws Exception
    {
        // The criteria for artifact names is that they must be allowed in a URI.  This
        // is because the internal artifact repository is accessed by ivy via HTTP.
        
        String validCharacters = "!()._-";
        String invalidCharacters = "@#%^&";

        // $ is not allowed in an artifact name 

        runBuildWithCharacterInArtifactName(validCharacters, ResultState.SUCCESS);
        runBuildWithCharacterInArtifactName(invalidCharacters, ResultState.ERROR);
    }

    // CIB-2171
    public void testDependencyStatusUpdates() throws Exception
    {
        DepAntProject projectA = projects.createDepAntProject(randomName + "A");
        projectA.addArtifacts("build/artifact.jar");
        projectA.addFilesToCreate("build/artifact.jar");
        projectA.getConfig().getDependencies().setStatus(STATUS_INTEGRATION);
        insertProject(projectA);

        buildRunner.triggerSuccessfulBuild(projectA);

        DepAntProject projectB = projects.createDepAntProject(randomName + "B");
        DependencyConfiguration dependency = projectB.addDependency(projectA.getConfig());
        dependency.setRevision("latest." + STATUS_INTEGRATION);
        projectB.addExpectedFiles("lib/artifact-1.jar");
        projectB.getConfig().getDependencies().setRetrievalPattern("lib/[artifact]-[revision].[ext]");
        insertProject(projectB);

        buildRunner.triggerSuccessfulBuild(projectB);

        dependency.setRevision("latest." + STATUS_RELEASE);
        updateProject(projectB);

        buildRunner.triggerFailedBuild(projectB);
    }

    private void runBuildWithCharacterInArtifactName(String testCharacters, ResultState expected) throws Exception
    {
        for (char c : testCharacters.toCharArray())
        {
            DepAntProject project = projects.createDepAntProject(randomName());
            project.addArtifacts("build/artifact-" + c + ".jar");

            // The ant script on unix evals its arguments, so we need to escape
            // these characters lest the shell choke on them.
            String resolvedChar = SystemUtils.IS_WINDOWS ? Character.toString(c) : "\\" + c;
            project.addFilesToCreate("build/artifact-" + resolvedChar + ".jar");

            insertProject(project);

            int buildNumber = buildRunner.triggerCompleteBuild(project.getConfig());
            assertEquals("Unexpected result for character: " + c, expected, getBuildStatus(project.getConfig().getName(), buildNumber));

            if (expected == ResultState.SUCCESS)
            {
                assertIvyInRepository(project, buildNumber);
                assertArtifactInRepository(project, "default", buildNumber, "artifact-" + c, "jar");
            }
            else
            {
                assertIvyNotInRepository(project, buildNumber);
                assertArtifactNotInRepository(project, "default", buildNumber, "artifact-" + c, "jar");
            }
        }
    }

    private void assertIvyStatus(String expectedStatus, ProjectConfigurationHelper project, int buildNumber) throws Exception
    {
        assertEquals(expectedStatus, repository.getIvyModuleDescriptor(project.getConfig().getOrganisation(), project.getConfig().getName(), buildNumber).getStatus());
    }

    private void assertIvyRevision(String expectedRevision, ProjectConfigurationHelper project, String version) throws Exception
    {
        assertEquals(expectedRevision, repository.getIvyModuleDescriptor(project.getConfig().getOrganisation(), project.getConfig().getName(), version).getRevision());
    }

    private void assertIvyInRepository(ProjectConfigurationHelper project, Object revision) throws Exception
    {
        assertInRepository(repository.getIvyModuleDescriptor(project.getConfig().getOrganisation(), project.getConfig().getName(), revision).getPath());
    }

    private void assertIvyNotInRepository(ProjectConfigurationHelper project, Object revision) throws Exception
    {
        assertNotInRepository(repository.getIvyModuleDescriptor(project.getConfig().getOrganisation(), project.getConfig().getName(), revision).getPath());
    }

    private void assertArtifactInRepository(ProjectConfigurationHelper project, String stageName, Object revision, String artifactName, String artifactExtension) throws IOException
    {
        assertInRepository(repository.getArtifactPath(project.getConfig().getOrganisation(), project.getConfig().getName(), stageName, revision, artifactName, artifactExtension));
    }

    private void assertArtifactNotInRepository(ProjectConfigurationHelper project, String stageName, Object revision, String artifactName, String artifactExtension) throws IOException
    {
        assertNotInRepository(repository.getArtifactPath(project.getConfig().getOrganisation(), project.getConfig().getName(), stageName, revision, artifactName, artifactExtension));
    }

    private void assertInRepository(String baseArtifactName) throws IOException
    {
        // all artifacts are being published with .md5 and .sha1 hashes.
        assertTrue(repository.waitUntilInRepository(baseArtifactName));
        assertTrue(repository.waitUntilInRepository(baseArtifactName + ".md5"));
        assertTrue(repository.waitUntilInRepository(baseArtifactName + ".sha1"));
    }

    public void assertNotInRepository(String baseArtifactName) throws IOException
    {
        assertFalse(repository.waitUntilInRepository(baseArtifactName));
    }
}
