package com.atlassian.plugin.web.descriptors;

import java.util.Map;

public interface WebPanel {

    String getHtml(Map<String, Object> context);
}
