package com.atlassian.plugin.refimpl.servlet;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.atlassian.plugin.servlet.ContentTypeResolver;

/**
 * A simple content type resolver that can identify css and js resources.
 */
public class SimpleContentTypeResolver implements ContentTypeResolver
{
    private final Map<String, String> mimeTypes;

    public SimpleContentTypeResolver()
    {
        final Map<String, String> types = new HashMap<String, String>();
        types.put(".js", "application/x-javascript");
        types.put(".css", "text/css");
        mimeTypes = Collections.unmodifiableMap(types);
    }

    public String getContentType(final String requestUrl)
    {
        final String extension = requestUrl.substring(requestUrl.lastIndexOf('.'));
        return mimeTypes.get(extension);
    }
}
