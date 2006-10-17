package com.atlassian.plugin.webresource;

import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.servlet.BaseFileServerServlet;
import com.atlassian.plugin.elements.ResourceDescriptor;

public abstract class AbstractStaticResourceLink implements ResourceLink
{
    /**
     * Return a resource link of the form:
     * <pre>
     * <code>/download/static/{build num}/{plugin version}/{system date}/pluginKey/{plugin key}:{module key}/{resource name}</code>
     * </pre>
     */
    public String getLinkToResource(ModuleDescriptor moduleDescriptor, ResourceDescriptor resourceDescriptor)
    {
         return "/" + BaseFileServerServlet.SERVLET_PATH + "/static/" + getSystemBuildNumber() + "/" +
                 moduleDescriptor.getPlugin().getPluginInformation().getVersion() + "/" + getCacheFlushDate()
                 + "/pluginkey/" + moduleDescriptor.getCompleteKey() + "/" + resourceDescriptor.getName();
    }

    /**
     * Represents the last date at which the cache should be flushed.  For most 'system-wide' resources,
     * this should be a date stored in the global application-properties.  For other resources (such as 'header' files
     * which contain colour changes), this should be the date of the last update.
     *
     * @return A string representing the date, which can be of format "yyMMddHHmmssZ".
     */
    protected abstract String getCacheFlushDate();

    /**
     * Represents the last time the system was updated.  This is generally obtained from BuildUtils or similar.
     */
    protected abstract String getSystemBuildNumber();
}
