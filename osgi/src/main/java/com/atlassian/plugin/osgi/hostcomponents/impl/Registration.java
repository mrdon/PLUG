package com.atlassian.plugin.osgi.hostcomponents.impl;

import java.util.Dictionary;
import java.util.Hashtable;


/**
 * A registration of a host component
 */
class Registration
{
    private Class[] mainInterface;
    private Object instance;
    private Dictionary<String,String> properties = new Hashtable<String,String>();

    public Registration(Class[] mainInterface)
    {
        this.mainInterface = mainInterface;
    }

    public Object getInstance()
    {
        return instance;
    }

    public void setInstance(Object instance)
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

    public Class[] getMainInterface()
    {
        return mainInterface;
    }
}
