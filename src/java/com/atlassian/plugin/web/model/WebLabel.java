package com.atlassian.plugin.web.model;

import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.web.WebFragmentHelper;
import com.atlassian.plugin.web.ContextProvider;
import com.atlassian.plugin.web.descriptors.AbstractWebFragmentModuleDescriptor;
import org.dom4j.Element;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

/**
 * A simple bean to represent labels in the web interface.
 */
public class WebLabel extends WebParam
{
    String key;
    String noKeyValue;

    public WebLabel(Element labelEl, WebFragmentHelper webFragmentHelper, ContextProvider contextProvider, AbstractWebFragmentModuleDescriptor descriptor) throws PluginParseException
    {
        super(labelEl, webFragmentHelper, contextProvider, descriptor);
        if (labelEl == null)
        {
            throw new PluginParseException("You must specify a label for the section.");
        }
        else
        {
            this.key = labelEl.attributeValue("key");

            if (this.key == null)
            {
                this.noKeyValue = labelEl.getTextTrim();
            }
        }
    }

    public String getKey()
    {
        return key;
    }

    public String getNoKeyValue()
    {
        return noKeyValue;
    }

    public String getDisplayableLabel(HttpServletRequest req, Map context)
    {
        context.putAll(getContextMap());
        if (key != null)
        {
            if (params == null || params.isEmpty())
            {
                return getWebFragmentHelper().getI18nValue(key, null, context);
            }
            else
            {
                List arguments = new ArrayList();

                // we know here because it's a tree map that the params are in alphabetical order
                for (Iterator iterator = params.keySet().iterator(); iterator.hasNext();)
                {
                    String key = (String) iterator.next();
                    if (key.startsWith("param"))
                        arguments.add(getWebFragmentHelper().renderVelocityFragment((String) params.get(key), context));
                }

                return getWebFragmentHelper().getI18nValue(key, arguments, context);
            }
        }
        else
        {
            return getWebFragmentHelper().renderVelocityFragment(noKeyValue, context);
        }
    }
}
