package com.zutubi.pulse.core.scm.svn;

import com.zutubi.pulse.core.scm.api.PersonalBuildUIAwareSupport;
import com.zutubi.pulse.core.scm.api.*;
import static com.zutubi.pulse.core.scm.svn.SubversionConstants.*;
import com.zutubi.util.config.Config;
import com.zutubi.util.config.ConfigSupport;
import org.tmatesoft.svn.core.*;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.internal.io.dav.DAVRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.svn.SVNRepositoryFactoryImpl;
import org.tmatesoft.svn.core.internal.wc.admin.SVNAdminAreaFactory;
import org.tmatesoft.svn.core.wc.*;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

/**
 */
public class SubversionWorkingCopy extends PersonalBuildUIAwareSupport implements WorkingCopy
{
    public static final String PROPERTY_ALLOW_EXTERNALS = "svn.allow.externals";

    static
    {
        // Initialise SVN library
        DAVRepositoryFactory.setup();
        SVNRepositoryFactoryImpl.setup();
        SVNAdminAreaFactory.setUpgradeEnabled(false);
    }

    private SVNClientManager getClientManager(WorkingCopyContext context, boolean addAuthenticationManager)
    {
        ISVNOptions options = SVNWCUtil.createDefaultOptions(true);
        SVNClientManager svnClientManager = SVNClientManager.newInstance(options);
        if (addAuthenticationManager)
        {
            svnClientManager = SVNClientManager.newInstance(options, getAuthenticationManager(context, svnClientManager));
        }

        return svnClientManager;
    }

    private ISVNAuthenticationManager getAuthenticationManager(WorkingCopyContext context, SVNClientManager defaultClientManager)
    {
        ISVNAuthenticationManager authenticationManager;
        Config config = context.getConfig();
        String user = config.getProperty(PROPERTY_USERNAME);

        if(user == null)
        {
            // See if there is a username specified in the working copy URL.
            try
            {
                SVNInfo info = defaultClientManager.getWCClient().doInfo(context.getBase(), null);
                user = info.getURL().getUserInfo();
            }
            catch (SVNException e)
            {
                // Ignore this error, we can proceed.
            }
        }

        if(user == null)
        {
            authenticationManager = SVNWCUtil.createDefaultAuthenticationManager();
        }
        else
        {
            authenticationManager = SVNWCUtil.createDefaultAuthenticationManager(user, getPassword(config));

            if(config.hasProperty(PROPERTY_KEYFILE))
            {
                String privateKeyFile = config.getProperty(PROPERTY_KEYFILE);
                String passphrase = config.getProperty(PROPERTY_PASSPHRASE);

                authenticationManager.setAuthenticationProvider(new SVNSSHAuthenticationProvider(user, privateKeyFile, passphrase));
            }
        }

        return authenticationManager;
    }

    public String getPassword(Config config)
    {
        String password = config.getProperty(PROPERTY_PASSWORD);
        if(password == null)
        {
            password = getUI().passwordPrompt("Subversion password");
            if(password == null)
            {
                password = "";
            }
        }

        return password;
    }

    public boolean matchesLocation(WorkingCopyContext context, String location) throws ScmException
    {
        // We just check that the URL matches
        SVNURL serverURL;

        try
        {
            serverURL = SVNURL.parseURIEncoded(location);
        }
        catch (SVNException e)
        {
            // Not the personal-builder's problem
            return true;
        }

        try
        {
            SVNClientManager svnClientManager = getClientManager(context, false);
            SVNInfo info = svnClientManager.getWCClient().doInfo(context.getBase(), null);
            SVNURL wcUrl = info.getURL();

            boolean eq = serverURL.getProtocol().equals(wcUrl.getProtocol()) &&
                    serverURL.hasPort() == wcUrl.hasPort() &&
                    serverURL.getPort() == wcUrl.getPort() &&
                    serverURL.getHost().equals(wcUrl.getHost()) &&
                    serverURL.getPath().equals(wcUrl.getPath());

            if (eq)
            {
                return true;
            }
            else
            {
                getUI().warning("Working copy's repository URL '" + wcUrl + "' does not match Pulse project's repository URL '" + location + "'");
                return false;
            }
        }
        catch (SVNException e)
        {
            throw convertException(e);
        }
    }

