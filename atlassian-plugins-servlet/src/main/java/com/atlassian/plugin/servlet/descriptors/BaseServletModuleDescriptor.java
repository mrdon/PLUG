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
import static com.atlassian.plugin.util.validation.ValidatePattern.createPattern;
import static com.atlassian.plugin.util.validation.ValidatePattern.test;
import com.atlassian.plugin.hostcontainer.HostContainer;
import com.atlassian.plugin.descriptors.AbstractModuleDescriptor;

/**
 * Acts as a base for other servlet type module descriptors to inherit.  It adds parsing and retrieval of any paths
 * declared in the descriptor with &lt;url-pattern&gt; as well as &lt;init-param&gt;s. 
 * 
 * @since 2.1.0
 */
public abstract class BaseServletModuleDescriptor<T> extends AbstractModuleDescriptor<T>
{
    protected static final Log log = LogFactory.getLog(BaseServletModuleDescriptor.class);

    private List<String> paths;
    private Map<String,String> initParams;

    public void init(Plugin plugin, Element element) throws PluginParseException
    {
        createPattern().
                rule(
                    test("@class").withError("The class is required"),
                    test("url-pattern").withError("There must be at least one path specified")).
                rule("init-param",
                    test("param-name").withError("Parameter name is required"),
                    test("param-value").withError("Parameter value is required")).
                evaluate(element);

        super.init(plugin, element);

        List<Element> urlPatterns = element.elements("url-pattern");
        paths = new ArrayList<String>(urlPatterns.size());

        for (Element urlPattern : urlPatterns)
        {
            paths.add(urlPattern.getTextTrim());
        }

        initParams = new HashMap<String,String>();
        List<Element> paramsList = element.elements("init-param");
        for (Element initParamEl : paramsList)
        {
            Element paramNameEl = initParamEl.element("param-name");
            Element paramValueEl = initParamEl.element("param-value");
            initParams.put(paramNameEl.getTextTrim(), paramValueEl.getTextTrim());
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
