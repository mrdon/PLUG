package it.com.atlassian.plugin.refimpl;

import com.atlassian.plugin.refimpl.ParameterUtils;
import com.atlassian.plugin.webresource.UrlMode;

import net.sourceforge.jwebunit.junit.WebTestCase;

public class TestIndex extends WebTestCase
{
    public TestIndex(String name)
    {
        super(name);
    }

    public void setUp() throws Exception {
        getTestContext().setBaseUrl(ParameterUtils.getBaseUrl(UrlMode.ABSOLUTE));
    }

    public void testIndex()
    {
        beginAt("/");
        assertTextPresent("com.atlassian.plugin.osgi.bridge");

        assertTextNotPresent("Resolved");
        assertTextNotPresent("Installed");
        assertTextPresent("General Decorator");
    }
}
