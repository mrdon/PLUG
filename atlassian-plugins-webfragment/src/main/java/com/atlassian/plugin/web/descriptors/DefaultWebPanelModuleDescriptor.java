package com.atlassian.plugin.web.descriptors;

import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.elements.ResourceDescriptor;
import com.atlassian.plugin.hostcontainer.HostContainer;
import com.atlassian.plugin.module.ModuleFactory;
import com.atlassian.plugin.web.WebInterfaceManager;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import org.apache.commons.lang.StringUtils;
import org.dom4j.Element;

import java.util.Collection;
import java.util.concurrent.Callable;

public class DefaultWebPanelModuleDescriptor extends AbstractWebFragmentModuleDescriptor<WebPanel>
{
    private Callable<WebPanel> webPanelFactory;
    private String location;
    private final HostContainer hostContainer;

    public DefaultWebPanelModuleDescriptor(HostContainer hostContainer, ModuleFactory moduleClassFactory, WebInterfaceManager webInterfaceManager)
    {
        super(moduleClassFactory, webInterfaceManager);
        this.hostContainer = hostContainer;
    }

    @Override
    public void init(final Plugin plugin, Element element) throws PluginParseException
    {
        super.init(plugin, element);
        location = element.attributeValue("location");

        if (moduleClassName == null)
        {
            final ResourceDescriptor resource = getRequiredViewResource();
            final String filename = resource.getLocation();
            if (StringUtils.isEmpty(filename))
            {
                final String body = Preconditions.checkNotNull(resource.getContent());
                webPanelFactory = new Callable<WebPanel>()
                {
                    public WebPanel call() throws Exception
                    {
                        final EmbeddedTemplateWebPanel panel = hostContainer.create(
                                EmbeddedTemplateWebPanel.class);
                        panel.setTemplateBody(body);
                        panel.setResourceType(resource.getType());
                        panel.setPlugin(plugin);
                        return panel;
                    }
                };
            }
            else
            {
                webPanelFactory = new Callable<WebPanel>()
                {
                    public WebPanel call() throws Exception
                    {
                        final ResourceTemplateWebPanel panel = hostContainer.create(
                                ResourceTemplateWebPanel.class);
                        panel.setResourceFilename(filename);
                        panel.setResourceType(resource.getType());
                        panel.setPlugin(plugin);
                        return panel;
                    }
                };
            }
        }
        else
        {
            final String moduleClassNameCopy = moduleClassName;
            webPanelFactory = new Callable<WebPanel>()
            {
                public WebPanel call() throws Exception
                {
                    return moduleFactory.createModule(moduleClassNameCopy, DefaultWebPanelModuleDescriptor.this);
                }
            };
        }

    }

    public String getLocation()
    {
        return location;
    }

    @Override
    public WebPanel getModule()
    {
        try
        {
            return webPanelFactory.call();
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * @return  the (first) resource with attribute <code>name="view"</code>
     * @throws PluginParseException when no resources with name "view" were found.
     */
    private ResourceDescriptor getRequiredViewResource() throws PluginParseException
    {
        final Collection<ResourceDescriptor> resources =
                Collections2.filter(getResourceDescriptors(), new Predicate<ResourceDescriptor>()
            {
                public boolean apply(ResourceDescriptor input)
                {
                    return "view".equals(input.getName());
                }
            });
        if (resources.isEmpty())
        {
            throw new PluginParseException("Required resource with name 'view' does not exist.");
        }
        else
        {
            return resources.iterator().next();
        }
    }
}
