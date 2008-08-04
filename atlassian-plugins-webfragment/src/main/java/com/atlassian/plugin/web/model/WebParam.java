package com.atlassian.plugin.web.model;

import com.atlassian.plugin.web.descriptors.WebFragmentModuleDescriptor;

import java.util.SortedMap;
import java.util.Map;

/**
 * Represents arbitrary number of key/value pairs
 */
public interface WebParam
{
    SortedMap<String,String> getParams();

    Object get(String key);

    String getRenderedParam(String paramKey, Map<String,Object> context);

    WebFragmentModuleDescriptor getDescriptor();
}
