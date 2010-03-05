/*
 * Created by IntelliJ IDEA.
 * User: Mike
 * Date: Jul 29, 2004
 * Time: 4:28:18 PM
 */
package com.atlassian.plugin.descriptors;

import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.StateAware;
import com.atlassian.plugin.elements.ResourceDescriptor;
import com.atlassian.plugin.impl.StaticPlugin;
import com.atlassian.plugin.mock.MockMineral;
import com.atlassian.plugin.module.ModuleFactory;
import com.atlassian.plugin.util.ClassLoaderUtils;
import junit.framework.TestCase;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import java.util.List;

import static org.mockito.Mockito.mock;

public class TestAbstractModuleDescriptor extends TestCase
{
    public void testAssertModuleClassImplements() throws DocumentException, PluginParseException
    {
        ModuleDescriptor descriptor = new AbstractModuleDescriptor(ModuleFactory.LEGACY_MODULE_FACTORY) {
            public void init(Plugin plugin, Element element) throws PluginParseException
            {
                super.init(plugin, element);
                enabled();
                assertModuleClassImplements(MockMineral.class);
            }

            public Object getModule()
            {
                return null;
            }
        };

        try
        {
            descriptor.init(new StaticPlugin(), DocumentHelper.parseText("<animal key=\"key\" name=\"bear\" class=\"com.atlassian.plugin.mock.MockBear\" />").getRootElement());
            ((StateAware)descriptor).enabled();
            fail("Should have blown up.");
        }
        catch (PluginParseException e)
        {
            assertEquals("Given module class: com.atlassian.plugin.mock.MockBear does not implement com.atlassian.plugin.mock.MockMineral", e.getMessage());
        }

        // now succeed
        descriptor.init(new StaticPlugin(), DocumentHelper.parseText("<animal key=\"key\" name=\"bear\" class=\"com.atlassian.plugin.mock.MockGold\" />").getRootElement());
    }

    public void testLoadClassFromNewModuleFactory()
    {
        ModuleFactory moduleFactory = mock(ModuleFactory.class);
        AbstractModuleDescriptor moduleDescriptor = new StringModuleDescriptor(moduleFactory, "foo");
        Plugin plugin = mock(Plugin.class);
        moduleDescriptor.loadClass(plugin, "foo");
        assertEquals(String.class, moduleDescriptor.getModuleClass());
    }

    public void testLoadClassFromNewModuleFactoryWithExtendsNumberType()
    {
        ModuleFactory moduleFactory = mock(ModuleFactory.class);
        AbstractModuleDescriptor moduleDescriptor = new ExtendsNumberModuleDescriptor(moduleFactory, "foo");
        Plugin plugin = mock(Plugin.class);

        try
        {
            moduleDescriptor.loadClass(plugin, "foo");
            fail("Should have complained about extends type");
        }
        catch (IllegalStateException ex)
        {
            // success
        }
    }

    public void testLoadClassFromNewModuleFactoryButUnknownType()
    {
        ModuleFactory moduleFactory = mock(ModuleFactory.class);
        AbstractModuleDescriptor moduleDescriptor = new AbstractModuleDescriptor(moduleFactory)
        {
            public AbstractModuleDescriptor init()
            {
                moduleClassName = "foo";
                return this;
            }

            @Override
            public Object getModule()
            {
                return null;
            }
        }.init();
        try
        {
            Plugin plugin = mock(Plugin.class);
            moduleDescriptor.loadClass(plugin, "foo");
            fail("Should have complained about unknown type");
        }
        catch (IllegalStateException ex)
        {
            // success
        }
    }

    public void testSingletonness() throws DocumentException, PluginParseException
    {
        ModuleDescriptor descriptor = makeSingletonDescriptor();

        // try a default descriptor with no singleton="" element. Should _be_ a singleton
        descriptor.init(new StaticPlugin(), DocumentHelper.parseText("<animal key=\"key\" name=\"bear\" class=\"com.atlassian.plugin.mock.MockBear\" />").getRootElement());
        ((StateAware)descriptor).enabled();
        Object module = descriptor.getModule();
        assertTrue(module == descriptor.getModule());

        // now try a default descriptor with singleton="true" element. Should still be a singleton
        descriptor = makeSingletonDescriptor();
        descriptor.init(new StaticPlugin(), DocumentHelper.parseText("<animal key=\"key\" name=\"bear\" class=\"com.atlassian.plugin.mock.MockBear\" singleton=\"true\" />").getRootElement());
        ((StateAware)descriptor).enabled();
        module = descriptor.getModule();
        assertTrue(module == descriptor.getModule());

        // now try reiniting as a non-singleton
        descriptor = makeSingletonDescriptor();
        descriptor.init(new StaticPlugin(), DocumentHelper.parseText("<animal key=\"key\" name=\"bear\" class=\"com.atlassian.plugin.mock.MockBear\" singleton=\"false\" />").getRootElement());
        ((StateAware)descriptor).enabled();
        module = descriptor.getModule();
        assertTrue(module != descriptor.getModule());
    }

    public void testGetResourceDescriptor() throws DocumentException, PluginParseException
    {
        ModuleDescriptor descriptor = makeSingletonDescriptor();
        descriptor.init(new StaticPlugin(), DocumentHelper.parseText("<animal key=\"key\" name=\"bear\" class=\"com.atlassian.plugin.mock.MockBear\">" +
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
        descriptor.init(new StaticPlugin(), DocumentHelper.parseText("<animal key=\"key\" name=\"bear\" class=\"com.atlassian.plugin.mock.MockBear\">" +
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
        ModuleDescriptor descriptor = new AbstractModuleDescriptor(ModuleFactory.LEGACY_MODULE_FACTORY) {
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

    private static class StringModuleDescriptor extends AbstractModuleDescriptor<String>
    {
        public StringModuleDescriptor(ModuleFactory moduleFactory, String className)
        {
            super(moduleFactory);
            moduleClassName = className;
        }

        @Override
        public String getModule()
        {
            return null;
        }

    }

    private static class ExtendsNumberModuleDescriptor<T extends Number> extends AbstractModuleDescriptor<T>
    {
        public ExtendsNumberModuleDescriptor(ModuleFactory moduleFactory, String className)
        {
            super(moduleFactory);
            moduleClassName = className;
        }

        @Override
        public T getModule()
        {
            return null;
        }
    }
}
