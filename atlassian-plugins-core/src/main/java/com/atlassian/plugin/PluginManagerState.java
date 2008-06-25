package com.atlassian.plugin;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;

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
public class PluginManagerState
{
    private Map map = new HashMap();

    public PluginManagerState()
    {

    }

    public PluginManagerState(Map map)
    {
        this.map = map;
    }

    /**
     * Get the state of a given plugin.
     */
    public Boolean getState(String key)
    {
        return (Boolean) map.get(key);
    }

    /**
     * Get the map of all states.
     */
    public Map getMap()
    {
        return map;
    }

    /**
     * Whether or not a plugin is enabled, calculated from it's current state AND default state.
     */
    public boolean isEnabled(Plugin plugin)
    {
        Boolean bool = getState(plugin.getKey());
        return (bool == null) ? plugin.isEnabledByDefault() : bool.booleanValue();
    }

    /**
     * Whether or not a given plugin module is enabled in this state, calculated from it's current state AND default state.
     */
    public boolean isEnabled(ModuleDescriptor pluginModule)
    {
        if (pluginModule == null)
            return false;
        
        Boolean bool = getState(pluginModule.getCompleteKey());
        return (bool == null) ? pluginModule.isEnabledByDefault() : bool.booleanValue();
    }

    /**
     * Set a plugins state.
     */
    public void setState(String key, Boolean enabled)
    {
        map.put(key, enabled);
    }

    /**
     * Remove a plugin's state.
     */
    public void removeState(String key)
    {
        map.remove(key);
    }

    public Map getPluginStateMap(final Plugin plugin)
    {
        Map state = new HashMap(getMap());
        CollectionUtils.filter(state.keySet(), new StringStartsWith(plugin.getKey()));
        return state;
    }

    private static class StringStartsWith implements Predicate
    {
        private final String prefix;

        public StringStartsWith(String keyPrefix)
        {
            this.prefix = keyPrefix;
        }

        public boolean evaluate(Object object)
        {
            String str = (String) object;
            return str.startsWith(prefix);
        }
    }
}
