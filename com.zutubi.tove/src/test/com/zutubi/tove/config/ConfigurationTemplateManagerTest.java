package com.zutubi.tove.config;

import com.zutubi.tove.annotations.Reference;
import com.zutubi.tove.annotations.SymbolicName;
import com.zutubi.events.AllEventListener;
import com.zutubi.events.Event;
import com.zutubi.tove.config.events.*;
import com.zutubi.tove.config.api.Configuration;
import com.zutubi.tove.config.api.NamedConfiguration;
import com.zutubi.tove.security.*;
import com.zutubi.tove.transaction.UserTransaction;
import com.zutubi.tove.type.CompositeType;
import com.zutubi.tove.type.MapType;
import com.zutubi.tove.type.TemplatedMapType;
import com.zutubi.tove.type.TypeException;
import com.zutubi.tove.type.record.MutableRecord;
import com.zutubi.tove.type.record.PathUtils;
import com.zutubi.tove.type.record.Record;
import com.zutubi.util.Pair;
import com.zutubi.validation.annotations.Required;

import java.util.*;

/**
 *
 *
 */
public class ConfigurationTemplateManagerTest extends AbstractConfigurationSystemTestCase
{
    private CompositeType typeA;
    private CompositeType typeB;

    protected void setUp() throws Exception
    {
        super.setUp();

        typeA = typeRegistry.register(MockA.class);
        typeB = typeRegistry.getType(MockB.class);
        MapType mapA = new MapType(typeA, typeRegistry);

        MapType templatedMap = new TemplatedMapType(typeA, typeRegistry);

        CompositeType typeReferer = typeRegistry.register(MockReferer.class);
        CompositeType typeReferee = typeRegistry.getType(MockReferee.class);
        MapType mapReferer = new MapType(typeReferer, typeRegistry);
        MapType mapReferee = new MapType(typeReferee, typeRegistry);

        configurationPersistenceManager.register("sample", mapA);
        configurationPersistenceManager.register("template", templatedMap);
        configurationPersistenceManager.register("referer", mapReferer);
        configurationPersistenceManager.register("referee", mapReferee);

        accessManager.registerAuthorityProvider(MockA.class, new AuthorityProvider<MockA>()
        {
            public Set<String> getAllowedAuthorities(String action, MockA resource)
            {
                return new HashSet<String>(Arrays.asList(action));
            }
        });
    }

    protected void tearDown() throws Exception
    {
        super.tearDown();
    }

    public void testInsertIntoCollection()
    {
        MockA a = new MockA("a");
        configurationTemplateManager.insert("sample", a);

        MockA loaded = (MockA) configurationTemplateManager.getInstance("sample/a");
        assertNotNull(loaded);
        assertEquals("a", loaded.getName());
        assertEquals(null, loaded.getB());
    }

    public void testInsertIntoObject()
    {
        MockA a = new MockA("a");
        configurationTemplateManager.insert("sample", a);

        MockB b = new MockB("b");
        configurationTemplateManager.insert("sample/a/mock", b);

        MockB loaded = (MockB) configurationTemplateManager.getInstance("sample/a/mock");
        assertNotNull(loaded);
        assertEquals("b", loaded.getB());
    }

    public void testInsertExistingPath()
    {
        MockA a = new MockA("a");
        configurationTemplateManager.insert("sample", a);

        MockB b = new MockB("b");
        configurationTemplateManager.insert("sample/a/mock", b);

        try
        {
            configurationTemplateManager.insert("sample/a/mock", b);
            fail();
        }
        catch(IllegalArgumentException e)
        {
            assertEquals("Invalid insertion path 'sample/a/mock': record already exists (use save to modify)", e.getMessage());
        }
    }

    public void testInsertNoPermission()
    {
        configurationSecurityManager.registerGlobalPermission("sample", AccessManager.ACTION_CREATE, AccessManager.ACTION_CREATE);
        accessManager.setActorProvider(new ActorProvider()
        {
            public Actor getActor()
            {
                return new DefaultActor("test", AccessManager.ACTION_DELETE, AccessManager.ACTION_VIEW, AccessManager.ACTION_WRITE);
            }
        });

        try
        {
            configurationTemplateManager.insert("sample", new MockA("a"));
            fail();
        }
        catch (Exception e)
        {
            assertEquals("Permission to create at path 'sample' denied", e.getMessage());
        }
    }

    public void testSave()
    {
        MockA a = new MockA("a");
        String path = configurationTemplateManager.insert("sample", a);

        a = configurationTemplateManager.getInstance(path, MockA.class);
        a = configurationTemplateManager.deepClone(a);
        a.setB("somevalue");
        configurationTemplateManager.save(a);

        MockA loaded = (MockA) configurationTemplateManager.getInstance("sample/a");
        assertNotNull(loaded);
        assertEquals("a", loaded.getName());
        assertEquals("somevalue", loaded.getB());
    }

    public void testSaveIsDeep()
    {
        MockA a = new MockA("a");
        a.setMock(new MockB("b"));
        String path = configurationTemplateManager.insert("sample", a);

        a = configurationTemplateManager.getInstance(path, MockA.class);
        assertEquals("b", a.getMock().getB());

        a = configurationTemplateManager.deepClone(a);
        a.getMock().setB("c");
        configurationTemplateManager.save(a);

        a = configurationTemplateManager.getInstance(path, MockA.class);
        assertEquals("c", a.getMock().getB());
    }

    public void testSaveInsertsTransitively()
    {
        MockA a = new MockA("a");
        String path = configurationTemplateManager.insert("sample", a);

        a = configurationTemplateManager.getInstance(path, MockA.class);
        assertEquals(0, a.getCs().size());

        a = configurationTemplateManager.deepClone(a);
        MockD d = new MockD("d");
        MockC c = new MockC("c");
        c.setD(d);
        a.getCs().put("c", c);
        configurationTemplateManager.save(a);

        a = configurationTemplateManager.getInstance(path, MockA.class);
        assertEquals(1, a.getCs().size());
        c = a.getCs().get("c");
        assertNotNull(c);
        d = c.getD();
        assertNotNull(d);
        assertEquals("d", d.getName());
    }

    public void testSaveSavesTransitively()
    {
        MockD d = new MockD("d");
        MockC c = new MockC("c");
        c.setD(d);
        MockA a = new MockA("a");
        a.getCs().put("c", c);

        String path = configurationTemplateManager.insert("sample", a);

        a = configurationTemplateManager.getInstance(path, MockA.class);
        assertEquals(1, a.getCs().size());
        c = a.getCs().get("c");
        assertNotNull(c);
        d = c.getD();
        assertNotNull(d);
        assertEquals("d", d.getName());

        a = configurationTemplateManager.deepClone(a);
        a.getCs().get("c").getD().setName("newname");
        configurationTemplateManager.save(a);

        a = configurationTemplateManager.getInstance(path, MockA.class);
        assertEquals("newname", a.getCs().get("c").getD().getName());
    }

