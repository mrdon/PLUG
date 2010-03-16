package com.atlassian.plugin.web.descriptors;

import java.util.Iterator;
import java.util.List;

import com.atlassian.plugin.StateAware;
import com.atlassian.plugin.descriptors.AbstractModuleDescriptor;
import com.atlassian.plugin.loaders.LoaderUtils;
import com.atlassian.plugin.util.validation.ValidationPattern;
import com.atlassian.plugin.web.Condition;
import com.atlassian.plugin.web.ContextProvider;
import com.atlassian.plugin.web.conditions.AbstractCompositeCondition;
import com.atlassian.plugin.web.conditions.AndCompositeCondition;
import com.atlassian.plugin.web.conditions.InvertedCondition;
import com.atlassian.plugin.web.conditions.OrCompositeCondition;
import com.atlassian.plugin.web.model.EmbeddedTemplateWebPanel;
import com.atlassian.plugin.web.model.ResourceTemplateWebPanel;
import com.atlassian.plugin.web.model.WebPanel;
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

import static com.atlassian.plugin.util.validation.ValidationPattern.test;

/**
 * <p>
 * The web panel module declares a single web panel in atlassian-plugin.xml.
 * Its XML element contains a location string that should match existing
 * locations in the host application where web panels can be embedded.
 * </p>
 * <p>
 * A web panel also contains a single resource child element that contains the
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
 * Note that <code>FooWebPanel</code> must implement {@link com.atlassian.plugin.web.model.WebPanel}.
 * </p>
 *
 * @since   2.5.0
 */
public class DefaultWebPanelModuleDescriptor extends AbstractModuleDescriptor<WebPanel> implements WeightedDescriptor, StateAware, ConditionalDescriptor
{
    /**
     * Host applications should use this string when registering the
     * web panel module descriptor.
     */
    public static final String XML_ELEMENT_NAME = "web-panel";

    protected int weight;

    protected Element element;
    protected Condition condition;
    protected ContextProvider contextProvider;
    protected WebInterfaceManager webInterfaceManager;

    private Supplier<WebPanel> webPanelFactory;
    private String location;
    private final HostContainer hostContainer;

    public DefaultWebPanelModuleDescriptor(final HostContainer hostContainer,
                                           final ModuleFactory moduleClassFactory,
                                           final WebInterfaceManager webInterfaceManager)
    {
        super(moduleClassFactory);
        this.hostContainer = hostContainer;
        this.webInterfaceManager = webInterfaceManager;
    }

