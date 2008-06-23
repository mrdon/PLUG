package com.atlassian.plugin.classloader.url;

import java.net.URL;
import java.net.URLConnection;
import java.io.InputStream;
import java.io.ByteArrayInputStream;

/**
 * Created by IntelliJ IDEA.
 * User: sleberrigaud
 * Date: May 9, 2008
 * Time: 11:06:59 AM
 * To change this template use File | Settings | File Templates.
 */
public class BytesUrlConnection extends URLConnection
{
    private final byte[] content;

    public BytesUrlConnection(URL url, byte[] content)
    {
        super(url);
        this.content = content;
    }

    public void connect()
    {
    }

    public InputStream getInputStream()
    {
        return new ByteArrayInputStream(content);
    }
}