    public void testSaveChildObjectAdded()
    {
        MockA a = new MockA("a");
        String path = configurationTemplateManager.insert("sample", a);

        a = configurationTemplateManager.getInstance(path, MockA.class);
        assertNull(a.getMock());

        a = configurationTemplateManager.deepClone(a);
        a.setMock(new MockB("b"));
        configurationTemplateManager.save(a);

        a = configurationTemplateManager.getInstance(path, MockA.class);
        assertNotNull(a.getMock());
        assertEquals("b", a.getMock().getB());
    }

    public void testSaveChildObjectDeleted()
    {
        MockA a = new MockA("a");
        a.setMock(new MockB("b"));
        String path = configurationTemplateManager.insert("sample", a);

        a = configurationTemplateManager.getInstance(path, MockA.class);
        assertNotNull(a.getMock());
        assertEquals("b", a.getMock().getB());

        a = configurationTemplateManager.deepClone(a);
        a.setMock(null);
        configurationTemplateManager.save(a);

        a = configurationTemplateManager.getInstance(path, MockA.class);
        assertNull(a.getMock());
    }

    public void testSaveCollectionElementAdded()
    {
        MockA a = new MockA("a");
        String path = configurationTemplateManager.insert("sample", a);

        a = configurationTemplateManager.getInstance(path, MockA.class);
        assertEquals(0, a.getCs().size());

        a = configurationTemplateManager.deepClone(a);
        a.getCs().put("jim", new MockC("jim"));
        configurationTemplateManager.save(a);

        a = configurationTemplateManager.getInstance(path, MockA.class);
        assertEquals(1, a.getCs().size());
        assertNotNull(a.getCs().get("jim"));
    }

    public void testSaveCollectionElementRemoved()
    {
        MockA a = new MockA("a");
        a.getCs().put("jim", new MockC("jim"));
        String path = configurationTemplateManager.insert("sample", a);

        a = configurationTemplateManager.getInstance(path, MockA.class);
        assertEquals(1, a.getCs().size());
        assertNotNull(a.getCs().get("jim"));

        a = configurationTemplateManager.deepClone(a);
        a.getCs().remove("jim");
        configurationTemplateManager.save(a);

        a = configurationTemplateManager.getInstance(path, MockA.class);
        assertEquals(0, a.getCs().size());
    }

    public void testSaveNoPermission()
    {
        configurationTemplateManager.insert("sample", new MockA("a"));

        accessManager.setActorProvider(new ActorProvider()
        {
            public Actor getActor()
            {
                return new DefaultActor("test", AccessManager.ACTION_CREATE, AccessManager.ACTION_DELETE, AccessManager.ACTION_VIEW);
            }
        });

        try
        {
            configurationTemplateManager.save(configurationTemplateManager.getInstance("sample/a"));

            fail();
        }
        catch (Exception e)
        {
            assertEquals("Permission to write at path 'sample/a' denied", e.getMessage());
        }
    }


    public void testRename()
    {
        MockA a = new MockA("a");
        String path = configurationTemplateManager.insert("sample", a);

        a = configurationTemplateManager.getInstance(path, MockA.class);
        assertNotNull(a);

        // change the ID field, effectively triggering a rename on save.
        a = configurationTemplateManager.deepClone(a);
        a.setName("b");
        configurationTemplateManager.save(a);

        assertNull(configurationTemplateManager.getInstance("sample/a"));

        MockA loaded = (MockA) configurationTemplateManager.getInstance("sample/b");
        assertNotNull(loaded);

        assertEquals("b", loaded.getName());
    }

    public void testAllInsertEventsAreGenerated()
    {
        final List<ConfigurationEvent> events = new LinkedList<ConfigurationEvent>();
        eventManager.register(new com.zutubi.events.EventListener()
        {
            public void handleEvent(Event evt)
            {
                events.add((ConfigurationEvent) evt);
            }

            public Class[] getHandledEvents()
            {
                return new Class[]{ConfigurationEvent.class};
            }
        });

        MockA a = new MockA("a");
        a.setMock(new MockB("b"));

        configurationTemplateManager.insert("sample", a);

        assertEquals(4, events.size());
        assertTrue(events.get(0) instanceof InsertEvent);
        assertEquals("sample/a", events.get(0).getInstance().getConfigurationPath());
        assertTrue(events.get(1) instanceof InsertEvent);
        assertEquals("sample/a/mock", events.get(1).getInstance().getConfigurationPath());
        assertTrue(events.get(2) instanceof PostInsertEvent);
        assertEquals("sample/a", events.get(2).getInstance().getConfigurationPath());
        assertTrue(events.get(3) instanceof PostInsertEvent);
        assertEquals("sample/a/mock", events.get(3).getInstance().getConfigurationPath());
    }

    public void testSaveRecord()
    {
        MutableRecord record = typeA.createNewRecord(true);
        record.put("name", "avalue");
        record.put("b", "bvalue");

        configurationTemplateManager.insertRecord("sample", record);

        record = typeA.createNewRecord(false);
        record.put("name", "avalue");
        record.put("b", "newb");

        configurationTemplateManager.saveRecord("sample/avalue", record);

        Record loaded = configurationTemplateManager.getRecord("sample/avalue");
        assertEquals("newb", loaded.get("b"));
    }

    public void testSaveRecordUnknownPath()
    {
        MutableRecord record = typeA.createNewRecord(false);
        record.put("name", "avalue");
        record.put("b", "newb");

        try
        {
            configurationTemplateManager.saveRecord("sample/avalue", record);
            fail();
        }
        catch (IllegalArgumentException e)
        {
            assertEquals("Illegal path 'sample/avalue': no existing record found", e.getMessage());
        }
    }

    public void testSaveRecordToCollectionPath()
    {
        MutableRecord record = typeA.createNewRecord(false);
        record.put("name", "avalue");
        record.put("b", "newb");

        try
        {
            configurationTemplateManager.saveRecord("sample", record);
            fail();
        }
        catch (IllegalArgumentException e)
        {
            assertEquals("Illegal path 'sample': attempt to save a collection", e.getMessage());
        }
    }

