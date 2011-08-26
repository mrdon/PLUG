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
 *           return new AbstractStringTransformedDownloadableResource(nextResource)
 *           {
 *               protected String transform(String originalContent)
 *               {
 *                   return "Prefix: "  + originalContent;
 *               }
 *           };
 *       }
 *    }
 * </pre>
 *
 * @since 2.5.0
 * @deprecated since 2.9.0 use {@link CharSequenceDownloadableResource} instead
 */
@Deprecated
public abstract class AbstractStringTransformedDownloadableResource extends AbstractTransformedDownloadableResource
{
    private final Function<CharSequence, CharSequence> transformer = new Function<CharSequence, CharSequence>()
    {
        public CharSequence apply(final CharSequence originalContent)
        {
            return transform(originalContent.toString());
        }
    };

    public AbstractStringTransformedDownloadableResource(final DownloadableResource originalResource)
    {
        super(originalResource);
    }

    public void streamResource(final OutputStream out) throws DownloadException
    {
        transformAndStreamResource(getOriginalResource(), Charset.forName(getEncoding()), out, transformer);
    }

    /**
     * @return the encoding used to read the original resource and encode the transformed string
     */
    protected String getEncoding()
    {
        return UTF8.name();
    }

    /**
     * Override this method to transform the original content into a new format.
     *
     * @param originalContent The original content from the original downloadable resource.
     * @return The transformed content you want returned
     */
    protected abstract String transform(String originalContent);
}
