package com.atlassian.plugin.osgi.factory.transform.stage;

import com.atlassian.plugin.JarPluginArtifact;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.osgi.container.OsgiContainerManager;
import com.atlassian.plugin.osgi.factory.transform.TransformContext;
import com.atlassian.plugin.osgi.factory.transform.model.SystemExports;
import com.atlassian.plugin.osgi.hostcomponents.HostComponentRegistration;
import com.atlassian.plugin.test.PluginJarBuilder;
import junit.framework.TestCase;
import org.osgi.framework.ServiceReference;

import java.io.File;
import java.util.Collections;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestScanInnerJarsStage extends TestCase
{
    public void testTransform() throws Exception
    {
        // create a jar with embedded jars.
        final File plugin = new PluginJarBuilder("plugin")
                .addFormattedResource("atlassian-plugin.xml",
                    "<atlassian-plugin name='Test Bundle instruction plugin 2' key='test.plugin'>",
                    "    <plugin-info>",
                    "        <version>1.0</version>",
                    "        <bundle-instructions>",
                    "            <Export-Package>!*.internal.*,*</Export-Package>",
                    "        </bundle-instructions>",
                    "    </plugin-info>",
                    "</atlassian-plugin>")
                .addFormattedJava("my.MyFooChild",
                        "package my;",
                        "public class MyFooChild extends com.atlassian.plugin.osgi.factory.transform.dummypackage2.DummyClass2 {",
                        "}")
                .addResource(ScanInnerJarsStage.INNER_JARS_BASE_LOCATION + "myjar1.jar", "content1")
                .addResource(ScanInnerJarsStage.INNER_JARS_BASE_LOCATION + "myjar2.jar", "content2")
                .addResource("myjar3.jar", "content3")
                .build();

        // execute the stage.
        final ScanInnerJarsStage stage = new ScanInnerJarsStage();
        OsgiContainerManager osgiContainerManager = mock(OsgiContainerManager.class);
        when(osgiContainerManager.getRegisteredServices()).thenReturn(new ServiceReference[0]);
        final TransformContext context = new TransformContext(Collections.<HostComponentRegistration> emptyList(), SystemExports.NONE, new JarPluginArtifact(plugin),
            null, PluginAccessor.Descriptor.FILENAME, osgiContainerManager);
        stage.execute(context);

        // the inner jar in wrong location should not be found.
        assertFalse(context.getBundleClassPathJars().contains("myjar3.jar"));

        // the inner jars in the right location should be found.
        assertEquals(context.getBundleClassPathJars().size(), 2);
        assertTrue(context.getBundleClassPathJars().contains(ScanInnerJarsStage.INNER_JARS_BASE_LOCATION + "myjar1.jar"));
        assertTrue(context.getBundleClassPathJars().contains(ScanInnerJarsStage.INNER_JARS_BASE_LOCATION + "myjar2.jar"));
    }
}
