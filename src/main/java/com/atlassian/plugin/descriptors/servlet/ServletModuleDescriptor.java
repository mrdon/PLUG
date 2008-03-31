package com.atlassian.plugin.descriptors.servlet;

import com.atlassian.plugin.descriptors.AbstractModuleDescriptor;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.StateAware;
import org.dom4j.Element;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.servlet.http.HttpServlet;
import java.util.*;

public abstract class ServletModuleDescriptor extends AbstractModuleDescriptor implements StateAware
{
    private static final Log log = LogFactory.getLog(ServletModuleDescriptor.class);
    List paths;
    private Map initParams;

    public void init(Plugin plugin, Element element) throws PluginParseException
    {
        super.init(plugin, element);

        List urlPatterns = element.elements("url-pattern");
        paths = new ArrayList(urlPatterns.size());

        for (Iterator iterator = urlPatterns.iterator(); iterator.hasNext();)
        {
            Element urlPattern = (Element) iterator.next();
            paths.add(urlPattern.getTextTrim());
        }

        initParams = new HashMap();
        List paramsList = element.elements("init-param");
        for (Iterator iterator = paramsList.iterator(); iterator.hasNext();)
        {
            Element initParamEl = (Element) iterator.next();
            Element paramNameEl = initParamEl.element("param-name");
            Element paramValueEl = initParamEl.element("param-value");
            if (paramNameEl != null && paramValueEl != null)
            {
                initParams.put(paramNameEl.getTextTrim(), paramValueEl.getTextTrim());
            }
            else
            {
                log.warn("Invalid init-param XML for servlet module: " + getCompleteKey());
            }
        }
    }

    public void enabled()
    {
        getServletModuleManager().addModule(this);
    }

    public void disabled()
    {
        getServletModuleManager().removeModule(this);
    }

    public Object getModule()
    {
        Object obj = null;
        try
        {
            obj = getModuleClass().newInstance();
            autowireObject(obj);
        }
        catch (InstantiationException e)
        {
            log.error("Error instantiating: " + getModuleClass(), e);
        }
        catch (IllegalAccessException e)
        {
            log.error("Error accessing: " + getModuleClass(), e);
        }
        return obj;
    }

    public HttpServlet getServlet()
    {
        return (HttpServlet)getModule();
    }

    public List getPaths()
    {
        return paths;
    }

    public Map getInitParams()
    {
        return initParams;
    }

    /**
     * Autowire an object. Implement this in your IoC framework or simply do nothing.
     */
    protected abstract void autowireObject(Object obj);

    /**
     * Retrieve the ServletModuleManager class from your container framework.
     */
    protected abstract ServletModuleManager getServletModuleManager();
}
