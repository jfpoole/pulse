package com.zutubi.pulse.xwork.dispatcher;

import com.opensymphony.webwork.ServletActionContext;
import com.opensymphony.xwork.MockActionInvocation;
import com.opensymphony.xwork.util.OgnlValueStack;
import com.zutubi.pulse.test.PulseTestCase;
import com.mockobjects.servlet.MockHttpServletResponse;

import java.util.List;
import java.util.LinkedList;
import java.io.InputStream;
import java.io.ByteArrayInputStream;

/**
 * <class-comment/>
 */
public class JsonResultTest extends PulseTestCase
{
    private JsonResult result;
    private OgnlValueStack stack;
    private MockHttpServletResponse response;
    private MockActionInvocation ai;

    public JsonResultTest()
    {
    }

    public JsonResultTest(String name)
    {
        super(name);
    }

    protected void setUp() throws Exception
    {
        super.setUp();

        result = new JsonResult();
        result.setLocation("/");
        stack = new OgnlValueStack();
        response = new MockHttpServletResponse()
        {
            private String encoding = "UTF-8";
            public String getCharacterEncoding()
            {
                return encoding;
            }

            public String getContentType()
            {
                return "text/plain";
            }

            public void setCharacterEncoding(String str)
            {
                this.encoding = str;
            }
        };
        ai = new MockActionInvocation();
        ai.setStack(stack);
        ServletActionContext.setResponse(response);
    }

    protected void tearDown() throws Exception
    {
        result = null;
        stack = null;
        response = null;
        ai = null;
        ServletActionContext.setResponse(null);

        super.tearDown();
    }

    public void testSinglePair() throws Exception
    {
        // set location.
        stack.push(new TestDataSource()
        {
            public String getString()
            {
                return "A";
            }
        });
        result.setDefinitionLoader(new SJDL("<?xml version=\"1.0\"?><object><pair key=\"key\">string</pair></object>"));
        result.execute(ai);

        assertEquals("{\"key\":\"A\"}", response.getOutputStreamContents());
    }

    public void testMultiplePairs() throws Exception
    {
        // set location.
        stack.push(new TestDataSource()
        {
            public String getString()
            {
                return "A";
            }
            public String getAnotherString()
            {
                return "B";
            }
        });
        result.setDefinitionLoader(new SJDL("<?xml version=\"1.0\"?>" +
                "<object>" +
                "<pair key=\"key\">string</pair>" +
                "<pair key=\"anotherKey\">anotherString</pair>" +
                "</object>"));
        result.execute(ai);

        assertEquals("{\"key\":\"A\",\"anotherKey\":\"B\"}", response.getOutputStreamContents());
    }

    public void testRetrieveDataFromNestedObject() throws Exception
    {
        // set location.
        stack.push(new TestDataSource()
        {
            public Object getA()
            {
                return new TestDataSource()
                {
                    public String getB()
                    {
                        return "B";
                    }
                };
            }
        });
        result.setDefinitionLoader(new SJDL("<?xml version=\"1.0\"?><object><pair key=\"key\">a.b</pair></object>"));
        result.execute(ai);

        assertEquals("{\"key\":\"B\"}", response.getOutputStreamContents());

    }

    public void testNestedObject() throws Exception
    {
        stack.push(new TestDataSource()
        {
            public Object getA()
            {
                return "A";
            }
        });
        result.setDefinitionLoader(new SJDL("<?xml version=\"1.0\"?><object><pair key=\"key\"><object><pair key=\"nestedKey\">a</pair></object></pair></object>"));
        result.execute(ai);

        assertEquals("{\"key\":{\"nestedKey\":\"A\"}}", response.getOutputStreamContents());
    }

    public void testArrayOfLiteralPrimitivesUsingCollection() throws Exception
    {
        stack.push(new TestDataSource()
        {
            public Object getA()
            {
                List<String> l = new LinkedList<String>();
                l.add("A");
                l.add("B");
                return l;
            }
        });
        result.setDefinitionLoader(new SJDL("<?xml version=\"1.0\"?><object><pair key=\"key\"><array ref=\"a\"/></pair></object>"));
        result.execute(ai);

        assertEquals("{\"key\":[\"A\",\"B\"]}", response.getOutputStreamContents());
    }

    public void testArrayOfLiteralPrimitivesUsingArray() throws Exception
    {
        stack.push(new TestDataSource()
        {
            public Object getA()
            {
                String[] a = new String[2];
                a[0] = "A";
                a[1] = "B";
                return a;
            }
        });
        result.setDefinitionLoader(new SJDL("<?xml version=\"1.0\"?><object><pair key=\"key\"><array ref=\"a\"/></pair></object>"));
        result.execute(ai);

        assertEquals("{\"key\":[\"A\",\"B\"]}", response.getOutputStreamContents());
    }

    public void testArrayOfEvaluatedPrimitives() throws Exception
    {
        stack.push(new TestDataSource()
        {
            public Object getA()
            {
                List<Object> l = new LinkedList<Object>();
                l.add(new ABC("A"));
                l.add(new ABC("B"));
                return l;
            }
        });
        result.setDefinitionLoader(new SJDL("<?xml version=\"1.0\"?><object><pair key=\"key\"><array ref=\"a\">a</array></pair></object>"));
        result.execute(ai);

        assertEquals("{\"key\":[\"A\",\"B\"]}", response.getOutputStreamContents());
    }

    public void testArrayOfObjects() throws Exception
    {
        stack.push(new TestDataSource()
        {
            public Object getA()
            {
                List<Object> l = new LinkedList<Object>();
                l.add(new ABC("A", "a"));
                l.add(new ABC("B", "b"));
                return l;
            }
        });
        result.setDefinitionLoader(new SJDL("<?xml version=\"1.0\"?>" +
                "<object>" +
                    "<pair key=\"key\">" +
                        "<array ref=\"a\">" +
                            "<object>" +
                                "<pair key=\"akey\">a</pair>" +
                                "<pair key=\"bkey\">b</pair>" +
                            "</object>" +
                        "</array>" +
                    "</pair>" +
                "</object>"));
        result.execute(ai);

        assertEquals("{\"key\":[{\"akey\":\"A\",\"bkey\":\"a\"},{\"akey\":\"B\",\"bkey\":\"b\"}]}", response.getOutputStreamContents());
    }

    interface TestDataSource
    {
    }

    class ABC
    {
        private String a;
        private String b;
        private String c;

        public ABC(String a)
        {
            this.a = a;
        }

        public ABC(String a, String b)
        {
            this.a = a;
            this.b = b;
        }

        public ABC(String a, String b, String c)
        {
            this.a = a;
            this.b = b;
            this.c = c;
        }

        public String getA()
        {
            return a;
        }

        public String getB()
        {
            return b;
        }

        public String getC()
        {
            return c;
        }
    }

    class SJDL implements JsonDefinitionLoader
    {
        private String def;
        public SJDL(String def)
        {
            this.def = def;
        }

        public InputStream load(String location)
        {
            return new ByteArrayInputStream(def.getBytes());
        }
    }
}
