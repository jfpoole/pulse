package com.zutubi.pulse.renderer;

import com.zutubi.pulse.core.model.*;
import com.zutubi.pulse.model.*;
import com.zutubi.pulse.test.PulseTestCase;
import com.zutubi.pulse.util.IOUtils;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.LinkedList;
import java.util.List;

/**
 */
public class FreemarkerBuildResultRendererTest extends PulseTestCase
{
    FreemarkerBuildResultRenderer renderer;

    protected void setUp() throws Exception
    {
        super.setUp();
        renderer = new FreemarkerBuildResultRenderer();

        File pulseRoot = new File(getPulseRoot(), "master/src/templates");

        Configuration freemarkerConfiguration = new Configuration();
        freemarkerConfiguration.setDirectoryForTemplateLoading(pulseRoot);
        freemarkerConfiguration.setObjectWrapper(new DefaultObjectWrapper());
        freemarkerConfiguration.addAutoInclude("macro.ftl");
        renderer.setFreemarkerConfiguration(freemarkerConfiguration);
    }

    protected void tearDown() throws Exception
    {
        renderer = null;
        super.tearDown();
    }

    public void testBasicSuccess() throws IOException
    {
        BuildResult result = createSuccessfulBuild();
        createAndVerify("basic", "test.url:8080", result);
    }

    public void testWithChanges() throws IOException
    {
        BuildResult result = createBuildWithChanges();

        createAndVerify("changes", "another.url", result);
    }

    public void testWithErrors() throws IOException
    {
        errorsHelper("plain");
    }

    public void testHTMLWithErrors() throws IOException
    {
        errorsHelper("html");
    }

    public void testWithFailures() throws IOException
    {
        failuresHelper("plain");
    }

    public void testHTMLWithFailures() throws IOException
    {
        failuresHelper("html");
    }

    private void errorsHelper(String type) throws IOException
    {
        BuildResult result = createBuildWithChanges();
        result.error("test error message");
        result.addFeature(Feature.Level.WARNING, "warning message on result");
        RecipeResultNode firstNode = result.getRoot().getChildren().get(0);
        firstNode.getResult().error("test recipe error message");

        RecipeResultNode nestedNode = firstNode.getChildren().get(0);
        nestedNode.getResult().failure("test recipe failure message with the unfortunate need to wrap because it is really quite ridiculously long");

        RecipeResultNode secondNode = result.getRoot().getChildren().get(1);
        RecipeResult secondResult = secondNode.getResult();

        CommandResult command = new CommandResult("test command");
        command.error("bad stuff happened, so wrap this: 000000000000000000000000000000000000000000000000000000000000000000000");
        secondResult.add(command);

        command = new CommandResult("artifact command");
        command.failure("artifacts let me down");

        StoredFileArtifact artifact = new StoredFileArtifact("first-artifact/testpath");
        artifact.addFeature(new Feature(Feature.Level.INFO, "info message"));
        artifact.addFeature(new Feature(Feature.Level.ERROR, "error message"));
        artifact.addFeature(new PlainFeature(Feature.Level.WARNING, "warning message", 19));
        command.addArtifact(new StoredArtifact("first-artifact", artifact));

        artifact = new StoredFileArtifact("second-artifact/this/time/a/very/very/very/very/long/pathname/which/will/look/ugly/i/have/no/doubt");
        artifact.addFeature(new PlainFeature(Feature.Level.ERROR, "error 1", 1000000));
        artifact.addFeature(new Feature(Feature.Level.ERROR, "error 2"));
        artifact.addFeature(new Feature(Feature.Level.ERROR, "error 3: in this case a longer error message so i can see how the wrapping works on the artifact messages"));
        command.addArtifact(new StoredArtifact("second-artifact", artifact));

        secondResult.add(command);

        createAndVerify("errors", type, "another.url", result);
    }

