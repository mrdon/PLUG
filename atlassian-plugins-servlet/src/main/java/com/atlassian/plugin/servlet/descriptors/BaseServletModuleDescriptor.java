package com.atlassian.plugin.servlet.descriptors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Element;

import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.descriptors.AbstractModuleDescriptor;

public abstract class BaseServletModuleDescriptor<T> extends AbstractModuleDescriptor<T>
{
    protected static final Log log = LogFactory.getLog(BaseServletModuleDescriptor.class);

    List<String> paths;
    private Map<String,String> initParams;

    public void init(Plugin plugin, Element element) throws PluginParseException
    {
        super.init(plugin, element);
    
        List<Element> urlPatterns = element.elements("url-pattern");
        paths = new ArrayList<String>(urlPatterns.size());
    
        for (Iterator<Element> iterator = urlPatterns.iterator(); iterator.hasNext();)
        {
            Element urlPattern = iterator.next();
            paths.add(urlPattern.getTextTrim());
        }
    
        initParams = new HashMap<String,String>();
        List<Element> paramsList = element.elements("init-param");
        for (Iterator<Element> i = paramsList.iterator(); i.hasNext();) {
            Element initParamEl = i.next();
            Element paramNameEl = initParamEl.element("param-name");
            Element paramValueEl = initParamEl.element("param-value");
            if (paramNameEl != null && paramValueEl != null) {
                initParams.put(paramNameEl.getTextTrim(), paramValueEl.getTextTrim());
            } else {
                log.warn("Invalid init-param XML for servlet module: " + getCompleteKey());
            }
        }
    }

    public List<String> getPaths()
    {
        return paths;
    }

    public Map<String,String> getInitParams()
    {
        return initParams;
    }

}
