package com.atlassian.plugin.web.descriptors;

import static com.atlassian.plugin.util.validation.ValidationPattern.test;

import com.atlassian.plugin.web.conditions.ConditionLoadingException;
import org.dom4j.Element;

import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.descriptors.AbstractModuleDescriptor;
import com.atlassian.plugin.hostcontainer.HostContainer;
import com.atlassian.plugin.module.ModuleFactory;
import com.atlassian.plugin.util.validation.ValidationPattern;
import com.atlassian.plugin.web.Condition;
import com.atlassian.plugin.web.ContextProvider;
import com.atlassian.plugin.web.WebInterfaceManager;
import com.atlassian.plugin.web.model.WebPanel;
import com.google.common.base.Supplier;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * The web panel module declares a single web panel in atlassian-plugin.xml. Its
 * XML element contains a location string that should match existing locations
 * in the host application where web panels can be embedded.
 * </p>
 * <p>
 * A web panel also contains a single resource child element that contains the
 * contents of the web panel. This can be plain HTML, or a (velocity) template
 * to provide dynamic content.
 * </p>
 * <p>
 * A resource element's <code>type</code> attribute identifies the format of the
 * panel's content (currently "static" and "velocity" are supported) which
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
 * 
 * <pre>
 *     &lt;web-panel key="myPanel" location="general">
 *         &lt;resource name="view" type="static">&lt;![CDATA[&lt;b>Hello World!&lt;/b>]]>&lt;/resource>
 *     &lt;/web-panel>
 * </pre>
 * 
 * </p>
 * <p>
 * A web panel that contains an embedded velocity template:
 * 
 * <pre>
 *     &lt;web-panel key="myPanel" location="general">
 *         &lt;resource name="view" type="velocity">&lt;![CDATA[#set($name = 'foo')My name is $name]]>&lt;/resource>
 *     &lt;/web-panel>
 * </pre>
 * 
 * </p>
 * <p>
 * A web panel that contains uses a velocity template that is on the classpath
 * (part of the plugin's jar file):
 * 
 * <pre>
 *     &lt;web-panel key="myPanel" location="general">
 *         &lt;resource name="view" type="velocity" location="templates/pie.vm"/>
 *     &lt;/web-panel>
 * </pre>
 * 
 * </p>
 * <p>
 * Finally it is also possible to provide your own custom class that is
 * responsible for producing the panel's HTML, by using the descriptor's
 * <code>class</code> attribute:
 * 
 * <pre>
 *     &lt;web-panel key="myPanel" location="general" class="com.example.FooWebPanel"/>
 * </pre>
 * 
 * Note that <code>FooWebPanel</code> must implement
 * {@link com.atlassian.plugin.web.model.WebPanel}.
 * </p>
 * 
 * @since 2.5.0
 */
public class DefaultWebPanelModuleDescriptor extends AbstractModuleDescriptor<WebPanel> implements WebPanelModuleDescriptor
{
    /**
     * Host applications should use this string when registering the web panel
     * module descriptor.
     */
    public static final String XML_ELEMENT_NAME = "web-panel";

    private final WebInterfaceManager webInterfaceManager;
    private WebPanelSupplierFactory webPanelSupplierFactory;

    /**
     * These suppliers are used to delay instantiation because the required
     * spring beans are not available for injection during the init() phase.
     */
    private Supplier<WebPanel> webPanelFactory;
    private Supplier<Condition> conditionFactory;
    private Supplier<ContextProvider> contextProviderFactory;

    private int weight;
    private String location;

    public DefaultWebPanelModuleDescriptor(final HostContainer hostContainer, final ModuleFactory moduleClassFactory, final WebInterfaceManager webInterfaceManager)
    {
        super(moduleClassFactory);
        this.webPanelSupplierFactory = new WebPanelSupplierFactory(this, hostContainer, moduleFactory);
        this.webInterfaceManager = webInterfaceManager;
    }

    @Override
    public void init(final Plugin plugin, final Element element) throws PluginParseException
    {
        super.init(plugin, element);

        weight = WeightElementParser.getWeight(element);
        location = element.attributeValue("location");
        conditionFactory = new Supplier<Condition>()
        {
            public Condition get()
            {
                return new ConditionElementParser(new ConditionElementParser.ConditionFactory()
                {
                    public Condition create(String className, Plugin plugin) throws ConditionLoadingException
                    {
                        return webInterfaceManager.getWebFragmentHelper().loadCondition(className, plugin);
                    }
                }).makeConditions(plugin, element, ConditionElementParser.CompositeType.AND);
            }
        };
        contextProviderFactory = new Supplier<ContextProvider>()
        {
            private ContextProvider contextProvider;

            public ContextProvider get()
            {
                if (contextProvider == null)
                {
                    contextProvider = new ContextProviderElementParser(webInterfaceManager.getWebFragmentHelper()).makeContextProvider(plugin, element);
                }
                return contextProvider;
            }
        };

        webPanelFactory = webPanelSupplierFactory.build(moduleClassName);
    }

    private class ContextAwareWebPanel implements WebPanel
    {
        private final WebPanel delegate;

        private ContextAwareWebPanel(WebPanel delegate)
        {
            this.delegate = delegate;
        }

        public String getHtml(final Map<String, Object> context)
        {
            return delegate.getHtml(getContextProvider().getContextMap(new HashMap<String, Object>(context)));
        }
    }

    @Override
    protected void provideValidationRules(final ValidationPattern pattern)
    {
        super.provideValidationRules(pattern);
        pattern.rule(test("@location").withError("The Web Panel location attribute is required."));
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
        return conditionFactory.get();
    }

    public ContextProvider getContextProvider()
    {
        return contextProviderFactory.get();
    }

    @Override
    public WebPanel getModule()
    {
        return new ContextAwareWebPanel(webPanelFactory.get());
    }

    @Override
    public void enabled()
    {
        super.enabled();
        webInterfaceManager.refresh();
    }

    @Override
    public void disabled()
    {
        webInterfaceManager.refresh();
        super.disabled();
    }
}
