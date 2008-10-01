package com.zutubi.pulse.core;

import static com.zutubi.pulse.core.BuildProperties.NAMESPACE_INTERNAL;
import static com.zutubi.pulse.core.BuildProperties.PROPERTY_OUTPUT_DIR;
import com.zutubi.pulse.core.model.CommandResult;
import com.zutubi.pulse.core.model.StoredArtifact;
import com.zutubi.pulse.core.model.StoredFileArtifact;
import com.zutubi.pulse.core.util.FileSystemUtils;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

/**
 * <class comment/>
 */
public class PrecapturedArtifact extends ArtifactSupport
{
    private List<ProcessArtifact> processes = new LinkedList<ProcessArtifact>();
    private String type;

    public void setProcesses(List<ProcessArtifact> processes)
    {
        this.processes = processes;
    }

    public void capture(CommandResult result, ExecutionContext context)
    {
        File dir = new File(context.getFile(NAMESPACE_INTERNAL, PROPERTY_OUTPUT_DIR), getName());
        
        StoredArtifact storedArtifact = new StoredArtifact(getName());

        if (dir.isDirectory())
        {
            for (File file : dir.listFiles())
            {
                StoredFileArtifact fileArtifact = new StoredFileArtifact(FileSystemUtils.composeFilename(getName(), file.getName()), type);
                storedArtifact.add(fileArtifact);

                processArtifact(fileArtifact, result, context, processes);
            }
            if (storedArtifact.getChildren().size() > 0)
            {
                result.addArtifact(storedArtifact);
            }
        }
    }

    public void setType(String type)
    {
        this.type = type;
    }
}
