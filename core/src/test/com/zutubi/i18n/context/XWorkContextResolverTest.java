package com.zutubi.i18n.context;

import junit.framework.TestCase;
import com.zutubi.i18n.mock.MockBook;

/**
 * <class-comment/>
 */
public class XWorkContextResolverTest extends TestCase
{
    private XWorkContextResolver resolver;

    protected void setUp() throws Exception
    {
        super.setUp();

        resolver = new XWorkContextResolver();
    }

    protected void tearDown() throws Exception
    {
        resolver = null;

        super.tearDown();
    }

    public void testJavaLangObject()
    {
        String[] resolvedBundleNames = resolver.resolve(new ClassContext(new Object()));
        assertEquals(4, resolvedBundleNames.length);
        assertEquals("java/lang/Object", resolvedBundleNames[0]);
        assertEquals("java/lang/package", resolvedBundleNames[1]);
        assertEquals("java/package", resolvedBundleNames[2]);
        assertEquals("package", resolvedBundleNames[3]);
    }

    public void testMockBook()
    {
        String[] resolvedBundleNames = resolver.resolve(new ClassContext(MockBook.class));
        assertEquals(6, resolvedBundleNames.length);
        assertEquals("com/zutubi/i18n/mock/MockBook", resolvedBundleNames[0]);
        assertEquals("com/zutubi/i18n/mock/package", resolvedBundleNames[1]);
        assertEquals("com/zutubi/i18n/package", resolvedBundleNames[2]);
        assertEquals("com/zutubi/package", resolvedBundleNames[3]);
        assertEquals("com/package", resolvedBundleNames[4]);
        assertEquals("package", resolvedBundleNames[5]);
    }

}