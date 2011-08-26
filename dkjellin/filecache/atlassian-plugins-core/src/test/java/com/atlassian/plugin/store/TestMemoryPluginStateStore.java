/*
 * Created by IntelliJ IDEA.
 * User: Mike
 * Date: Jul 29, 2004
 * Time: 3:47:36 PM
 */
package com.atlassian.plugin.store;

import com.atlassian.plugin.manager.DefaultPluginPersistentState;
import com.atlassian.plugin.manager.PluginPersistentState;
import com.atlassian.plugin.manager.store.MemoryPluginPersistentStateStore;

import junit.framework.TestCase;

public class TestMemoryPluginStateStore extends TestCase
{
    public void testStore()
    {
        final MemoryPluginPersistentStateStore store = new MemoryPluginPersistentStateStore();
        final PluginPersistentState state = new DefaultPluginPersistentState();
        store.save(state);
        assertEquals(state.getMap(), store.load().getMap());
    }
}