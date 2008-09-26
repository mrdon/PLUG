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
import com.atlassian.plugin.elements.ResourceDescriptor;
import com.atlassian.plugin.util.ClassLoaderUtils;
import com.atlassian.plugin.impl.StaticPlugin;
import com.atlassian.plugin.mock.MockMineral;
import org.dom4j.DocumentHelper;
import org.dom4j.DocumentException;
import org.dom4j.Element;

import java.util.List;

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
            descriptor.init(new StaticPlugin(), DocumentHelper.parseText("<animal name=\"bear\" class=\"com.atlassian.plugin.mock.MockBear\" />").getRootElement());
            fail("Should have blown up.");
        }
        catch (PluginParseException e)
        {
            assertEquals("Given module class: com.atlassian.plugin.mock.MockBear does not implement com.atlassian.plugin.mock.MockMineral", e.getMessage());
        }

        // now succeed
        descriptor.init(new StaticPlugin(), DocumentHelper.parseText("<animal name=\"bear\" class=\"com.atlassian.plugin.mock.MockGold\" />").getRootElement());
    }

    public void testSingletonness() throws DocumentException, PluginParseException
    {
        ModuleDescriptor descriptor = makeSingletonDescriptor();

        // try a default descriptor with no singleton="" element. Should _be_ a singleton
        descriptor.init(new StaticPlugin(), DocumentHelper.parseText("<animal name=\"bear\" class=\"com.atlassian.plugin.mock.MockBear\" />").getRootElement());
        Object module = descriptor.getModule();
        assertTrue(module == descriptor.getModule());

        // now try a default descriptor with singleton="true" element. Should still be a singleton
        descriptor = makeSingletonDescriptor();
        descriptor.init(new StaticPlugin(), DocumentHelper.parseText("<animal name=\"bear\" class=\"com.atlassian.plugin.mock.MockBear\" singleton=\"true\" />").getRootElement());
        module = descriptor.getModule();
        assertTrue(module == descriptor.getModule());

        // now try reiniting as a non-singleton
        descriptor = makeSingletonDescriptor();
        descriptor.init(new StaticPlugin(), DocumentHelper.parseText("<animal name=\"bear\" class=\"com.atlassian.plugin.mock.MockBear\" singleton=\"false\" />").getRootElement());
        module = descriptor.getModule();
        assertTrue(module != descriptor.getModule());
    }

    public void testGetResourceDescriptor() throws DocumentException, PluginParseException
    {
        ModuleDescriptor descriptor = makeSingletonDescriptor();
        descriptor.init(new StaticPlugin(), DocumentHelper.parseText("<animal name=\"bear\" class=\"com.atlassian.plugin.mock.MockBear\">" +
                "<resource type='velocity' name='view' location='foo' />" +
                "</animal>").getRootElement());

        assertNull(descriptor.getResourceLocation("foo", "bar"));
        assertNull(descriptor.getResourceLocation("velocity", "bar"));
        assertNull(descriptor.getResourceLocation("foo", "view"));
        assertEquals(new ResourceDescriptor(DocumentHelper.parseText("<resource type='velocity' name='view' location='foo' />").getRootElement()).getResourceLocationForName("view").getLocation(), descriptor.getResourceLocation("velocity", "view").getLocation());
    }

    public void testGetResourceDescriptorByType() throws DocumentException, PluginParseException
    {
        ModuleDescriptor descriptor = makeSingletonDescriptor();
        descriptor.init(new StaticPlugin(), DocumentHelper.parseText("<animal name=\"bear\" class=\"com.atlassian.plugin.mock.MockBear\">" +
                "<resource type='velocity' name='view' location='foo' />" +
                "<resource type='velocity' name='input-params' location='bar' />" +
                "</animal>").getRootElement());

        final List resourceDescriptors = descriptor.getResourceDescriptors("velocity");
        assertNotNull(resourceDescriptors);
        assertEquals(2, resourceDescriptors.size());

        ResourceDescriptor resourceDescriptor = (ResourceDescriptor) resourceDescriptors.get(0);
        assertEquals(new ResourceDescriptor(DocumentHelper.parseText("<resource type='velocity' name='view' location='foo' />").getRootElement()), resourceDescriptor);

        resourceDescriptor = (ResourceDescriptor) resourceDescriptors.get(1);
        assertEquals(new ResourceDescriptor(DocumentHelper.parseText("<resource type='velocity' name='input-params' location='bar' />").getRootElement()), resourceDescriptor);
    }

    private ModuleDescriptor makeSingletonDescriptor()
    {
        ModuleDescriptor descriptor = new AbstractModuleDescriptor() {
            Object module;

            public void init(Plugin plugin, Element element) throws PluginParseException
            {
                super.init(plugin, element);
            }

            public Object getModule()
            {
                try
                {
                    if (!isSingleton())
                    {
                        return ClassLoaderUtils.loadClass(getModuleClass().getName(), TestAbstractModuleDescriptor.class).newInstance();
                    }
                    else
                    {
                        if (module == null)
                        {
                            module = ClassLoaderUtils.loadClass(getModuleClass().getName(), TestAbstractModuleDescriptor.class).newInstance();
                        }

                        return module;
                    }
                }
                catch (Exception e)
                {
                    throw new RuntimeException("What happened Dave?");
                }
            }
        };
        return descriptor;
    }
}
