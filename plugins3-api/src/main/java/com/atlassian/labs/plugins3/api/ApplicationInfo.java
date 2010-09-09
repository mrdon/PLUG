package com.atlassian.labs.plugins3.api;

/**
 *
 */
public interface ApplicationInfo
{
    static final String FISHEYE = "fisheye";
    static final String FECRU = "fecru";
    static final String CONFLUENCE = "confluence";
    static final String JIRA = "jira";
    static final String BAMBOO = "bamboo";
    static final String CROWD = "crowd";
    static final String REFAPP = "refapp";

    String getVersion();
    long getBuildNumber();
    String getType();
}