    public WorkingCopyStatus getLocalStatus(WorkingCopyContext context, String... paths) throws ScmException
    {
        File base = context.getBase();
        File[] files = pathsToFiles(base, paths);
        if (files == null)
        {
            return getStatus(context, base);
        }
        else
        {
            return getStatus(context, files);
        }
    }

    private WorkingCopyStatus getStatus(WorkingCopyContext context, File... files) throws ScmException
    {
        SVNClientManager clientManager = getClientManager(context, false);
        StatusHandler handler = new StatusHandler(context);

        try
        {
            SVNStatusClient statusClient = clientManager.getStatusClient();
            statusClient.setEventHandler(handler);
            for (File f : files)
            {
                statusClient.doStatus(f, SVNRevision.HEAD, SVNDepth.INFINITY, false, true, false, false, handler, null);
            }

            WorkingCopyStatus wcs = handler.getStatus();

            // Now find out if any changed files have an eol-style
            getProperties(clientManager, wcs, handler.propertyChangedPaths);

            return wcs;
        }
        catch (SVNException e)
        {
            throw convertException(e);
        }
    }

    private void getProperties(SVNClientManager clientManager, WorkingCopyStatus wcs, List<String> propertyChangedPaths) throws SVNException
    {
        SVNWCClient wcc = clientManager.getWCClient();

        for (FileStatus fs : wcs.getFileStatuses())
        {
            if (fs.getState().requiresFile())
            {
                SVNPropertyData property = wcc.doGetProperty(new File(wcs.getBase(), fs.getPath()), SVN_PROPERTY_EOL_STYLE, SVNRevision.WORKING, SVNRevision.WORKING);
                if (property != null)
                {
                    fs.setProperty(FileStatus.PROPERTY_EOL_STYLE, convertEOLStyle(property.getValue().getString()));
                }
            }

            if (fs.getState() == FileStatus.State.ADDED)
            {
                // For new files, check for svn:executable
                SVNPropertyData property = wcc.doGetProperty(new File(wcs.getBase(), fs.getPath()), SVN_PROPERTY_EXECUTABLE, SVNRevision.WORKING, SVNRevision.WORKING);
                if (property != null)
                {
                    fs.setProperty(FileStatus.PROPERTY_EXECUTABLE, "true");
                }
            }
        }

        // For items with changed properties, check if the executable property has flipped
        for (String path : propertyChangedPaths)
        {
            FileStatus fs = wcs.getFileStatus(path);
            SVNPropertyData baseProperty = wcc.doGetProperty(new File(wcs.getBase(), path), SVN_PROPERTY_EXECUTABLE, SVNRevision.BASE, SVNRevision.BASE);
            SVNPropertyData workingProperty = wcc.doGetProperty(new File(wcs.getBase(), path), SVN_PROPERTY_EXECUTABLE, SVNRevision.WORKING, SVNRevision.WORKING);

            if (baseProperty == null)
            {
                if (workingProperty != null)
                {
                    // Added svn:executable
                    fs.setProperty(FileStatus.PROPERTY_EXECUTABLE, "true");
                }
            }
            else
            {
                if (workingProperty == null)
                {
                    // Removed svn:executable
                    fs.setProperty(FileStatus.PROPERTY_EXECUTABLE, "false");
                }
            }
        }
    }

    private String convertEOLStyle(String eol)
    {
        if (eol.equals("native"))
        {
            return EOLStyle.NATIVE.toString();
        }
        else if (eol.equals("CR"))
        {
            return EOLStyle.CARRIAGE_RETURN.toString();
        }
        else if (eol.equals("CRLF"))
        {
            return EOLStyle.CARRIAGE_RETURN_LINEFEED.toString();
        }
        else if (eol.equals("LF"))
        {
            return EOLStyle.LINEFEED.toString();
        }
        else
        {
            return EOLStyle.BINARY.toString();
        }
    }

