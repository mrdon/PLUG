package com.atlassian.plugin.web.descriptors;

import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.web.renderer.RendererException;
import com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringWriter;
import java.util.Map;

public class ResourceTemplateWebPanel extends AbstractWebPanel
{
    private String resourceFilename;
    private static final Logger logger = LoggerFactory.getLogger(ResourceTemplateWebPanel.class.getName());

    public ResourceTemplateWebPanel(PluginAccessor pluginAccessor)
    {
        super(pluginAccessor);
    }

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
