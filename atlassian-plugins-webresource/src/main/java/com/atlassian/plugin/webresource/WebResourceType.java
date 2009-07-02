package com.atlassian.plugin.webresource;

import java.util.List;
import java.util.ArrayList;

public enum WebResourceType
{
    CSS("css"), JAVASCRIPT("js");

    private final String key;

    private WebResourceType(String key)
    {
        this.key = key;
    }

    private static List<WebResourceType> types = new ArrayList<WebResourceType>(2);

    static {
        types.add(CSS);
        types.add(JAVASCRIPT);
    }

    public static WebResourceType parse(String key)
    {
        if (key == null)
            return null;

        for (WebResourceType type : types)
        {
            if (key.equalsIgnoreCase(type.key))
                return type;
        }

        throw new IllegalArgumentException("No Resource Type for string: " + key);
    }
}
