package com.atlassian.plugin.web.descriptors;

import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.elements.ResourceDescriptor;
import com.atlassian.plugin.hostcontainer.HostContainer;
import com.atlassian.plugin.module.ModuleFactory;
import com.atlassian.plugin.web.model.EmbeddedTemplateWebPanel;
import com.atlassian.plugin.web.model.ResourceTemplateWebPanel;
import com.atlassian.plugin.web.model.WebPanel;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Supplier;
import com.google.common.collect.Iterables;
import org.apache.commons.lang.StringUtils;

import java.util.Iterator;

/**
 * Produces {@link Supplier} objects that in turn produce {@link WebPanel}
 * instances.
 * <br>
 * This class exists to simplify the {@link DefaultWebPanelModuleDescriptor} and
 * make it more testable.
 */
class WebPanelSupplierFactory
{
    private final WebPanelModuleDescriptor webPanelModuleDescriptor;
    private final HostContainer hostContainer;
    private final ModuleFactory moduleFactory;

    public WebPanelSupplierFactory(
        WebPanelModuleDescriptor webPanelModuleDescriptor,
        HostContainer hostContainer, ModuleFactory moduleFactory)
    {
        this.webPanelModuleDescriptor = webPanelModuleDescriptor;
        this.hostContainer = hostContainer;
        this.moduleFactory = moduleFactory;
    }

    public Supplier<WebPanel> build(final String moduleClassName)
    {
        if (moduleClassName != null)
        {
            // If a classname is specified return the WebPanel for that class
            return new Supplier<WebPanel>()
            {
                public WebPanel get()
                {
                    return moduleFactory.createModule(moduleClassName, webPanelModuleDescriptor);
                }
            };
        }

        final ResourceDescriptor resource = getRequiredViewResource();
        final String filename = resource.getLocation();
        if (StringUtils.isNotEmpty(filename))
        {
            // If a resource file is specified it is the template for the panel.
            return new Supplier<WebPanel>()
            {
                public WebPanel get()
                {
                    final ResourceTemplateWebPanel panel = hostContainer.create(ResourceTemplateWebPanel.class);
                    panel.setResourceFilename(filename);
                    panel.setResourceType(getRequiredResourceType(resource));
                    panel.setPlugin(webPanelModuleDescriptor.getPlugin());
                    return panel;
                }
            };
        }

        // If no resource file is specified the panel template must be
        // embedded.
        final String body = Preconditions.checkNotNull(resource.getContent());
        return new Supplier<WebPanel>()
        {
            public WebPanel get()
            {
                final EmbeddedTemplateWebPanel panel = hostContainer.create(EmbeddedTemplateWebPanel.class);
                panel.setTemplateBody(body);
                panel.setResourceType(getRequiredResourceType(resource));
                panel.setPlugin(webPanelModuleDescriptor.getPlugin());
                return panel;
            }
        };
    }

    /**
     * @return the (first) resource with attribute <code>name="view"</code>
     * @throws PluginParseException when no resources with name "view" were
     *             found.
     */
    private ResourceDescriptor getRequiredViewResource() throws PluginParseException
    {
        final Iterable<ResourceDescriptor> resources = Iterables.filter(webPanelModuleDescriptor.getResourceDescriptors(), new Predicate<ResourceDescriptor>()
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

    private String getRequiredResourceType(final ResourceDescriptor resource)
    {
        final String type = resource.getType();
        if (StringUtils.isEmpty(type))
        {
            throw new PluginParseException("Resource element is lacking a type attribute.");
        }
        else
        {
            return type;
        }
    }
}
