package com.atlassian.plugin.web.descriptors;

import com.atlassian.plugin.web.model.WebPanel;

import java.util.Map;

/**
 * A test WebPanel that outputs the values passed in the context, or a filler
 * message if the context is empty.
 */
public class MockWebPanel implements WebPanel
{
    static final String NOTHING_IN_CONTEXT = "nothing in context";

    public String getHtml(Map<String, Object> context)
    {
        StringBuffer sb = new StringBuffer();
        if (!context.isEmpty())
        {
            for (Object value : context.values())
            {
                sb.append(value.toString());
            }
        }
        else
        {
            sb.append(NOTHING_IN_CONTEXT);
        }
        return sb.toString();
    }
}
