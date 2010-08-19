package com.atlassian.plugin.osgi.factory.transform.stage;

import com.atlassian.plugin.JarPluginArtifact;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.osgi.container.OsgiContainerManager;
import com.atlassian.plugin.osgi.factory.transform.TransformContext;
import com.atlassian.plugin.osgi.factory.transform.model.SystemExports;
import com.atlassian.plugin.osgi.hostcomponents.HostComponentRegistration;
import com.atlassian.plugin.test.PluginJarBuilder;
import junit.framework.TestCase;
import org.dom4j.Element;
import org.dom4j.DocumentHelper;
import org.dom4j.DocumentException;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

import com.atlassian.plugin.osgi.external.SingleModuleDescriptorFactory;
import com.atlassian.plugin.PluginParseException;
import org.osgi.framework.ServiceReference;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestModuleTypeSpringStage extends TestCase
{
    public void testTransform() throws IOException, DocumentException
    {
        Element pluginRoot = DocumentHelper.createDocument().addElement("atlassian-plugin");
        Element moduleType = pluginRoot.addElement("module-type");
        moduleType.addAttribute("key", "foo");
        moduleType.addAttribute("class", "my.FooDescriptor");

        SpringTransformerTestHelper.transform(new ModuleTypeSpringStage(), pluginRoot,
                "beans:bean[@id='springHostContainer' and @class='"+ ModuleTypeSpringStage.SPRING_HOST_CONTAINER+"']",
                "beans:bean[@id='moduleType-foo' and @class='"+ SingleModuleDescriptorFactory.class.getName()+"']",
                "osgi:service[@id='moduleType-foo_osgiService' and @auto-export='interfaces']");
    }

    public void testTransformForOneApp() throws IOException, DocumentException
    {
        Element pluginRoot = DocumentHelper.createDocument().addElement("atlassian-plugin");
        Element moduleType = pluginRoot.addElement("module-type");
        moduleType.addAttribute("key", "foo");
        moduleType.addAttribute("class", "my.FooDescriptor");
        moduleType.addAttribute("application", "bar");
        SpringTransformerTestHelper.transform(new ModuleTypeSpringStage(), pluginRoot,
                "not(beans:bean[@id='moduleType-foo' and @class='" + SingleModuleDescriptorFactory.class.getName()+"'])");

        pluginRoot = DocumentHelper.createDocument().addElement("atlassian-plugin");
        moduleType = pluginRoot.addElement("module-type");
        moduleType.addAttribute("key", "foo");
        moduleType.addAttribute("class", "my.FooDescriptor");
        moduleType.addAttribute("application", "foo");
        SpringTransformerTestHelper.transform(new ModuleTypeSpringStage(), pluginRoot,
                "beans:bean[@id='moduleType-foo' and @class='"+ SingleModuleDescriptorFactory.class.getName()+"']");
    }

    public void testTransformOfBadElement() throws IOException, DocumentException
    {
        Element pluginRoot = DocumentHelper.createDocument().addElement("atlassian-plugin");
        Element moduleType = pluginRoot.addElement("module-type");
        moduleType.addAttribute("key", "foo");

        try
        {
            SpringTransformerTestHelper.transform(new ModuleTypeSpringStage(), pluginRoot,
                    "beans:bean[@id='moduleType-foo' and @class='"+ SingleModuleDescriptorFactory.class.getName()+"']",
                    "osgi:service[@id='moduleType-foo_osgiService' and @auto-export='interfaces']");
            fail();
        }
        catch (PluginParseException ex)
        {
            // pass
        }
    }

    public void testTransformOfBadElementKey() throws IOException, DocumentException
    {
        Element pluginRoot = DocumentHelper.createDocument().addElement("atlassian-plugin");
        Element moduleType = pluginRoot.addElement("module-type");

        try
        {
            SpringTransformerTestHelper.transform(new ModuleTypeSpringStage(), pluginRoot,
                    "beans:bean[@id='moduleType-foo' and @class='"+ SingleModuleDescriptorFactory.class.getName()+"']",
                    "osgi:service[@id='moduleType-foo_osgiService' and @auto-export='interfaces']");
            fail();
        }
        catch (PluginParseException ex)
        {
            // pass
        }
    }

    public void testBeanTracking() throws Exception
    {
        OsgiContainerManager osgiContainerManager = mock(OsgiContainerManager.class);
        when(osgiContainerManager.getRegisteredServices()).thenReturn(new ServiceReference[0]);

        final File plugin = new PluginJarBuilder("plugin")
                .addFormattedResource("atlassian-plugin.xml",
                        "<atlassian-plugin name='Test Bundle instruction plugin 2' key='test.plugin'>",
                        "    <plugin-info>",
                        "        <version>1.0</version>",
                        "    </plugin-info>",
                        "    <module-type key='mod1' class='my.FooDescriptor' />",
                        "</atlassian-plugin>")
                .build();

        ModuleTypeSpringStage stage = new ModuleTypeSpringStage();
        final TransformContext context = new TransformContext(Collections.<HostComponentRegistration> emptyList(), SystemExports.NONE, new JarPluginArtifact(plugin),
            null, PluginAccessor.Descriptor.FILENAME, osgiContainerManager);
        stage.execute(context);

        // springHostContainer should always exist if a module type is declared.
        assertTrue(context.beanExists("springHostContainer"));
        // these two names are generated from the base key, one for the module type itself and the other for service export.
        assertTrue(context.beanExists("moduleType-mod1"));
        assertTrue(context.beanExists("moduleType-mod1_osgiService"));
    }
}
