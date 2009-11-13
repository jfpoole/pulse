package com.zutubi.pulse.core.scm.git;

import com.zutubi.diff.Patch;
import com.zutubi.diff.PatchFile;
import com.zutubi.diff.PatchFileParser;
import com.zutubi.diff.PatchParseException;
import com.zutubi.pulse.core.engine.api.ExecutionContext;
import com.zutubi.pulse.core.engine.api.Feature;
import com.zutubi.pulse.core.scm.api.*;
import static com.zutubi.pulse.core.scm.git.GitConstants.*;
import com.zutubi.pulse.core.scm.git.diff.GitPatchParser;
import com.zutubi.pulse.core.scm.patch.api.FileStatus;
import com.zutubi.pulse.core.scm.patch.api.PatchFormat;
import com.zutubi.pulse.core.util.process.AsyncProcess;
import com.zutubi.pulse.core.util.process.ForwardingCharHandler;
import com.zutubi.util.CollectionUtils;
import com.zutubi.util.Mapping;
import com.zutubi.util.io.IOUtils;

import java.io.*;
import static java.util.Arrays.asList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * A {@link com.zutubi.pulse.core.scm.patch.api.PatchFormat} implementation for
 * git.  Uses git's own support for creating and applying patches, including
 * binary patches.
 */
public class GitPatchFormat implements PatchFormat
{
    public boolean writePatchFile(WorkingCopy workingCopy, WorkingCopyContext context, File patchFile, String... scope) throws ScmException
    {
        // Scope is of the form:
        //   [:<range>] <file> ...
        // where the optional <range> itself is of the form:
        //   [<commit>[..[.]<commit>]]
        // and is either empty (in which case we pass --cached), a single
        // commit (passed through to diff) or a commit range (passed through).
        //
        // If no commit range is present, we use <remote>/<remote branch>.  We
        // always add "--" after the commit range to avoid ambiguities.
        NativeGit git = new NativeGit();
        git.setWorkingDirectory(context.getBase());

        List<String> args = new LinkedList<String>();
        args.add(git.getGitCommand());
        args.add(COMMAND_DIFF);
        args.add(FLAG_BINARY);
        args.add(FLAG_FIND_COPIES);

        if (scope.length > 0 && scope[0].startsWith(":"))
        {
            String range = scope[0].substring(1);
            if (range.length() == 0)
            {
                args.add(FLAG_CACHED);
            }
            else
            {
                args.add(range);
            }

            if (scope.length > 1)
            {
                args.add(FLAG_SEPARATOR);
                args.addAll(asList(scope).subList(1, scope.length));
            }
        }
        else
        {
            GitWorkingCopy gitWorkingCopy = (GitWorkingCopy) workingCopy;
            args.add(gitWorkingCopy.getRemoteRef(git));
            if (scope.length > 0)
            {
                args.add(FLAG_SEPARATOR);
                args.addAll(asList(scope));
            }
        }

        // Run the process directly so we can capture raw output.  Going
        // through a line handler munges newlines.
        AsyncProcess async = null;
        Writer output = null;
        StringWriter error = new StringWriter();
        try
        {
            output = new FileWriter(patchFile);

            ProcessBuilder builder = new ProcessBuilder(args);
            builder.directory(context.getBase());
            Process p = builder.start();

            async = new AsyncProcess(p, new ForwardingCharHandler(output, error), true);
            int exitCode = async.waitFor();
            if (exitCode != 0)
            {
                context.getUI().error(error.toString());
                context.getUI().error("git diff exited with code " + exitCode + ".");
                return false;
            }
        }
        catch (Exception e)
        {
            context.getUI().error("Error writing patch file: " + e.getMessage(), e);
            return false;
        }
        finally
        {
            IOUtils.close(output);
            if (async != null)
            {
                async.destroy();
            }
        }

        if (patchFile.length() == 0)
        {
            context.getUI().status("No changes found.");
            if (!patchFile.delete())
            {
                throw new GitException("Can't remove empty patch '" + patchFile.getAbsolutePath() + "'");
            }

            return false;
        }

        return true;
    }

    public List<Feature> applyPatch(ExecutionContext context, File patchFile, File baseDir, EOLStyle localEOL, ScmFeedbackHandler scmFeedbackHandler) throws ScmException
    {
        NativeGit git = new NativeGit();
        git.setWorkingDirectory(baseDir);
        git.apply(scmFeedbackHandler, patchFile);
        return Collections.emptyList();
    }

    public List<FileStatus> readFileStatuses(File patchFile) throws ScmException
    {
        try
        {
            PatchFileParser parser = new PatchFileParser(new GitPatchParser());
            PatchFile gitPatch = parser.parse(new FileReader(patchFile));
            return CollectionUtils.map(gitPatch.getPatches(), new Mapping<Patch, FileStatus>()
            {
                public FileStatus map(Patch patch)
                {
                    return new FileStatus(patch.getNewFile(), FileStatus.State.valueOf(patch.getType()), false);
                }
            });
        }
        catch (IOException e)
        {
            throw new GitException("I/O error reading git patch: " + e.getMessage(), e);
        }
        catch (PatchParseException e)
        {
            throw new GitException("Unable to parse git patch: " + e.getMessage(), e);
        }
    }

    public boolean isPatchFile(File patchFile)
    {
        try
        {
            // Check the first hundred lines for anything that looks like a git
            // diff header.
            BufferedReader reader = new BufferedReader(new FileReader(patchFile));
            for (int i = 0; i < 100; i++)
            {
                String line = reader.readLine();
                if (line == null)
                {
                    break;
                }

                if (line.startsWith("diff --git"))
                {
                    return true;
                }
            }
        }
        catch (IOException e)
        {
            // Fall through.
        }

        return false;
    }
}