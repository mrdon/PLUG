package com.atlassian.plugin.osgi.hostcomponents.impl;

import com.atlassian.plugin.osgi.hostcomponents.HostComponentRegistration;

import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.framework.ServiceRegistration;


/**
 * A registration of a host component
 */
class Registration implements HostComponentRegistration
{
    private String[] mainInterfaces;
    private Object instance;
    private Dictionary<String,String> properties = new Hashtable<String,String>();

    public Registration(Class[] ifs)
    {
        mainInterfaces = new String[ifs.length];
        for (int x=0; x<ifs.length; x++)
            mainInterfaces[x] = ifs[x].getName();
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

    public String[] getMainInterfaces()
    {
        return mainInterfaces;
    }

}
