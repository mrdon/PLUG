package it.com.atlassian.plugin.refimpl;

import net.sourceforge.jwebunit.junit.WebTestCase;

public class TestIndex extends WebTestCase
{
    public TestIndex(String name)
    {
        super(name);
    }

    public void setUp() throws Exception {
        getTestContext().setBaseUrl("http://localhost:8480/atlassian-plugins-refimpl");
    }

    public void testIndex()
    {
        beginAt("/");
        assertTextPresent("com.atlassian.sal.refimpl");
        assertTextPresent("com.atlassian.sal.api");
        assertTextPresent("com.atlassian.sal.sal-refimpl-plugin");
    }

}
