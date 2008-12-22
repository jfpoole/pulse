package com.zutubi.pulse.command;

import org.apache.commons.cli.*;
import org.apache.xmlrpc.XmlRpcException;

import java.io.IOException;
import java.util.*;


/**
 * The shutdown admin command, supports the force argument.
 *
 * @author Daniel Ostermeier
 */
public class ShutdownCommand extends AdminCommand
{
    private boolean force;

    private boolean exitJvm = true;

    public void setForce(boolean b)
    {
        this.force = b;
    }

    public void setExitJvm(boolean exitJvm)
    {
        this.exitJvm = exitJvm;
    }

    @SuppressWarnings({"ACCESS_STATIC_VIA_INSTANCE"})
    private void parse(String... argv) throws ParseException
    {
        Options options = getSharedOptions();
        options.addOption(new Option("F", "force", false, "force shutdown"));

        CommandLineParser parser = new PosixParser();
        CommandLine commandLine = parser.parse(options, argv, true);

        setForce(commandLine.hasOption('F'));
        processSharedOptions(commandLine);
    }

    public String getHelp()
    {
        return "shutdown the pulse server";
    }

    public String getDetailedHelp()
    {
        return "Triggers a shutdown of a local pulse server.  By default, the shutdown is\n" +
               "'clean': i.e. pulse will wait for running builds to complete.  This may be\n" +
               "overriden with the --force option, which causes builds to be aborted.";
    }

    public List<String> getAliases()
    {
        return Arrays.asList(new String[] { "sd", "shut", "stop" });
    }


    public Map<String, String> getOptions()
    {
        Map<String, String> options = new LinkedHashMap<String, String>();
        options.put("-F [--force]", "force an immediate shutdown, aborting running builds");
        options.putAll(super.getOptions());
        return options;
    }

    public int doExecute(BootContext context) throws XmlRpcException, IOException, ParseException
    {
        parse(context.getCommandArgv());
        return execute();
    }

    public int execute() throws XmlRpcException, IOException
    {
        xmlRpcClient.execute("RemoteApi.shutdown", new Vector<Object>(Arrays.asList(new Object[]{adminToken, force, exitJvm})));
        return 0;
    }

    public static void main(String argv[])
    {
        ShutdownCommand command = new ShutdownCommand();
        try
        {
            command.parse(argv);
            command.execute(new BootContext(null, argv, null, null, null));
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}