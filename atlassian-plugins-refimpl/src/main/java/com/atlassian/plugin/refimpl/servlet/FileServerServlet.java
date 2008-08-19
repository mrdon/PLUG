package com.atlassian.plugin.refimpl.servlet;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.codec.net.URLCodec;

import com.atlassian.plugin.refimpl.ContainerManager;
import com.atlassian.plugin.servlet.BaseFileServerServlet;
import com.atlassian.plugin.servlet.DownloadStrategy;
import com.atlassian.plugin.servlet.PluginResourceDownload;

public class FileServerServlet extends BaseFileServerServlet
{
    
    private static final Map<String, String> mimeTypes;
    
    static {
        Map<String, String> types = new HashMap<String, String>();
        types.put("js", "application/x-javascript");
        types.put("css", "text/css");
        mimeTypes = Collections.unmodifiableMap(types);
    }

    @Override
    protected String getContentType(String location)
    {
        String extension = location.substring(location.lastIndexOf('.'));
        return mimeTypes.get(extension);
    }

    @Override
    public String getDecodedPathInfo(HttpServletRequest httpServletRequest)
    {
        return urlDecode(httpServletRequest.getPathInfo());
    }

    @Override
    protected DownloadStrategy instantiateDownloadStrategy(Class downloadStrategyClass)
    {
        try
        {
            DownloadStrategy strategy = (DownloadStrategy) downloadStrategyClass.newInstance();
            if (strategy instanceof PluginResourceDownload) 
                ((PluginResourceDownload) strategy).setPluginManager(ContainerManager.getInstance().getPluginManager());
            return strategy;
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected String urlDecode(String url)
    {
        if (url == null)
        {
            return null;
        }

        try
        {
            URLCodec codec = new URLCodec();
            return codec.decode(url);
        }
        catch (Exception e)
        {
            return url;
        }
    }

}
