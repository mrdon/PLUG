package com.atlassian.plugin.servlet.descriptors;

import com.atlassian.plugin.AutowireCapablePlugin;
import com.atlassian.plugin.StateAware;
import com.atlassian.plugin.servlet.ServletModuleManager;

import javax.servlet.http.HttpServlet;

/**
 * A module descriptor that allows plugin developers to define servlets. Developers can define what urls the
 * servlet should be serve by defining one or more &lt;url-pattern&gt; elements.
 */
public abstract class ServletModuleDescriptor<T extends HttpServlet> extends BaseServletModuleDescriptor<T> implements StateAware
{
    public void enabled()
    {
        super.enabled();
        getServletModuleManager().addServletModule(this);
    }

    public void disabled()
    {
        getServletModuleManager().removeServletModule(this);
        super.disabled();
    }

    public T getModule()
    {
        T obj = null;
        try
        {
            // Give the plugin a go first
            if (plugin instanceof AutowireCapablePlugin)
            {
                obj = ((AutowireCapablePlugin) plugin).autowire(getModuleClass());
            }
            else
            {
                obj = getModuleClass().newInstance();
                autowireObject(obj);
            }
        }
        catch (InstantiationException e)
        {
            log.error("Error instantiating: " + getModuleClass(), e);
        }
        catch (IllegalAccessException e)
        {
            log.error("Error accessing: " + getModuleClass(), e);
        }
        return obj;
    }

    /**
     * @deprecated Since 2.0.0, use {@link #getModule}
     */
    public T getServlet()
    {
        return getModule();
    }

    /**
     * Autowire an object. Implement this in your IoC framework or simply do nothing.
     */
    protected abstract void autowireObject(Object obj);

    /**
     * Retrieve the DefaultServletModuleManager class from your container framework.
     */
    protected abstract ServletModuleManager getServletModuleManager();
}
