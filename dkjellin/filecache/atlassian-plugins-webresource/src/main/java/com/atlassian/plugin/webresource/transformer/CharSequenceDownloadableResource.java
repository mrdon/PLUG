package com.atlassian.plugin.webresource.transformer;

import static com.atlassian.plugin.webresource.transformer.TransformerUtils.UTF8;
import static com.atlassian.plugin.webresource.transformer.TransformerUtils.transformAndStreamResource;

import com.atlassian.plugin.servlet.DownloadException;
import com.atlassian.plugin.servlet.DownloadableResource;

import com.google.common.base.Function;

import java.io.OutputStream;
import java.nio.charset.Charset;

/**
 * Abstract class that makes it easy to create transforms that go from string to string.  Override
 * {@link #getEncoding()} to customize the character encoding of the underlying content and transformed content.
 * <p>
 * For example, here is a minimal transformer that prepends text to the underlying resource:
 * <pre>
 * public class PrefixTransformer implements WebResourceTransformer
 *   {
 *       public DownloadableResource transform(Element configElement, ResourceLocation location, String filePath, DownloadableResource nextResource)
 *       {
 *           return new CharSequenceDownloadableResource(nextResource)
 *           {
 *               protected CharSequence transform(CharSequence originalContent)
 *               {
 *                   return "Prefix: "  + originalContent;
 *               }
 *           };
 *       }
 *    }
 * </pre>
 *
 * @since 2.9.0
 */
public abstract class CharSequenceDownloadableResource extends AbstractTransformedDownloadableResource
{
    protected CharSequenceDownloadableResource(final DownloadableResource originalResource)
    {
        super(originalResource);
    }

    public void streamResource(final OutputStream out) throws DownloadException
    {
        transformAndStreamResource(getOriginalResource(), UTF8, out, new Function<CharSequence, CharSequence>()
        {
            public CharSequence apply(final CharSequence originalContent)
            {
                return transform(originalContent);
            }
        });
    }

    protected Charset encoding()
    {
        return UTF8;
    }

    /**
     * Override this method to transform the original content into a new format.
     *
     * @param original The content from the original resource.
     * @return transformed content
     */
    protected abstract CharSequence transform(CharSequence original);
}
