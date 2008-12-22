package com.zutubi.pulse.core;

import com.zutubi.pulse.core.model.CommandResult;
import com.zutubi.pulse.core.model.StoredFileArtifact;
import com.zutubi.pulse.core.model.TestSuiteResult;
import com.zutubi.pulse.test.PulseTestCase;
import com.zutubi.pulse.util.FileSystemUtils;
import com.zutubi.pulse.util.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * <class-comment/>
 */
public class OCUnitReportPostProcessorTest extends PulseTestCase
{
    private File tmpDir = null;
    private StoredFileArtifact artifact = null;
    private CommandResult result = null;

    public OCUnitReportPostProcessorTest()
    {
    }

    public OCUnitReportPostProcessorTest(String name)
    {
        super(name);
    }

    protected void setUp() throws Exception
    {
        super.setUp();

        tmpDir = FileSystemUtils.createTempDir("OCUnitReportPostProcessorTest", getName());

        artifact = prepareArtifact(this.getName());

        result = new CommandResult("output");
    }

    protected void tearDown() throws Exception
    {
        artifact = null;
        removeDirectory(tmpDir);

        super.tearDown();
    }

    private StoredFileArtifact prepareArtifact(String name) throws IOException
    {
        File tmpFile = new File(tmpDir, name + ".txt");
        IOUtils.joinStreams(
                this.getClass().getResourceAsStream("OCUnitReportPostProcessorTest."+name+".txt"),
                new FileOutputStream(tmpFile),
                true
        );

        return new StoredFileArtifact( name + ".txt");
    }

    public void testEmptySuite()
    {
        TestSuiteResult tests = process();
        assertEquals(1, tests.getSuites().size());
        TestSuiteResult suite = tests.getSuites().get(0);
        assertEquals("SenInterfaceTestCase", suite.getName());
        assertEquals(2, suite.getDuration());
        assertEquals(0, suite.getErrors());
        assertEquals(0, suite.getFailures());
    }

    public void testNestedSuites()
    {
        TestSuiteResult tests = process();
        assertEquals(1, tests.getSuites().size());
        TestSuiteResult suite = tests.getSuites().get(0);
        assertEquals("All tests", suite.getName());
        assertEquals(1, suite.getSuites().size());
        assertEquals(2768, suite.getDuration());
        assertEquals(0, suite.getErrors());
        assertEquals(0, suite.getFailures());

        suite = suite.getSuites().get(0);
        assertEquals("/System/Library/Frameworks/SenTestingKit.framework(Tests)", suite.getName());
        assertEquals(1, suite.getSuites().size());
        assertEquals(30, suite.getDuration());
        assertEquals(0, suite.getErrors());
        assertEquals(0, suite.getFailures());

        suite = suite.getSuites().get(0);
        assertEquals("SenInterfaceTestCase", suite.getName());
        assertEquals(0, suite.getCases().size());
        assertEquals(4, suite.getDuration());
        assertEquals(0, suite.getErrors());
        assertEquals(0, suite.getFailures());
    }

    public void testSingleSuiteWithTests()
    {
        assertSingleSuite(process(), 7);
    }

    public void testIncompleteSuite()
    {
        assertSingleSuite(process(), -1);
    }

    public void testMissingSummary()
    {
        assertSingleSuite(process(), -1);
    }

    public void testMismatchedSuiteName()
    {
        assertSingleSuite(process(), 7);
    }

    public void testIncompleteNestedSuites()
    {
        TestSuiteResult suite = process();
        suite  = assertOneNestedSuite(suite, "All tests");
        suite = assertOneNestedSuite(suite, "/System/Library/Frameworks/SenTestingKit.framework(Tests)");
        assertOneNestedSuite(suite, "SenInterfaceTestCase");
    }

    private TestSuiteResult assertOneNestedSuite(TestSuiteResult suite, String name)
    {
        assertEquals(0, suite.getCases().size());
        assertEquals(1, suite.getSuites().size());
        suite = suite.getSuites().get(0);
        assertEquals(name, suite.getName());
        return suite;
    }

    private void assertSingleSuite(TestSuiteResult tests, long duration)
    {
        assertEquals(1, tests.getSuites().size());
        TestSuiteResult suite = tests.getSuites().get(0);
        assertEquals("TestCNYieldSorting", suite.getName());
        assertEquals(duration, suite.getDuration());
        assertEquals(0, suite.getErrors());
        assertEquals(0, suite.getFailures());
        assertEquals(4, suite.getTotal());
    }

    public void testRealSample()
    {
        //Executed 583 tests, with 3 failures (1 unexpected) in 2.768 (3.503) seconds

        TestSuiteResult tests = process();
        assertEquals(1, tests.getSuites().size());
        TestSuiteResult suite = tests.getSuites().get(0);
        assertEquals("All tests", suite.getName());
        assertEquals(2768, suite.getDuration());
        assertEquals(0, suite.getErrors());
        assertEquals(3, suite.getFailures());
        assertEquals(583, suite.getTotal());
    }

    public void testExtendedSampleBuild()
    {
        TestSuiteResult tests = process();
        assertEquals(1, tests.getSuites().size());
        TestSuiteResult suite = tests.getSuites().get(0);
        assertEquals("/opt/pulse/pulse-data/recipes/2555916/base/build/Release/UnitTests.octest(Tests)", suite.getName());
        assertEquals(3, suite.getDuration());
        assertEquals(0, suite.getErrors());
        assertEquals(0, suite.getFailures());
        assertEquals(11, suite.getTotal());

    }

    public void testMultipleNestedSuites()
    {
        TestSuiteResult tests = process();
        assertEquals(1, tests.getSuites().size());
        TestSuiteResult suite = tests.getSuites().get(0);
        assertEquals("All tests", suite.getName());
        assertEquals(2, suite.getSuites().size());
    }

    private TestSuiteResult process()
    {
        OCUnitReportPostProcessor pp = new OCUnitReportPostProcessor();
        TestSuiteResult testResults = new TestSuiteResult();
        CommandContext context = new CommandContext(null, tmpDir, testResults);
        pp.process(artifact, result, context);
        return testResults;
    }
}