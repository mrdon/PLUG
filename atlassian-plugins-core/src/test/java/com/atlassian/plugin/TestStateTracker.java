package com.atlassian.plugin;

import static com.atlassian.plugin.StateTracker.State.NOT_STARTED;
import static com.atlassian.plugin.StateTracker.State.SHUTDOWN;
import static com.atlassian.plugin.StateTracker.State.SHUTTING_DOWN;
import static com.atlassian.plugin.StateTracker.State.STARTED;
import static com.atlassian.plugin.StateTracker.State.STARTING;

import com.atlassian.plugin.StateTracker.State;

import junit.framework.TestCase;

public class TestStateTracker extends TestCase
{
    public void testStandardTransitions() throws Exception
    {
        new StateTracker().setState(STARTING).setState(STARTED).setState(SHUTTING_DOWN).setState(SHUTDOWN);
    }

    public void testIllegalNotStartedTransitions() throws Exception
    {
        assertIllegalState(new StateTracker(), NOT_STARTED, STARTED, SHUTTING_DOWN, SHUTDOWN);
    }

    public void testIllegalStartingTransitions() throws Exception
    {
        assertIllegalState(new StateTracker().setState(STARTING), NOT_STARTED, STARTING, SHUTTING_DOWN, SHUTDOWN);
    }

    public void testIllegalStartedTransitions() throws Exception
    {
        assertIllegalState(new StateTracker().setState(STARTING).setState(STARTED), STARTED, NOT_STARTED, STARTING, SHUTDOWN);
    }

    public void testIllegalShuttingDownTransitions() throws Exception
    {
        assertIllegalState(new StateTracker().setState(STARTING).setState(STARTED).setState(SHUTTING_DOWN), NOT_STARTED, STARTING, STARTED,
            SHUTTING_DOWN);
    }

    public void testIllegalShutdownTransitions() throws Exception
    {
        assertIllegalState(new StateTracker().setState(STARTING).setState(STARTED).setState(SHUTTING_DOWN).setState(SHUTDOWN), NOT_STARTED, STARTED,
            SHUTTING_DOWN, SHUTDOWN);
    }

    void assertIllegalState(final StateTracker tracker, final State... states)
    {
        for (final State state : states)
        {
            try
            {
                final State oldState = tracker.get();
                tracker.setState(state);
                fail(oldState + " should not be allowed to transition to: " + state);
            }
            catch (final IllegalStateException expected)
            {}
        }
    }
}
