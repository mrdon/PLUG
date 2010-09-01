package com.atlassian.plugin.web.model;

import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.web.ContextProvider;
import com.atlassian.plugin.web.renderer.RendererException;
import com.atlassian.plugin.web.renderer.StaticWebPanelRenderer;
import com.atlassian.plugin.web.renderer.WebPanelRenderer;
import com.google.common.base.Preconditions;

import java.util.Map;

/**
 * @since   2.5.0
 */
public abstract class AbstractWebPanel implements WebPanel
{
    private final PluginAccessor pluginAccessor;
    private ContextProvider contextProvider;
    protected Plugin plugin;
    private String resourceType;

    protected AbstractWebPanel(PluginAccessor pluginAccessor)
    {
        this.pluginAccessor = pluginAccessor;
    }

    public void setPlugin(Plugin plugin)
    {
        this.plugin = plugin;
    }

    public void setResourceType(String resourceType)
    {
        this.resourceType = Preconditions.checkNotNull(resourceType);
    }

    public String getHtml(Map<String, Object> context)
    {
        if (contextProvider == null)
        {
            throw new  IllegalStateException("AbstractWebPanel implementation must call setContextProvider before calling getHtml");
        }

        return render(contextProvider.getContextMap(context));
    }

    /**
     * Returns the HTML that will be placed in the host application's page.
     *
     * @param context   the contextual information that can be used during
     *  rendering. The application-specific context may have been augmented by the
     *  {@link ContextProvider} attached to the {@link AbstractWebPanel}.
     * @return  the HTML that will be placed in the host application's page.
     */
    protected abstract String render(Map<String, Object> context);

    protected final WebPanelRenderer getRenderer()
    {
        if (StaticWebPanelRenderer.RESOURCE_TYPE.equals(resourceType))
        {
            return StaticWebPanelRenderer.RENDERER;
        }
        else
        {
            for (WebPanelRenderer webPanelRenderer : pluginAccessor.getEnabledModulesByClass(WebPanelRenderer.class))
            {
                if (Preconditions.checkNotNull(resourceType).equals(webPanelRenderer.getResourceType()))
                {
                    return webPanelRenderer;
                }
            }
            throw new RendererException("No renderer found for resource type: " + resourceType);
        }
    }

    /**
     * Sets the {@link ContextProvider} used to decorate the context Map supplied to the {@link #getHtml(Map)} method.
     *
     * @param contextProvider a {@link ContextProvider} implementation
     */
    public void setContextProvider(ContextProvider contextProvider)
    {
        this.contextProvider = contextProvider;
    }
}
