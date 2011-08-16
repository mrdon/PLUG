package com.atlassian.plugin;

import com.atlassian.plugin.util.PluginUtils;
import com.atlassian.plugin.webresource.WebResourceIntegration;
import org.apache.commons.collections.map.LRUMap;

import java.io.File;
import java.util.Collections;
import java.util.Map;


/**
 * @since 2.9.3
 */
public class FileCacheServiceImpl implements FileCacheService
{
    private final Map files;
    private final File tempDir;
    private final WebResourceIntegration webResourceIntegration;

    /**
     * Sole constructor.
     * @param webResourceIntegration can not be null.
     */
    public FileCacheServiceImpl(final WebResourceIntegration webResourceIntegration, int cacheSize)
    {
        files = Collections.synchronizedMap(new CustomizedLRUMap(cacheSize));
        this.tempDir = webResourceIntegration.getTemporaryDirectory();
        this.webResourceIntegration = webResourceIntegration;
    }

    public File getTempDir()
    {
        if (!tempDir.exists())
        {
            if (!tempDir.mkdirs())
            {
                //log failure, may just mean it was created between the check and our attempt to creating
            }
        }
        return tempDir;
    }

    public File getFile(String hash)
    {
        File f = (File) files.get(hash);
        if (f != null && !f.exists())
        {
            files.remove(hash);
        }
        return f;
    }

    public void putFile(String hash, File file)
    {
        files.put(hash, file);
    }

    public String getStaticResourceLocale()
    {
        return webResourceIntegration.getStaticResourceLocale();
    }


    private static class CustomizedLRUMap extends LRUMap
    {

        private CustomizedLRUMap(int maxSize)
        {
            super(maxSize);
        }


        /**
         * Overridden to enable deletion of file.
         * This method will always remove the file represented by
         * the entry passed in and return true to let the LRU map remove
         * the entry.
         *
         * @param entry
         * @return
         */
        protected boolean removeLRU(LinkEntry entry)
        {
            Object value = entry.getValue();
            if (value instanceof File)
            {
                File fileToRemove = (File) value;
                if (fileToRemove.exists())
                {
                    if (!fileToRemove.delete())
                    {
                        //log failure to remove
                    }
                }
            }

            return true;
        }
    }
}
