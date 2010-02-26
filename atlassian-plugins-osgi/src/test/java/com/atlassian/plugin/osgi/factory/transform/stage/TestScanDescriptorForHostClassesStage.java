package com.atlassian.plugin.osgi.factory.transform.stage;

import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.JarPluginArtifact;
import com.atlassian.plugin.osgi.factory.transform.TransformContext;
import com.atlassian.plugin.osgi.factory.transform.model.SystemExports;
import com.atlassian.plugin.osgi.hostcomponents.HostComponentRegistration;
import com.atlassian.plugin.osgi.container.OsgiContainerManager;
import com.atlassian.plugin.test.PluginJarBuilder;

import java.io.File;
import java.util.Collections;

import junit.framework.TestCase;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.osgi.framework.ServiceReference;

public class TestScanDescriptorForHostClassesStage extends TestCase
{
    private OsgiContainerManager osgiContainerManager;

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        osgiContainerManager = mock(OsgiContainerManager.class);
        when(osgiContainerManager.getRegisteredServices()).thenReturn(new ServiceReference[0]);
    }

    public void testTransform() throws Exception
    {
        final File plugin = new PluginJarBuilder("plugin")
                .addFormattedResource("atlassian-plugin.xml",
                        "<atlassian-plugin name='Test Bundle instruction plugin 2' key='test.plugin'>",
                        "    <plugin-info>",
                        "        <version>1.0</version>",
                        "    </plugin-info>",
                        "    <foo key='bar' class='com.atlassian.plugin.osgi.Foo' />",
                        "</atlassian-plugin>")
                .build();

        ScanDescriptorForHostClassesStage stage = new ScanDescriptorForHostClassesStage();
        SystemExports exports = new SystemExports("com.atlassian.plugin.osgi");
        final TransformContext context = new TransformContext(Collections.<HostComponentRegistration> emptyList(), exports, new JarPluginArtifact(plugin),
            null, PluginAccessor.Descriptor.FILENAME, osgiContainerManager);
        stage.execute(context);
        assertTrue(context.getExtraImports().contains("com.atlassian.plugin.osgi"));
    }

    public void testTransformButPackageInPlugin() throws Exception
    {
        final File plugin = new PluginJarBuilder("plugin")
                .addFormattedResource("atlassian-plugin.xml",
                        "<atlassian-plugin name='Test Bundle instruction plugin 2' key='test.plugin'>",
                        "    <plugin-info>",
                        "        <version>1.0</version>",
                        "    </plugin-info>",
                        "    <foo key='bar' class='com.atlassian.plugin.osgi.Foo' />",
                        "</atlassian-plugin>")
                .addResource("com/atlassian/plugin/osgi/", "")
                .addResource("com/atlassian/plugin/osgi/Foo.class", "asd")
                .build();

        ScanDescriptorForHostClassesStage stage = new ScanDescriptorForHostClassesStage();
        SystemExports exports = new SystemExports("com.atlassian.plugin.osgi");
        final TransformContext context = new TransformContext(Collections.<HostComponentRegistration> emptyList(), exports, new JarPluginArtifact(plugin),
            null, PluginAccessor.Descriptor.FILENAME, osgiContainerManager);
        stage.execute(context);
        assertTrue(!context.getExtraImports().contains("com.atlassian.plugin.osgi"));
    }

    public void testTransformIgnoreUnknown() throws Exception
    {
        final File plugin = new PluginJarBuilder("plugin")
                .addFormattedResource("atlassian-plugin.xml",
                        "<atlassian-plugin name='Test Bundle instruction plugin 2' key='test.plugin'>",
                        "    <plugin-info>",
                        "        <version>1.0</version>",
                        "    </plugin-info>",
                        "    <foo key='bar' class='blat.Foo' />",
                        "</atlassian-plugin>")
                .build();

        ScanDescriptorForHostClassesStage stage = new ScanDescriptorForHostClassesStage();
        SystemExports exports = new SystemExports("com.atlassian.plugin.osgi");
        final TransformContext context = new TransformContext(Collections.<HostComponentRegistration> emptyList(), exports, new JarPluginArtifact(plugin),
            null, PluginAccessor.Descriptor.FILENAME, osgiContainerManager);
        stage.execute(context);
        assertFalse(context.getExtraImports().contains("blat"));
    }
}