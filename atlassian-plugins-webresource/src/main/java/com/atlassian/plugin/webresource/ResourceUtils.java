package com.atlassian.plugin.webresource;

class ResourceUtils
{
    /**
     * Determines the type (css/js) of the resource from the given path.
     */
    public static String getType(String path)
    {
        int index = path.lastIndexOf('.');
        if (index > -1 && index < path.length())
            return path.substring(index + 1);

        return "";
    }
}
