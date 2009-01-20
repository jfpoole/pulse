package com.zutubi.pulse.master.cli;

import com.zutubi.pulse.core.cli.HelpCommand;
import com.zutubi.pulse.servercore.cli.AdminCommand;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.xmlrpc.XmlRpcException;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

/**
 * A command to reset a user's password.
 */
public class SetPasswordCommand extends AdminCommand
{
    private String user;
    private String password;

    /**
     * Set the username identifying the account for which the password will be updated.
     * @param username the name of the user for whom the password will be reset
     */
    public void setUser(String username)
    {
        this.user = username;
    }

    /**
     * Set the new password that will be set.
     *
     * @param newPassword the new password
     */
    public void setPassword(String newPassword)
    {
        this.password = newPassword;
    }

    public String getHelp()
    {
        return "reset a user's password";
    }

    public String getDetailedHelp()
    {
        return "Used to reset a user's password, typically because the user has forgotten\n" +
               "their current password.";
    }


    public List<String> getUsages()
    {
        return Arrays.asList("<username> <password>");
    }

    public List<String> getAliases()
    {
        return Arrays.asList("passwd", "password", "sp", "setp");
    }

    public int doExecute(String[] argv) throws XmlRpcException, IOException, ParseException
    {
        CommandLineParser parser = new PosixParser();
        CommandLine commandLine = parser.parse(getSharedOptions(), argv, true);

        String[] args = commandLine.getArgs();
        if(args.length < 2)
        {
            HelpCommand helpCommand = new HelpCommand();
            helpCommand.showHelp("setpassword", this);
            return 1;
        }
        // process the command.
        setUser(args[0]);
        setPassword(args[1]);

        xmlRpcClient.execute("RemoteApi.setPassword", new Vector<Object>(Arrays.asList(
                adminToken, user, password)));
        return 0;
    }

    public static void main(String argv[])
    {
        SetPasswordCommand command = new SetPasswordCommand();
        try
        {
            command.execute(argv);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}