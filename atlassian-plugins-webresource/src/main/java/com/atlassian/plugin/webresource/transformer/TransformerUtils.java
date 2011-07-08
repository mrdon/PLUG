package com.atlassian.plugin.webresource.transformer;

import com.atlassian.plugin.servlet.DownloadException;
import com.atlassian.plugin.servlet.DownloadableResource;
import com.google.common.base.Function;
import org.apache.commons.io.IOUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;

/**
 * Utility class for transforming resources
 */
public class TransformerUtils
{
    /**
     * Write apply a given transform a resource and then write the transformed content
     * to the supplied OutputStream.
     * Note that the OutputStream will not be closed by this method.
     * @param originalResource - the resource to transform
     * @param encoding - the encoding to use for writing
     * @param out - the output stream
     * @param transform - a function for transforming the content
     * @throws DownloadException - thrown if it is not possible to stream the output
     * @since 2.9.0
     */
    public static void transformAndStreamResource(DownloadableResource originalResource, String encoding, OutputStream out, Function<String, String> transform) throws DownloadException
    {
        final ByteArrayOutputStream delegateOut = new ByteArrayOutputStream();
        originalResource.streamResource(delegateOut);

        try
        {
            final String originalContent = new String(delegateOut.toByteArray(), encoding);
            IOUtils.copy(new StringReader(transform.apply(originalContent)), out, encoding);
        }
        catch (final IOException e)
        {
            throw new DownloadException("Unable to stream to the output", e);
        }
    }
}
