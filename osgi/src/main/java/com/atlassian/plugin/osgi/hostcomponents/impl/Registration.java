package com.atlassian.plugin.osgi.hostcomponents.impl;

import java.util.Dictionary;
import java.util.Hashtable;


/**
 * A registration of a host component
 */
class Registration
{
    private Class[] mainInterfaces;
    private Object instance;
    private Dictionary<String,String> properties = new Hashtable<String,String>();

    public Registration(Class[] mainInterfaces)
    {
        this.mainInterfaces = mainInterfaces;
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

    public Class[] getMainInterfaces()
    {
        return mainInterfaces;
    }
}