    private void failuresHelper(String type) throws IOException
    {
        BuildResult result = createSuccessfulBuild();
        result.failure("test failed tests");

        RecipeResultNode firstNode = result.getRoot().getChildren().get(0);
        firstNode.getResult().failure("tests failed dude");

        RecipeResultNode nestedNode = firstNode.getChildren().get(0);
        nestedNode.getResult().failure("tests failed nested dude");

        RecipeResultNode secondNode = result.getRoot().getChildren().get(1);
        RecipeResult secondResult = secondNode.getResult();

        CommandResult command = new CommandResult("failing tests");
        command.failure("tests let me down");

        StoredFileArtifact artifact = new StoredFileArtifact("first-artifact/testpath");
        TestSuiteResult rootSuite = new TestSuiteResult("root test suite");
        rootSuite.add(new TestCaseResult("1 passed"));
        rootSuite.add(new TestCaseResult("2 failed", 0, TestCaseResult.Status.FAILURE, "a failure message which is bound to be detailed, potentially to the extreme but in this case just to wrap a bit"));
        rootSuite.add(new TestCaseResult("3 error", 0, TestCaseResult.Status.ERROR, "short error"));
        rootSuite.add(new TestCaseResult("4 passed"));

        TestSuiteResult nestedSuite = new TestSuiteResult("nested suite");
        nestedSuite.add(new TestCaseResult("n1 failed", 0, TestCaseResult.Status.FAILURE, "a failure message which is bound to be detailed, potentially to the extreme but in this case just to wrap a bit"));
        nestedSuite.add(new TestCaseResult("n2 error", 0, TestCaseResult.Status.ERROR, "short error"));
        nestedSuite.add(new TestCaseResult("n3 passed"));
        rootSuite.add(nestedSuite);

        TestSuiteResult nestedPassSuite = new TestSuiteResult("you shouldn't see this!");
        nestedPassSuite.add(new TestCaseResult("nps"));
        rootSuite.add(nestedPassSuite);

        TestSuiteResult nestedEmptySuite = new TestSuiteResult("mmmm, boundary conditions");
        rootSuite.add(nestedEmptySuite);

        artifact.addTest(rootSuite);
        command.addArtifact(new StoredArtifact("first-artifact", artifact));

        artifact = new StoredFileArtifact("second-artifact/this/time/a/very/very/very/very/long/pathname/which/will/look/ugly/i/have/no/doubt");
        artifact.addTest(new TestCaseResult("test case at top level", 0, TestCaseResult.Status.FAILURE, "and i failed"));
        command.addArtifact(new StoredArtifact("second-artifact", artifact));

        secondResult.add(command);

        createAndVerify("failures", type, "host.url", result);
    }

    private BuildResult createBuildWithChanges()
    {
        BuildResult result = createSuccessfulBuild();

        Revision buildRevision = new Revision();
        buildRevision.setRevisionString("656");

        List<Changelist> changes = new LinkedList<Changelist>();
        Changelist list = new Changelist("scm", new Revision("test author", "short comment", 324252, "655"));
        changes.add(list);
        list = new Changelist("scm", new Revision("author2", "this time we will use a longer comment to make sure that the renderer is applying some sort of trimming to the resulting output dadada da dadad ad ad adadad ad ad ada d adada dad ad ad d ad ada da d", 310000, "656"));
        changes.add(list);

        BuildScmDetails details = new BuildScmDetails(buildRevision, changes);
        result.setScmDetails(details);
        return result;
    }

    private BuildResult createSuccessfulBuild()
    {
        BuildResult result = new BuildResult(new TriggerBuildReason("scm trigger"), new Project("test project", "test description"), "test spec", 101);
        result.setId(11);
        result.setScmDetails(new BuildScmDetails());
        result.commence(10000);

        RecipeResult recipeResult = new RecipeResult("first recipe");
        RecipeResultNode node = new RecipeResultNode("first stage", recipeResult);
        result.getRoot().addChild(node);

        recipeResult = new RecipeResult("second recipe");
        node = new RecipeResultNode("second stage", recipeResult);
        result.getRoot().addChild(node);

        recipeResult = new RecipeResult("nested recipe");
        node = new RecipeResultNode("nested stage", recipeResult);
        result.getRoot().getChildren().get(0).addChild(node);

        result.complete();
        result.getStamps().setEndTime(100000);
        return result;
    }

    protected void createAndVerify(String expectedName, String hostUrl, BuildResult result) throws IOException
    {
        createAndVerify(expectedName, "plain", hostUrl, result);
    }

    protected void createAndVerify(String expectedName, String type, String hostUrl, BuildResult result) throws IOException
    {
        String extension = "txt";
        if (type.equals("html"))
        {
            extension = "html";
        }

        InputStream expectedStream = null;

        try
        {
            expectedStream = getInput(expectedName, extension);

            StringWriter writer = new StringWriter();
            renderer.render(hostUrl, result, type, writer);
            String got = replaceTimestamps(writer.getBuffer().toString());
            String expected = replaceTimestamps(IOUtils.inputStreamToString(expectedStream));
            assertEquals(expected, got);
        }
        finally
        {
            IOUtils.close(expectedStream);
        }
    }

    private String replaceTimestamps(String str)
    {
        return str.replaceAll("\n.*ago<", "@@@@");
    }
}
