package com.atlassian.plugin.loaders.classloading.osgi.hostcomponents.impl;

import java.util.Map;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;

class Registration<T>
{
    private Class<T>[] mainInterface;
    private T instance;
    private Dictionary<String,String> properties = new Hashtable<String,String>();

    public Registration(Class<T>[] mainInterface)
    {
        this.mainInterface = mainInterface;
    }

    public T getInstance()
    {
        return instance;
    }

    public void setInstance(T instance)
    {
        this.instance = instance;
    }

    public Dictionary<String, String> getProperties()
    {
        return properties;
    }

    public void setProperties(Dictionary<String, String> properties)
    {
        this.properties = properties;
    }

    public Class<T>[] getMainInterface()
    {
        return mainInterface;
    }
}
