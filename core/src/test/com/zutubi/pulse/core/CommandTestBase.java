package com.zutubi.pulse.core;

import com.zutubi.pulse.core.model.CommandResult;
import com.zutubi.pulse.core.model.ResultState;
import com.zutubi.pulse.test.PulseTestCase;
import com.zutubi.pulse.util.FileSystemUtils;
import com.zutubi.pulse.util.IOUtils;
import com.zutubi.pulse.BuildContext;

import java.io.*;

/**
 * <class-comment/>
 */
public abstract class CommandTestBase extends PulseTestCase
{
    File baseDir;
    File outputDir;

    public CommandTestBase()
    {
    }

    public CommandTestBase(String name)
    {
        super(name);
    }

    public void setUp() throws IOException
    {
        baseDir = FileSystemUtils.createTempDir(getClass().getName(), ".base");
        outputDir = FileSystemUtils.createTempDir(getClass().getName(), ".out");
    }

    public void tearDown() throws IOException
    {
        removeDirectory(baseDir);
        removeDirectory(outputDir);
    }

    public void test()
    {
        // a dummy test method so that junit does not complain... sigh.
    }

    protected void failedRun(ExecutableCommand command, String message) throws IOException
    {
        CommandResult commandResult = runCommand(command);
        assertEquals(ResultState.FAILURE, commandResult.getState());
        checkOutput(commandResult, message);
    }

    protected void successRun(ExecutableCommand command, String ...contents) throws IOException
    {
        CommandResult commandResult = runCommand(command);
        assertEquals(ResultState.SUCCESS, commandResult.getState());
        checkOutput(commandResult, contents);
    }

    protected CommandResult runCommand(ExecutableCommand command)
    {
        return runCommand(command, null);
    }

    protected CommandResult runCommand(ExecutableCommand command, BuildContext buildContext)
    {
        command.setWorkingDir(baseDir);
        CommandResult commandResult = new CommandResult("test");
        CommandContext context = new CommandContext(new SimpleRecipePaths(baseDir, null), outputDir, null);
        context.setBuildContext(buildContext);
        command.execute(context, commandResult);
        return commandResult;
    }

    protected void checkOutput(CommandResult commandResult, String ...contents) throws IOException
    {
        FileInputStream is = null;
        try
        {
            is = new FileInputStream(new File(outputDir, FileSystemUtils.composeFilename(Command.OUTPUT_ARTIFACT_NAME, "output.txt")));
            String output = IOUtils.inputStreamToString(is);
            for (String content : contents)
            {
                if (!output.contains(content))
                {
                    fail("Output '" + output + "' does not contain '" + content + "'");
                }
            }
        }
        finally
        {
            IOUtils.close(is);
        }
    }

    /**
     * The default build filename.
     *
     */
    protected abstract String getBuildFileName();

    /**
     * The template build file extension.
     *
     */
    protected abstract String getBuildFileExt();

    protected void copyBuildFile(String name) throws IOException
    {
        copyBuildFile(name, getBuildFileName());
    }

    protected void copyBuildFile(String name, String filename) throws IOException
    {
        InputStream is = null;
        OutputStream os = null;
        try
        {
            is = getInput(name, getBuildFileExt());
            os = new FileOutputStream(new File(baseDir, filename));
            IOUtils.joinStreams(is, os);
        }
        finally
        {
            IOUtils.close(is);
            IOUtils.close(os);
        }
    }
}