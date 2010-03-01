package com.atlassian.plugin.module;

import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.PluginParseException;

import java.lang.reflect.Constructor;

/**
 * Created by IntelliJ IDEA.
 * User: ervzijst
 * Date: Mar 1, 2010
 * Time: 5:30:37 PM
 * To change this template use File | Settings | File Templates.
 */
public class LegacyModuleClassFactory implements ModuleClassFactory {

    public <T> T createModuleClass(String name, ModuleDescriptor<T> moduleDescriptor) throws PluginParseException
    {
        throw new UnsupportedOperationException(" create Module Class not supported by LegacyModuleClassFactory. Use DefaultModuleClassFactory instead.");
    }

    public <T> Class<T> getModuleClass(String name, ModuleDescriptor<T> moduleDescriptor) throws ModuleClassNotFoundException {

        try
        {
            // First try and load the class, to make sure the class exists
            @SuppressWarnings ("unchecked")
            final Class<T> loadedClass = (Class<T>) moduleDescriptor.getPlugin().loadClass(name, null); // TODO: null means context classloader?

            // Then instantiate the class, so we can see if there are any dependencies that aren't satisfied
            try
            {
                final Constructor<T> noargConstructor = loadedClass.getConstructor(new Class[] { });
                if (noargConstructor != null)
                {
                    loadedClass.newInstance();
                }
            }
            catch (final NoSuchMethodException e)
            {
                // If there is no "noarg" constructor then don't do the check
            }
            return loadedClass;
        }
        catch (final ClassNotFoundException e)
        {
            throw new PluginParseException("Could not load class: " + name, e);
        }
        catch (final NoClassDefFoundError e)
        {
            throw new PluginParseException("Error retrieving dependency of class: " + name + ". Missing class: " + e.getMessage(), e);
        }
        catch (final UnsupportedClassVersionError e)
        {
            throw new PluginParseException("Class version is incompatible with current JVM: " + name, e);
        }
        catch (final Throwable t)
        {
            throw new PluginParseException(t);
        }
    }
}
