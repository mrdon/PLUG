/*
 * Created by IntelliJ IDEA.
 * User: Mike
 * Date: Jul 29, 2004
 * Time: 4:28:18 PM
 */
package com.atlassian.plugin.descriptors;

import junit.framework.TestCase;
import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.mock.MockMineral;
import org.dom4j.DocumentHelper;
import org.dom4j.DocumentException;
import org.dom4j.Element;

public class TestAbstractModuleDescriptor extends TestCase
{
    public void testAssertModuleClassImplements() throws DocumentException, PluginParseException
    {
        ModuleDescriptor descriptor = new AbstractModuleDescriptor() {
            public void init(Plugin plugin, Element element) throws PluginParseException
            {
                super.init(plugin, element);
                assertModuleClassImplements(MockMineral.class);
            }

            public Object getModule()
            {
                return null;
            }
        };

        try
        {
            descriptor.init(null, DocumentHelper.parseText("<animal name=\"bear\" class=\"com.atlassian.plugin.mock.MockBear\" />").getRootElement());
            fail("Should have blown up.");
        }
        catch (PluginParseException e)
        {
            assertEquals("Given module class: com.atlassian.plugin.mock.MockBear does not implement com.atlassian.plugin.mock.MockMineral", e.getMessage());
        }

        // now succeed
        descriptor.init(null, DocumentHelper.parseText("<animal name=\"bear\" class=\"com.atlassian.plugin.mock.MockGold\" />").getRootElement());
    }
}