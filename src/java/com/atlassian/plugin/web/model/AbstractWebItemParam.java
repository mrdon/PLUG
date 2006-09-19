package com.atlassian.plugin.web.model;

import com.atlassian.plugin.web.ContextProvider;
import com.atlassian.plugin.web.WebFragmentHelper;
import com.atlassian.plugin.loaders.LoaderUtils;

import java.util.Map;
import java.util.HashMap;

import org.dom4j.Element;

public class AbstractWebItemParam extends AbstractWebItem
{
    protected Map params;

    protected AbstractWebItemParam(Element element, WebFragmentHelper webFragmentHelper, ContextProvider contextProvider)
    {
        super(webFragmentHelper, contextProvider);
        this.params = LoaderUtils.getParams(element);
    }

    public Map getParams()
    {
        return params;
    }

    public String getRenderedParam(String paramKey)
    {
        return getRenderedParam(paramKey, new HashMap());
    }

    public String getRenderedParam(String paramKey, Map context)
    {
        context.putAll(getContextMap());
        return webFragmentHelper.renderVelocityFragment((String) params.get(paramKey), context);
    }
}