    public void testSaveRecordDoesNotRemoveKeys()
    {
        MutableRecord record = typeA.createNewRecord(true);
        record.put("name", "avalue");
        record.put("b", "bvalue");
        record.put("c", "cvalue");

        configurationTemplateManager.insertRecord("sample", record);

        record = typeA.createNewRecord(false);
        record.put("name", "avalue");
        record.put("b", "newb");
        record.remove("c");
        configurationTemplateManager.saveRecord("sample/avalue", record);

        Record loaded = configurationTemplateManager.getRecord("sample/avalue");
        assertEquals("newb", loaded.get("b"));
        assertEquals("cvalue", loaded.get("c"));
    }

    public void testDeepClone()
    {
        MockA a = new MockA("aburger");
        MockB b = new MockB("bburger");
        a.setMock(b);

        configurationTemplateManager.insert("sample", a);

        a = configurationTemplateManager.getInstance("sample/aburger", MockA.class);
        MockA aClone = configurationTemplateManager.deepClone(a);

        assertNotSame(a, aClone);
        assertNotSame(a.getMock(), aClone.getMock());
        assertEquals(a.getHandle(), aClone.getHandle());
        assertEquals(a.getConfigurationPath(), aClone.getConfigurationPath());
        assertEquals("aburger", aClone.getName());
        assertEquals("bburger", aClone.getMock().getB());
    }

    public void testDeepCloneWithReferences()
    {
        MockReferee ee = new MockReferee("ee");
        configurationTemplateManager.insert("referee", ee);
        ee = configurationTemplateManager.getInstance("referee/ee", MockReferee.class);

        MockReferer er = new MockReferer("er");
        er.setRefToRef(ee);
        er.getRefToRefs().add(ee);

        configurationTemplateManager.insert("referer", er);
        er = configurationTemplateManager.getInstance("referer/er", MockReferer.class);
        ee = configurationTemplateManager.getInstance("referee/ee", MockReferee.class);

        MockReferer clone = configurationTemplateManager.deepClone(er);

        assertNotSame(er, clone);
        assertSame(ee, clone.getRefToRef());
        assertSame(ee, clone.getRefToRefs().get(0));
        assertEquals("er", er.getName());
    }

    public void testDeepCloneWithInternalReference()
    {
        // We need to jump through hoops a bit to set this up.  It is not
        // possible to set up a reference to something until it is saved.
        // Hence we need to insert the referer and nested referee first, and
        // then later we can add references to the nested referee.
        MockReferee ee = new MockReferee("ee");
        MockReferer er = new MockReferer("er");
        er.setRef(ee);
        configurationTemplateManager.insert("referer", er);
        er = configurationTemplateManager.getInstance("referer/er", MockReferer.class);
        er = configurationTemplateManager.deepClone(er);
        ee = er.getRef();
        er.setRefToRef(ee);
        er.getRefToRefs().add(ee);
        configurationTemplateManager.save(er);

        er = configurationTemplateManager.getInstance("referer/er", MockReferer.class);
        ee = er.getRef();
        assertSame(ee, er.getRefToRef());
        assertSame(ee, er.getRefToRefs().get(0));

        // Now we can actually clone and test.
        MockReferer clone = configurationTemplateManager.deepClone(er);
        MockReferee refClone = clone.getRef();

        assertNotSame(er, clone);
        assertNotSame(ee, refClone);
        assertSame(refClone, clone.getRefToRef());
        assertSame(refClone, clone.getRefToRefs().get(0));
    }

    public void testDeepClonePreservesPaths() throws TypeException
    {
        MockA a = new MockA("a");
        a.setMock(new MockB("b"));
        a.getCs().put("c", new MockC("c"));
        a.getDs().add(new MockD("d"));
        configurationTemplateManager.insert("sample", a);

        a = (MockA) configurationTemplateManager.getInstance("sample/a");

        MockA clone = configurationTemplateManager.deepClone(a);
        assertEquals(a.getConfigurationPath(), clone.getConfigurationPath());
        assertEquals(a.getMock().getConfigurationPath(), clone.getMock().getConfigurationPath());
        assertEquals(a.getCs().get("c").getConfigurationPath(), clone.getCs().get("c").getConfigurationPath());
        assertEquals(a.getDs().get(0).getConfigurationPath(), clone.getDs().get(0).getConfigurationPath());
    }

    public void testDeepClonePreservesHandles() throws TypeException
    {
        MockA a = new MockA("a");
        a.setMock(new MockB("b"));
        a.getCs().put("c", new MockC("c"));
        a.getDs().add(new MockD("d"));
        configurationTemplateManager.insert("sample", a);

        a = (MockA) configurationTemplateManager.getInstance("sample/a");

        MockA clone = configurationTemplateManager.deepClone(a);
        assertEquals(a.getHandle(), clone.getHandle());
        assertEquals(a.getMock().getHandle(), clone.getMock().getHandle());
        assertEquals(a.getCs().get("c").getHandle(), clone.getCs().get("c").getHandle());
        assertEquals(a.getDs().get(0).getHandle(), clone.getDs().get(0).getHandle());
    }

    public void testDeepCloneAndSavePreservesMeta() throws TypeException
    {
        MockA a = new MockA("a");
        MutableRecord record = typeA.unstantiate(a);
        record.putMeta("testkey", "value");

        String path = configurationTemplateManager.insertRecord("sample", record);
        Record savedRecord = configurationTemplateManager.getRecord(path);
        assertEquals("value", savedRecord.getMeta("testkey"));

        a = configurationTemplateManager.getInstance(path, MockA.class);
        a = configurationTemplateManager.deepClone(a);
        configurationTemplateManager.save(a);
        
        savedRecord = configurationTemplateManager.getRecord(path);
        assertEquals("value", savedRecord.getMeta("testkey"));
    }

    public void testValidate() throws TypeException
    {
        MutableRecord record = typeA.createNewRecord(true);
        Configuration instance = configurationTemplateManager.validate("template", "a", record, true, false);
        List<String> aErrors = instance.getFieldErrors("name");
        assertEquals(1, aErrors.size());
        assertEquals("name requires a value", aErrors.get(0));
    }

    public void testValidateNullParentPath() throws TypeException
    {
        MutableRecord record = typeA.createNewRecord(true);
        record.put("name", "value");
        Configuration instance = configurationTemplateManager.validate(null, "a", record, true, false);
        assertTrue(instance.isValid());
    }

    public void testValidateNullBaseName() throws TypeException
    {
        MutableRecord record = typeA.createNewRecord(true);
        record.put("name", "value");
        Configuration instance = configurationTemplateManager.validate("template", null, record, true, false);
        assertTrue(instance.isValid());
    }

