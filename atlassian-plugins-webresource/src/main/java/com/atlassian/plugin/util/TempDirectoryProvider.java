package com.atlassian.plugin.util;

public interface TempDirectoryProvider
{
    /**
     * Returns a an absolute path to the temporary directory the implementer wishes the plugin framework to use.
     * @return a string representing a directory to use for temporary files.
     */
    String getTemporaryPath();
}
