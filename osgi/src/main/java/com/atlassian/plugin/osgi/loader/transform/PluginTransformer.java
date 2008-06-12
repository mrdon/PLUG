package com.atlassian.plugin.osgi.loader.transform;

import com.atlassian.plugin.classloader.PluginClassLoader;
import com.atlassian.plugin.PluginParseException;

import java.io.File;
import java.io.IOException;

public interface PluginTransformer
{
    File transform(PluginClassLoader loader, File pluginJar) throws IOException, PluginParseException;
}