    public void testValidateTemplate() throws TypeException
    {
        MutableRecord record = typeA.createNewRecord(true);
        record.put("name", "a");
        configurationTemplateManager.markAsTemplate(record);
        configurationTemplateManager.insertRecord("template", record);

        record = typeB.createNewRecord(false);
        MockB instance = configurationTemplateManager.validate("template/a", "mock", record, false, false);
        assertTrue(instance.isValid());
    }

    public void testValidateTemplateIdStillRequired() throws TypeException
    {
        MutableRecord record = typeA.createNewRecord(true);
        configurationTemplateManager.markAsTemplate(record);
        MockA instance = configurationTemplateManager.validate("template", "", record, false, false);
        assertFalse(instance.isValid());
        final List<String> errors = instance.getFieldErrors("name");
        assertEquals(1, errors.size());
        assertEquals("name requires a value", errors.get(0));
    }

    public void testValidateNestedPath() throws TypeException
    {
        MutableRecord record = typeA.createNewRecord(true);
        record.put("name", "a");
        configurationTemplateManager.insertRecord("template", record);

        record = typeB.createNewRecord(true);
        Configuration instance = configurationTemplateManager.validate("template/a", "b", record, true, false);
        List<String> aErrors = instance.getFieldErrors("b");
        assertEquals(1, aErrors.size());
        assertEquals("b requires a value", aErrors.get(0));
    }

    public void testValidateTemplateNestedPath() throws TypeException
    {
        // Check that a record not directly marked us a template is correctly
        // detected as a template for validation (by checking the owner).
        MutableRecord record = typeA.createNewRecord(true);
        record.put("name", "a");
        configurationTemplateManager.markAsTemplate(record);
        configurationTemplateManager.insertRecord("template", record);

        record = typeB.createNewRecord(true);
        MockB instance = configurationTemplateManager.validate("template/a", "b", record, false, false);
        assertTrue(instance.isValid());
        assertNull(instance.getB());
    }

    public void testCachedInstancesAreValidated() throws TypeException
    {
        MockA a = new MockA("a");
        a.setMock(new MockB());
        configurationTemplateManager.insert("sample", a);

        MockB instance = configurationTemplateManager.getInstance("sample/a/mock", MockB.class);
        final List<String> errors = instance.getFieldErrors("b");
        assertEquals(1, errors.size());
        assertEquals("b requires a value", errors.get(0));
    }

    public void testCachedTemplateInstancesAreValidated() throws TypeException
    {
        MutableRecord record = typeA.createNewRecord(true);
        record.put("name", "a$");
        configurationTemplateManager.markAsTemplate(record);
        configurationTemplateManager.insertRecord("template", record);

        MockA instance = configurationTemplateManager.getInstance("template/a$", MockA.class);
        final List<String> errors = instance.getFieldErrors("name");
        assertEquals(1, errors.size());
        assertTrue(errors.get(0), errors.get(0).contains("dollar sign"));
    }

    public void testCachedTemplateInstancesAreValidatedAsTemplates() throws TypeException
    {
        MutableRecord record = typeA.createNewRecord(true);
        record.put("name", "a");
        configurationTemplateManager.markAsTemplate(record);
        configurationTemplateManager.insertRecord("template", record);

        configurationTemplateManager.insert("template/a/mock", new MockB());

        MockB instance = configurationTemplateManager.getInstance("template/a/mock", MockB.class);
        assertTrue(instance.isValid());
    }

    public void testValidateNotDeep() throws TypeException
    {
        MockA a = new MockA("a");
        a.setMock(new MockB());
        MutableRecord record = typeA.unstantiate(a);

        MockA instance = configurationTemplateManager.validate("sample", null, record, true, false);
        assertTrue(instance.isValid());
        assertTrue(instance.getMock().isValid());
    }

    public void testValidateDeep() throws TypeException
    {
        MockA a = new MockA("a");
        a.setMock(new MockB());
        MutableRecord record = typeA.unstantiate(a);

        MockA instance = configurationTemplateManager.validate("sample", null, record, true, true);
        assertTrue(instance.isValid());
        assertFalse(instance.getMock().isValid());
    }

    public void testValidateDeepNestedList() throws TypeException
    {
        MockA a = new MockA("a");
        a.getDs().add(new MockD());
        Record record = typeA.unstantiate(a);

        MockA validated = configurationTemplateManager.validate("sample", null, record, true, true);
        assertTrue(validated.isValid());
        MockD mockD = validated.getDs().get(0);
        assertMissingName(mockD);
    }

    public void testValidateDeepNestedMap() throws TypeException
    {
        MockA a = new MockA("a");
        a.getCs().put("name", new MockC());
        Record record = typeA.unstantiate(a);

        MockA validated = configurationTemplateManager.validate("sample", null, record, true, true);
        assertTrue(validated.isValid());
        MockC mockC = validated.getCs().get("name");
        assertMissingName(mockC);
    }

    public void testIsTemplatedCollection()
    {
        assertFalse(configurationTemplateManager.isTemplatedCollection("sample"));
        assertTrue(configurationTemplateManager.isTemplatedCollection("template"));
    }

    public void testIsTemplatedCollectionUnknownPath()
    {
        assertFalse(configurationTemplateManager.isTemplatedCollection("unknown"));
    }

    public void testIsTemplatedCollectionChildPath()
    {
        assertFalse(configurationTemplateManager.isTemplatedCollection("template/a"));

        MockA a = new MockA("a");
        configurationTemplateManager.insert("template", a);

        assertFalse(configurationTemplateManager.isTemplatedCollection("template/a"));
    }

    public void testCanDeleteInvalidPath()
    {
        assertFalse(configurationTemplateManager.canDelete("skgjkg"));
    }

    public void testCanDeleteScope()
    {
        assertFalse(configurationTemplateManager.canDelete("sample"));
    }

    public void testCanDeletePermanent()
    {
        MockA a = new MockA("mock");
        a.setPermanent(true);
        String path = configurationTemplateManager.insert("sample", a);
        assertFalse(configurationTemplateManager.canDelete(path));
    }

    public void testCanDeleteInheritedComposite() throws TypeException
    {
        assertFalse(configurationTemplateManager.canDelete(PathUtils.getPath(insertInherited(), "mock")));
    }

    public void testCanDeleteSimple()
    {
        MockA a = new MockA("mock");
        String path = configurationTemplateManager.insert("sample", a);
        assertTrue(configurationTemplateManager.canDelete(path));
    }

