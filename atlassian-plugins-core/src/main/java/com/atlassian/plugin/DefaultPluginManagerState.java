package com.atlassian.plugin;

import com.atlassian.plugin.util.concurrent.CopyOnWriteMap;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a configuration state for plugins and plugin modules. The configuration state (enabled
 * or disabled) is separate from the plugins and modules themselves because a plugin may have multiple
 * states depending on the context.
 * <p/>
 * <p>The state stored in this object represents only the <i>differences</i> between the desired state
 * and the default state configured in the plugin. So if "getPluginState()" or "getPluginModuleState()" return
 * null, then the manager should assume that the default state applies instead.
 * <p>
 * Please note that this method is not threadsafe.  Access to instances should be synchronised.
 */
public class DefaultPluginManagerState implements Serializable, PluginManagerState
{
    private final Map<String, Boolean> map;

    public DefaultPluginManagerState()
    {
        map = CopyOnWriteMap.newHashMap();
    }

    public DefaultPluginManagerState(final Map<String, Boolean> map)
    {
        this.map = CopyOnWriteMap.newHashMap(map);
    }

    public DefaultPluginManagerState(final PluginManagerState state)
    {
        map = CopyOnWriteMap.newHashMap(state.getMap());
    }

    /* (non-Javadoc)
     * @see com.atlassian.plugin.PluginManagerStateAccessor#getState(java.lang.String)
     */
    public Boolean getState(final String key)
    {
        return map.get(key);
    }

    /* (non-Javadoc)
     * @see com.atlassian.plugin.PluginManagerStateAccessor#getMap()
     */
    public Map<String, Boolean> getMap()
    {
        return Collections.unmodifiableMap(map);
    }

    /* (non-Javadoc)
     * @see com.atlassian.plugin.PluginManagerStateAccessor#isEnabled(com.atlassian.plugin.Plugin)
     */
    public boolean isEnabled(final Plugin plugin)
    {
        final Boolean bool = getState(plugin.getKey());
        return (bool == null) ? plugin.isEnabledByDefault() : bool.booleanValue();
    }

    /* (non-Javadoc)
     * @see com.atlassian.plugin.PluginManagerStateAccessor#isEnabled(com.atlassian.plugin.ModuleDescriptor)
     */
    public boolean isEnabled(final ModuleDescriptor<?> pluginModule)
    {
        if (pluginModule == null)
        {
            return false;
        }

        final Boolean bool = getState(pluginModule.getCompleteKey());
        return (bool == null) ? pluginModule.isEnabledByDefault() : bool.booleanValue();
    }

    /**
     * Set a plugins state.
     */
    public void setState(final String key, final Boolean enabled)
    {
        map.put(key, enabled);
    }

    /**
     * reset all plugin's state.
     */
    public void setState(final PluginManagerState state)
    {
        map.clear();
        map.putAll(state.getMap());
    }

    /**
     * Add the plugin state.
     */
    public void addState(final Map<String, Boolean> state)
    {
        map.putAll(state);
    }

    /**
     * Remove a plugin's state.
     */
    public void removeState(final String key)
    {
        map.remove(key);
    }

    /* (non-Javadoc)
     * @see com.atlassian.plugin.PluginManagerStateAccessor#getPluginStateMap(com.atlassian.plugin.Plugin)
     */
    public Map<String, Boolean> getPluginStateMap(final Plugin plugin)
    {
        final Map<String, Boolean> state = new HashMap<String, Boolean>(getMap());
        CollectionUtils.filter(state.keySet(), new StringStartsWith(plugin.getKey()));
        return state;
    }

    private static class StringStartsWith implements Predicate
    {
        private final String prefix;

        public StringStartsWith(final String keyPrefix)
        {
            prefix = keyPrefix;
        }

        public boolean evaluate(final Object object)
        {
            final String str = (String) object;
            return str.startsWith(prefix);
        }
    }
}
