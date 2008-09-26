package com.atlassian.plugin.servlet.descriptors;

import org.apache.commons.lang.StringUtils;
import org.dom4j.Element;

import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.descriptors.AbstractModuleDescriptor;

/**
 * Allows plugin developers to specify init parameters they would like added to the plugin local {@link ServletContext}.
 */
public class ServletContextParamDescriptor extends AbstractModuleDescriptor<Void>
{
    private String paramName;
    private String paramValue;
    
    @Override
    public void init(Plugin plugin, Element element) throws PluginParseException
    {
        super.init(plugin, element);
        
        paramName = element.elementTextTrim("param-name");
        if (StringUtils.isEmpty(paramName))
            throw new IllegalArgumentException("param-name must be specified");
        
        paramValue = element.elementTextTrim("param-value");
        if (StringUtils.isEmpty(paramValue))
            throw new IllegalArgumentException("param-value must be specified");
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