    public void testCanDeleteCompositeChild()
    {
        MockA a = new MockA("mock");
        a.setMock(new MockB("b"));
        String path = configurationTemplateManager.insert("sample", a);
        assertTrue(configurationTemplateManager.canDelete(PathUtils.getPath(path, "mock")));
    }

    public void testCanDeleteMapItem()
    {
        MockA a = new MockA("mock");
        a.getCs().put("cee", new MockC("cee"));
        String path = configurationTemplateManager.insert("sample", a);
        assertTrue(configurationTemplateManager.canDelete(PathUtils.getPath(path, "cs/cee")));
    }

    public void testCanDeleteOwnedComposite() throws TypeException
    {
        insertInherited();
        assertTrue(configurationTemplateManager.canDelete("template/mock/mock"));
    }
    
    public void testCanDeleteInheritedMapItem() throws TypeException
    {
        assertTrue(configurationTemplateManager.canDelete(PathUtils.getPath(insertInherited(), "cs/cee")));
    }

    private String insertInherited() throws TypeException
    {
        MockA a = new MockA("mock");
        a.setMock(new MockB("bee"));
        a.getCs().put("cee", new MockC("cee"));
        MutableRecord record = typeA.unstantiate(a);
        configurationTemplateManager.markAsTemplate(record);
        String path = configurationTemplateManager.insertRecord("template", record);

        record = typeA.unstantiate(new MockA("child"));
        configurationTemplateManager.setParentTemplate(record, configurationReferenceManager.getHandleForPath(path));
        path = configurationTemplateManager.insertRecord("template", record);
        return path;
    }

    public void testDelete()
    {
        MockA a = new MockA("mock");
        String path = configurationTemplateManager.insert("sample", a);

        configurationTemplateManager.delete(path);

        // Are both record and instance gone?
        assertNoSuchPath(path);
        assertEmptyMap("sample");
    }

    public void testDeletePermanent()
    {
        try
        {
            MockA a = new MockA("mock");
            a.setPermanent(true);
            String path = configurationTemplateManager.insert("sample", a);

            configurationTemplateManager.delete(path);
            fail();
        }
        catch(IllegalArgumentException e)
        {
            assertEquals("Cannot delete instance at path 'sample/mock': marked permanent", e.getMessage());
        }
    }

    public void testDeleteNoPermission()
    {
        configurationTemplateManager.insert("sample", new MockA("a"));
        configurationSecurityManager.registerGlobalPermission("sample/*", AccessManager.ACTION_DELETE, AccessManager.ACTION_DELETE);
        accessManager.setActorProvider(new ActorProvider()
        {
            public Actor getActor()
            {
                return new DefaultActor("test", AccessManager.ACTION_CREATE, AccessManager.ACTION_VIEW, AccessManager.ACTION_WRITE);
            }
        });

        try
        {
            configurationTemplateManager.delete("sample/a");
            fail();
        }
        catch (Exception e)
        {
            assertEquals("Permission to delete at path 'sample/a' denied", e.getMessage());
        }
    }

    public void testDeleteNonExistant()
    {
        try
        {
            configurationTemplateManager.delete("sample/nope");
            fail();
        }
        catch(IllegalArgumentException e)
        {
            assertEquals("No such path 'sample/nope'", e.getMessage());
        }
    }

    public void testDeleteAllTrivial()
    {
        MockA a = new MockA("mock");
        String path = configurationTemplateManager.insert("sample", a);

        assertEquals(1, configurationTemplateManager.deleteAll(path));

        // Are both record and instance gone?
        assertNoSuchPath(path);
        assertEmptyMap("sample");
    }

    public void testDeleteAllNoMatches()
    {
        assertEquals(0, configurationTemplateManager.deleteAll("sample/none"));
    }

    public void testDeleteAllMultipleMatches()
    {
        MockA a1 = new MockA("a1");
        MockA a2 = new MockA("a2");
        String path1 = configurationTemplateManager.insert("sample", a1);
        String path2 = configurationTemplateManager.insert("sample", a2);

        assertEquals(2, configurationTemplateManager.deleteAll("sample/*"));

        assertNoSuchPath(path1);
        assertNoSuchPath(path2);
        assertEmptyMap("sample");
    }

    public void testDeleteListItem()
    {
        MockA a = new MockA("a");
        MockD d1 = new MockD("d1");
        MockD d2 = new MockD("d2");
        a.getDs().add(d1);
        a.getDs().add(d2);

        String path = configurationTemplateManager.insert("sample", a);
        a = configurationTemplateManager.getInstance(path, MockA.class);
        assertEquals(2, a.getDs().size());
        assertEquals("d1", a.getDs().get(0).getName());
        assertEquals("d2", a.getDs().get(1).getName());

        configurationTemplateManager.delete(a.getDs().get(0).getConfigurationPath());
        a = configurationTemplateManager.getInstance(path, MockA.class);
        assertEquals(1, a.getDs().size());
        assertEquals("d2", a.getDs().get(0).getName());

        Record aRecord = configurationTemplateManager.getRecord(path);
        Record dsRecord = (Record) aRecord.get("ds");
        assertEquals(1, dsRecord.size());
        Record d2Record = (Record) dsRecord.get(dsRecord.keySet().iterator().next());
        assertEquals("d2", d2Record.get("name"));
    }

    public void testDeleteListItemFromTemplateChild() throws TypeException
    {
        MockA parent = new MockA("parent");
        MockA child = new MockA("child");
        MockD parentD = new MockD("pd");
        MockD childD = new MockD("cd");
        parent.getDs().add(parentD);
        child.getDs().add(childD);

        MutableRecord parentRecord = typeA.unstantiate(parent);
        configurationTemplateManager.markAsTemplate(parentRecord);
        String parentPath = configurationTemplateManager.insertRecord("template", parentRecord);

        Record loadedParent = configurationTemplateManager.getRecord(parentPath);

        MutableRecord childRecord = typeA.unstantiate(child);
        configurationTemplateManager.setParentTemplate(childRecord, loadedParent.getHandle());
        String childPath = configurationTemplateManager.insertRecord("template", childRecord);

        child = configurationTemplateManager.getInstance(childPath, MockA.class);
        assertEquals(2, child.getDs().size());
        assertEquals("pd", child.getDs().get(0).getName());
        assertEquals("cd", child.getDs().get(1).getName());

        configurationTemplateManager.delete(child.getDs().get(1).getConfigurationPath());
        child = configurationTemplateManager.getInstance(childPath, MockA.class);
        assertEquals(1, child.getDs().size());
        assertEquals("pd", child.getDs().get(0).getName());

        Record loadedChild = configurationTemplateManager.getRecord(childPath);
        Record dsRecord = (Record) loadedChild.get("ds");
        assertEquals(1, dsRecord.size());
        Record d2Record = (Record) dsRecord.get(dsRecord.keySet().iterator().next());
        assertEquals("pd", d2Record.get("name"));
    }

