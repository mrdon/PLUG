package com.atlassian.plugin.webresource.batch;

import static com.atlassian.plugin.servlet.AbstractFileServerServlet.PATH_SEPARATOR;
import com.atlassian.plugin.servlet.AbstractFileServerServlet;

import java.util.Map;
import java.util.TreeMap;

public class BatchResource
{
    public static final String URL_PREFIX = PATH_SEPARATOR + AbstractFileServerServlet.SERVLET_PATH + PATH_SEPARATOR + "batch";

    private String type;
    private String moduleCompleteKey;
    private Map<String, String> params;

    public BatchResource(String type, String moduleCompleteKey, Map<String, String> params)
    {
        this.type = type;
        this.moduleCompleteKey = moduleCompleteKey;
        this.params = params;
    }

    public String getType()
    {
        return type;
    }

    public String getModuleCompleteKey()
    {
        return moduleCompleteKey;
    }

    public Map<String, String> getParams()
    {
        return params;
    }

    public String getUrl()
    {
        // e.g. /download/batch/css/example.plugin:webresources/all.css?ie=true
        StringBuffer sb = new StringBuffer();
        sb.append(URL_PREFIX).append(PATH_SEPARATOR)
            .append(type).append(PATH_SEPARATOR)
            .append(moduleCompleteKey).append(PATH_SEPARATOR)
            .append("all.").append(type);

        if(params.size() > 0 )
        {
            sb.append("?");
            int count = 0;
            
            for (Map.Entry<String, String> entry: params.entrySet())
            {
                sb.append(entry.getKey()).append("=").append(entry.getValue());

                if(++count < params.size())
                    sb.append("&");
            }
        }

        return sb.toString();
    }

    public static BatchResource parse(String url)
    {
        int startIndex = url.indexOf(URL_PREFIX) + URL_PREFIX.length() + 1;
        int queryIndex = url.indexOf("?");

        String typeAndModuleKey;
        if(queryIndex == -1)
            typeAndModuleKey = url.substring(startIndex);
        else
            typeAndModuleKey = url.substring(startIndex, queryIndex);

        String[] parts = typeAndModuleKey.split("/");
        if (parts.length < 2)
            return null;

        String type = parts[0];
        String moduleKey = parts[1];
        Map<String, String> params = new TreeMap<String, String>();

        String queryParams = url.substring(queryIndex + 1);
        if(queryIndex != -1)
        {
            String[] queryParts = queryParams.split("&");
            for(String queryPart : queryParts)
            {
                String[] s = queryPart.split("=", 2);
                if(s.length == 1)
                {
                    params.put(s[0], "");
                }
                else if(s.length == 2)
                {
                    params.put(s[0], s[1]);
                }
            }
        }
        return new BatchResource(type, moduleKey, params);
    }

    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BatchResource that = (BatchResource) o;

        if (moduleCompleteKey != null ? !moduleCompleteKey.equals(that.moduleCompleteKey) : that.moduleCompleteKey != null)
            return false;
        if (params != null ? !params.equals(that.params) : that.params != null) return false;
        if (type != null ? !type.equals(that.type) : that.type != null) return false;

        return true;
    }

    public int hashCode()
    {
        int result;
        result = (type != null ? type.hashCode() : 0);
        result = 31 * result + (moduleCompleteKey != null ? moduleCompleteKey.hashCode() : 0);
        result = 31 * result + (params != null ? params.hashCode() : 0);
        return result;
    }

    public String toString()
    {
        return "[moduleCompleteKey=" + moduleCompleteKey + ", type=" + type + ", params=" + params + "]";
    }
}
