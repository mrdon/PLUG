package com.atlassian.plugin.web.descriptors;

import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.descriptors.AbstractModuleDescriptor;
import com.atlassian.plugin.module.ModuleFactory;
import com.atlassian.plugin.util.validation.ValidationPattern;
import com.atlassian.plugin.web.renderer.WebPanelRenderer;
import org.dom4j.Element;

import static com.atlassian.plugin.util.validation.ValidationPattern.test;

/**
 * The web panel renderer module is used to add web panel renderers to the
 * plugin system.
 *
 * @since   2.5.0
 */
public class WebPanelRendererModuleDescriptor extends AbstractModuleDescriptor<WebPanelRenderer>
{
    /**
     * Host applications should use this string when registering the
     * {@link WebPanelRendererModuleDescriptor}.
     */
    public static final String XML_ELEMENT_NAME = "web-panel-renderer";
    private WebPanelRenderer rendererModule;

    public WebPanelRendererModuleDescriptor(ModuleFactory moduleClassFactory)
    {
        super(moduleClassFactory);
    }

    @Override
    public void init(Plugin plugin, Element element) throws PluginParseException
    {
        super.init(plugin, element);
    }

    @Override
    protected void provideValidationRules(ValidationPattern pattern)
    {
        super.provideValidationRules(pattern);
        pattern.
                rule(
                    test("@class").withError("The class is required"));
    }

    @Override
    public void enabled()
    {
        super.enabled();
        if (!(WebPanelRenderer.class.isAssignableFrom(getModuleClass())))
        {
            throw new PluginParseException(String.format(
                    "Supplied module class (%s) is not a %s", getModuleClass().getName(), WebPanelRenderer.class.getName()));
        }
    }

    @Override
    public WebPanelRenderer getModule()
    {
        if (rendererModule == null) {
            rendererModule = moduleFactory.createModule(moduleClassName, this);
        }
        return rendererModule;
    }
}