    public void testPathExistsEmptyPath()
    {
        assertFalse(configurationTemplateManager.pathExists(""));
    }

    public void testPathExistsNonExistantScope()
    {
        assertFalse(configurationTemplateManager.pathExists("nosuchscope"));
    }

    public void testPathExistsScopeExistsPathDoesnt()
    {
        assertFalse(configurationTemplateManager.pathExists("sample/nosuchpath"));
    }

    public void testPathExistsExistantScope()
    {
        assertTrue(configurationTemplateManager.pathExists("sample"));
    }

    public void testPathExistsExistantPath()
    {
        MockA a = new MockA("mock");
        String path = configurationTemplateManager.insert("sample", a);
        assertTrue(configurationTemplateManager.pathExists(path));
    }

    public void testEventGeneratedOnSave()
    {
        MockA a = new MockA("a");
        configurationTemplateManager.insert("sample", a);

        RecordingEventListener listener = new RecordingEventListener();
        eventManager.register(listener);

        assertEquals(0, listener.getEvents().size());

        MockA instance = (MockA) configurationTemplateManager.getInstance("sample/a");
        instance.setB("B");

        configurationTemplateManager.save(instance);

        assertEquals(2, listener.getEvents().size());
        Event evt = listener.getEvents().get(0);
        assertTrue(evt instanceof SaveEvent);
        evt = listener.getEvents().get(1);
        assertTrue(evt instanceof PostSaveEvent);
    }

    public void testEventsGeneratedOnInsert()
    {
        RecordingEventListener listener = new RecordingEventListener();
        eventManager.register(listener);

        MockA a = new MockA("a");
        configurationTemplateManager.insert("sample", a);

        assertEquals(2, listener.getEvents().size());
        Event evt = listener.getEvents().get(0);
        assertTrue(evt instanceof InsertEvent);
        evt = listener.getEvents().get(1);
        assertTrue(evt instanceof PostInsertEvent);
    }

    public void testEventsGeneratedOnDelete()
    {
        MockA a = new MockA("a");
        configurationTemplateManager.insert("sample", a);

        RecordingEventListener listener = new RecordingEventListener();
        eventManager.register(listener);

        assertEquals(0, listener.getEvents().size());

        configurationTemplateManager.delete("sample/a");

        assertEquals(2, listener.getEvents().size());
        Event evt = listener.getEvents().get(0);
        assertTrue(evt instanceof DeleteEvent);
        evt = listener.getEvents().get(1);
        assertTrue(evt instanceof PostDeleteEvent);
    }

    public void testEventsArePublishedOnPostCommit()
    {
        UserTransaction transaction = new UserTransaction(transactionManager);
        transaction.begin();

        RecordingEventListener listener = new RecordingEventListener();
        eventManager.register(listener);

        MockA a = new MockA("a");
        configurationTemplateManager.insert("sample", a);

        assertEquals(1, listener.getEvents().size());

        transaction.commit();

        assertEquals(2, listener.getEvents().size());
    }

    public void testEventsAreNotPublishedOnPostRollback()
    {
        UserTransaction transaction = new UserTransaction(transactionManager);
        transaction.begin();

        RecordingEventListener listener = new RecordingEventListener();
        eventManager.register(listener);

        MockA a = new MockA("a");
        configurationTemplateManager.insert("sample", a);

        assertEquals(1, listener.getEvents().size());

        transaction.rollback();

        assertEquals(1, listener.getEvents().size());
    }

    public void testInstanceCacheAwareOfRollback()
    {
        UserTransaction transaction = new UserTransaction(transactionManager);
        transaction.begin();

        MockA a = new MockA("a");
        configurationTemplateManager.insert("sample", a);

        assertNotNull(configurationTemplateManager.getInstance("sample/a"));

        transaction.rollback();

        assertNull(configurationTemplateManager.getInstance("sample/a"));
    }

    public void testInstanceCacheThreadIsolation()
    {
        UserTransaction transaction = new UserTransaction(transactionManager);
        transaction.begin();

        MockA a = new MockA("a");
        configurationTemplateManager.insert("sample", a);

        assertNotNull(configurationTemplateManager.getInstance("sample/a"));

        executeOnSeparateThreadAndWait(new Runnable()
        {
            public void run()
            {
                assertNull(configurationTemplateManager.getInstance("sample/a"));
            }
        });

        transaction.commit();

        executeOnSeparateThreadAndWait(new Runnable()
        {
            public void run()
            {
                assertNotNull(configurationTemplateManager.getInstance("sample/a"));
            }
        });
    }

    public void testInTemplateParentInvalidPath()
    {
        assertFalse(configurationTemplateManager.existsInTemplateParent("there/is/no/such/path"));
    }

    public void testInTemplateParentEmptyPath()
    {
        assertFalse(configurationTemplateManager.existsInTemplateParent(""));
    }

    public void testInTemplateParentScope()
    {
        assertFalse(configurationTemplateManager.existsInTemplateParent("sample"));
    }

    public void testInTemplateParentTemplatedScope()
    {
        assertFalse(configurationTemplateManager.existsInTemplateParent("template"));
    }

    public void testInTemplateParentRegularRecord()
    {
        MockA a = new MockA("a");
        a.setMock(new MockB("b"));
        String path = configurationTemplateManager.insert("sample", a);
        assertFalse(configurationTemplateManager.existsInTemplateParent(PathUtils.getPath(path, "mock")));
    }

    public void testInTemplateParentTemplateRecord()
    {
        MockA a = new MockA("a");
        a.setMock(new MockB("b"));
        String path = configurationTemplateManager.insert("template", a);
        assertFalse(configurationTemplateManager.existsInTemplateParent(PathUtils.getPath(path, "mock")));
    }

    public void testInTemplateParentInheritedComposite() throws TypeException
    {
        MockA parent = new MockA("parent");
        parent.setMock(new MockB("inheritme"));
        MockA child = new MockA("child");
        String childPath = insertParentAndChildA(parent, child).second;
        assertTrue(configurationTemplateManager.existsInTemplateParent(PathUtils.getPath(childPath, "mock")));
    }

