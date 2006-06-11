package com.zutubi.pulse.services;

import com.zutubi.pulse.test.PulseTestCase;
import com.zutubi.pulse.util.FileSystemUtils;
import com.zutubi.pulse.bootstrap.*;

import java.io.File;
import java.io.IOException;

/**
 */
public class ServiceTokenManagerTest extends PulseTestCase
{
    private static final String TEST_TOKEN = "test token string";

    private File tempDir;
    private ServiceTokenManager tokenManager;

    protected void setUp() throws Exception
    {
        super.setUp();
        tempDir = FileSystemUtils.createTempDirectory(ServiceTokenManager.class.getName(), "");
        tokenManager = new ServiceTokenManager();
        DefaultSystemPaths paths = new DefaultSystemPaths(tempDir);
        paths.getConfigRoot().mkdirs();
        tokenManager.setConfigurationManager(new MockConfigurationManager(paths));
    }

    protected void tearDown() throws Exception
    {
        removeDirectory(tempDir);
        super.tearDown();
    }

    public void testGeneratesToken()
    {
        assertNull(tokenManager.getToken());
        tokenManager.init();
        assertNotNull(tokenManager.getToken());
    }

    public void testUsesExistingToken() throws IOException
    {
        assertNull(tokenManager.getToken());
        File tokenFile = tokenManager.getTokenFile();
        FileSystemUtils.createFile(tokenFile, TEST_TOKEN);
        tokenManager.init();
        assertEquals(TEST_TOKEN, tokenManager.getToken());
    }

    public void testAcceptsToken()
    {
        tokenManager.init();
        String token = tokenManager.getToken();
        tokenManager.validateToken(token);
    }

    public void testRejectsInvalidToken()
    {
        tokenManager.init();
        String token = tokenManager.getToken();
        try
        {
            tokenManager.validateToken(token + "invalid");
            fail();
        }
        catch (InvalidTokenException e)
        {
        }
    }

    public void testAcceptsFirstToken()
    {
        tokenManager.setGenerate(false);
        tokenManager.init();
        assertNull(tokenManager.getToken());
        tokenManager.validateToken(TEST_TOKEN);
        assertEquals(TEST_TOKEN, tokenManager.getToken());
    }

    public void testGeneratedTokenPersists()
    {
        tokenManager.init();
        String token = tokenManager.getToken();

        ServiceTokenManager another = new ServiceTokenManager();
        another.setConfigurationManager(new MockConfigurationManager(new DefaultSystemPaths(tempDir)));
        assertNull(another.getToken());
        another.init();
        assertEquals(token, another.getToken());
    }

    public void testAcceptedTokenPersists()
    {
        tokenManager.setGenerate(false);
        tokenManager.init();
        tokenManager.validateToken(TEST_TOKEN);

        ServiceTokenManager another = new ServiceTokenManager();
        another.setConfigurationManager(new MockConfigurationManager(new DefaultSystemPaths(tempDir)));
        assertNull(another.getToken());
        another.init();
        assertEquals(TEST_TOKEN, another.getToken());
    }

    private class MockConfigurationManager implements ConfigurationManager
    {
        private SystemPaths paths;

        public MockConfigurationManager(SystemPaths paths)
        {
            this.paths = paths;
        }

        public ApplicationConfiguration getAppConfig()
        {
            return null;
        }

        public UserPaths getUserPaths()
        {
            return null;
        }

        public SystemPaths getSystemPaths()
        {
            return paths;
        }
    }
}
