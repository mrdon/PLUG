package com.atlassian.plugin;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Simple thread-safe state machine that prevents illegal transitions.
 * <p>
 * Could be easily extended to allow custom workflows by interfacing 
 * out the State type and having this class generified with that.
 */
class StateTracker
{
    enum State
    {
        NOT_STARTED, STARTING, STARTED, SHUTTING_DOWN, SHUTDOWN
        {
            @Override
            void check(final State newState)
            {
                if (newState != STARTING)
                {
                    illegalState(newState);
                }
            }
        };

        void check(final State newState)
        {
            if ((ordinal() + 1) != newState.ordinal())
            {
                illegalState(newState);
            }
        }

        void illegalState(final State newState)
        {
            throw new IllegalStateException("Cannot go from State: " + this + " to: " + newState);
        }
    }

    private final AtomicReference<State> state = new AtomicReference<State>(State.NOT_STARTED);

    public State get()
    {
        return state.get();
    }

    StateTracker setState(final State newState)
    {
        for (;;)
        {
            final State oldState = get();
            oldState.check(newState);
            if (state.compareAndSet(oldState, newState))
            {
                return this;
            }
        }
    }

    @Override
    public String toString()
    {
        return get().toString();
    }
}
