package com.zutubi.tove.type;

import com.zutubi.tove.annotations.ID;
import com.zutubi.tove.annotations.ReadOnly;
import com.zutubi.tove.annotations.SymbolicName;
import com.zutubi.tove.annotations.Transient;
import com.zutubi.tove.config.api.AbstractConfiguration;
import com.zutubi.tove.config.api.AbstractNamedConfiguration;
import com.zutubi.util.junit.ZutubiTestCase;
import com.zutubi.validation.annotations.Required;

import java.util.List;
import java.util.Map;

/**
 *
 *
 */
public class TypeRegistryTest extends ZutubiTestCase
{
    private TypeRegistry typeRegistry;

    protected void setUp() throws Exception
    {
        super.setUp();

        typeRegistry = new TypeRegistry();
    }

    protected void tearDown() throws Exception
    {
        typeRegistry = null;

        super.tearDown();
    }

    public void testSimpleObject() throws TypeException
    {
        CompositeType type = typeRegistry.register(Mock.class);

        assertTrue(type.hasProperty("name"));
        assertTrue(type.hasProperty("names"));
        assertTrue(type.hasProperty("mock"));
        assertTrue(type.hasProperty("mocks"));
        assertTrue(type.hasProperty("anotherMock"));
    }

    public void testSimpleInterfaceHolder() throws TypeException
    {
        CompositeType type = typeRegistry.register(SimpleInterfaceHolder.class);

        assertTrue(type.hasProperty("simpleInterface"));
        assertEquals(1, type.getProperties().size());
    }

    public void testAnnotations() throws TypeException
    {
        // Note that the registry also gathers meta-annotations.
        CompositeType type = typeRegistry.register(Mock.class);
        assertEquals(1, type.getAnnotations(false).size());
        TypeProperty propertyType = type.getProperty("name");
        assertEquals(3, propertyType.getAnnotations().size());
        propertyType = type.getProperty("mock");
        assertEquals(0, propertyType.getAnnotations().size());
        propertyType = type.getProperty("anotherMock");
        assertEquals(2, propertyType.getAnnotations().size());
    }

    public void testPropertyTypes() throws TypeException
    {
        CompositeType type = typeRegistry.register(Mock.class);

        List<String> mapProperties = type.getPropertyNames(MapType.class);
        assertEquals(1, mapProperties.size());
        assertEquals("mocks", mapProperties.get(0));

        List<String> listProperties = type.getPropertyNames(ListType.class);
        assertEquals(1, listProperties.size());
        assertEquals("names", listProperties.get(0));

        List<String> simpleProperties = type.getPropertyNames(PrimitiveType.class);
        assertEquals(1, simpleProperties.size());
        assertEquals("name", simpleProperties.get(0));

        List<String> nestedProperties = type.getPropertyNames(CompositeType.class);
        assertEquals(2, nestedProperties.size());
        assertTrue(nestedProperties.contains("mock"));
        assertTrue(nestedProperties.contains("anotherMock"));
    }

    public void testRegistration() throws TypeException
    {
        Type type = typeRegistry.register(SimpleObject.class);

        assertEquals(type, typeRegistry.getType(SimpleObject.class));

        // registering the same class a second time will return the original class.
        assertEquals(type, typeRegistry.register(SimpleObject.class));
    }

    public void testRegistrationRequiresSymbolicName()
    {
        try
        {
            typeRegistry.register(InvalidObject.class);
            fail();
        }
        catch (TypeException e)
        {
        }
    }

    public void testReadOnlyFields() throws TypeException
    {
        CompositeType c = typeRegistry.register(ReadOnlyFieldA.class);
        TypeProperty a = c.getProperty("a");
        assertTrue(a.isReadable());
        assertFalse(a.isWriteable());
    }

    public void testReadOnlyFieldViaAnnotation() throws TypeException
    {
        CompositeType c = typeRegistry.register(ReadOnlyFieldB.class);
        TypeProperty b = c.getProperty("b");
        assertTrue(b.isReadable());
        assertFalse(b.isWriteable());
    }

