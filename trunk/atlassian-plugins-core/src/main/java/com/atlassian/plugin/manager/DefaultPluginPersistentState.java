package com.atlassian.plugin.manager;

import static com.atlassian.plugin.manager.PluginPersistentState.Util.buildStateKey;
import static java.util.Collections.unmodifiableMap;

import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginRestartState;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Immutable implementation of the {@link PluginPersistentState} interface.
 * <p>
 * The state stored in this object represents only the <i>differences</i> between the desired state
 * and the default state configured in the plugin. So if "getPluginState()" or "getPluginModuleState()" return
 * null, then the manager should assume that the default state applies instead.
 */
public final class DefaultPluginPersistentState implements Serializable, PluginPersistentState
{
    private final Map<String, Boolean> map;

    /**
     * Creates an empty {@link PluginPersistentState}.
     * @deprecated create {@link PluginPersistentState} instances using the 
     * {@link PluginPersistentState.Builder}
     */
    @Deprecated
    public DefaultPluginPersistentState()
    {
        map = Collections.emptyMap();
    }

    /**
     * Creates a {@link PluginPersistentState} with the supplied states.
     * 
     * @param map of the plugin states using the {@link Plugin#getKey()} as the key.
     * @deprecated create {@link PluginPersistentState} instances using the 
     * {@link PluginPersistentState.Builder}
     */
    @Deprecated
    public DefaultPluginPersistentState(final Map<String, Boolean> map)
    {
        this.map = unmodifiableMap(new HashMap<String, Boolean>(map));
    }

    /* for use from within this package, the second parameter is ignored */
    DefaultPluginPersistentState(final Map<String, Boolean> map, final boolean ignore)
    {
        this.map = unmodifiableMap(new HashMap<String, Boolean>(map));
    }

    /**
     * Copy constructor. Doesn't make sense to use as the map is immutable. Just use the 
     * @param state
     */
    @Deprecated
    public DefaultPluginPersistentState(final PluginPersistentState state)
    {
        this(state.getMap());
    }

    /* (non-Javadoc)
     * @see com.atlassian.plugin.PluginPersistentState#getMap()
     */
    public Map<String, Boolean> getMap()
    {
        return Collections.unmodifiableMap(map);
    }

    /* (non-Javadoc)
     * @see com.atlassian.plugin.PluginPersistentState#isEnabled(com.atlassian.plugin.Plugin)
     */
    public boolean isEnabled(final Plugin plugin)
    {
        final Boolean bool = map.get(plugin.getKey());
        return (bool == null) ? plugin.isEnabledByDefault() : bool.booleanValue();
    }

    /* (non-Javadoc)
     * @see com.atlassian.plugin.PluginPersistentState#isEnabled(com.atlassian.plugin.ModuleDescriptor)
     */
    public boolean isEnabled(final ModuleDescriptor<?> pluginModule)
    {
        if (pluginModule == null)
        {
            return false;
        }

        final Boolean bool = map.get(pluginModule.getCompleteKey());
        return (bool == null) ? pluginModule.isEnabledByDefault() : bool.booleanValue();
    }

    /* (non-Javadoc)
     * @see com.atlassian.plugin.PluginPersistentState#getPluginStateMap(com.atlassian.plugin.Plugin)
     */
    public Map<String, Boolean> getPluginStateMap(final Plugin plugin)
    {
        final Map<String, Boolean> state = new HashMap<String, Boolean>(getMap());
        CollectionUtils.filter(state.keySet(), new StringStartsWith(plugin.getKey()));
        return state;
    }

    public PluginRestartState getPluginRestartState(final String pluginKey)
    {
        for (final PluginRestartState state : PluginRestartState.values())
        {
            if (map.containsKey(buildStateKey(pluginKey, state)))
            {
                return state;
            }
        }
        return PluginRestartState.NONE;
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
