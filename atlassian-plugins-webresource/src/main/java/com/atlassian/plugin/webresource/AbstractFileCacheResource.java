package com.atlassian.plugin.webresource;

import com.atlassian.plugin.FileCacheService;
import com.atlassian.plugin.servlet.DownloadException;
import com.atlassian.plugin.servlet.DownloadableResource;
import com.atlassian.plugin.util.PluginUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Provides basic operations for downloadable resources that want to spool to disk.
 * @since 2.9.1
 */
public abstract class AbstractFileCacheResource
{
    private final FileCacheService fileCacheService;

    protected AbstractFileCacheResource(FileCacheService fileCacheService)
    {
        this.fileCacheService = fileCacheService;

    }

    /**
     * Ammends the locale to the hash passed in if file caching is enabled.
     * @param hash has to ammend.
     * @return has on the form hash_locale (for example 123423_en_uk)
     */
    protected String ammendHashWithLocale(final String hash)
    {
        if(isFileCacheEnabled()){
            return hash+"_"+fileCacheService.getStaticResourceLocale();
        }
        return hash;
    }

    /**
     * Returns true if file caching is enabled.
     * @return true if file caching is enabled, otherwise false.
     */
    protected boolean isFileCacheEnabled()
    {
        return fileCacheService!=null && !Boolean.getBoolean(PluginUtils.ATLASSIAN_DEV_MODE);
    }

    /**
     * Returns the stream to serve up the resource using the hash provided. If the fiel does not
     * exist it will spool the contents from the downloadedable resource to disk and then
     * return a stream to the file.
     * @param hash unique hash of the resource.
     * @param type type of content.
     * @param delegate delegate to read from if the file does not exist.
     * @return an open stream to serve the resource. The caller is expected to close the stream.
     * @throws DownloadException if something goes wrong serving up the resource. The cause may give additional
     * information on what went wrong.
     */
    protected InputStream getStream(String hash, String type, DownloadableResource delegate) throws DownloadException
    {
        try
        {
            File input = fileCacheService.getFile(hash);
            if (input == null)
            {

                input = new File(fileCacheService.getTempDir(), hash + "." + type);
                fileCacheService.putFile(hash, input);
            }

            if (!input.exists())
            {

                FileOutputStream fout = null;
                try
                {
                    fout = new FileOutputStream(input);
                    delegate.streamResource(fout);
                    fout.flush();
                }
                catch (IOException e)
                {
                    throw new DownloadException(e);
                }
                finally
                {
                    IOUtils.closeQuietly(fout);
                }
            }

            return new FileInputStream(input);
        }
        catch (IOException e)
        {
            throw new DownloadException(e);
        }

    }

    /**
     * Copy from the supplied OutputStream to the supplied InputStream. Note
     * that the InputStream will be closed on completion.
     *
     * @param in  the stream to read from
     * @param out the stream to write to
     * @throws DownloadException if an IOException is encountered writing to the
     *                           out stream
     */
    protected void streamResource(final InputStream in, final OutputStream out) throws DownloadException
    {
        try
        {
            IOUtils.copy(in, out);
        }
        catch (final IOException e)
        {
            throw new DownloadException(e);
        }
        finally
        {
            IOUtils.closeQuietly(in);
            try
            {
                out.flush();
            }
            catch (final IOException e)
            {
                throw new DownloadException(e);
            }
        }
    }
}
