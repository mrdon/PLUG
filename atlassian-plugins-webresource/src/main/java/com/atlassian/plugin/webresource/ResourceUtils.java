package com.atlassian.plugin.webresource;

import com.atlassian.util.concurrent.NotNull;
import org.apache.commons.codec.binary.Hex;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

class ResourceUtils
{
    private static final String MD5 = "MD5";
    private static final String UTF8 = "UTF-8";

    /**
     * Determines the type (css/js) of the resource from the given path.
     * @param path - the path to use
     * @return the type of resource
     */
    public static String getType(@NotNull String path)
    {
        int index = path.lastIndexOf('.');
        if (index > -1 && index < path.length())
            return path.substring(index + 1);

        return "";
    }

    /**
     * Extracts the filename out of the path. Will throw a NullPointerException
     * if the path is null. If no file pattern is found it will return empty string.
     * @param path path to look for file names in.
     * @return filename found, or empty string if no filename was found
     * @since 2.9.3
     */
    public static String getFileName(@NotNull String path)
    {
        int index = path.lastIndexOf('/');
        if (index > -1 && index < path.length())
            return path.substring(index + 1);

        return "";
    }

    /**
     * Creates a hash of the keys and versions and returns it as a hex encoded string.
     * The contents of the resources are not considered by this method.
     * @param resources to encode. Can not be null.
     * @return a hex encoded hash of the resources passed in.
     * @since 2.9.3
     */
    public static String createHash(Iterable<WebResourceModuleDescriptor> resources)
    {
        try
        {
            MessageDigest md5 = MessageDigest.getInstance(MD5);
            for (WebResourceModuleDescriptor moduleDescriptor : resources)
            {
                String version = moduleDescriptor.getPlugin().getPluginInformation().getVersion();
                md5.update(moduleDescriptor.getCompleteKey().getBytes(UTF8 ));
                md5.update(version.getBytes(UTF8));
            }

            return new String(Hex.encodeHex(md5.digest()));
        }
        catch (NoSuchAlgorithmException e)
        {
            throw new AssertionError(MD5 + " hashing algorithm is not available.");
        }
        catch (UnsupportedEncodingException e)
        {
            throw new AssertionError(UTF8 + " encoding is not available.");
        }
    }

    /**
     * Creates a hex encoded hash of the string that is passed in.
     * @param input input, can not be null.
     * @return a hex encoded hash of the string passed in.
     * @since 2.9.3
     */
    public static String hash(String input)
    {
        try
        {
            MessageDigest md5 = MessageDigest.getInstance(MD5);
            md5.update(input.getBytes(UTF8));

            return new String(Hex.encodeHex(md5.digest()));
        }
        catch (NoSuchAlgorithmException e)
        {
            throw new AssertionError(MD5 + " hashing algorithm is not available.");
        }
        catch (UnsupportedEncodingException e)
        {
            throw new AssertionError(UTF8 + " encoding is not available.");
        }
    }
}
