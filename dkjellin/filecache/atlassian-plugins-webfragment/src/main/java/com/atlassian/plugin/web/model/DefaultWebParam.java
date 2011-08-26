package com.atlassian.plugin.web.model;

import com.atlassian.plugin.web.ContextProvider;
import com.atlassian.plugin.web.WebFragmentHelper;
import com.atlassian.plugin.web.descriptors.WebFragmentModuleDescriptor;
import com.atlassian.plugin.loaders.LoaderUtils;

import java.util.Map;
import java.util.TreeMap;
import java.util.SortedMap;

import org.dom4j.Element;

/**
 * Represents a sorted map of parameters. Individual value of the param can be rendered using velocity.
 */
public class DefaultWebParam extends AbstractWebItem implements WebParam
{
    /**
     * parameters are sorted in order for the i18n arguments to be in order
     */
    protected SortedMap<String,String> params;

    public DefaultWebParam(Element element, WebFragmentHelper webFragmentHelper, ContextProvider contextProvider, WebFragmentModuleDescriptor descriptor)
    {
        super(webFragmentHelper, contextProvider, descriptor);
        this.params =  new TreeMap<String,String>(LoaderUtils.getParams(element));
    }

    public DefaultWebParam(Map<String,String> params, WebFragmentHelper webFragmentHelper, ContextProvider contextProvider, WebFragmentModuleDescriptor descriptor)
    {
        super(webFragmentHelper, contextProvider, descriptor);
        this.params = new TreeMap<String,String>(params);
    }

    public SortedMap<String,String> getParams()
    {
        return params;
    }

    public Object get(String key)
    {
        return params.get(key);
    }

    public String getRenderedParam(String paramKey, Map<String,Object> context)
    {
        context.putAll(getContextMap(context));
        return getWebFragmentHelper().renderVelocityFragment((String) params.get(paramKey), context);
    }
}
