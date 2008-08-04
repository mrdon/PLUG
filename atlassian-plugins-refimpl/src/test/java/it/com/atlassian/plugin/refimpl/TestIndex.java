package it.com.atlassian.plugin.refimpl;

import net.sourceforge.jwebunit.junit.WebTestCase;

public class TestIndex extends WebTestCase
{
    public TestIndex(String name)
    {
        super(name);
    }

    public void setUp() throws Exception {
        getTestContext().setBaseUrl(ParameterUtils.getBaseUrl());
    }

    public void testIndex()
    {
        beginAt("/");
        assertTextPresent("com.atlassian.sal.refimpl");
        assertTextPresent("com.atlassian.sal.api");
        assertTextPresent("com.atlassian.sal.sal-refimpl-plugin");

        assertTextNotPresent("RESOLVED");
        assertTextNotPresent("INSTALLED");
    }

}
