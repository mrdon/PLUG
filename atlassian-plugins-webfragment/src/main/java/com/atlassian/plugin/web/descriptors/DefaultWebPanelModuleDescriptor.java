package com.atlassian.plugin.web.descriptors;

import java.util.Iterator;

import org.apache.commons.lang.StringUtils;
import org.dom4j.Element;

import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.elements.ResourceDescriptor;
import com.atlassian.plugin.hostcontainer.HostContainer;
import com.atlassian.plugin.module.ModuleFactory;
import com.atlassian.plugin.web.WebInterfaceManager;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Supplier;
import com.google.common.collect.Iterables;

public class DefaultWebPanelModuleDescriptor extends AbstractWebFragmentModuleDescriptor<WebPanel>
{
    private Supplier<WebPanel> webPanelFactory;
    private String location;
    private final HostContainer hostContainer;

    public DefaultWebPanelModuleDescriptor(final HostContainer hostContainer, final ModuleFactory moduleClassFactory, final WebInterfaceManager webInterfaceManager)
    {
        super(moduleClassFactory, webInterfaceManager);
        this.hostContainer = hostContainer;
    }

    @Override
    public void init(final Plugin plugin, final Element element) throws PluginParseException
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
                webPanelFactory = new Supplier<WebPanel>()
                {
                    public WebPanel get()
                    {
                        final EmbeddedTemplateWebPanel panel = hostContainer.create(EmbeddedTemplateWebPanel.class);
                        panel.setTemplateBody(body);
                        panel.setResourceType(resource.getType());
                        panel.setPlugin(plugin);
                        return panel;
                    }
                };
            }
            else
            {
                webPanelFactory = new Supplier<WebPanel>()
                {
                    public WebPanel get()
                    {
                        final ResourceTemplateWebPanel panel = hostContainer.create(ResourceTemplateWebPanel.class);
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
            webPanelFactory = new Supplier<WebPanel>()
            {
                public WebPanel get()
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
        return webPanelFactory.get();
    }

    /**
     * @return the (first) resource with attribute <code>name="view"</code>
     * @throws PluginParseException when no resources with name "view" were
     *             found.
     */
    private ResourceDescriptor getRequiredViewResource() throws PluginParseException
    {
        final Iterable<ResourceDescriptor> resources = Iterables.filter(getResourceDescriptors(), new Predicate<ResourceDescriptor>()
        {
            public boolean apply(final ResourceDescriptor input)
            {
                return "view".equals(input.getName());
            }
        });
        final Iterator<ResourceDescriptor> iterator = resources.iterator();
        if (!iterator.hasNext())
        {
            throw new PluginParseException("Required resource with name 'view' does not exist.");
        }
        else
        {
            return iterator.next();
        }
    }
}
