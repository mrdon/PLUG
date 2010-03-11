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

/**
 * <p>
 * The web panel module declares a single web panel in atlassian-plugin.xml.
 * Its XML element contains a location string that should match existing
 * locations in the host application where web panels can be embedded.
 * </p>
 * <p>
 * A web panel also contain a single resource child element that contains the
 * contents of the web panel. This can be plain HTML, or a (velocity) template
 * to provide dynamic content.
 * </p>
 * <p>
 * A resource element's <code>type</code> attribute identifies the format of
 * the panel's content (currently "static" and "velocity" are supported) which
 * allows the plugin framework to use the appropriate
 * {@link com.atlassian.plugin.web.renderer.WebPanelRenderer}.
 * </p>
 * <p>
 * A web panel's resource element can either contain its contents embedded in
 * the resource element itself, as part of the <code>atlassian-plugin.xml</code>
 * file, or it can link to a file on the classpath when the
 * <code>location</code> attribute is used.
 * </p>
 * <b>Examples</b>
 * <p>
 * A web panel that contains static, embedded HTML:
 * <pre>
 *     &lt;web-panel key="myPanel" location="general">
 *         &lt;resource name="view" type="static">&lt;![CDATA[&lt;b>Hello World!&lt;/b>]]>&lt;/resource>
 *     &lt;/web-panel>
 * </pre>
 * </p>
 * <p>
 * A web panel that contains an embedded velocity template:
 * <pre>
 *     &lt;web-panel key="myPanel" location="general">
 *         &lt;resource name="view" type="velocity">&lt;![CDATA[#set($name = 'foo')My name is $name]]>&lt;/resource>
 *     &lt;/web-panel>
 * </pre>
 * </p>
 * <p>
 * A web panel that contains uses a velocity template that is on the classpath
 * (part of the plugin's jar file):
 * <pre>
 *     &lt;web-panel key="myPanel" location="general">
 *         &lt;resource name="view" type="velocity" location="templates/pie.vm"/>
 *     &lt;/web-panel>
 * </pre>
 * </p>
 * <p>
 * Finally it is also possible to provide your own custom class that is
 * responsible for producing the panel's HTML, by using the descriptor's
 * <code>class</code> attribute:
 * <pre>
 *     &lt;web-panel key="myPanel" location="general" class="com.example.FooWebPanel"/>
 * </pre>
 * Note that <code>FooWebPanel</code> must implement {@link com.atlassian.plugin.web.descriptors.WebPanel}.
 * </p>
 *
 * @since   2.5.0
 */
public class DefaultWebPanelModuleDescriptor extends AbstractWebFragmentModuleDescriptor<WebPanel>
{
    /**
     * Host applications should use this string when registering the
     * web panel module descriptor.
     */
    public static final String XML_ELEMENT_NAME = "web-panel";
    private Supplier<WebPanel> webPanelFactory;
    private String location;
    private final HostContainer hostContainer;

    public DefaultWebPanelModuleDescriptor(final HostContainer hostContainer,
                                           final ModuleFactory moduleClassFactory,
                                           final WebInterfaceManager webInterfaceManager)
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
