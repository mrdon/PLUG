package com.atlassian.plugin.servlet.descriptors;

import org.apache.commons.lang.StringUtils;
import org.dom4j.Element;

import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginParseException;
import static com.atlassian.plugin.util.validation.ValidatePattern.createPattern;
import static com.atlassian.plugin.util.validation.ValidatePattern.test;
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
    
    @Override
    public void init(Plugin plugin, Element element) throws PluginParseException
    {
        createPattern().
                rule(
                    test("param-name").withError("Parameter name is required"),
                    test("param-value").withError("Parameter value is required")).
                evaluate(element);

        super.init(plugin, element);
        
        paramName = element.elementTextTrim("param-name");
        paramValue = element.elementTextTrim("param-value");
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
