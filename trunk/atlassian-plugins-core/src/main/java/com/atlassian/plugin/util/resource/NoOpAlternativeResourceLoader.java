package com.atlassian.plugin.util.resource;

import java.net.URL;
import java.io.InputStream;

/**
 * Resource loader that always returns null
 *
 * @since 2.2.0
 */
public class NoOpAlternativeResourceLoader implements AlternativeResourceLoader
{
    public URL getResource(String path)
    {
        return null;
    }

    public InputStream getResourceAsStream(String name)
    {
        return null;
    }
}
