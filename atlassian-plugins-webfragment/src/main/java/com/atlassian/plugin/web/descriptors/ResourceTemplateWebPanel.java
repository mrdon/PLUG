package com.atlassian.plugin.web.descriptors;

import com.atlassian.plugin.PluginAccessor;
import com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringWriter;
import java.util.Map;

/**
 * @since   2.5.0
 */
public class ResourceTemplateWebPanel extends AbstractWebPanel
{
    private static final Logger logger = LoggerFactory.getLogger(ResourceTemplateWebPanel.class.getName());
    private String resourceFilename;

    public ResourceTemplateWebPanel(PluginAccessor pluginAccessor)
    {
        super(pluginAccessor);
    }

    /**
     * Specifies the name of the template file that is to be rendered.
     * This file will be loaded from the (plugin's) classpath.
     *
     * @param resourceFilename  the name of the template file that is to be rendered.
     *  May not be null.
     */
    public void setResourceFilename(String resourceFilename)
    {
        this.resourceFilename = Preconditions.checkNotNull(resourceFilename, "resourceFilename");
    }

    public String getHtml(Map<String, Object> context)
    {
        try
        {
            final StringWriter sink = new StringWriter();
            getRenderer().render(resourceFilename, plugin, context, sink);
            return sink.toString();
        }
        catch (Exception e)
        {
            final String message = String.format("Error rendering WebPanel (%s): %s", resourceFilename, e.getMessage());
            logger.warn(message, e);
            return message;
        }
    }
}
