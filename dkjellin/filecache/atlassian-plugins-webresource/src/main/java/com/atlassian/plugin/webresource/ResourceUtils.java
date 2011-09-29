package com.atlassian.plugin.webresource;

import com.atlassian.plugin.cache.filecache.FileCacheKey;
import com.atlassian.util.concurrent.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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
     * @since 2.10.0
     */
    public static FileCacheKey buildCacheKey(@NotNull String path, @NotNull Map<String, String> params)
    {


        List<ParamEntry> sortedParams = new ArrayList<ParamEntry>(params.size());
        for(Map.Entry<String,String> entry: params.entrySet())
        {
            sortedParams.add(new ParamEntry(entry.getKey(),entry.getValue()));
        }
        Collections.sort(sortedParams, new Comparator<ParamEntry>()
        {
            public int compare(ParamEntry o1, ParamEntry o2)
            {
                if (o1.getKey().equals(o2.getKey()))
                {
                    return o1.getValue().compareTo(o2.getValue());
                }
                return o1.getKey().compareTo(o2.getKey());
            }
        });

        return new UrlBasedFileCacheKey(path,sortedParams);
    }

    private static class UrlBasedFileCacheKey implements FileCacheKey
    {
        private final String url;
        private final List<ParamEntry> sortedArguments;

        private UrlBasedFileCacheKey(@NotNull String url, @NotNull List<ParamEntry> sortedArguments)
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

    private static class ParamEntry
    {
        private final String key;
        private final String value;

        private ParamEntry(@NotNull String key, @NotNull String value)
        {
            this.key = key;
            this.value = value;
        }

        public String getKey()
        {
            return key;
        }

        public String getValue()
        {
            return value;
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ParamEntry that = (ParamEntry) o;

            if (!key.equals(that.key)) return false;
            if (!value.equals(that.value)) return false;

            return true;
        }

        @Override
        public int hashCode()
        {
            int result = key.hashCode();
            result = 31 * result + value.hashCode();
            return result;
        }
    }
}
