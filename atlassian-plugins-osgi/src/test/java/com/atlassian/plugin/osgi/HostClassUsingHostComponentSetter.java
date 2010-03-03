package com.atlassian.plugin.osgi;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Ryan Thomas
 */
public class HostClassUsingHostComponentSetter
{
    private SomeInterface someInterface;

    public HostClassUsingHostComponentSetter()
    {
    }

    public void setSomeInterface(SomeInterface someInterface)
    {
        this.someInterface = someInterface;
    }

    public SomeInterface getSomeInterface()
    {
        return someInterface;
    }
}
