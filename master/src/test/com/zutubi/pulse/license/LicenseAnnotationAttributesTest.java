package com.zutubi.pulse.license;

import com.zutubi.pulse.test.PulseTestCase;
import com.zutubi.pulse.util.Sort;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Comparator;

/**
 * <class-comment/>
 */
public class LicenseAnnotationAttributesTest extends PulseTestCase
{
    private LicenseAnnotationAttributes attributes;

    public LicenseAnnotationAttributesTest()
    {
    }

    public LicenseAnnotationAttributesTest(String name)
    {
        super(name);
    }

    protected void setUp() throws Exception
    {
        super.setUp();

        attributes = new LicenseAnnotationAttributes();

    }

    protected void tearDown() throws Exception
    {
        attributes = null;

        super.tearDown();
    }

    public void testClassLevelAnnotations()
    {
        Object[] attribs = attributes.getAttributes(AnnotatedClass.class).toArray();
        assertEquals(1, attribs.length);
        assertEquals("a", attribs[0]);
    }

    public void testMethodLevelAnnotations() throws NoSuchMethodException
    {
        Method method = AnnotatedClass.class.getMethod("annotatedMethod");
        Object[] attribs = attributes.getAttributes(method).toArray();
        assertEquals(1, attribs.length);
        assertEquals("b", attribs[0]);
    }

    public void testMultipleAnnotations()
    {
        Object[] attribs = attributes.getAttributes(MultipleAnnotatedClass.class).toArray();
        assertEquals(2, attribs.length);
        Comparator comparator = new Sort.StringComparator();
        Arrays.sort(attribs, comparator);
        assertEquals("a", attribs[0]);
        assertEquals("b", attribs[1]);
    }

    @Licensed("a")
    private class AnnotatedClass
    {

        @Licensed("b")
        public void annotatedMethod()
        {

        }
    }

    @Licensed({"a", "b"})
    private class MultipleAnnotatedClass
    {

    }
}