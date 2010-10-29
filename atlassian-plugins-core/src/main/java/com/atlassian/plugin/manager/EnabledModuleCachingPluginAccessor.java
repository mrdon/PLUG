package com.atlassian.plugin.manager;

import static com.google.common.collect.ImmutableList.copyOf;

import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.PluginRestartState;
import com.atlassian.plugin.event.PluginEventManager;
import com.atlassian.plugin.predicate.ModuleDescriptorPredicate;
import com.atlassian.plugin.predicate.PluginPredicate;
import com.atlassian.plugin.tracker.DefaultPluginModuleTracker;
import com.atlassian.plugin.tracker.PluginModuleTracker;

import com.google.common.base.Function;
import com.google.common.collect.MapMaker;

import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

/**
 * A caching decorator which caches {@link #getEnabledModuleDescriptorsByClass(Class)} on {@link com.atlassian.plugin.PluginAccessor} interface.
 *
 * @since 2.7.0
 */
public class EnabledModuleCachingPluginAccessor implements PluginAccessor
{
   private final PluginAccessor delegate;
   private final PluginEventManager pluginEventManager;
   private final ModuleCache cache = new ModuleCache();

   public EnabledModuleCachingPluginAccessor(final PluginAccessor delegate, final PluginEventManager pluginEventManager)
   {
       this.delegate = delegate;
       this.pluginEventManager = pluginEventManager;
   }

   //
   // cached
   //

   public <D extends ModuleDescriptor<?>> List<D> getEnabledModuleDescriptorsByClass(final Class<D> descriptorClazz)
   {
       return copyOf(cache.descriptors(descriptorClazz));
   }

   //
   // simple delegate
   //

   public <M> List<M> getEnabledModulesByClass(final Class<M> moduleClass)
   {
       return delegate.getEnabledModulesByClass(moduleClass);
   }

   public ClassLoader getClassLoader()
   {
       return delegate.getClassLoader();
   }

   public Class<?> getDynamicPluginClass(final String className) throws ClassNotFoundException
   {
       return delegate.getDynamicPluginClass(className);
   }

   public InputStream getDynamicResourceAsStream(final String resourcePath)
   {
       return delegate.getDynamicResourceAsStream(resourcePath);
   }

   public <D extends ModuleDescriptor<?>> List<D> getEnabledModuleDescriptorsByClass(final Class<D> descriptorClazz, final boolean verbose)
   {
       return delegate.getEnabledModuleDescriptorsByClass(descriptorClazz, verbose);
   }

   public <M> List<ModuleDescriptor<M>> getEnabledModuleDescriptorsByType(final String type) throws PluginParseException
   {
       return delegate.getEnabledModuleDescriptorsByType(type);
   }

   public <M> List<M> getEnabledModulesByClassAndDescriptor(final Class<ModuleDescriptor<M>> descriptorClass, final Class<M> moduleClass)
   {
       return delegate.getEnabledModulesByClassAndDescriptor(descriptorClass, moduleClass);
   }

   public <M> List<M> getEnabledModulesByClassAndDescriptor(final Class<ModuleDescriptor<M>>[] descriptorClazz, final Class<M> moduleClass)
   {
       return delegate.getEnabledModulesByClassAndDescriptor(descriptorClazz, moduleClass);
   }

   public Plugin getEnabledPlugin(final String pluginKey) throws IllegalArgumentException
   {
       return delegate.getEnabledPlugin(pluginKey);
   }

   public ModuleDescriptor<?> getEnabledPluginModule(final String completeKey)
   {
       return delegate.getEnabledPluginModule(completeKey);
   }

   public Collection<Plugin> getEnabledPlugins()
   {
       return delegate.getEnabledPlugins();
   }

   public <M> Collection<ModuleDescriptor<M>> getModuleDescriptors(final ModuleDescriptorPredicate<M> moduleDescriptorPredicate)
   {
       return delegate.getModuleDescriptors(moduleDescriptorPredicate);
   }

   public <M> Collection<M> getModules(final ModuleDescriptorPredicate<M> moduleDescriptorPredicate)
   {
       return delegate.getModules(moduleDescriptorPredicate);
   }

   public Plugin getPlugin(final String key) throws IllegalArgumentException
   {
       return delegate.getPlugin(key);
   }

   public ModuleDescriptor<?> getPluginModule(final String completeKey)
   {
       return delegate.getPluginModule(completeKey);
   }

   public InputStream getPluginResourceAsStream(final String pluginKey, final String resourcePath)
   {
       return delegate.getPluginResourceAsStream(pluginKey, resourcePath);
   }

   public PluginRestartState getPluginRestartState(final String key)
   {
       return delegate.getPluginRestartState(key);
   }

   public Collection<Plugin> getPlugins()
   {
       return delegate.getPlugins();
   }

   public Collection<Plugin> getPlugins(final PluginPredicate pluginPredicate)
   {
       return delegate.getPlugins(pluginPredicate);
   }

   public boolean isPluginEnabled(final String key) throws IllegalArgumentException
   {
       return delegate.isPluginEnabled(key);
   }

   public boolean isPluginModuleEnabled(final String completeKey)
   {
       return delegate.isPluginModuleEnabled(completeKey);
   }

   public boolean isSystemPlugin(final String key)
   {
       return delegate.isSystemPlugin(key);
   }

   /**
    * Cache implementation.
    */
   final class ModuleCache
   {
       private final ConcurrentMap<Class<ModuleDescriptor<Object>>, PluginModuleTracker<Object, ModuleDescriptor<Object>>> cache;

       ModuleCache()
       {
           cache = new MapMaker().makeComputingMap(new Function<Class<ModuleDescriptor<Object>>, PluginModuleTracker<Object, ModuleDescriptor<Object>>>()
           {
               public PluginModuleTracker<Object, ModuleDescriptor<Object>> apply(final Class<ModuleDescriptor<Object>> moduleDescriptorClass)
               {
                   // need some generic trickery here as we don't know the specific moduleDescriptor type
                   return DefaultPluginModuleTracker.create(delegate, pluginEventManager, moduleDescriptorClass);
               }
           });
       }

       <D> Iterable<D> descriptors(final Class<D> moduleDescriptorClass)
       {
           @SuppressWarnings("unchecked")
           final Iterable<D> descriptors = (Iterable<D>) cache.get(moduleDescriptorClass).getModuleDescriptors();
           return descriptors;
       }
   }
}