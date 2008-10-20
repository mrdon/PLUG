package com.atlassian.plugin.resourcedownload;

import java.util.Map;

public interface Resource
{
    String getResourceName();

    String getModuleCompleteKey();

    Map<String, String> getParams();

    String getUrl();
}
