package com.atlassian.plugin.osgi.factory.transform.stage;

import com.atlassian.plugin.PluginManager;
import com.atlassian.plugin.osgi.factory.transform.TransformContext;
import com.atlassian.plugin.osgi.hostcomponents.HostComponentRegistration;
import com.atlassian.plugin.test.PluginJarBuilder;
import junit.framework.TestCase;

import java.io.File;
import java.util.Collections;

public class TestAddBundleOverridesStage extends TestCase
{
    public void testTransform() throws Exception
    {
        File plugin = new PluginJarBuilder("plugin")
                .addFormattedResource("atlassian-plugin.xml",
                        "<atlassian-plugin name='Test Bundle instruction plugin 2' key='test.plugin'>",
                        "    <plugin-info>",
                        "        <version>1.0</version>",
                        "        <bundle-instructions>",
                        "            <Export-Package>!*.internal.*,*</Export-Package>",
                        "        </bundle-instructions>",
                        "    </plugin-info>",
                        "</atlassian-plugin>")
                .build();

        AddBundleOverridesStage stage = new AddBundleOverridesStage();
        TransformContext context = new TransformContext(Collections.<HostComponentRegistration>emptyList(), plugin, PluginManager.PLUGIN_DESCRIPTOR_FILENAME);
        stage.execute(context);
        assertEquals("!*.internal.*,*", context.getBndInstructions().get("Export-Package"));
    }
}
