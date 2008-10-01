package com.atlassian.plugin.osgi.factory.transform;

import com.atlassian.plugin.osgi.hostcomponents.HostComponentRegistration;

import java.util.Dictionary;
import java.util.Hashtable;

public class StubHostComponentRegistration implements HostComponentRegistration
{
    private String[] mainInterfaces;
    private Dictionary<String,String> properties;
    private Class[] mainInterfaceClasses;

    public StubHostComponentRegistration(Class... ifs)
    {
        this(null, ifs);
    }

    public StubHostComponentRegistration(String name, Class... ifs)
    {
        this.mainInterfaceClasses = ifs;
        mainInterfaces = new String[ifs.length];
        for (int x=0; x<ifs.length; x++)
            mainInterfaces[x] = ifs[x].getName();
        this.properties = new Hashtable<String,String>();
        if (name != null)
            properties.put("bean-name", name);
    }

    public StubHostComponentRegistration(String[] ifs, Dictionary<String,String> props)
    {
        mainInterfaces = ifs;
        this.properties = props;
    }

    public Object getInstance()
    {
        return null;
    }

    public Class[] getMainInterfaceClasses()
    {
        return mainInterfaceClasses;
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