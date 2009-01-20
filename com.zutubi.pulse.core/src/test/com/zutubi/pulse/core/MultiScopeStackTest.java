package com.zutubi.pulse.core;

import com.zutubi.pulse.core.engine.api.Property;
import com.zutubi.pulse.core.test.api.PulseTestCase;

import java.util.HashSet;
import java.util.Set;

/**
 */
public class MultiScopeStackTest extends PulseTestCase
{
    private static final String NS1 = "ns1";
    private static final String NS2 = "ns2";
    private static final String NS3 = "ns3";

    private MultiScopeStack stack = new MultiScopeStack(NS1, NS2, NS3);
    private PulseScope scope1;
    private PulseScope scope2;
    private PulseScope scope3;

    protected void setUp() throws Exception
    {
        super.setUp();

        scope1 = stack.getScope(NS1);
        scope1.add(new Property("1", "v1 1"));
        scope1.add(new Property("1 2", "v1 1 2"));
        scope1.add(new Property("1 3", "v1 1 3"));
        scope1.add(new Property("1 2 3", "v1 1 2 3"));

        scope2 = stack.getScope(NS2);
        scope2.add(new Property("2", "v2 2"));
        scope2.add(new Property("1 2", "v2 1 2"));
        scope2.add(new Property("2 3", "v2 2 3"));
        scope2.add(new Property("1 2 3", "v2 1 2 3"));

        scope3 = stack.getScope(NS3);
        scope3.add(new Property("3", "v3 3"));
        scope3.add(new Property("1 3", "v3 1 3"));
        scope3.add(new Property("2 3", "v3 2 3"));
        scope3.add(new Property("1 2 3", "v3 1 2 3"));
    }

    public void testScopeChain()
    {
        assertOriginalProperties(stack);
    }

    public void testGetNamedScope()
    {
        PulseScope scope = stack.getScope(NS1);
        assertEquals(4, scope.getReferences().size());
        assertTrue(scope.containsReference("1"));
        assertTrue(scope.containsReference("1 2"));
        assertTrue(scope.containsReference("1 3"));
        assertTrue(scope.containsReference("1 2 3"));
    }

    public void testGetNamedScopeNoSuchScope()
    {
        try
        {
            stack.getScope("blah");
            fail();
        }
        catch(IllegalArgumentException e)
        {
            assertEquals("No such scope 'blah'", e.getMessage());
        }
    }

    public void testPushPopProperties()
    {
        pushAndAddProperties();
        assertPushedProperties(stack);

        stack.pop();
        
        // All new values gone
        assertNull(stack.getReference("p 1"));
        assertNull(stack.getReference("p 2"));
        assertNull(stack.getReference("p 3"));
        assertNull(stack.getReference("p 1 2"));
        assertNull(stack.getReference("p 1 3"));

        // All original values there
        assertOriginalProperties(stack);
    }

    public void testPushPopScopeChain()
    {
        assertScopeChain(scope3, scope2, scope1);
        stack.push();
        assertScopeChain(stack.getScope(NS3), scope3, stack.getScope(NS2), scope2, stack.getScope(NS1), scope1);
        stack.pop();
        assertScopeChain(scope3, scope2, scope1);
    }

    public void testPopEmpty()
    {
        try
        {
            stack.pop();
            fail();
        }
        catch(IllegalStateException e)
        {
            assertEquals("Attempt to pop an empty stack", e.getMessage());
        }
    }

    public void testPopTo(String label)
    {
        stack.setLabel("foo");
        stack.push();
        stack.add(new Property("a", "av"));
        stack.push();
        stack.add(new Property("b", "bv"));
        stack.popTo("foo");

        assertFalse(stack.containsReference("a"));
        assertFalse(stack.containsReference("b"));
        assertOriginalProperties(stack);
    }

    public void testPopToSame(String label)
    {
        stack.push();
        stack.add(new Property("a", "av"));
        stack.setLabel("foo");
        stack.popTo("foo");

        assertTrue(stack.containsReference("a"));
        assertOriginalProperties(stack);
    }

    public void testPopToNonExistantLabel(String label)
    {
        stack.setLabel("foo");
        stack.push();
        stack.add(new Property("a", "av"));
        stack.push();
        stack.add(new Property("b", "bv"));
        stack.popTo("non");

        assertTrue(stack.containsReference("a"));
        assertTrue(stack.containsReference("b"));
        assertOriginalProperties(stack);
    }

    public void testCopy()
    {
        MultiScopeStack copy = new MultiScopeStack(stack);
        assertOriginalProperties(copy);

        assertCopyIndependent(copy);
    }

    public void testCopyMultiLevel()
    {
        pushAndAddProperties();

        MultiScopeStack copy = new MultiScopeStack(stack);
        assertPushedProperties(copy);

        copy.pop();
        assertOriginalProperties(copy);

        assertCopyIndependent(copy);
    }

