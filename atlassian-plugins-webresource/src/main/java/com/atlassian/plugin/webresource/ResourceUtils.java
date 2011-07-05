package com.atlassian.plugin.webresource;

import com.atlassian.util.concurrent.NotNull;

class ResourceUtils
{
    /**
     * Determines the type (css/js) of the resource from the given path.
     * @param path - the path to use
     * @return the type of resource
     */
    public static String getType(@NotNull String path)
    {
        int index = path.lastIndexOf('.');
        if (index > -1 && index < path.length())
            return path.substring(index + 1);

        return "";
    }
}
