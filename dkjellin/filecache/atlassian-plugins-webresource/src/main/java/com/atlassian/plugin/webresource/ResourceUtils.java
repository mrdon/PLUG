package com.atlassian.plugin.webresource;

import com.atlassian.util.concurrent.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
     * This method will provide a deterministic url even for varying order in the parameter map.
     * @param path leading path to use in the cache key. Can not be null.
     * @param params parameters, these will be appended in a deterministic order without any added characters. Can not be null
     * @return a string that can be used as a cache key. This method will never return null.
     * @since 2.10.0
     */
    public static String buildCacheKey(@NotNull String path, @NotNull Map<String, String> params)
    {

        /**
         * This looks a bit overly complex, but performance testing shows this is more efficent than using a
         * plain old Stringbuilder. Essentially what we do is sort the param map, we then
         * add them to the list of parts, and counting their length.
         * We can now allocate a stringbuilder that is large enough for us and just append the parts to it.
         * Calling this in a tight loop, 10 000 000 times gave for this implementation the following results
         * Empty map: 1976
         * One param map: 5105
         * The same input but using just a stringbuilder (still sorting the params map of course)
         * Empty map: 6673
         * One param map: 8872
         */
        List<String> parts = new ArrayList<String>(1 + params.size() * 2);
        List<Map.Entry<String, String>> sortedParams = new ArrayList<Map.Entry<String, String>>(params.entrySet());
        Collections.sort(sortedParams, new Comparator<Map.Entry<String, String>>()
        {
            public int compare(Map.Entry<String, String> o1, Map.Entry<String, String> o2)
            {
                if (o1.getKey().equals(o2.getKey()))
                {
                    return o1.getValue().compareTo(o2.getValue());
                }
                return o1.getKey().compareTo(o2.getKey());
            }
        });
        int totalLength = path.length();
        parts.add(path);

        for (Map.Entry<String, String> entry : sortedParams)
        {
            parts.add(entry.getKey());
            parts.add(entry.getValue());
            totalLength += entry.getKey().length();
            totalLength += entry.getValue().length();
        }
        StringBuilder sb = new StringBuilder(totalLength); //right size, no growth and array copying needed.
        for (String string : parts)
        {
            sb.append(string);
        }
        return sb.toString();
    }
}
