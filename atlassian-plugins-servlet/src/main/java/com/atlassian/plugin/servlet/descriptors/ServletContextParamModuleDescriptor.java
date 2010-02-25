package com.atlassian.plugin.servlet.descriptors;

import com.atlassian.plugin.module.ModuleClassFactory;
import org.dom4j.Element;

import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginParseException;
import static com.atlassian.plugin.util.validation.ValidationPattern.test;
import com.atlassian.plugin.util.validation.ValidationPattern;
import com.atlassian.plugin.descriptors.AbstractModuleDescriptor;

/**
 * Allows plugin developers to specify init parameters they would like added to the plugin local {@link javax.servlet.ServletContext}.
 *
 * @since 2.1.0
 */
public class ServletContextParamModuleDescriptor extends AbstractModuleDescriptor<Void>
{
    private String paramName;
    private String paramValue;

    public ServletContextParamModuleDescriptor()
    {
        super(ModuleClassFactory.NOOP_MODULE_CREATOR);
    }

    @Override
    public void init(Plugin plugin, Element element) throws PluginParseException
    {
        super.init(plugin, element);
        
        paramName = element.elementTextTrim("param-name");
        paramValue = element.elementTextTrim("param-value");
    }

    @Override
    protected void provideValidationRules(ValidationPattern pattern)
    {
        super.provideValidationRules(pattern);
        pattern.
                rule(
                    test("param-name").withError("Parameter name is required"),
                    test("param-value").withError("Parameter value is required"));
    }

    public String getParamName()
    {
        return paramName;
    }

    public String getParamValue()
    {
        return paramValue;
    }

    @Override
    public Void getModule()
    {
        return null;
    }
}
