package com.atlassian.plugin.webresource.transformer;

import com.atlassian.plugin.servlet.DownloadException;
import com.atlassian.plugin.servlet.DownloadableResource;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.CharSequenceReader;

import com.google.common.base.Function;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;

/**
 * Utility class for transforming resources
 */
public class TransformerUtils
{
    public static final Charset UTF8 = Charset.forName("UTF-8");

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
    public static void transformAndStreamResource(final DownloadableResource originalResource, final Charset encoding, final OutputStream out, final Function<CharSequence, CharSequence> transform) throws DownloadException
    {
        try
        {
            final StringBuilder originalContent = new StringBuilder();
            final WriterOutputStream output = new WriterOutputStream(new AppendableWriter(originalContent), encoding);
            originalResource.streamResource(output);
            output.flush();
            IOUtils.copy(new CharSequenceReader(transform.apply(originalContent)), out, encoding.name());
        }
        catch (final IOException e)
        {
            throw new DownloadException("Unable to stream to the output", e);
        }
    }
}
