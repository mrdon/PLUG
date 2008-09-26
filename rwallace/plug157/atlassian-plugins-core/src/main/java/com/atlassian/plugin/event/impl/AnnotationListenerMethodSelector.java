package com.atlassian.plugin.event.impl;

import com.atlassian.plugin.event.PluginEventListener;

import java.lang.reflect.Method;
import java.lang.annotation.Annotation;

/**
 * Listener method selector that looks for a specific marker annotation
 */
public class AnnotationListenerMethodSelector implements ListenerMethodSelector
{
    private final Class<? extends Annotation> markerAnnotation;

    public AnnotationListenerMethodSelector()
    {
        this(PluginEventListener.class);
    }

    public AnnotationListenerMethodSelector(Class<? extends Annotation> markerAnnotation)
    {
        this.markerAnnotation = markerAnnotation;
    }
    public boolean isListenerMethod(Method method)
    {
        return (method.getAnnotation(markerAnnotation) != null);
    }
}
