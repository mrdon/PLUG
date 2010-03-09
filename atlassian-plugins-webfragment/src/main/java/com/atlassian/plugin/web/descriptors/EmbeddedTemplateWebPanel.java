package com.atlassian.plugin.web.descriptors;

import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.web.renderer.RendererException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class EmbeddedTemplateWebPanel extends AbstractWebPanel
{

    private String templateBody;
    private static final Logger logger = LoggerFactory.getLogger(EmbeddedTemplateWebPanel.class.getName());

    public EmbeddedTemplateWebPanel(PluginAccessor pluginAccessor)
    {
        super(pluginAccessor);
    }

    public void setTemplateBody(String templateBody)
    {
        this.templateBody = templateBody;
    }

    public String getHtml(Map<String, Object> context)
    {
        try
        {
            return getRenderer().renderFragment(templateBody, plugin.getClassLoader(), context);
        }
        catch (RendererException e)
        {
            logger.warn(String.format("Error rendering WebPanel: %s\n" +
                    "Template contents: %s", e.getMessage(), templateBody), e);
            return e.toString();
        }
    }
}
