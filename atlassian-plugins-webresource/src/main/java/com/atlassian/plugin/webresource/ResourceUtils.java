package com.atlassian.plugin.webresource;

import com.atlassian.plugin.cache.filecache.FileCacheKey;
import com.atlassian.util.concurrent.NotNull;

import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

class ResourceUtils
{
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
     * Builds a cache key based on the input. Input is typically a url and a map of request parameters.
     * This method will provide a deterministic key even for varying order in the parameter map.
     * @param path leading path to use in the cache key. Can not be null.
     * @param params parameters, these will be appended in a deterministic order without any added characters. Can not be null
     * @return a FileCacheKey that can be used as a unique cache key. This method will never return null.
     * @since 2.11.0
     */
    public static FileCacheKey buildCacheKey(@NotNull String path, @NotNull Map<String, String> params)
    {
        if(path == null)
        {
            throw new NullPointerException("Path can not be null");
        }
        if(params == null)
        {
            throw new NullPointerException("Params can not be null");
        }
        SortedMap<String, String> sortedParams = new TreeMap<String, String>(params);
        return new UrlBasedFileCacheKey(path,sortedParams);
    }

    private static class UrlBasedFileCacheKey implements FileCacheKey
    {
        private final String url;
        private final SortedMap<String,String> sortedArguments;

        private UrlBasedFileCacheKey(@NotNull String url, @NotNull SortedMap<String,String> sortedArguments)
        {
            this.sortedArguments = sortedArguments;
            this.url = url;
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            UrlBasedFileCacheKey that = (UrlBasedFileCacheKey) o;

            if (!sortedArguments.equals(that.sortedArguments)) return false;
            if (!url.equals(that.url)) return false;

            return true;
        }

        @Override
        public int hashCode()
        {
            int result = url.hashCode();
            result = 31 * result + sortedArguments.hashCode();
            return result;
        }

        @Override
        public String toString()
        {
            return "UrlBasedFileCacheKey{" +
                    "sortedArguments=" + sortedArguments +
                    ", url='" + url + '\'' +
                    '}';
        }
    }
}
