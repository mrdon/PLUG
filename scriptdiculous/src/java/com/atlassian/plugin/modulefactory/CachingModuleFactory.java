package com.atlassian.plugin.modulefactory;

import com.atlassian.plugin.ModuleDescriptor;

/**
 * A trivial caching {@link com.atlassian.plugin.modulefactory.ModuleFactory} that acts as decorator to another.
 * Use this when your modules are expensive to create. Be careful not to leak references to this object since it
 * will hold a reference to the instance after the first call to getModule. For resource cleanup purposes, call
 * {@link #clear()} .
 * <p>
 * Should be thread safe.
 */
public class CachingModuleFactory implements ModuleFactory
{

    /**
     * The cached module object.
     */
    private Object instance;

    /**
     * A mutual exclusion lock for reads and writes on the instance.
     */
    final private Object mutex = new Object();

    /**
     * The wrapped ModuleFactory.
     */
    private ModuleFactory delegate;

    /**
     * Creates the CachingModuleFactory which will keep a lazily created instance of the delegate's module.
     *
     * @param delegate the implementation to which this will delegate actual creation.
     */
    public CachingModuleFactory(ModuleFactory delegate)
    {
        this.delegate = delegate;
    }

    /**
     * Returns the cached instance of the delegate's module that was acquired from the delegate on the first call.
     * If this is the first call, the delegate will be consulted and concurrent threads will block until this is
     * complete.
     *
     * @return
     */
    public Object getModule()
    {
        synchronized (mutex)
        {
            if (instance == null)
            {
                instance = delegate.getModule();
            }
            return instance;
        }
    }


    /**
     * Passes through to the delegate always because this method is a configuration time call.
     * @param moduleDescriptor represents the resources available to the ModuleFactory.
     */
    public void setModuleDescriptor(ModuleDescriptor moduleDescriptor)
    {
        delegate.setModuleDescriptor(moduleDescriptor);
    }


    /**
     * Empties the cache enabling a fresh recreation of the object
     */
    public void clear()
    {
        synchronized (mutex)
        {
            instance = null;
        }
    }

}
