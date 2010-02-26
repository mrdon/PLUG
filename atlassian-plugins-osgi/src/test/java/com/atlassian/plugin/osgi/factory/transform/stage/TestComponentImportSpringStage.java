package com.atlassian.plugin.osgi.factory.transform.stage;

import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.JarPluginArtifact;
import com.atlassian.plugin.osgi.SomeInterface;
import com.atlassian.plugin.osgi.container.OsgiContainerManager;
import com.atlassian.plugin.osgi.factory.transform.TransformContext;
import com.atlassian.plugin.osgi.factory.transform.model.SystemExports;
import com.atlassian.plugin.test.PluginJarBuilder;

import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.slf4j.Logger;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Matchers.anyString;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Constants;

import java.io.File;
import java.io.IOException;

import junit.framework.TestCase;

public class TestComponentImportSpringStage extends TestCase
{
    public void testTransform() throws IOException, DocumentException
    {
        final ComponentImportSpringStage stage = new ComponentImportSpringStage();

        // interface as attribute
        Element pluginRoot = DocumentHelper.createDocument().addElement("atlassian-plugin");
        Element component = pluginRoot.addElement("component-import");
        component.addAttribute("key", "foo");
        component.addAttribute("interface", "my.Foo");
        SpringTransformerTestHelper.transform(stage, pluginRoot, "osgi:reference[@id='foo']/osgi:interfaces/beans:value/text()='my.Foo'");

        // interface as element
        pluginRoot = DocumentHelper.createDocument().addElement("atlassian-plugin");
        component = pluginRoot.addElement("component-import");
        component.addAttribute("key", "foo");
        final Element inf = component.addElement("interface");
        inf.setText("my.IFoo");
        SpringTransformerTestHelper.transform(stage, pluginRoot, "osgi:reference[@id='foo']/osgi:interfaces/beans:value/text()='my.IFoo'");

    }

    public void testTransformWithCustomFilter() throws IOException, DocumentException
    {
        final ComponentImportSpringStage stage = new ComponentImportSpringStage();

        Element pluginRoot = DocumentHelper.createDocument().addElement("atlassian-plugin");
        Element component = pluginRoot.addElement("component-import");
        component.addAttribute("key", "foo");
        component.addAttribute("interface", "my.Foo");
        component.addAttribute("filter", "(foo=bar)");
        SpringTransformerTestHelper.transform(stage, pluginRoot, "osgi:reference[@id='foo' and @filter='(foo=bar)']'");
    }

    public void testTransformForOneApp() throws IOException, DocumentException
    {
        final ComponentImportSpringStage stage = new ComponentImportSpringStage();

        Element pluginRoot = DocumentHelper.createDocument().addElement("atlassian-plugin");
        Element component = pluginRoot.addElement("component-import");
        component.addAttribute("key", "foo");
        component.addAttribute("interface", "my.Foo");
        component.addAttribute("application", "bob");
        SpringTransformerTestHelper.transform(stage, pluginRoot, "not(osgi:reference[@id='foo']/osgi:interfaces/beans:value/text()='my.Foo')");

        pluginRoot = DocumentHelper.createDocument().addElement("atlassian-plugin");
        component = pluginRoot.addElement("component-import");
        component.addAttribute("key", "foo");
        component.addAttribute("interface", "my.Foo");
        component.addAttribute("application", "foo");
        SpringTransformerTestHelper.transform(stage, pluginRoot, "osgi:reference[@id='foo']/osgi:interfaces/beans:value/text()='my.Foo'");
    }

    public void testTransformImportEvenUnusedPackages() throws Exception, DocumentException
    {
        final ComponentImportSpringStage stage = new ComponentImportSpringStage();
        final File jar = new PluginJarBuilder().addFormattedResource("atlassian-plugin.xml", "<atlassian-plugin>",
            "  <component-import key='foo' interface='com.atlassian.plugin.osgi.SomeInterface' />", "</atlassian-plugin>").build();

        OsgiContainerManager osgiContainerManager = mock(OsgiContainerManager.class);
        when(osgiContainerManager.getRegisteredServices()).thenReturn(new ServiceReference[0]);
        final TransformContext context = new TransformContext(null, SystemExports.NONE, new JarPluginArtifact(jar), null, PluginAccessor.Descriptor.FILENAME, osgiContainerManager);
        stage.execute(context);
        assertTrue(context.getExtraImports().contains(SomeInterface.class.getPackage().getName()));
    }

    public void testTransformImportWarnUnverifiedService() throws Exception, DocumentException
    {
        final ComponentImportSpringStage stage = new ComponentImportSpringStage();
        stage.log = mock(Logger.class);
        final File jar = new PluginJarBuilder()
            .addFormattedResource("atlassian-plugin.xml", "<atlassian-plugin>",
            "  <component-import key='foo' interface='my.UnknownService' />", "</atlassian-plugin>").build();

        ServiceReference serviceReference = mock(ServiceReference.class);
        when(serviceReference.getProperty(Constants.OBJECTCLASS)).thenReturn(new String[] { "my.Service"});

        OsgiContainerManager osgiContainerManager = mock(OsgiContainerManager.class);
        when(osgiContainerManager.getRegisteredServices()).thenReturn(new ServiceReference[] {serviceReference});

        final TransformContext context = new TransformContext(null, SystemExports.NONE, new JarPluginArtifact(jar), null, PluginAccessor.Descriptor.FILENAME, osgiContainerManager);
        stage.execute(context);
        verify(stage.log).warn(anyString());
    }

    public void testTransformImportNoWarnForVerifiedService() throws Exception
    {
        final ComponentImportSpringStage stage = new ComponentImportSpringStage();
        stage.log = mock(Logger.class);
        final File jar = new PluginJarBuilder()
            .addFormattedResource("atlassian-plugin.xml", "<atlassian-plugin>",
            "  <component-import key='foo' interface='my.Service' />", "</atlassian-plugin>").build();

        ServiceReference serviceReference = mock(ServiceReference.class);
        when(serviceReference.getProperty(Constants.OBJECTCLASS)).thenReturn(new String[] { "my.Service"});

        OsgiContainerManager osgiContainerManager = mock(OsgiContainerManager.class);
        when(osgiContainerManager.getRegisteredServices()).thenReturn(new ServiceReference[] {serviceReference});

        final TransformContext context = new TransformContext(null, SystemExports.NONE, new JarPluginArtifact(jar), null, PluginAccessor.Descriptor.FILENAME, osgiContainerManager);
        stage.execute(context);
        verify(stage.log, never()).warn(anyString());
    }
}
