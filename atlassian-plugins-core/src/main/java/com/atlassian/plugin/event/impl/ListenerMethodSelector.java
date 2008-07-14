package com.atlassian.plugin.event.impl;

import java.lang.reflect.Method;

/**
 * Determines if a method on a listener is a listener method or not
 */
public interface ListenerMethodSelector
{
    /**
     * @param method The possible listener method
     * @return True if this is a listener method
     */
    boolean isListenerMethod(Method method);
}
