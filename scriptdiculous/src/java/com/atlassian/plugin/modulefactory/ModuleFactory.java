package com.atlassian.plugin.modulefactory;

import com.atlassian.plugin.ModuleDescriptor;

/**
 * An abstraction for encapsulating the module instantiation process so that across all
 * ModuleDescriptor implementations, descriptors can declare a delegate to perform the
 * actual creation of the module object. The purpose of this is to enable dynamic
 * module instantiation where the implementation class is unknown until runtime, or
 * where the implementation class is actually a proxy.
 * <p>
 * Implementations should have a default constructor.
 */
public interface ModuleFactory
{

    /**
     * Instantiates the module.
     *
     * @return the new module object.
     */
    public Object getModule();


    /**
     * Gives the ModuleFactory the power to configure itself. 
     * @param moduleDescriptor
     */
    public void setModuleDescriptor(ModuleDescriptor moduleDescriptor);

}