    @Override
    public void init(final Plugin plugin, final Element element) throws PluginParseException
    {
        super.init(plugin, element);
        this.element = element;

        weight = 1000;
        try
        {
            weight = Integer.parseInt(element.attributeValue("weight"));
        }
        catch (final NumberFormatException e)
        {}

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
                        panel.setResourceType(getRequiredResourceType(resource));
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
                        panel.setResourceType(getRequiredResourceType(resource));
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

    @Override
    protected void provideValidationRules(ValidationPattern pattern)
    {
        super.provideValidationRules(pattern);
        pattern.
                rule(
                    test("@location").withError("The Web Panel location attribute is required."));
    }

    public String getLocation()
    {
        return location;
    }

    public int getWeight()
    {
        return weight;
    }

    public Condition getCondition()
    {
        return condition;
    }

    public ContextProvider getContextProvider()
    {
        return contextProvider;
    }

    @Override
    public WebPanel getModule()
    {
        return webPanelFactory.get();
    }

    private String getRequiredResourceType(ResourceDescriptor resource)
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

    @Override
    public void enabled()
    {
        super.enabled();    //To change body of overridden methods use File | Settings | File Templates.
        // this was moved to the enabled() method because spring beans declared
        // by the plugin are not available for injection during the init() phase
        try
        {
            if (element.element("context-provider") != null)
            {
                contextProvider = makeContextProvider(element.element("context-provider"));
            }
        }
        catch (final PluginParseException e)
        {
            // is there a better exception to throw?
            throw new RuntimeException("Unable to enable web fragment", e);
        }

        webInterfaceManager.refresh();
    }

    @Override
    public void disabled()
    {
        webInterfaceManager.refresh();
        super.disabled();
    }

    /**
     * Create a condition for when this web fragment should be displayed
     *
     * @param element Element of web-section or web-item
     * @param type logical operator type {@link #getCompositeType}
     * @throws com.atlassian.plugin.PluginParseException
     */
    @SuppressWarnings("unchecked")
    protected Condition makeConditions(final Element element, final int type) throws PluginParseException
    {
        // make single conditions (all Anded together)
        final List singleConditionElements = element.elements("condition");
        Condition singleConditions = null;
        if ((singleConditionElements != null) && !singleConditionElements.isEmpty())
        {
            singleConditions = makeConditions(singleConditionElements, type);
        }

        // make composite conditions (logical operator can be specified by
        // "type")
        final List nestedConditionsElements = element.elements("conditions");
        AbstractCompositeCondition nestedConditions = null;
        if ((nestedConditionsElements != null) && !nestedConditionsElements.isEmpty())
        {
            nestedConditions = getCompositeCondition(type);
            for (final Iterator iterator = nestedConditionsElements.iterator(); iterator.hasNext();)
            {
                final Element nestedElement = (Element) iterator.next();
                nestedConditions.addCondition(makeConditions(nestedElement, getCompositeType(nestedElement.attributeValue("type"))));
            }
        }

        if ((singleConditions != null) && (nestedConditions != null))
        {
            // Join together the single and composite conditions by this type
            final AbstractCompositeCondition compositeCondition = getCompositeCondition(type);
            compositeCondition.addCondition(singleConditions);
            compositeCondition.addCondition(nestedConditions);
            return compositeCondition;
        }
        else if (singleConditions != null)
        {
            return singleConditions;
        }
        else if (nestedConditions != null)
        {
            return nestedConditions;
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    protected Condition makeConditions(final List elements, final int type) throws PluginParseException
    {
        if (elements.size() == 0)
        {
            return null;
        }
        else if (elements.size() == 1)
        {
            return makeCondition((Element) elements.get(0));
        }
        else
        {
            final AbstractCompositeCondition compositeCondition = getCompositeCondition(type);
            for (final Iterator it = elements.iterator(); it.hasNext();)
            {
                final Element element = (Element) it.next();
                compositeCondition.addCondition(makeCondition(element));
            }

            return compositeCondition;
        }
    }

    protected Condition makeCondition(final Element element) throws PluginParseException
    {
        try
        {
            final Condition condition = webInterfaceManager.getWebFragmentHelper().loadCondition(element.attributeValue("class"), plugin);
            condition.init(LoaderUtils.getParams(element));

            if ((element.attribute("invert") != null) && "true".equals(element.attributeValue("invert")))
            {
                return new InvertedCondition(condition);
            }

            return condition;
        }
        catch (final ClassCastException e)
        {
            throw new PluginParseException("Configured condition class does not implement the Condition interface");
        }
        catch (final Throwable t)
        {
            throw new PluginParseException(t);
        }
    }

    protected ContextProvider makeContextProvider(final Element element) throws PluginParseException
    {
        try
        {
            final ContextProvider context = webInterfaceManager.getWebFragmentHelper().loadContextProvider(element.attributeValue("class"), plugin);
            context.init(LoaderUtils.getParams(element));

            return context;
        }
        catch (final ClassCastException e)
        {
            throw new PluginParseException("Configured context-provider class does not implement the ContextProvider interface");
        }
        catch (final Throwable t)
        {
            throw new PluginParseException(t);
        }
    }

    private int getCompositeType(final String type) throws PluginParseException
    {
        if ("or".equalsIgnoreCase(type))
        {
            return WebFragmentModuleDescriptor.COMPOSITE_TYPE_OR;
        }
        else if ("and".equalsIgnoreCase(type))
        {
            return WebFragmentModuleDescriptor.COMPOSITE_TYPE_AND;
        }
        throw new PluginParseException("Invalid condition type specified. type = " + type);
    }

    private AbstractCompositeCondition getCompositeCondition(final int type) throws PluginParseException
    {
        switch (type)
        {
        case WebFragmentModuleDescriptor.COMPOSITE_TYPE_OR:
        {
            return new OrCompositeCondition();
        }
        case WebFragmentModuleDescriptor.COMPOSITE_TYPE_AND:
        {
            return new AndCompositeCondition();
        }
        }
        throw new PluginParseException("Invalid condition type specified. type = " + type);
    }
}