    public void testInTemplateParentOwnedComposite() throws TypeException
    {
        MockA parent = new MockA("parent");
        MockA child = new MockA("child");
        child.setMock(new MockB("ownme"));
        String childPath = insertParentAndChildA(parent, child).second;
        assertFalse(configurationTemplateManager.existsInTemplateParent(PathUtils.getPath(childPath, "mock")));
    }

    public void testInTemplateParentOverriddenComposite() throws TypeException
    {
        MockA parent = new MockA("parent");
        parent.setMock(new MockB("overrideme"));
        MockA child = new MockA("child");
        MockB b = new MockB("overrideme");
        b.setB("hehe");
        child.setMock(b);
        String childPath = insertParentAndChildA(parent, child).second;
        assertTrue(configurationTemplateManager.existsInTemplateParent(PathUtils.getPath(childPath, "mock")));
    }

    public void testInTemplateParentInheritedValue() throws TypeException
    {
        MockA parent = new MockA("parent");
        parent.setB("pb");
        MockA child = new MockA("child");
        String childPath = insertParentAndChildA(parent, child).second;
        assertTrue(configurationTemplateManager.existsInTemplateParent(PathUtils.getPath(childPath, "b")));
    }

    public void testInTemplateParentOwnedValue() throws TypeException
    {
        MockA parent = new MockA("parent");
        parent.setB("pb");
        MockA child = new MockA("child");
        child.setB("cb");
        String childPath = insertParentAndChildA(parent, child).second;
        assertTrue(configurationTemplateManager.existsInTemplateParent(PathUtils.getPath(childPath, "b")));
    }

    public void testIsOverriddenInvalidPath()
    {
        assertFalse(configurationTemplateManager.isOverridden("there/is/no/such/path"));
    }

    public void testIsOverriddenEmptyPath()
    {
        assertFalse(configurationTemplateManager.isOverridden(""));
    }

    public void testIsOverriddenScope()
    {
        assertFalse(configurationTemplateManager.isOverridden("sample"));
    }

    public void testIsOverriddenTemplatedScope()
    {
        assertFalse(configurationTemplateManager.isOverridden("template"));
    }

    public void testIsOverriddenRegularRecord()
    {
        MockA a = new MockA("a");
        a.setMock(new MockB("b"));
        String path = configurationTemplateManager.insert("sample", a);
        assertFalse(configurationTemplateManager.isOverridden(PathUtils.getPath(path, "mock")));
    }

    public void testIsOverriddenTemplateRecord()
    {
        MockA a = new MockA("a");
        a.setMock(new MockB("b"));
        String path = configurationTemplateManager.insert("template", a);
        assertFalse(configurationTemplateManager.isOverridden(PathUtils.getPath(path, "mock")));
    }

    public void testIsOverriddenInheritedComposite() throws TypeException
    {
        MockA parent = new MockA("parent");
        parent.setMock(new MockB("inheritme"));
        MockA child = new MockA("child");
        Pair<String, String> paths = insertParentAndChildA(parent, child);
        assertFalse(configurationTemplateManager.isOverridden(PathUtils.getPath(paths.first, "mock")));
        assertFalse(configurationTemplateManager.isOverridden(PathUtils.getPath(paths.second, "mock")));
    }

    public void testIsOverriddenOwnedComposite() throws TypeException
    {
        MockA parent = new MockA("parent");
        MockA child = new MockA("child");
        child.setMock(new MockB("ownme"));
        Pair<String, String> paths = insertParentAndChildA(parent, child);
        assertTrue(configurationTemplateManager.isOverridden(PathUtils.getPath(paths.first, "mock")));
        assertFalse(configurationTemplateManager.isOverridden(PathUtils.getPath(paths.second, "mock")));
    }

    public void testIsOverriddenOverriddenComposite() throws TypeException
    {
        MockA parent = new MockA("parent");
        parent.setMock(new MockB("overrideme"));
        MockA child = new MockA("child");
        MockB b = new MockB("overrideme");
        b.setB("hehe");
        child.setMock(b);
        Pair<String, String> paths = insertParentAndChildA(parent, child);
        assertTrue(configurationTemplateManager.isOverridden(PathUtils.getPath(paths.first, "mock")));
        assertFalse(configurationTemplateManager.isOverridden(PathUtils.getPath(paths.second, "mock")));
    }

    public void testIsOverriddenInheritedValue() throws TypeException
    {
        MockA parent = new MockA("parent");
        parent.setB("pb");
        MockA child = new MockA("child");
        Pair<String, String> paths = insertParentAndChildA(parent, child);
        assertFalse(configurationTemplateManager.isOverridden(PathUtils.getPath(paths.first, "b")));
        assertFalse(configurationTemplateManager.isOverridden(PathUtils.getPath(paths.second, "b")));
    }

    public void testIsOverriddenOverriddenValue() throws TypeException
    {
        MockA parent = new MockA("parent");
        parent.setB("pb");
        MockA child = new MockA("child");
        child.setB("cb");
        Pair<String, String> paths = insertParentAndChildA(parent, child);
        assertTrue(configurationTemplateManager.isOverridden(PathUtils.getPath(paths.first, "b")));
        assertFalse(configurationTemplateManager.isOverridden(PathUtils.getPath(paths.second, "b")));
    }

    public void testIsOverriddenOverriddenInGrandchild() throws TypeException
    {
        MockA parent = new MockA("parent");
        parent.setB("pb");
        MockA child = new MockA("child");
        MockA grandchild = new MockA("grandchild");
        grandchild.setB("cb");
        String[] paths = insertParentChildAndGrandchildA(parent, child, grandchild);
        assertTrue(configurationTemplateManager.isOverridden(PathUtils.getPath(paths[0], "b")));
        assertTrue(configurationTemplateManager.isOverridden(PathUtils.getPath(paths[1], "b")));
        assertFalse(configurationTemplateManager.isOverridden(PathUtils.getPath(paths[2], "b")));
    }

    public void testIsOverriddenCompositeOverriddenInGrandchild() throws TypeException
    {
        MockA parent = new MockA("parent");
        parent.setMock(new MockB("overrideme"));
        MockA child = new MockA("child");
        MockA grandchild = new MockA("grandchild");
        MockB b = new MockB("overrideme");
        b.setB("hehe");
        grandchild.setMock(b);
        String[] paths = insertParentChildAndGrandchildA(parent, child, grandchild);
        assertTrue(configurationTemplateManager.isOverridden(PathUtils.getPath(paths[0], "mock")));
        assertTrue(configurationTemplateManager.isOverridden(PathUtils.getPath(paths[1], "mock")));
        assertFalse(configurationTemplateManager.isOverridden(PathUtils.getPath(paths[2], "mock")));
    }

