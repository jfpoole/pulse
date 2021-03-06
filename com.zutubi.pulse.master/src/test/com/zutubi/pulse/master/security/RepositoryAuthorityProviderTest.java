/* Copyright 2017 Zutubi Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.zutubi.pulse.master.security;

import com.google.common.collect.Sets;
import com.zutubi.pulse.core.dependency.RepositoryAttributes;
import com.zutubi.pulse.core.test.api.PulseTestCase;
import com.zutubi.pulse.master.tove.config.admin.RepositoryConfiguration;
import com.zutubi.pulse.master.tove.config.group.UserGroupConfiguration;
import com.zutubi.pulse.master.tove.config.project.ProjectConfiguration;
import com.zutubi.tove.config.ConfigurationProvider;
import com.zutubi.tove.type.record.PathUtils;
import org.eclipse.jetty.http.HttpMethod;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static com.zutubi.pulse.master.tove.config.MasterConfigurationRegistry.PROJECTS_SCOPE;
import static com.zutubi.tove.security.AccessManager.ACTION_VIEW;
import static com.zutubi.tove.security.AccessManager.ACTION_WRITE;
import static org.mockito.Mockito.*;

public class RepositoryAuthorityProviderTest extends PulseTestCase
{
    private RepositoryAuthorityProvider provider;
    private ConfigurationProvider configurationProvider;
    private ProjectConfigurationAuthorityProvider delegateProvider;
    private RepositoryAttributes repositoryAttributes;

    private int handle = 1;
    private RepositoryConfiguration repositoryConfiguration;

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();

        delegateProvider = mock(ProjectConfigurationAuthorityProvider.class);

        configurationProvider = mock(ConfigurationProvider.class);
        repositoryConfiguration = new RepositoryConfiguration();
        stub(configurationProvider.get(RepositoryConfiguration.class)).toReturn(repositoryConfiguration);
        
        repositoryAttributes =  mock(RepositoryAttributes.class);

        provider = new RepositoryAuthorityProvider();
        provider.setConfigurationProvider(configurationProvider);
        provider.setProjectConfigurationAuthorityProvider(delegateProvider);
        provider.setRepositoryAttributes(repositoryAttributes);
    }

    public void testPathWithoutOrg()
    {
        ProjectConfiguration project = newProject(null, "project");
        provider.getAllowedAuthorities(null, newInvocation(HttpMethod.PUT.asString(), "project/ivy.xml"));
        verify(delegateProvider, times(1)).getAllowedAuthorities(ACTION_WRITE, project);
    }

    public void testPathWithMismatchingOrg()
    {
        newProject("orgA", "project");
        provider.getAllowedAuthorities(null, newInvocation(HttpMethod.PUT.asString(), "orgB/project/ivy.xml"));
        verify(delegateProvider, times(0)).getAllowedAuthorities(eq(ACTION_WRITE), (ProjectConfiguration) anyObject());
    }

    public void testPathWithNoMatches()
    {
        provider.getAllowedAuthorities(null, newInvocation(HttpMethod.PUT.asString(), "org/project/ivy.xml"));
        verify(delegateProvider, times(0)).getAllowedAuthorities(eq(ACTION_WRITE), (ProjectConfiguration) anyObject());
    }

    public void testDelegation_WriteRequest()
    {
        ProjectConfiguration project = newProject("org", "project");
        stub(delegateProvider.getAllowedAuthorities(ACTION_WRITE, project)).toReturn(Sets.newHashSet("write"));

        Set<String> allowedAuthorities = provider.getAllowedAuthorities(null, newInvocation(HttpMethod.PUT.asString(), "org/project/ivy.xml"));
        assertEquals(1, allowedAuthorities.size());
        assertTrue(allowedAuthorities.contains("write"));

        verify(delegateProvider, times(1)).getAllowedAuthorities(ACTION_WRITE, project);
    }

    public void testDelegation_ReadRequest()
    {
        ProjectConfiguration project = newProject("org", "project");
        stub(delegateProvider.getAllowedAuthorities(ACTION_VIEW, project)).toReturn(Sets.newHashSet("read"));

        Set<String> allowedAuthorities = provider.getAllowedAuthorities(null, newInvocation(HttpMethod.GET.asString(), "org/project/ivy.xml"));
        assertEquals(1, allowedAuthorities.size());
        assertTrue(allowedAuthorities.contains("read"));

        verify(delegateProvider, times(1)).getAllowedAuthorities(ACTION_VIEW, project);
    }

    public void testUnknown_WriteRequest()
    {
        addDefaultWriteGroup("writers");

        Set<String> allowedAuthorities = provider.getAllowedAuthorities(null, newInvocation(HttpMethod.PUT.asString(), "org/project/ivy.xml"));
        assertEquals(1, allowedAuthorities.size());
        assertTrue(allowedAuthorities.contains("group:writers"));
    }

    public void testUnknown_ReadRequest()
    {
        addDefaultReadGroup("readers");

        Set<String> allowedAuthorities = provider.getAllowedAuthorities(null, newInvocation(HttpMethod.GET.asString(), "org/project/ivy.xml"));
        assertEquals(1, allowedAuthorities.size());
        assertTrue(allowedAuthorities.contains("group:readers"));
    }

    public void testUnknownMethod()
    {
        newProject("org", "project");
        Set<String> allowedAuthorities = provider.getAllowedAuthorities(null, newInvocation("unknown", "org/project/ivy.xml"));
        assertEquals(0, allowedAuthorities.size());
        
        verify(configurationProvider, times(0)).get(RepositoryConfiguration.class);
        verify(delegateProvider, times(0)).getAllowedAuthorities(anyString(), (ProjectConfiguration) anyObject());
    }

    private void addDefaultWriteGroup(String name)
    {
        UserGroupConfiguration group = new UserGroupConfiguration(name);
        repositoryConfiguration.getWriteAccess().add(group);
    }

    private void addDefaultReadGroup(String name)
    {
        UserGroupConfiguration group = new UserGroupConfiguration(name);
        repositoryConfiguration.getReadAccess().add(group);
    }

    private ProjectConfiguration newProject(String org, String name)
    {
        ProjectConfiguration project = new ProjectConfiguration(org, name);
        project.setHandle(handle++);
        stub(configurationProvider.get(PROJECTS_SCOPE + "/" + name, ProjectConfiguration.class)).toReturn(project);
        stub(configurationProvider.get(project.getHandle(), ProjectConfiguration.class)).toReturn(project);
        String repositoryPath = PathUtils.getPath(org == null ? "" : org, name);
        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put(RepositoryAttributes.PROJECT_HANDLE, String.valueOf(project.getHandle()));
        stub(repositoryAttributes.getMergedAttributes(repositoryPath + "/ivy.xml")).toReturn(attributes);

        return project;
    }

    private HttpInvocation newInvocation(String method, String path)
    {
        HttpInvocation invocation = mock(HttpInvocation.class);
        stub(invocation.getPath()).toReturn(path);
        stub(invocation.getMethod()).toReturn(method);
        return invocation;
    }
    
}
