package com.atlassian.plugin.descriptors;

import java.util.Map;

/**
 * An interface to be implemented by plugins which need to pass specific parameters to resources.
 */
public interface ResourceParameterGenerator
{
    /**
     * Generate a map of parameters which will be passed to all resources for this plugin.
     */ 
    Map generateParameters();
}
