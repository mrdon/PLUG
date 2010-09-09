package com.atlassian.labs.plugins3.extender;

import com.atlassian.plugin.module.ContainerAccessor;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.util.tracker.ServiceTracker;
import org.springframework.context.ApplicationContext;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collection;

/**
 *
 */
public class SpringContainerAccessor implements ContainerAccessor, Closeable
{
    private final ServiceTracker applicationContextTracker;
    private final BundleContext hostContext;

    public SpringContainerAccessor(BundleContext hostContext, Bundle targetBundle)
    {
        this.hostContext = hostContext;
        Filter filter;
        try
        {
            filter = hostContext.createFilter("(&(Bundle-SymbolicName=" + targetBundle.getSymbolicName()
                    + ")(Bundle-Version=" + targetBundle.getVersion() + ")(objectClass=" + ApplicationContext.class.getName() + "))");
        }
        catch (InvalidSyntaxException e)
        {
            throw new RuntimeException(e);
        }
        this.applicationContextTracker = new ServiceTracker(hostContext, filter, null);
        this.applicationContextTracker.open(true);

    }

    public <T> T createBean(Class<T> tClass)
    {
        return (T) ((ApplicationContext)applicationContextTracker.getService()).getAutowireCapableBeanFactory().createBean(tClass);
    }

    public <T> Collection<T> getBeansOfType(Class<T> tClass)
    {
        return (Collection<T>) ((ApplicationContext)applicationContextTracker.getService()).getBeansOfType(tClass);
    }

    public void close() throws IOException
    {
        this.applicationContextTracker.close();
    }
}