    public void testLabel()
    {
        stack.setLabel("test");
        assertEquals("test", stack.getLabel());
        assertEquals("test", scope1.getLabel());
        assertEquals("test", scope2.getLabel());
        assertEquals("test", scope3.getLabel());
    }

    private void pushAndAddProperties()
    {
        stack.push();
        stack.getScope(NS1).add(new Property("1", "v1p 1"));
        stack.getScope(NS1).add(new Property("p 1", "v1p p 1"));
        stack.getScope(NS1).add(new Property("p 1 2", "v1p p 1 2"));
        stack.getScope(NS1).add(new Property("p 1 3", "v1p p 1 3"));
        stack.getScope(NS2).add(new Property("2", "v2p 2"));
        stack.getScope(NS2).add(new Property("p 2", "v2p p 2"));
        stack.getScope(NS2).add(new Property("p 1 2", "v2p p 1 2"));
        stack.getScope(NS3).add(new Property("3", "v3p 3"));
        stack.getScope(NS3).add(new Property("p 3", "v3p p 3"));
        stack.getScope(NS3).add(new Property("p 1 3", "v3p p 1 3"));
    }

    private void assertPushedProperties(MultiScopeStack stack)
    {
        // New values (no override)
        assertValue(stack, "v1p p 1", "p 1");
        assertValue(stack, "v1p p 1", NS1, "p 1");
        assertValue(stack, "v2p p 2", "p 2");
        assertValue(stack, "v2p p 2", NS2, "p 2");
        assertValue(stack, "v3p p 3", "p 3");
        assertValue(stack, "v3p p 3", NS3, "p 3");

        // New values overriding other new values
        assertValue(stack, "v2p p 1 2", "p 1 2");
        assertValue(stack, "v1p p 1 2", NS1, "p 1 2");
        assertValue(stack, "v2p p 1 2", NS2, "p 1 2");
        assertValue(stack, "v2p p 1 2", NS3, "p 1 2");
        assertValue(stack, "v3p p 1 3", "p 1 3");
        assertValue(stack, "v1p p 1 3", NS1, "p 1 3");
        assertValue(stack, "v1p p 1 3", NS2, "p 1 3");
        assertValue(stack, "v3p p 1 3", NS3, "p 1 3");

        // New values overriding existing values
        assertValue(stack, "v1p 1", "1");
        assertValue(stack, "v1p 1", NS1, "1");
        assertValue(stack, "v2p 2", "2");
        assertValue(stack, "v2p 2", NS2, "2");
        assertValue(stack, "v3p 3", "3");
        assertValue(stack, "v3p 3", NS3, "3");

        // Original values
        assertValue(stack, "v3 1 2 3", "1 2 3");
        assertValue(stack, "v1 1 2 3", NS1, "1 2 3");
        assertValue(stack, "v2 1 2 3", NS2, "1 2 3");
        assertValue(stack, "v3 1 2 3", NS3, "1 2 3");
    }

    private void assertCopyIndependent(MultiScopeStack copy)
    {
        stack.getScope(NS1).add(new Property("after copy", "foo"));
        stack.getScope(NS2).add(new Property("after copy", "foo"));
        stack.getScope(NS3).add(new Property("after copy", "foo"));
        assertNull(copy.getReference("after copy"));

        copy.getScope(NS1).add(new Property("in copy", "foo"));
        copy.getScope(NS2).add(new Property("in copy", "foo"));
        copy.getScope(NS3).add(new Property("in copy", "foo"));
        assertNull(stack.getReference("in copy"));
    }

    private void assertScopeChain(PulseScope... expectedScopes)
    {
        Set<PulseScope> seen = new HashSet<PulseScope>();
        PulseScope current = stack.getScope();
        for(PulseScope expected: expectedScopes)
        {
            assertFalse(seen.contains(current));
            seen.add(current);
            assertSame(expected, current);
            current = current.getParent();
        }

        assertNull(current);
    }

    private void assertOriginalProperties(MultiScopeStack stack)
    {
        assertValue(stack, "v1 1", "1");
        assertValue(stack, "v2 2", "2");
        assertValue(stack, "v3 3", "3");
        assertValue(stack, "v2 1 2", "1 2");
        assertValue(stack, "v3 1 3", "1 3");
        assertValue(stack, "v3 2 3", "2 3");
        assertValue(stack, "v3 1 2 3", "1 2 3");
    }

    private void assertValue(MultiScopeStack stack, String expected, String name)
    {
        assertEquals(expected, stack.getScope().getReferenceValue(name, String.class));
    }

    private void assertValue(MultiScopeStack stack, String expected, String namespace, String name)
    {
        assertEquals(expected, stack.getScope(namespace).getReferenceValue(name, String.class));
    }
}