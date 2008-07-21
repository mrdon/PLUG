package com.atlassian.plugin.osgi.hostcomponents.impl;

import com.atlassian.plugin.osgi.hostcomponents.HostComponentRegistration;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Arrays;

import org.osgi.framework.ServiceRegistration;


/**
 * A registration of a host component
 */
class Registration implements HostComponentRegistration
{
    private String[] mainInterfaces;
    private Class[] mainInterfaceClasses;
    private Object instance;
    private Dictionary<String,String> properties = new Hashtable<String,String>();

    public Registration(Class[] ifs)
    {
        this.mainInterfaceClasses = ifs;
        mainInterfaces = new String[ifs.length];
        for (int x=0; x<ifs.length; x++)
        {
            if (!ifs[x].isInterface())
                throw new IllegalArgumentException("Services can only be registered against interfaces");
            
            mainInterfaces[x] = ifs[x].getName();
        }
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

    public Class[] getMainInterfaceClasses()
    {
        return mainInterfaceClasses;
    }

    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Registration that = (Registration) o;

        if (!instance.equals(that.instance)) return false;
        if (!Arrays.equals(mainInterfaces, that.mainInterfaces)) return false;
        if (!properties.equals(that.properties)) return false;

        return true;
    }

    public int hashCode()
    {
        int result;
        result = Arrays.hashCode(mainInterfaces);
        result = 31 * result + instance.hashCode();
        result = 31 * result + properties.hashCode();
        return result;
    }
}
