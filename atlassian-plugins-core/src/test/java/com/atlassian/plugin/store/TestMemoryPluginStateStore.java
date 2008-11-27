/*
 * Created by IntelliJ IDEA.
 * User: Mike
 * Date: Jul 29, 2004
 * Time: 3:47:36 PM
 */
package com.atlassian.plugin.store;

import com.atlassian.plugin.PluginManagerState;

import junit.framework.TestCase;

public class TestMemoryPluginStateStore extends TestCase
{
    public void testStore()
    {
        final MemoryPluginStateStore store = new MemoryPluginStateStore();
        final PluginManagerState state = new PluginManagerState();
        store.savePluginState(state);
        assertEquals(state.getMap(), store.loadPluginState().getMap());
    }
}