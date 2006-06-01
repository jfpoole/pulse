package com.zutubi.pulse.core;

import com.zutubi.pulse.core.model.*;
import nu.xom.*;

import java.util.Map;
import java.util.TreeMap;

/**
 */
public class CppUnitReportPostProcessor extends XMLReportPostProcessor
{
    private static final String ELEMENT_SUCCESSFUL_TESTS = "SuccessfulTests";
    private static final String ELEMENT_TEST = "Test";
    private static final String ELEMENT_FAILED_TESTS = "FailedTests";
    private static final String ELEMENT_FAILED_TEST = "FailedTest";
    private static final String ELEMENT_NAME = "Name";
    private static final String ELEMENT_FAILURE_TYPE = "FailureType";
    private static final String ELEMENT_LOCATION = "Location";
    private static final String ELEMENT_FILE = "File";
    private static final String ELEMENT_LINE = "Line";
    private static final String ELEMENT_MESSAGE = "Message";

    private static final String FAILURE_TYPE_ERROR = "Error";
    private static final String FAILURE_TYPE_ASSERTION = "Assertion";

    private Map<String, TestSuiteResult> suites;

    public CppUnitReportPostProcessor()
    {
        super("CppUnit");
        suites = new TreeMap<String, TestSuiteResult>();
    }

    protected void processDocument(Document doc, StoredFileArtifact artifact)
    {
        Element root = doc.getRootElement();

        // We should get FailedTests and SuccessfulTests sections
        Elements testElements = root.getChildElements(ELEMENT_FAILED_TESTS);
        for(int i = 0; i < testElements.size(); i++)
        {
            processFailedTests(testElements.get(i));
        }

        testElements = root.getChildElements(ELEMENT_SUCCESSFUL_TESTS);
        for(int i = 0; i < testElements.size(); i++)
        {
            processSuccessfulTests(testElements.get(i));
        }

        addSuites(artifact);
    }

    private void processFailedTests(Element element)
    {
        Elements elements = element.getChildElements(ELEMENT_FAILED_TEST);
        for(int i = 0; i < elements.size(); i++)
        {
            // We expect name, failure type, location and message child elements
            Element testElement = elements.get(i);
            String[] name = getTestName(testElement);

            TestCaseResult.Status status = getStatus(testElement);
            String message = getMessage(testElement);

            TestSuiteResult suite = getSuite(name[0]);
            TestCaseResult result = new TestCaseResult(name[1], TestResult.UNKNOWN_DURATION, status, message);
            suite.add(result);
        }
    }

    private void processSuccessfulTests(Element element)
    {
        // We expect a bunch of Test's with Name subelements
        Elements elements = element.getChildElements(ELEMENT_TEST);
        for(int i = 0; i < elements.size(); i++)
        {
            Element testElement = elements.get(i);
            String[] name = getTestName(testElement);

            TestSuiteResult suite = getSuite(name[0]);
            TestCaseResult result = new TestCaseResult(name[1]);
            suite.add(result);
        }
    }

    private void addSuites(StoredFileArtifact artifact)
    {
        for(TestSuiteResult suite: suites.values())
        {
            artifact.addTest(suite);
        }
    }

    private TestSuiteResult getSuite(String name)
    {
        if(suites.containsKey(name))
        {
            return suites.get(name);
        }
        else
        {
            TestSuiteResult suite = new TestSuiteResult(name);
            suites.put(name, suite);
            return suite;
        }
    }

    private String[] getTestName(Element testElement)
    {
        Element nameElement = testElement.getFirstChildElement(ELEMENT_NAME);
        if(nameElement == null)
        {
            return null;
        }

        String name = getText(nameElement);
        if(name == null)
        {
            return null;
        }

        String[] bits = name.split("::", 2);
        if(bits.length == 1)
        {
            return new String[]{ "[unknown]", bits[0] };
        }
        else
        {
            return bits;
        }
    }

    private TestCaseResult.Status getStatus(Element element)
    {
        TestCaseResult.Status status = TestCaseResult.Status.FAILURE;

        Element typeElement = element.getFirstChildElement(ELEMENT_FAILURE_TYPE);
        if(typeElement != null)
        {
            String type = getText(typeElement);
            if(type != null && type.equals(FAILURE_TYPE_ERROR))
            {
                status = TestCaseResult.Status.ERROR;
            }
        }

        return status;
    }

    private String getMessage(Element element)
    {
        String message = "";

        // Include the location if available.
        Element locationElement = element.getFirstChildElement(ELEMENT_LOCATION);
        if(locationElement != null)
        {
            String location = "At";
            Element fileElement = locationElement.getFirstChildElement(ELEMENT_FILE);
            if(fileElement != null)
            {
                String file = getText(fileElement);
                if(file != null)
                {
                    location += " file " + file;
                }
            }

            Element lineElement = locationElement.getFirstChildElement(ELEMENT_LINE);
            if(lineElement != null)
            {
                String line = getText(lineElement);
                if(line != null)
                {
                    location += " line " + line;
                }
            }

            message += location + "\n";
        }

        Element messageElement = element.getFirstChildElement(ELEMENT_MESSAGE);
        if(messageElement != null)
        {
            String text = getText(messageElement);
            if(text != null)
            {
                message += text;
            }
        }

        return message;
    }

}