    public void testCanSetNameFieldOnNamedConfigurationToReadOnly() throws TypeException
    {
        CompositeType c = typeRegistry.register(ReadOnlyFieldName.class);
        TypeProperty name = c.getProperty("name");
        assertTrue(name.isReadable());
        assertFalse(name.isWriteable());
    }

    public void testTransientFieldsNotIncluded() throws TypeException
    {
        CompositeType c = typeRegistry.register(TransientFieldA.class);
        assertNull(c.getProperty("a"));
    }

    /**
     * Read only field A defined by the absence of a setter.
     */
    @SymbolicName("readOnlyFieldA")
    public static class ReadOnlyFieldA extends AbstractConfiguration
    {
        private String a;

        public ReadOnlyFieldA()
        {
            
        }

        public ReadOnlyFieldA(String a)
        {
            this.a = a;
        }

        public String getA()
        {
            return a;
        }
    }

    /**
     * Read only field b defined by the presence of the @ReadOnly annotation.
     */
    @SymbolicName("readOnlyFieldB")
    public static class ReadOnlyFieldB extends AbstractConfiguration
    {
        @ReadOnly
        private String b;

        public String getB()
        {
            return b;
        }

        public void setB(String b)
        {
            this.b = b;
        }
    }

    @SymbolicName("transientFieldA")
    public static class TransientFieldA extends AbstractConfiguration
    {
        @Transient
        private String a;

        public String getA()
        {
            return a;
        }

        public void setA(String a)
        {
            this.a = a;
        }
    }

    @SymbolicName("readOnlyFieldName")
    public static class ReadOnlyFieldName extends AbstractNamedConfiguration
    {
        @ReadOnly
        public String getName()
        {
            return super.getName();
        }

        public void setName(String name)
        {
            super.setName(name);
        }
    }

    @SymbolicName("mockName")
    public static class Mock extends AbstractConfiguration
    {
        @ID
        private String name;

        private List<String> names;

        private Mock mock;

        private Mock anotherMock;

        private Map<String, Mock> mocks;

        public String getName()
        {
            return name;
        }

        public void setName(String name)
        {
            this.name = name;
        }

        public List<String> getNames()
        {
            return names;
        }

        public void setNames(List<String> names)
        {
            this.names = names;
        }

        public Map<String, Mock> getMocks()
        {
            return mocks;
        }

        public void setMocks(Map<String, Mock> mocks)
        {
            this.mocks = mocks;
        }

        public Mock getMock()
        {
            return mock;
        }

        public void setMock(Mock mock)
        {
            this.mock = mock;
        }

        @Required()
        public Mock getAnotherMock()
        {
            return anotherMock;
        }

        public void setAnotherMock(Mock anotherMock)
        {
            this.anotherMock = anotherMock;
        }
    }

    @SymbolicName("simpleObject")
    public static class SimpleObject extends AbstractConfiguration
    {
        private String b;

        public String getB()
        {
            return b;
        }

        public void setB(String b)
        {
            this.b = b;
        }
    }

    /**
     * No symbolic name makes this invalid.
     */
    public static class InvalidObject extends AbstractConfiguration
    {
        private String c;

        public String getC()
        {
            return c;
        }

        public void setC(String c)
        {
            this.c = c;
        }
    }

    @SymbolicName("simpleInterface")
    public static interface SimpleInterface
    {
        String getA();
        void setA(String str);
    }

    @SymbolicName("simpleInterfaceHolder")
    public static class SimpleInterfaceHolder extends AbstractConfiguration
    {
        private SimpleInterface simpleInterface;

        public SimpleInterface getSimpleInterface()
        {
            return simpleInterface;
        }

        public void setSimpleInterface(SimpleInterface simpleInterface)
        {
            this.simpleInterface = simpleInterface;
        }
    }

}