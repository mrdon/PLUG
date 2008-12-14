package com.atlassian.plugin.scripting.variables;

import org.dom4j.Element;
import org.dom4j.DocumentFactory;
import org.dom4j.DocumentException;
import org.dom4j.Document;
import org.dom4j.io.SAXReader;

import java.io.StringReader;

import com.atlassian.plugin.PluginException;

/**
 * Created by IntelliJ IDEA.
 * User: mrdon
 * Date: Dec 14, 2008
 * Time: 8:50:43 PM
 * To change this template use File | Settings | File Templates.
 */
public class JsScript
{
    private final String path;
    private Document config;
    private Object result;

    public JsScript(String path)
    {
        this.path = path;
    }

    public String getPath()
    {
        return path;
    }

    public Document getConfig()
    {
        return config;
    }

    public void setConfig(Object config)
    {
        SAXReader reader = new SAXReader();
        try
        {
            this.config = reader.read(new StringReader(config.toString()));
        }
        catch (DocumentException e)
        {
            throw new PluginException("Invalid configuration XML: "+config.toString(), e);
        }
    }

    public Object getResult()
    {
        return result;
    }

    public void setResult(Object result)
    {
        this.result = result;
    }
}