    public void testIsOverriddenCompositeHiddenInChild() throws TypeException
    {
        MockA parent = new MockA("parent");
        parent.getCs().put("hideme", new MockC("hideme"));
        MockA child = new MockA("child");
        MockA grandchild = new MockA("grandchild");
        String[] paths = insertParentChildAndGrandchildA(parent, child, grandchild);

        configurationTemplateManager.delete(PathUtils.getPath(paths[1], "cs", "hideme"));

        assertFalse(configurationTemplateManager.isOverridden(PathUtils.getPath(paths[0], "cs", "hideme")));
        assertFalse(configurationTemplateManager.isOverridden(PathUtils.getPath(paths[1], "cs", "hideme")));
        assertFalse(configurationTemplateManager.isOverridden(PathUtils.getPath(paths[2], "cs", "hideme")));
    }

    private Pair<String, String> insertParentAndChildA(MockA parent, MockA child) throws TypeException
    {
        MutableRecord record = typeA.unstantiate(parent);
        configurationTemplateManager.markAsTemplate(record);
        String parentPath = configurationTemplateManager.insertRecord("template", record);
        long parentHandle = configurationReferenceManager.getHandleForPath(parentPath);

        record = typeA.unstantiate(child);
        configurationTemplateManager.setParentTemplate(record, parentHandle);
        return new Pair(parentPath, configurationTemplateManager.insertRecord("template", record));
    }

    private String[] insertParentChildAndGrandchildA(MockA parent, MockA child, MockA grandchild) throws TypeException
    {
        MutableRecord record = typeA.unstantiate(parent);
        configurationTemplateManager.markAsTemplate(record);
        String parentPath = configurationTemplateManager.insertRecord("template", record);
        long parentHandle = configurationReferenceManager.getHandleForPath(parentPath);

        record = typeA.unstantiate(child);
        configurationTemplateManager.markAsTemplate(record);
        configurationTemplateManager.setParentTemplate(record, parentHandle);
        String childPath = configurationTemplateManager.insertRecord("template", record);
        long childHandle = configurationReferenceManager.getHandleForPath(childPath);

        record = typeA.unstantiate(grandchild);
        configurationTemplateManager.setParentTemplate(record, childHandle);
        String grandChildPath = configurationTemplateManager.insertRecord("template", record);
        return new String[]{parentPath, childPath, grandChildPath};
    }

    private void assertNoSuchPath(String path)
    {
        assertFalse(configurationTemplateManager.pathExists(path));
        assertNull(configurationTemplateManager.getRecord(path));
        assertNull(configurationTemplateManager.getInstance(path));
    }

    private void assertEmptyMap(String path)
    {
        // Are they removed from the parent record and instance?
        assertEquals(0, configurationTemplateManager.getRecord(path).size());
        assertEquals(0, ((Map) configurationTemplateManager.getInstance(path)).size());
    }

    private void assertMissingName(NamedConfiguration instance)
    {
        assertFalse(instance.isValid());
        List<String> fieldErrors = instance.getFieldErrors("name");
        assertEquals(1, fieldErrors.size());
        assertEquals("name requires a value", fieldErrors.get(0));
    }

    @SymbolicName("mockA")
    public static class MockA extends AbstractNamedConfiguration
    {
        private String b;
        private String c;

        private MockB mock;
        private Map<String, MockC> cs = new HashMap<String, MockC>();
        private List<MockD> ds = new LinkedList<MockD>();
        private List<String> pl = new LinkedList<String>();

        public MockA()
        {
        }

        public MockA(String name)
        {
            super(name);
        }

        public String getB()
        {
            return b;
        }

        public void setB(String b)
        {
            this.b = b;
        }

        public String getC()
        {
            return c;
        }

        public void setC(String c)
        {
            this.c = c;
        }

        public MockB getMock()
        {
            return mock;
        }

        public void setMock(MockB mock)
        {
            this.mock = mock;
        }

        public Map<String, MockC> getCs()
        {
            return cs;
        }

        public void setCs(Map<String, MockC> cs)
        {
            this.cs = cs;
        }

        public List<MockD> getDs()
        {
            return ds;
        }

        public void setDs(List<MockD> ds)
        {
            this.ds = ds;
        }

        public List<String> getPl()
        {
            return pl;
        }

        public void setPl(List<String> pl)
        {
            this.pl = pl;
        }
    }

    @SymbolicName("mockB")
    public static class MockB extends AbstractConfiguration
    {
        @Required
        private String b;

        public MockB()
        {
        }

        public MockB(String b)
        {
            this.b = b;
        }

        public String getB()
        {
            return b;
        }

        public void setB(String b)
        {
            this.b = b;
        }
    }

    @SymbolicName("mockC")
    public static class MockC extends AbstractNamedConfiguration
    {
        private MockD d;

        public MockC()
        {
        }

        public MockC(String name)
        {
            super(name);
        }

        public MockD getD()
        {
            return d;
        }

        public void setD(MockD d)
        {
            this.d = d;
        }
    }

    @SymbolicName("mockD")
    public static class MockD extends AbstractNamedConfiguration
    {
        public MockD()
        {
        }

        public MockD(String name)
        {
            super(name);
        }
    }

    @SymbolicName("mockReferer")
    public static class MockReferer extends AbstractNamedConfiguration
    {
        MockReferee ref;
        @Reference
        MockReferee refToRef;
        @Reference
        List<MockReferee> refToRefs = new LinkedList<MockReferee>();

        public MockReferer()
        {
        }

        public MockReferer(String name)
        {
            super(name);
        }

        public MockReferee getRef()
        {
            return ref;
        }

        public void setRef(MockReferee ref)
        {
            this.ref = ref;
        }

        public MockReferee getRefToRef()
        {
            return refToRef;
        }

        public void setRefToRef(MockReferee refToRef)
        {
            this.refToRef = refToRef;
        }

        public List<MockReferee> getRefToRefs()
        {
            return refToRefs;
        }

        public void setRefToRefs(List<MockReferee> refToRefs)
        {
            this.refToRefs = refToRefs;
        }
    }

    @SymbolicName("mockReferee")
    public static class MockReferee extends AbstractNamedConfiguration
    {
        public MockReferee()
        {
        }

        public MockReferee(String name)
        {
            super(name);
        }
    }

    private class RecordingEventListener extends AllEventListener
    {
        private List<Event> events = new LinkedList<Event>();

        public void handleEvent(Event evt)
        {
            events.add(evt);
        }

        public List<Event> getEvents()
        {
            return events;
        }
    }
}
