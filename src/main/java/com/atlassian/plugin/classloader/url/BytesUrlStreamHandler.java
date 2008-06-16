package com.atlassian.plugin.classloader.url;

import java.net.URLConnection;
import java.net.URL;
import java.net.URLStreamHandler;

/**
 * Created by IntelliJ IDEA.
 * User: sleberrigaud
 * Date: May 9, 2008
 * Time: 11:06:09 AM
 * To change this template use File | Settings | File Templates.
 */
public class BytesUrlStreamHandler extends URLStreamHandler
{
    private final byte[] content;

    public BytesUrlStreamHandler(byte[] content)
    {
        this.content = content;
    }

    public URLConnection openConnection(URL url)
    {
        return new BytesUrlConnection(url, content);
    }
}
