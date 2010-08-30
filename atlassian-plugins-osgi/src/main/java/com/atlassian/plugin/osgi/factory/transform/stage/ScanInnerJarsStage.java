package com.atlassian.plugin.osgi.factory.transform.stage;

import com.atlassian.plugin.osgi.factory.transform.PluginTransformationException;
import com.atlassian.plugin.osgi.factory.transform.TransformContext;
import com.atlassian.plugin.osgi.factory.transform.TransformStage;

import java.util.jar.JarEntry;

/**
 * The stages which scan for inner jars in attempt to create bundle classpath.
 *
 * @since 2.6.0
 */
public class ScanInnerJarsStage implements TransformStage
{
    protected static final String INNER_JARS_BASE_LOCATION = "META-INF/lib/";

    public void execute(TransformContext context) throws PluginTransformationException
    {
        for (final JarEntry jarEntry : context.getPluginJarEntries())
        {
            // we only want jar files under the defined base location.
            if (jarEntry.getName().startsWith(INNER_JARS_BASE_LOCATION)
                && jarEntry.getName().endsWith(".jar"))
            {
                context.addBundleClasspathJar(jarEntry.getName());
            }
        }
    }
}
