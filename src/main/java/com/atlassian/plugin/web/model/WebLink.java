package com.atlassian.plugin.web.model;

import com.atlassian.plugin.web.descriptors.WebFragmentModuleDescriptor;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

public interface WebLink
{
    String getRenderedUrl(Map context);

    String getDisplayableUrl(HttpServletRequest req, Map context);

    boolean hasAccessKey();

    String getAccessKey(Map context);

    String getId();

    WebFragmentModuleDescriptor getDescriptor();
}
