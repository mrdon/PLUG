package com.atlassian.plugin.osgi.factory.transform.stage;

import com.atlassian.plugin.osgi.factory.transform.PluginTransformationException;
import com.atlassian.plugin.osgi.factory.transform.TransformContext;
import com.atlassian.plugin.osgi.factory.transform.TransformStage;
import org.dom4j.Attribute;
import org.dom4j.DocumentHelper;
import org.dom4j.XPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Scans the plugin descriptor for any "class" attribute, and ensures that it will be imported, if appropriate.
 *
 * @since 2.2.0
 */
public class ScanDescriptorForHostClassesStage implements TransformStage
{
    private static final Logger log = LoggerFactory.getLogger(ScanDescriptorForHostClassesStage.class);

    public void execute(TransformContext context) throws PluginTransformationException
    {
        XPath xpath = DocumentHelper.createXPath("//@class");
        List<Attribute> attributes = xpath.selectNodes(context.getDescriptorDocument());
        for (Attribute attr : attributes)
        {
            String className = attr.getValue();
            int dotpos = className.lastIndexOf(".");
            if (dotpos > -1)
            {
                String pkg = className.substring(0, dotpos);
                String pkgPath = pkg.replace('.', '/') + '/';

                // Only add an import if the system exports it and the plugin isn't using the package
                if (context.getSystemExports().isExported(pkg))
                {
                    if (context.getPluginArtifact().doesResourceExist(pkgPath))
                    {
                        log.warn("The plugin '" + context.getPluginArtifact().toString() + "' uses a package '" +
                                pkg + "' that is also exported by the application.  It is highly recommended that the " +
                                "plugin use its own packages.");
                    }
                    else
                    {
                        context.getExtraImports().add(pkg);
                    }
                }
            }
        }
    }
}
