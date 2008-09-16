package it.com.atlassian.plugin.refimpl;

import com.atlassian.plugin.refimpl.ParameterUtils;

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
        assertTextPresent("com.springsource.slf4j.log4j");

        assertTextNotPresent("Resolved");
        assertTextNotPresent("Installed");
        assertTextPresent("General Decorator");
    }

}