    public Revision update(WorkingCopyContext context, Revision revision) throws ScmException
    {
        SVNClientManager clientManager = getClientManager(context, true);

        SVNUpdateClient updateClient = clientManager.getUpdateClient();
        updateClient.setEventHandler(new UpdateHandler());

        try
        {
            SVNRevision svnRevision = revision == null ? SVNRevision.HEAD : SVNRevision.parse(revision.getRevisionString());
            long rev = updateClient.doUpdate(context.getBase(), svnRevision, SVNDepth.INFINITY, false, false);
            return new Revision(Long.toString(rev));
        }
        catch (SVNException e)
        {
            throw convertException(e);
        }
    }

    private File[] pathsToFiles(File base, String... spec) throws ScmException
    {
        if(spec.length == 0)
        {
            return null;
        }

        File[] result = new File[spec.length];
        for(int i = 0; i < spec.length; i++)
        {
            result[i] = new File(base, spec[i]);
            if(!result[i].exists())
            {
                throw new ScmException("File '" + spec[i] + "' does not exist");
            }
        }

        return result;
    }

    private ScmException convertException(SVNException e)
    {
        return new ScmException(e.getMessage(), e);
    }

    private FileStatus convertStatus(File base, ConfigSupport configSupport, SVNStatus svnStatus, List<String> propertyChangedPaths)
    {
        SVNStatusType contentsStatus = svnStatus.getContentsStatus();
        String path = svnStatus.getFile().getPath();
        boolean directory = svnStatus.getKind() == SVNNodeKind.DIR;

        if (path.startsWith(base.getPath()))
        {
            path = path.substring(base.getPath().length());
        }

        if (path.startsWith("/") || path.startsWith(File.separator))
        {
            path = path.substring(1);
        }

        FileStatus.State fileState;

        if (contentsStatus == SVNStatusType.STATUS_NORMAL)
        {
            // CIB-730: unchanged children of moved directories need to be
            // marked as added in our status.
            if(svnStatus.isCopied())
            {
                fileState = FileStatus.State.ADDED;
            }
            else
            {
                fileState = FileStatus.State.UNCHANGED;
            }
        }
        else if (contentsStatus == SVNStatusType.STATUS_ADDED)
        {
            fileState = FileStatus.State.ADDED;
        }
        else if (contentsStatus == SVNStatusType.STATUS_CONFLICTED)
        {
            fileState = FileStatus.State.UNRESOLVED;
        }
        else if (contentsStatus == SVNStatusType.STATUS_DELETED)
        {
            fileState = FileStatus.State.DELETED;
        }
        else if (contentsStatus == SVNStatusType.STATUS_EXTERNAL)
        {
            if(configSupport.getBooleanProperty(PROPERTY_ALLOW_EXTERNALS, false))
            {
                fileState = FileStatus.State.IGNORED;
            }
            else
            {
                fileState = FileStatus.State.UNSUPPORTED;
            }
        }
        else if (contentsStatus == SVNStatusType.STATUS_INCOMPLETE)
        {
            fileState = FileStatus.State.INCOMPLETE;
        }
        else if (contentsStatus == SVNStatusType.MERGED)
        {
            fileState = FileStatus.State.MERGED;
        }
        else if (contentsStatus == SVNStatusType.STATUS_MISSING)
        {
            fileState = FileStatus.State.MISSING;
        }
        else if (contentsStatus == SVNStatusType.STATUS_MODIFIED)
        {
            fileState = FileStatus.State.MODIFIED;
        }
        else if (contentsStatus == SVNStatusType.STATUS_OBSTRUCTED)
        {
            fileState = FileStatus.State.OBSTRUCTED;
        }
        else if (contentsStatus == SVNStatusType.STATUS_REPLACED)
        {
            fileState = FileStatus.State.REPLACED;
        }
        else
        {
            fileState = FileStatus.State.UNCHANGED;
        }

        SVNStatusType propertiesStatus = svnStatus.getPropertiesStatus();
        if (propertiesStatus != SVNStatusType.STATUS_NONE && propertiesStatus != SVNStatusType.UNCHANGED && propertiesStatus != SVNStatusType.STATUS_NORMAL)
        {
            // if we record a property change path, we MUST have an interesting file
            // status to ensure that it is recorded.
            if (!fileState.isInteresting())
            {
                fileState = FileStatus.State.METADATA_MODIFIED;
            }
            propertyChangedPaths.add(path);
        }

        return new FileStatus(path, fileState, directory);
    }

