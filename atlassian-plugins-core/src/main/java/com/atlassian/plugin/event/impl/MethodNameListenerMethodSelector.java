package com.atlassian.plugin.event.impl;

import java.lang.reflect.Method;

/**
 * Listener method selector that makes its determination by matching the method name
 */
public class MethodNameListenerMethodSelector implements ListenerMethodSelector
{
    private final String methodName;

    public MethodNameListenerMethodSelector()
    {
        this("channel");
    }

    public MethodNameListenerMethodSelector(String s)
    {
        this.methodName = s;
    }


    public boolean isListenerMethod(Method method)
    {
        return methodName.equals(method.getName());
    }
}
