package com.atlassian.plugin.web.renderer;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.Map;

public class StaticWebPanelRenderer implements WebPanelRenderer
{
    public static final String RESOURCE_TYPE = "static";
    private final ClassLoader classLoader;

    public StaticWebPanelRenderer(ClassLoader classLoader)
    {
        this.classLoader = classLoader;
    }

    public String getResourceType()
    {
        return RESOURCE_TYPE;
    }

    public void render(String templateName, ClassLoader classLoader, Map<String, Object> context, Writer writer) throws RendererException, IOException
    {
        InputStream in = null;
        try
        {
            IOUtils.copy(loadTemplate(templateName), writer);
        }
        finally
        {
            IOUtils.closeQuietly(in);
        }
    }

    public String renderFragment(String fragment, ClassLoader classLoader, Map<String, Object> context) throws RendererException
    {
        return fragment;
    }

    private InputStream loadTemplate(String templateName) throws IOException
    {
        InputStream in = classLoader.getResourceAsStream(templateName);
        if (in == null)
        {
            // template not found in the plugin, try the host application:
            if ((in = getClass().getResourceAsStream(templateName)) == null)
            {
                throw new RendererException(String.format("Static web panel template %s not found.", templateName));
            }
        }
        return in;
    }
}
