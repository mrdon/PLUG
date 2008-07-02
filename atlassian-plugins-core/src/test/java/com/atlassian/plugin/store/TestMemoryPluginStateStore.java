/*
 * Created by IntelliJ IDEA.
 * User: Mike
 * Date: Jul 29, 2004
 * Time: 3:47:36 PM
 */
package com.atlassian.plugin.store;

import junit.framework.TestCase;
import com.atlassian.plugin.PluginManagerState;

public class TestMemoryPluginStateStore extends TestCase
{
    public void testStore()
    {
        MemoryPluginStateStore store = new MemoryPluginStateStore();
        PluginManagerState state = new PluginManagerState();
        store.savePluginState(state);
        assertEquals(state, store.loadPluginState());
    }
}