    private class StatusHandler implements ISVNEventHandler, ISVNStatusHandler
    {
        private File base;
        private ConfigSupport configSupport;
        private WorkingCopyStatus status;
        private List<String> propertyChangedPaths = new LinkedList<String>();

        public StatusHandler(WorkingCopyContext context)
        {
            base = context.getBase();
            configSupport = new ConfigSupport(context.getConfig());
            status = new WorkingCopyStatus(base);
        }

        public void handleEvent(SVNEvent event, double progress)
        {
            SVNEventAction action = event.getAction();
            if (action == SVNEventAction.STATUS_COMPLETED)
            {
                getUI().status("Repository revision: " + event.getRevision());
            }
        }

        public void checkCancelled() throws SVNCancelException
        {
        }

        public void handleStatus(SVNStatus svnStatus)
        {
            FileStatus fs = convertStatus(base, configSupport, svnStatus, propertyChangedPaths);
            if (fs.isInteresting())
            {
                getUI().status(fs.toString());
                status.addFileStatus(fs);
            }
        }

        public WorkingCopyStatus getStatus()
        {
            return status;
        }
    }

    private class UpdateHandler implements ISVNEventHandler
    {
        public void handleEvent(SVNEvent event, double progress)
        {
            SVNEventAction action = event.getAction();
            String pathChangeType = " ";
            if (action == SVNEventAction.UPDATE_ADD)
            {
                pathChangeType = "A";
            }
            else if (action == SVNEventAction.UPDATE_DELETE)
            {
                pathChangeType = "D";
            }
            else if (action == SVNEventAction.UPDATE_UPDATE)
            {
                // Find out in detail what state the item is in (after  having  been
                // updated).
                SVNStatusType contentsStatus = event.getContentsStatus();
                if (contentsStatus == SVNStatusType.CHANGED)
                {
                    pathChangeType = "U";
                }
                else if (contentsStatus == SVNStatusType.CONFLICTED)
                {
                    pathChangeType = "C";
                }
                else if (contentsStatus == SVNStatusType.MERGED)
                {
                    pathChangeType = "G";
                }
            }
            else if (action == SVNEventAction.UPDATE_EXTERNAL)
            {
                getUI().status("Fetching external item into '" + event.getFile().getAbsolutePath() + "'");
                getUI().status("External at revision " + event.getRevision());
                return;
            }
            else if (action == SVNEventAction.UPDATE_COMPLETED)
            {
                /*
                * Updating the working copy is completed. Prints out the revision.
                */
                getUI().status("Updated to revision " + event.getRevision());
                return;
            }
            else if (action == SVNEventAction.ADD)
            {
                getUI().status("A     " + event.getFile().getPath());
                return;
            }
            else if (action == SVNEventAction.DELETE)
            {
                getUI().status("D     " + event.getFile().getPath());
                return;
            }
            else if (action == SVNEventAction.LOCKED)
            {
                getUI().status("L     " + event.getFile().getPath());
                return;
            }
            else if (action == SVNEventAction.LOCK_FAILED)
            {
                getUI().status("Failed to lock: " + event.getFile().getPath());
                return;
            }

            // For added, delete or updated files, check the properties
            // status.
            SVNStatusType propertiesStatus = event.getPropertiesStatus();

            String propertiesChangeType = " ";
            if (propertiesStatus == SVNStatusType.CHANGED)
            {
                propertiesChangeType = "U";
            }
            else if (propertiesStatus == SVNStatusType.CONFLICTED)
            {
                propertiesChangeType = "C";
            }
            else if (propertiesStatus == SVNStatusType.MERGED)
            {
                propertiesChangeType = "G";
            }

            // Also get the loack status
            String lockLabel = " ";
            SVNStatusType lockType = event.getLockStatus();

            if (lockType == SVNStatusType.LOCK_UNLOCKED)
            {
                lockLabel = "B";
            }

            String message = pathChangeType + propertiesChangeType + lockLabel + "       " + event.getFile().getPath();
            if(message.trim().length() > 0)
            {
                getUI().status(message);
            }
        }

        /*
        * Should be implemented to check if the current operation is cancelled. If
        * it is, this method should throw an SVNCancelException.
        */
        public void checkCancelled() throws SVNCancelException
        {
        }
    }
}