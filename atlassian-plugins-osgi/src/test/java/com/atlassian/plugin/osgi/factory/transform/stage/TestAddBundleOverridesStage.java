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

public class TestAddBundleOverridesStage extends TestCase
{
    public void testTransform() throws Exception
    {
        final File plugin = new PluginJarBuilder("plugin").addFormattedResource("atlassian-plugin.xml",
            "<atlassian-plugin name='Test Bundle instruction plugin 2' key='test.plugin'>", "    <plugin-info>", "        <version>1.0</version>",
            "        <bundle-instructions>", "            <Export-Package>!*.internal.*,*</Export-Package>", "        </bundle-instructions>",
            "    </plugin-info>", "</atlassian-plugin>").build();

        final AddBundleOverridesStage stage = new AddBundleOverridesStage();
        OsgiContainerManager osgiContainerManager = mock(OsgiContainerManager.class);
        when(osgiContainerManager.getRegisteredServices()).thenReturn(new ServiceReference[0]);
        final TransformContext context = new TransformContext(Collections.<HostComponentRegistration> emptyList(), SystemExports.NONE, new JarPluginArtifact(plugin),
            null, PluginAccessor.Descriptor.FILENAME, osgiContainerManager);
        stage.execute(context);
        assertEquals("!*.internal.*,*", context.getBndInstructions().get("Export-Package"));
    }
}
