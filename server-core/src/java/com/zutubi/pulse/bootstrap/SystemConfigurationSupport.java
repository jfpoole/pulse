package com.zutubi.pulse.bootstrap;

import com.opensymphony.util.TextUtils;
import com.zutubi.pulse.config.CompositeConfig;
import com.zutubi.pulse.config.Config;
import com.zutubi.pulse.config.ConfigSupport;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * <class-comment/>
 */
public class SystemConfigurationSupport extends ConfigSupport implements SystemConfiguration
{
    public SystemConfigurationSupport(Config... config)
    {
        super(new CompositeConfig(config));
    }

    public String getBindAddress()
    {
        String result = config.getProperty(WEBAPP_BIND_ADDRESS);
        if(TextUtils.stringSet(result))
        {
            return result;
        }
        else
        {
            return "0.0.0.0";
        }
    }

    public int getServerPort()
    {
        return getInteger(WEBAPP_PORT, 8080);
    }

    public String getContextPath()
    {
        return getProperty(CONTEXT_PATH, "/");
    }

    public void setDataPath(String path)
    {
        setProperty(PULSE_DATA, path);
    }

    public String getDataPath()
    {
        return getProperty(PULSE_DATA);
    }

    public String getHostUrl()
    {
        String hostname = "localhost";
        try
        {
            InetAddress address = InetAddress.getLocalHost();
            hostname = address.getCanonicalHostName();
        }
        catch (UnknownHostException e)
        {
            // noop.
        }
        String hostUrl = "http://" + hostname + ":" + getServerPort();
        if (!getContextPath().startsWith("/"))
        {
            hostUrl = hostUrl + "/";
        }
        return hostUrl + getContextPath();
    }

    public boolean isSslEnabled()
    {
        return getBooleanProperty(SSL_ENABLED, false);
    }

    public String getSslKeystore()
    {
        return getProperty(SSL_KEYSTORE);
    }

    public String getSslPassword()
    {
        return getProperty(SSL_PASSWORD);
    }

    public String getSslKeyPassword()
    {
        return getProperty(SSL_KEY_PASSWORD);
    }
}
